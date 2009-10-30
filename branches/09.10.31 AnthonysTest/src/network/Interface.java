package network;

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
    void send(Message<?> message)
            throws DisconnectedException, InterruptedException;
    Message<?> receive() throws InterruptedException;
    
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
