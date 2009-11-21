package network.impl;

import static org.testng.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.AbstractKernel;
import network.Interface;
import network.Message;
import network.OperatingSystem;
import network.Process;
import network.UserKernel;
import network.Interface.DisconnectedException;

public class UserKernelImpl extends AbstractKernel implements UserKernel {
    private volatile ExecutorService executor;
    
    private ThreadGroup processThreadGroup;
    private final AtomicReference<Process> nextProcess =
        new AtomicReference<Process>();
    
    public void setProcess(Process process) {
    	assertNotNull(logger(),"Logger is null\n");
    	
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
        while (true) {
	        while ((process = nextProcess.getAndSet(null)) != null) {
	            assert this.executor == null : "Preexisting executor found";
	            
	            final ExecutorService executor = this.executor = 
	                Executors.newCachedThreadPool(new ProcessThreadFactory());
	            
	            try {
	                executor.execute(new ProcessRunner(process));
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
	        
	        try {
	            Thread.sleep(2000);
	        } catch (InterruptedException e) {
	            return;
	        }
        }
    }
    
    public void shutDown() throws InterruptedException {
        logger().info("Shutting down");
        
        final ExecutorService executor = this.executor;
        if (executor == null)
            return;
        
        executor.shutdownNow();
        while (!executor.awaitTermination(5, TimeUnit.SECONDS))
            continue;
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
            try {
                process.run(new OperatingSystemImpl());
            } catch (InterruptedException e) {
                // Do nothing; we're shutting down as expected
            }
        }
    }
    
    private class OperatingSystemImpl implements OperatingSystem {
        private static final String
            OS_LOG_NAME_BASE = "network.OperatingSystem.",
            PROCESS_LOG_NAME_BASE = "network.Process.";
        
        private ArrayList<Integer> knownaddress = new ArrayList<Integer>();
        private ConcurrentHashMap<Integer,LinkedBlockingQueue<Message>> msgQByPort = new ConcurrentHashMap<Integer, LinkedBlockingQueue<Message>>();
        private Timer checkMessages;
        
        private final Logger osLogger, processLogger;
        {
            final String suffix = NodeImpl.loggerNameSuffix(name(), address());
            osLogger = Logger.getLogger(OS_LOG_NAME_BASE + suffix);
            processLogger = Logger.getLogger(PROCESS_LOG_NAME_BASE + suffix);
        }
        
        private final ExecutorService executor = UserKernelImpl.this.executor;
        
        public Logger logger() {
            return processLogger;
        }
        
        // We only support one interface in a UserKernel, so this method
        // returns it.
        private Interface iface() {
            // This is kinda hacky, but for the moment it suffices to
            // assume that each non-router host gets connected exactly once
            // to exactly one router.
            
            final Interface ans = interfaces().get(0);
            if (ans == null)
                throw new IllegalStateException("No interfaces are connected"); 
            
            return ans;
        }

        public Message<?> receive(int port) throws InterruptedException {
        	
        	
        	
        	
//            final Message<?> message = iface().receive();
//            osLogger.log(Level.FINER, "Message received: {0}", message);
//            return message;
        	
        	return receive(port, Long.MAX_VALUE, TimeUnit.HOURS);
        }

       

		public Message<?> receive(int port, long timeout, TimeUnit unit)
                throws InterruptedException {
            
			if(checkMessages == null){
				checkMessages = new Timer("checkMessages");
				checkMessages.schedule(new PutMessagesInMap(), 4000, 1000);
			}
			
			//see if we have a blocking queue:
			if(msgQByPort.get(port) == null){
				msgQByPort.putIfAbsent(port, new LinkedBlockingQueue<Message>());
			}
			
			final Message<?> ans;
	        if (unit != null) {
	        	ans = msgQByPort.get(port).poll(timeout, unit);
	        }else{
	        	ans = msgQByPort.get(port).take();
	        }
	        
	        
	        if (ans == null){
	              osLogger.log(Level.FINER,
	                      "receive() timed out after {0}",
	                      formatTimeout(timeout, unit));
	        } else {
	              osLogger.log(Level.FINER,
	                      "Message received: {0}", ans);
	        }
			
//			final Message<?> ans = iface().receive(timeout, unit); 
//            
//            if (ans == null)
//                osLogger.log(Level.FINER,
//                        "receive() timed out after {0}",
//                        formatTimeout(timeout, unit));
//            else
//                osLogger.log(Level.FINER,
//                        "Message received: {0}", ans);
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

        public void send(Message<?> message) throws InterruptedException {
            try {
                osLogger.log(Level.FINER, "Sending message: {0}", message);
                iface().send(message);
            } catch (DisconnectedException e) {
                osLogger.warning("Interface has been disconnected");
                throw new IllegalStateException("No interfaces are connected");
            }
        }

        public void fork(Runnable runnable) {
            executor.execute(runnable);
        }
        
        public void replaceProcess(Process process) throws InterruptedException {
            setProcess(process);
            throw new InterruptedException();
        }
        
        /**
    	 * Class that checks to see if we have any messages to process in the
    	 * interfaces
    	 */
    	class PutMessagesInMap extends TimerTask {

    		@SuppressWarnings("unchecked")
    		@Override
    		public void run() {
    			// we want to check all of the interfaces to see if they have any
    			// messages for us to process:
    			
    				try {
    					Message recievedMessage = iface().receive();
    					
    					if(msgQByPort.get(recievedMessage.destinationPort) == null){
    						msgQByPort.putIfAbsent(recievedMessage.destinationPort, new LinkedBlockingQueue<Message>());
    					}
    					msgQByPort.get(recievedMessage.destinationPort).add(recievedMessage);

    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}
    		

    	}

		@Override
		public int address() {
			return UserKernelImpl.this.address();
		}
    }
}