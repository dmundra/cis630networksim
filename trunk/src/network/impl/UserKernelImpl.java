package network.impl;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.AbstractKernel;
import network.Interface;
import network.KnownPort;
import network.Message;
import network.OperatingSystem;
import network.Process;
import network.UserKernel;
import network.protocols.RIP;

public class UserKernelImpl extends AbstractKernel implements UserKernel {
    private volatile ExecutorService executor;
    
    private ThreadGroup processThreadGroup;
    private final AtomicReference<Process> nextProcess;
    
    public UserKernelImpl() {
        this(null);
    }
    
    public UserKernelImpl(Process process) {
        nextProcess = new AtomicReference<Process>(process);
    }

    public void setProcess(Process process) {
    	assert logger() != null : "Logger is null";
    	
    	logger().info("Setting process " + process);
        
        // Set the next process to run ...
        {
            final Process oldProcess = nextProcess.getAndSet(process);
            if (oldProcess != null)
                logger().log(Level.WARNING,
                        "Process replaced before execution: {0}", oldProcess);
        }
        
        // ... and if we're already running, shut down the executor to cause
        // the while loop in start() to loop around
        final ExecutorService executor = this.executor;
        if (executor != null)
            executor.shutdownNow();
    }
    
    public void start() throws InterruptedException {
        logger().info("Starting up");
        
        processThreadGroup = new ThreadGroup("Process");
        
        Process process;
        while ((process = nextProcess.getAndSet(null)) != null) {
            assert this.executor == null : "Preexisting executor found";
            
            final ExecutorService executor = this.executor = createExecutor();
            
            try {
                executor.execute(new ProcessRunner(process));
                // Keep on going until someone calls executor.shutdownNow()
                while (!executor.awaitTermination(5, TimeUnit.SECONDS))
                    continue;
            } catch (InterruptedException e) {
                logger().warning("Interrupted; shutting down now");
                executor.shutdownNow();
                throw e;
            } finally {
                this.executor = null;
            }
        }
    }

    private ExecutorService createExecutor() {
        return new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS,
                    new SynchronousQueue<Runnable>(),
                    this.new ProcessThreadFactory());
    }
    
    public void shutDown() throws InterruptedException {
        logger().info("Shutting down");
        
        final ExecutorService executor = this.executor;
        if (executor == null)
            return;
        
        executor.shutdownNow();
        try {
            while (!executor.awaitTermination(5, TimeUnit.SECONDS))
                continue;
        } catch (InterruptedException e) {
            logger().warning("Interrupted during shutdown");
            throw e;
        }

        // It may be one of the Executor's worker threads themselves that
        // signalled its termination, so we can't destroy the process group
        // quite yet
        Thread.sleep(100);
        
        processThreadGroup.destroy();
    }
    
    private class ProcessThreadFactory implements ThreadFactory {
        private static final String
            PROCESS_NAME = "Process",
            FORK_NAME_PREFIX = "Fork-";
        
        private final AtomicInteger nextThreadNum =
            new AtomicInteger(0);
        
        public Thread newThread(Runnable runnable) {
            final int num = nextThreadNum.getAndIncrement();
            
            final Thread ans = new Thread(processThreadGroup, runnable,
                    num == 0 ? PROCESS_NAME : FORK_NAME_PREFIX + num);
            
            logger().log(Level.FINER,
                    "Creating new thread for runnable: {0}", ans);
            return ans;
        }
    }
    
    private class ProcessRunner implements Runnable {
        private final Process process;
        
        private ProcessRunner(Process process) {
            this.process = process;
        }
        
        @Override
        public void run() {
            logger().fine("Running process");
            final OperatingSystemImpl os = new OperatingSystemImpl();
            os.start();
            try {
                process.run(os);
            } catch (InterruptedException e) {
                logger().fine("Process interrupted");
                // Do nothing; we're shutting down as expected
            }
        }
    }
    
    private class OperatingSystemImpl implements OperatingSystem {
        private static final String
            OS_LOG_NAME_BASE = "network.OperatingSystem.",
            PROCESS_LOG_NAME_BASE = "network.Process.";
        
        private ConcurrentMap<Integer, BlockingQueue<Message<?>>> msgQByPort =
            new ConcurrentHashMap<Integer, BlockingQueue<Message<?>>>();
        
        private final Logger osLogger, processLogger;
        {
            final String suffix = NodeImpl.loggerNameSuffix(name(), address());
            osLogger = Logger.getLogger(OS_LOG_NAME_BASE + suffix);
            processLogger = Logger.getLogger(PROCESS_LOG_NAME_BASE + suffix);
        }
        
        // The O.S. should have the same executor over its lifetime; make sure
        // of this by locking in the current value of the executor now
        private final ExecutorService executor = UserKernelImpl.this.executor;
        
        public Logger logger() {
            return processLogger;
        }
        
        private void start() {
            executor.execute(new PutMessagesInMap());
        }

        // We only support one interface in a UserKernel, so this method
        // returns it.
        private Interface iface() {
            // This is kinda hacky, but for the moment it suffices to
            // assume that each non-router host gets connected exactly once
            // to exactly one other node.
            
            final List<Interface> ifaces = interfaces();
            try {
                return ifaces.get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalStateException("No interfaces are connected");
            }
        }

        public Message<?> receive(KnownPort port) throws InterruptedException {
            return receive(port.number());
        }
        
        public Message<?> receive(int port) throws InterruptedException {
            return receive(port, 0, null);
        }
        
        public Message<?> receive(KnownPort port, long timeout, TimeUnit unit)
                throws InterruptedException {
            return receive(port.number(), timeout, unit);
        }

        private BlockingQueue<Message<?>> queue(int port) {
            BlockingQueue<Message<?>> ans = msgQByPort.get(port);
            if (ans != null)
                return ans;
            
            final BlockingQueue<Message<?>> newQueue =
                new LinkedBlockingQueue<Message<?>>();
            ans = msgQByPort.putIfAbsent(port, newQueue);
            
            // If ans isn't null, someone else beat us to it
            return ans == null ? newQueue : ans;
        }

        public Message<?> receive(int port, long timeout, TimeUnit unit)
                throws InterruptedException {
            final BlockingQueue<Message<?>> queue = queue(port);
            
            final Message<?> ans;
            if (unit != null) {
                ans = queue.poll(timeout, unit);
            } else {
                ans = queue.take();
            }
	        	        
            if (ans == null)
                osLogger.log(Level.FINER,
                        "receive() timed out after {0}",
                        formatTimeout(timeout, unit));
            else
                osLogger.log(Level.FINER,
                        "Message received: {0}", ans);

            return ans;
        }
        
        private Object formatTimeout(final long timeout, final TimeUnit unit) {
            return new Object() {
                public String toString() {
                    return unit == null ? "an unknown time" :
                        timeout + " " + unit.toString().toLowerCase();
                }
            };
        }

        public void send(Message<?> message)
                throws InterruptedException, DisconnectedException {
            try {
                osLogger.log(Level.FINER, "Sending message: {0}", message);
                iface().send(message);
            } catch (Interface.DisconnectedException e) {
                throw new DisconnectedException();
            }
        }
        
        public void send(int dest, int sourcePort, int destPort,
                Serializable content)
                    throws DisconnectedException, InterruptedException {
            send(new Message<Serializable>(
                    address(), dest, sourcePort, destPort, content));
        }
        
        public void send(int dest, KnownPort sourcePort, KnownPort destPort,
                Serializable content) throws DisconnectedException,
                InterruptedException {
            send(dest, sourcePort.number(), destPort.number(), content);
        }
        
        public Future<?> fork(Runnable runnable) {
            return executor.submit(runnable);
        }
        
        public void replaceProcess(Process process)
                throws InterruptedException {
            setProcess(process);
            throw new InterruptedException();
        }
        
        /**
         * Class that checks to see if we have any messages to process in the
         * interface
         */
        private class PutMessagesInMap implements Runnable {
            @Override
            public void run() {
                Thread.currentThread().setName("Message Processor");
                
                try {
                    while (true) {
                        final Message<?> message = iface().receive();
                        final int destPort = message.destinationPort;
                        
                        if (KnownPort.KERNEL_WHO.is(destPort)) {
                            try {
                                send(message.source, KnownPort.KERNEL_WHO,
                                        KnownPort.KERNEL_WHO,
                                        RIP.Datagram.notARouter());
                            } catch (DisconnectedException e) {
                                // We got disconnected after they send the
                                // WHO message; doesn't matter
                            }
                        }
                        
                        queue(destPort).add(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public int address() {
            return UserKernelImpl.this.address();
        }
    }
}