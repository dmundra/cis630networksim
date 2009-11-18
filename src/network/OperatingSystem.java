package network;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The API by which a {@link Process} can perform system operations such as
 * {@link #send(Message)} and {@link #receive()}.
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
     * Send a message to another node, according to its
     * {@link Message#destination} field. Blocks until the message leaves the
     * interface.
     * 
     * @param message The message to send.
     * @throws InterruptedException If the thread is interrupted.
     */
    void send(Message<?> message) throws InterruptedException;
    
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
     * Receive the next message sent to this node, without regard to the
     * destination port or other niceties. Blocks until the message is
     * received.
     * 
     * @return The next message this node receives.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive() throws InterruptedException;
    
    /**
     * Receive the next message sent to this node, without regard to the
     * destination port or other niceties. Blocks until the message is
     * received or timeout occurs.
     * 
     * @param timeout How long to wait before timing out.
     * @param unit The unit for <tt>timeout</tt>.
     * @return The next message this node receives, or <tt>null</tt> if timeout
     *          occurs before then.
     * @throws InterruptedException If the thread is interrupted. 
     */
    Message<?> receive(long timeout, TimeUnit unit)
            throws InterruptedException;
    
    // XXX We only really *need* send and receive, but we should support
    // sockets that bind to ports and can be select()'d somehow.
    
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
}
