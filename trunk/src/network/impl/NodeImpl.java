package network.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final ThreadGroup threadGroup;
    private final MainThread thread;
    
    private volatile boolean running;
    private volatile boolean shuttingDown;
    
    NodeImpl(SimulatorImpl sim, int address, String name,
            Kernel kernel, List<NodeImpl> neighbors) {
        super(sim);

        this.address = address;
        this.name = name;
        this.threadGroup = new ThreadGroup(name);
        this.thread = this.new MainThread();
        this.kernel = kernel != null ? kernel : sim.createRouterKernel();
        
        for (NodeImpl node : neighbors)
            connectTo(node);
    }
    
    InterfaceImpl connectTo(NodeImpl node) {
        final InterfaceImpl iface = this.unusedInterface();
        iface.connect(node.unusedInterface());
        return iface;
    }
    
    private InterfaceImpl unusedInterface() {
        final InterfaceImpl unused = unusedInterfaces.poll();
        if (unused != null)
            return unused;
        
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
        kernel.interfaceConnected(iface);
    }
    
    void disconnected(InterfaceImpl iface) {
        kernel.interfaceDisconnected(iface);
        unusedInterfaces.add(iface);
    }
    
    void startUp() {
        thread.start();
    }
    
    class MainThread extends Thread {
        private static final String NAME = "Main";
        
        MainThread() {
            super(threadGroup, NAME);
        }
        
        public void run() {
            running = true;
            kernel.start();
        }
    }
    
    boolean running() {
        return running;
    }
    
    boolean shuttingDown() {
        return shuttingDown;
    }
    
    void shutDown() {
        shuttingDown = true;
        
        // No interfaces are allowed to be added now, so this is safe
        for (InterfaceImpl iface : interfaces)
            iface.disconnect();
        
        kernel.shutDown();
        try {
            // Wait until the main thread finishes
            thread.join();
            threadGroup.destroy();
            running = false;
        } catch (InterruptedException e) {
            // TODO Should probably log a warning
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
