package network.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import network.Interface;
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
    
    NodeImpl(SimulatorImpl sim, int address, String name,
            Kernel kernel, List<NodeImpl> neighbors) {
        super(sim);

        this.address = address;
        this.name = name;
        this.kernel = kernel;
        
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
        
        return new InterfaceImpl(this);
    }
    
    synchronized int register(InterfaceImpl iface) {
        interfaces.add(iface);
        return interfaces.size() - 1;
    }
    
    void markUnused(InterfaceImpl iface) {
        unusedInterfaces.add(iface);
    }
    
    public int address() {
        return address;
    }
    
    public String name() {
        return name;
    }
    
    public List<? extends Interface> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }
    
    public Kernel kernel() {
        return kernel;
    }
    
    public String toString() {
        return name + " @ " + Integer.toHexString(address);
    }
}
