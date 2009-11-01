package network.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import network.Interface;
import network.Message;
import network.NodeNotRunningException;

final class InterfaceImpl extends SimulationObject<Interface>
        implements Interface {
    final NodeImpl node;
    final int index;
    private volatile Wire wire;
    private volatile InterfaceImpl peer;
    private final BlockingQueue<byte[]> queue =
        // TODO Explore using things like a bounded buffer, or a 
        // DelayedBlockingQueue for adding latency, etc.
        new LinkedBlockingQueue<byte[]>();
    
    void connect(InterfaceImpl other) {
        new Wire(this, other).connect();
    }
        
    void disconnect() {
        wire.disconnect();
        queue.clear();
    }   
    
    interface NewInterfaceCallback {
        int registerInterface(InterfaceImpl iface);
    }

    InterfaceImpl(NodeImpl node, InterfaceImpl.NewInterfaceCallback callback) {
        super(node.sim);
        this.node = node;
        this.index = callback.registerInterface(this);
    }
    
    /**
     * Object to handle the synchronization of connection and disconnection
     * operations. These need to synchronize on both interfaces involved, but
     * we need to make sure we never end up with a deadlock because of lock
     * ordering.
     *
     * @author Luke Maurer
     */
    private static class Wire {
        private final InterfaceImpl left, right;
        Wire(InterfaceImpl left, InterfaceImpl right) {
            // Be consistent about lock ordering
            if (System.identityHashCode(left) > System.identityHashCode(right)) {
                InterfaceImpl temp = left;
                left = right;
                right = temp;
            }
            
            this.left = left;
            this.right = right;
        }
        
        private void connect() {
            if (left.node.shuttingDown() || right.node.shuttingDown())
                throw new IllegalStateException("Node shutting down");
            
            synchronized(left) {
                synchronized(right) {
                    if (left.wire != null || right.wire != null)
                        throw new IllegalStateException("Already connected");
                    
                    assert left.peer == null && right.peer == null;
                    
                    left.wire = right.wire = this;
                    left.peer = right;
                    right.peer = left;
                }
            }
        }
        
        private void disconnect() {
            synchronized(left) {
                synchronized(right) {
                    if (left.wire != this || right.wire != this)
                        throw new IllegalStateException("Not connected to this wire");
                    
                    left.wire = right.wire = null;
                    left.peer = right.peer = null;
                }
            }
            
            left.node.disconnected(left);
            right.node.disconnected(right);
        }
    }

    public void send(Message<?> message) throws DisconnectedException,
            InterruptedException {
        if (!node.running())
            throw new NodeNotRunningException(node);
        
        final InterfaceImpl peer = this.peer;
        if (peer == null)
            throw new DisconnectedException();
        
        final byte[] data;
        try {
            data = serialize(message);
        } catch (IOException e) {
            // TODO Need a better exception.
            throw new RuntimeException(e);
        }
        
        peer.queue.put(data);
    }
    
    public Message<?> receive() throws InterruptedException {
        return receive(-1, null);
    }
    
    public Message<?> receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (!node.running())
            throw new NodeNotRunningException(node);
        
        final byte[] data;
        if (unit != null)
            data = queue.poll(timeout, unit);
        else
            data = queue.take();
        
        if (data == null)
            return null;
        
        final Message<?> message;
        try {
            message = (Message<?>) deserialize(data);
        } catch (IOException e) {
            // TODO Need a better exception.
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            // TODO Need a better exception.
            throw new RuntimeException(e);
        }
        
        return message;
    }
    
    private static byte[] serialize(Serializable object) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        final ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(object);
        return out.toByteArray();
    }
    
    private static Object deserialize(byte[] data)
            throws IOException, ClassNotFoundException {
        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        final ObjectInputStream objIn = new ObjectInputStream(in);
        return objIn.readObject();
    } 
    
    public String toString() {
        return "Interface: " + node + "/" + index + " -> " 
                + peer.node + "/" + peer.index;
    }
}
