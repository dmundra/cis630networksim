package network;

/**
 * Logic responsible for handling routing and messaging for a single node.
 * <p>
 * A router will have a kernel decidated solely to routing, with no userspace
 * code involved. A workstation or server will have a {@link UserKernel}, which
 * can contain and run a {@link Process}.
 * 
 * @see Simulator#createRouterKernel()
 * @see Simulator#createUserKernel()
 * 
 * @author Luke Maurer
 */
public interface Kernel {
    void interfaceAdded(Interface iface);
    void interfaceConnected(Interface iface);
    void interfaceDisconnected(Interface iface);
    
    /**
     * Boot up the system. Should spawn its own thread(s).
     */
    void start();
    
    /**
     * Shut down the system. Should return immediately.
     */
    void shutDown();
}
