package network;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The API by which a {@link Process} can perform system operations such as
 * {@link #send(Message)} and {@link #receive(int)}.
 *
 * @see Process
 * @see UserKernel
 *
 * @author Luke Maurer
 */
public interface OperatingSystem {
    /**
     * Get a logger to use for output.
     * 
     * @return A JDK Logger object, preconfigured for this node.
     */
    Logger logger();
    
    /**
     * returns the address of the kernel it's on...
     * 
     * @return the address of the node we are on
     */
    public int address();
    
    /**
     * Send a message to another node, according to its
     * {@link Message#destination} field. Blocks until the message leaves the
     * interface.
     * 
     * @param message The message to send.
     * @throws InterruptedException If the thread is interrupted.
     */
    void send(Message<?> message)
            throws DisconnectedException, InterruptedException;
    
    /**
     * Send a message to another node. Blocks until the message leaves the
     * interface.
     * <p>
     * This method is equivalent to calling {@link #send(Message)} with a
     * newly constructed Message object.
     * 
     * @param dest The node to which to send the message.
     * @param sourcePort The port from which the message is said to originate.
     * @param destPort The port to which to send the message.
     * @param content The content of the message.
     * @throws InterruptedException If the thread is interrupted.
     */
    void send(int dest, int sourcePort, int destPort, Serializable content)
            throws DisconnectedException, InterruptedException;
    
    /**
     * Send a message to another node. Blocks until the message leaves the
     * interface.
     * <p>
     * This method is equivalent to calling {@link #send(Message)} with a
     * newly constructed Message object.
     * 
     * @param dest The node to which to send the message.
     * @param sourcePort The port from which the message is said to originate.
     * @param destPort The port to which to send the message.
     * @param content The content of the message.
     * @throws InterruptedException If the thread is interrupted.
     */
    void send(int dest, KnownPort sourcePort, KnownPort destPort,
                Serializable content)
            throws DisconnectedException, InterruptedException;
    
//    /**
//     * Send a message to another node, according to its
//     * {@link Message#destination} field. Blocks until the message leaves the
//     * interface or timeout occurs.
//     * 
//     * @param message The message to send.
//     * @param timeout How long to wait before timing out.
//     * @param unit The unit for <tt>timeout</tt>.
//     * @return Whether the send was successful (true) or it timed out (false).
//     * @throws InterruptedException If the thread is interrupted.
//     */
//    boolean send(Message<?> message, long timeout, TimeUnit unit)
//            throws InterruptedException;
    
    /**
     * Receive the next message sent to the given port on this node. Blocks
     * until the message is received.
     * 
     * @param port The port to listen on.
     * @return The next message this node receives.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive(int port) throws InterruptedException;
    
    /**
     * Receive the next message sent to the given port on this node. Blocks
     * until the message is received.
     * 
     * @param port The port to listen on.
     * @return The next message this node receives.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive(KnownPort port) throws InterruptedException;
    
    /**
     * Receive the next message sent to the given port on this node. Blocks
     * until the message is received or timeout occurs.
     * 
     * @param port The port to listen on.
     * @param timeout How long to wait before timing out.
     * @param unit The unit for <tt>timeout</tt>.
     * @return The next message this node receives, or <tt>null</tt> if timeout
     *          occurs before then.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive(int port, long timeout, TimeUnit unit)
            throws InterruptedException;
    
    /**
     * Receive the next message sent to the given port on this node. Blocks
     * until the message is received or timeout occurs.
     * 
     * @param port The port to listen on.
     * @param timeout How long to wait before timing out.
     * @param unit The unit for <tt>timeout</tt>.
     * @return The next message this node receives, or <tt>null</tt> if timeout
     *          occurs before then.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive(KnownPort port, long timeout, TimeUnit unit)
            throws InterruptedException;
    
    // XXX Also we need more exceptions, like RoutingExceptions for when the
    // destination can't be found and such.
    
    /**
     * Halt the running process, replacing it with the given one.
     * 
     * @param process The process to run in place of the current one.
     * @throws InterruptedException Always.
     */
    void replaceProcess(Process process) throws InterruptedException;
    
    /**
     * Spawn a new thread. This should be used instead of built-in Java thread
     * creation mechanisms so that the threads can be kept track of. 
     * 
     * @param runnable The Runnable to run in the new thread.
     */
    void fork(Runnable runnable);
    
    /**
     * Exception indicating that an {@link OperatingSystem#send(Message)} call
     * was made when the node is not connected to any other.
     *
     * @author Luke Maurer
     */
    class DisconnectedException extends Exception {
        public DisconnectedException() {
            this("Node is disconnected");
        }
        
        public DisconnectedException(String msg) {
            super(msg);
        }
    }
}
