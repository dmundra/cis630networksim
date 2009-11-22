package network;

import java.util.concurrent.TimeUnit;

/**
 * A network interface. Accessed by the {@link Kernel} and the
 * {@link OperatingSystem} to perform the low-level {@link #send(Message)} and
 * {@link #receive()} operations.
 * 
 * @see Simulator#connect(Node, Node)
 *
 * @author Luke Maurer
 */
public interface Interface {
    /**
     * Sends a message through the interface.
     * 
     * @param message The message to send.
     * @throws DisconnectedException If the interface has been disconnected.
     * @throws InterruptedException If the thread is interrupted while blocking.
     * @throws NodeNotRunningException If the node containing this interface
     *          has not started or has shut down.
     */
    void send(Message<?> message)
            throws DisconnectedException, InterruptedException;
    
    /**
     * Receives the next message from the interface, blocking until one is
     * sent.
     * 
     * @return The message sent.
     * @throws InterruptedException If the thread is interrupted while blocking.
     * @throws NodeNotRunningException If the node containing this interface
     *          has not started or has shut down.
     */
    Message<?> receive() throws InterruptedException;
    /**
     * Receives the next message from the interface, blocking until one is
     * sent or until timeout.
     * 
     * @param time The length of time to wait before timeout.
     * @param timeUnit The unit of <tt>time</tt>.
     * @return The message sent.
     * @throws InterruptedException If the thread is interrupted while blocking.
     * @throws NodeNotRunningException If the node containing this interface
     *          has not started or has shut down.
     */
    Message<?> receive(long time, TimeUnit timeUnit) throws InterruptedException;
    
    /**
     * Get the index of the interface within the node's list of interfaces.
     * Each interface has a unique index within the node, and it's guaranteed
     * that <code>node.interfaces().get(iface.index()) == iface</code>.
     * 
     * @return The interface's index.
     */
    int index();
    
    /**
     * Exception indicating that an {@link Interface#send(Message)} call was
     * made to an interface that has been disconnected.
     *
     * @author Luke Maurer
     */
    class DisconnectedException extends Exception {
        public DisconnectedException() {
            this("Interface is disconnected");
        }
        
        public DisconnectedException(String msg) {
            super(msg);
        }
    }
}
