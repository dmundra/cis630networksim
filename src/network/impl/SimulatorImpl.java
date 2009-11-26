package network.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.Interface;
import network.Kernel;
import network.Node;
import network.Process;
import network.Simulator;
import network.UserKernel;
import network.impl.kernel.KernelImpl;

class SimulatorImpl implements Simulator {
    private final AtomicInteger autoNodeNameIx = new AtomicInteger();
    private final AtomicInteger nextAddress = new AtomicInteger(1);
    private final ConcurrentMap<Integer, NodeImpl> nodes =
        new ConcurrentHashMap<Integer, NodeImpl>();
    private volatile boolean started;
    private volatile boolean shuttingDown;
    
    private final ConcurrentMap<Integer, Object> reservedAddresses =
        new ConcurrentHashMap<Integer, Object>();
    
    private static final Logger log;
    
    static {
        final Logger rootLog = Logger.getLogger("");
        final Formatter formatter = new OutputFormatter();
        
        for (Handler handler : rootLog.getHandlers())
            handler.setFormatter(formatter);
        
        log = Logger.getLogger("network.Simulator");
    }
    
    public NodeBuilder buildNode() {
        return buildNode(0);
    }

    public NodeBuilder buildNode(final int givenAddress) {
        final Object addressReservationTag;
        
        if (givenAddress != 0) {
            increaseNextAddressBeyond(givenAddress);
            addressReservationTag = new Object();
            if (nodes.containsKey(givenAddress) ||
                    reservedAddresses.putIfAbsent(
                            givenAddress, addressReservationTag) != null)
                throw new IllegalArgumentException(
                        "Address already used: " + givenAddress);
        } else
            addressReservationTag = null;
        
        return new NodeBuilder() {
            private String name;
            private Kernel kernel;
            private final List<NodeImpl> neighbors = new ArrayList<NodeImpl>();
            private final AtomicBoolean used = new AtomicBoolean(false);
            
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
                if (used.getAndSet(true))
                    // TODO It'd be nice to be able to reuse NodeBuilders, but
                    // it'd be very tricky since we don't ever want them all to
                    // share the same Kernel and Process and such. Only way to
                    // make that work would be to make everything cloneable
                    // (pain in the ass) or to have the NodeBuilder take some
                    // kind of KernelFactory object rather than the kernel
                    // itself (which would've been a better design anyway ...).
                    // Anyhoo. This works fine for version 1.0 :-)
                    throw new IllegalStateException(
                            "Can only use a NodeBuilder once");
                
                final String name = this.name != null ? this.name :
                    "Node " + autoNodeNameIx.getAndIncrement();
                
                final int address;
                if (givenAddress != 0) {
                    address = givenAddress;
                    final boolean removed =
                        reservedAddresses.remove(
                                givenAddress, addressReservationTag);
                    assert removed :
                        "Address may have been assigned twice: " + givenAddress;
                } else
                    address = nextAddress.getAndIncrement();
                
                final NodeImpl node =
                    new NodeImpl(
                            SimulatorImpl.this, address, name, kernel,
                            neighbors);
                
                logger().log(Level.INFO, "Created node: {0}", node);
                
                synchronized (SimulatorImpl.this) {
                    if (shuttingDown)
                        throw new IllegalStateException(
                                "Shutting down; cannot add nodes");
                    nodes.put(address, node);
                    if (started)
                        node.startUp();
                }
                
                return node;
            }
            
            @Override
            protected void finalize() throws Throwable {
                reservedAddresses.remove(givenAddress, addressReservationTag);
                super.finalize();
            }
        };
    }
    
    private void increaseNextAddressBeyond(int address) {
        // Increase nextAddress to something at least as big as the
        // given address. The loop avoids a race condition where
        // two threads each set nextAddress; we want to make sure
        // we don't accidentally *decrease* it.
        while (true) {
            final int next = nextAddress.get();
            if (address >= next)
                if (nextAddress.compareAndSet(next, address + 1))
                    return;
        }
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
        return new KernelImpl();
    }
    
    public UserKernel createUserKernel() {
        return new UserKernelImpl();
    }
    
    public UserKernel createUserKernel(Process process) {
        return new UserKernelImpl(process);
    }
    
    public synchronized void start() {
        started = true;
        for (NodeImpl node : nodes.values())
            node.startUp();
    }
    
    public void destroy() {
        // Prevent more nodes from being added to the map
        shuttingDown = true;
        
        for (Iterator<NodeImpl> iter = nodes.values().iterator();
                iter.hasNext();) {
            iter.next().shutDown();
            iter.remove();
        }
    }
    
    public Logger logger() {
        return log;
    }
    
    <I, T extends SimulationObject<I>> T checkOwnership(I node) {
        if (!(node instanceof SimulationObject<?> &&
                ((SimulationObject<?>) node).sim == this))
            throw new IllegalArgumentException(
                    "Can't mix nodes from different simulators");
        
        @SuppressWarnings("unchecked")
        final T nodeImpl = (T) node;
        return nodeImpl;
    }
}