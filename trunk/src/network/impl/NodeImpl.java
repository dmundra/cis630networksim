package network.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.Kernel;
import network.Node;

class NodeImpl extends SimulationObject<Node> implements Node {
    private final int address;
    private final String name;
    private final Kernel kernel;
    private final CopyOnWriteArrayList<InterfaceImpl> interfaces =
        new CopyOnWriteArrayList<InterfaceImpl>();
    private final BlockingQueue<InterfaceImpl> unusedInterfaces =
        new LinkedBlockingQueue<InterfaceImpl>();
    private final ThreadGroup kernelThreadGroup;
    private final KernelThread kernelThread;
    private final Logger kernelLogger;
    final Logger logger;
    
    private volatile boolean running;
    private volatile boolean shuttingDown;
    
    private static final String
        NODE_LOG_NAME_BASE = "network.Node.",
        KERNEL_LOG_NAME_BASE = "network.Kernel.";
    
    NodeImpl(SimulatorImpl sim, int address, String name,
            Kernel kernel, List<NodeImpl> neighbors) {
        super(sim);

        this.address = address;
        this.name = name;
        this.kernelThreadGroup = new ThreadGroup(name);
        this.kernelThread = this.new KernelThread();
        this.kernel = kernel != null ? kernel : sim.createRouterKernel();
        
        {
            final String suffix = loggerNameSuffix(name, address);
            this.logger = Logger.getLogger(NODE_LOG_NAME_BASE + suffix);
            this.kernelLogger = Logger.getLogger(KERNEL_LOG_NAME_BASE + suffix);
        }
        
        this.kernel.setAddress(address);
        this.kernel.setName(name);
        this.kernel.setLogger(this.kernelLogger);
        
        for (NodeImpl node : neighbors)
            connectTo(node);
    }
    
    static String loggerNameSuffix(String name, int address) {
        return String.format("[%s].[@%x]",
                name.replace("_", "__").replace('.', '_'), address);
    }
    
    InterfaceImpl connectTo(NodeImpl node) {
        final InterfaceImpl iface = this.unusedInterface();
        iface.connect(node.unusedInterface());
        return iface;
    }
    
    private InterfaceImpl unusedInterface() {
        final InterfaceImpl unused = unusedInterfaces.poll();
        if (unused != null) {
            logger.log(Level.FINER, 
                    "Reusing disconnected interface: {0}", unused);
            return unused;
        }
        
        final InterfaceImpl ans;
        synchronized (this) {
            ans = shuttingDown ? null : new InterfaceImpl(this,
                new InterfaceImpl.NewInterfaceCallback() {
                    public int registerInterface(InterfaceImpl iface) { 
                        interfaces.add(iface);
                        return interfaces.size() - 1;
                    }
                });
        }
        
        if (ans != null)
            kernel.interfaceAdded(ans);
        
        return ans;
    }
    
    void connected(InterfaceImpl iface) {
        logger.log(Level.FINE, "Connected interface: {0}", iface);
        kernel.interfaceConnected(iface);
    }
    
    void disconnected(InterfaceImpl iface) {
        logger.log(Level.FINE, "Disconnected interface: {0}", iface);
        kernel.interfaceDisconnected(iface);
        unusedInterfaces.add(iface);
    }
    
    void startUp() {
        logger.info("Starting");
        
        kernelThread.start();
    }
    
    private class KernelThread extends Thread {
        private static final String NAME = "Kernel";
        
        private KernelThread() {
            super(kernelThreadGroup, NAME);
        }
        
        public void run() {
            logger.fine("Kernel thread started");
            running = true;

            try {
                logger.log(Level.FINE, "Starting kernel: {0}", kernel);
                kernel.start();
            } catch (InterruptedException e) {
                // Do nothing; we're shutting down as intended
            }
        }
    }
    
    boolean running() {
        return running;
    }
    
    boolean shuttingDown() {
        return shuttingDown;
    }
    
    void shutDown() {
        logger.info("Shutting down");
        
        shuttingDown = true;
        
        // No interfaces are allowed to be added now, so this is safe
        for (InterfaceImpl iface : interfaces)
            iface.disconnect();
        
        try {
            kernel.shutDown();
            // Wait until the main thread finishes
            kernelThread.join();
            kernelThreadGroup.destroy();
            running = false;
        } catch (InterruptedException e) {
            logger.warning("Interrupted during shutdown");
            Thread.currentThread().interrupt();
        }
        
        shuttingDown = false;
    }
    
    public int address() {
        return address;
    }
    
    public String name() {
        return name;
    }
    
    public List<? extends InterfaceImpl> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }
    
    public Kernel kernel() {
        return kernel;
    }
    
    public String toString() {
        return name + " @ " + Integer.toHexString(address);
    }
}
