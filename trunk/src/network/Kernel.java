package network;

import java.util.logging.Logger;

/**
 * Logic responsible for handling routing and messaging for a single node.
 * <p>
 * A router will have a kernel decidated solely to routing, with no userspace
 * code involved. A workstation or server will have a {@link UserKernel}, which
 * can contain and run a {@link Process}.
 * <p>
 * Note that {@link #interfaceAdded(Interface)} and such may be called
 * <em>before</em> {@link #start()}, in which case any operations on the passed
 * interfaces (besides storing them) must wait for {@link #start()}. 
 * 
 * @see Simulator#createRouterKernel()
 * @see Simulator#createUserKernel()
 * 
 * @author Luke Maurer
 */
public interface Kernel {
    /**
     * Set the address of the node. Will be called once <em>before</em>
     * {@link #start()}.
     * 
     * @param address The address assigned to this node.
     * 
     * @see Node#address()
     */
    void setAddress(int address);
    
    /**
     * Set the name of the node. Will be called once <em>before</em>
     * {@link #start()}.
     * 
     * @param name The name assigned to this node. Note that this is not
     * guaranteed to be unique.
     * 
     * @see Node#name()
     */
    void setName(String name);
    
    /**
     * Set the logger the node can use. Will be called once <em>before</em>
     * {@link #start()}.
     * 
     * @param logger The logger for this node to use.
     * 
     * @see OperatingSystem#logger()
     * @see Simulator#logger()
     * @see java.util.logging
     */
    void setLogger(Logger logger);
    
    /**
     * Set the thread in which {@link #start()} will be run. Will be called
     * once <em>before</em> {@link #start()}; the thread may or not yet be
     * running.
     * 
     * @param thread
     */
    void setMainThread(Thread thread);
    
    /**
     * Handle a new interface. The interface will not yet be connected at the
     * time this is called, though it may become connected immediately after.
     * 
     * @param iface The new interface.
     */
    void interfaceAdded(Interface iface);
    
    /**
     * Handle the connection of an interface to another node.
     * 
     * @param iface The interface that was connected. Will already have been
     * passed to {@link #interfaceAdded(Interface)}.
     */
    void interfaceConnected(Interface iface);
    
    /**
     * Handle the disconnection of an interface from its peer.
     * 
     * @param iface The interface that was disconnected.
     */
    void interfaceDisconnected(Interface iface);
    
    /**
     * Boot up the system. Will be called from a new thread.
     */
    void start() throws InterruptedException;
    
    /**
     * Shut down the system. When this method returns, any extra threads
     * created by the node must have stopped.
     */
    void shutDown() throws InterruptedException;
}
