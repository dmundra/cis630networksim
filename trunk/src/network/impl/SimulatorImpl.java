package network.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import network.Interface;
import network.Kernel;
import network.Node;
import network.Simulator;
import network.UserKernel;

class SimulatorImpl implements Simulator {
    private AtomicInteger autoNodeNameIx = new AtomicInteger();
    private AtomicInteger nextAddress = new AtomicInteger(1);
    private ConcurrentMap<Integer, NodeImpl> nodes =
        new ConcurrentHashMap<Integer, NodeImpl>();
    private volatile boolean started;
    
    public NodeBuilder buildNode() {
        return new NodeBuilder() {
            private String name;
            private Kernel kernel;
            private List<NodeImpl> neighbors = new ArrayList<NodeImpl>();
            
            public NodeBuilder name(String name) {
                this.name = name;
                return this;
            }
            
            public NodeBuilder kernel(Kernel kernel) {
                this.kernel = kernel;
                return this;
            }
            
            public NodeBuilder connections(Node ... nodes) {
                for (Node node : nodes) {
                    final NodeImpl nodeImpl = checkOwnership(node);
                    neighbors.add(nodeImpl);
                }
                return this;
            }
            
            public Node create() {
                final String name = this.name != null ? this.name :
                    "Node " + autoNodeNameIx.getAndIncrement();
                final int address = nextAddress.getAndIncrement();
                final NodeImpl node =
                    new NodeImpl(
                            SimulatorImpl.this, address, name, kernel, neighbors);
                
                synchronized (SimulatorImpl.this) {
                    nodes.put(address, node);
                    if (started)
                        node.startUp();
                }
                
                return node;
            }
        };
    }
    
    public void destroy(Node node) {
        final NodeImpl nodeImpl = checkOwnership(node);
        nodeImpl.shutDown();
        nodes.remove(nodeImpl.address());
    }
    
    public Node nodeAt(int address) {
        return nodes.get(address);
    }
    
    public Interface connect(Node a, Node b) {
        final NodeImpl aImpl = checkOwnership(a), bImpl = checkOwnership(b);
        return aImpl.connectTo(bImpl);
    }
    
    public void disconnect(Interface iface) {
        final InterfaceImpl impl = checkOwnership(iface);
        impl.disconnect();
    }
    
    public Kernel createRouterKernel() {
        // TODO Implement
        return null;
    }
    
    public UserKernel createUserKernel() {
        // TODO Implement
        return null;
    }
    
    public synchronized void start() {
        started = true;
        for (NodeImpl node : nodes.values())
            node.startUp();
    }
    
    public Logger logger() {
        return Logger.getLogger("network.Simulator");
    }
    
    <I, T extends SimulationObject<I>> T checkOwnership(I node) {
        if (!(node instanceof SimulationObject<?> &&
                ((SimulationObject<?>) node).sim == SimulatorImpl.this))
            throw new IllegalArgumentException(
                    "Can't mix nodes from different simulators");
        
        @SuppressWarnings("unchecked")
        final T nodeImpl = (T) node;
        return nodeImpl;
    }
}