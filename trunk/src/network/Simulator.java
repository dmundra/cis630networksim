package network;

import java.util.logging.Logger;

/**
 * The main control interface to the simulation. From here, nodes are created,
 * destroyed, and manipulated at will. Use this power wisely.
 *
 * @author Luke Maurer
 */
public interface Simulator {
    NodeBuilder buildNode();
    void destroy(Node node);
    
    Node nodeAt(int address);
    
    /**
     * Connect one node to another. This actually creates a new
     * {@link Interface} object with a new address on each node; nodes can have
     * arbitrarily many interfaces.
     * 
     * @param a One of the nodes to connect.
     * @param b The other node.
     * @return The new interface on <tt>a</tt>, connecting it to <tt>b</tt>.
     */
    Interface connect(Node a, Node b);
    /**
     * Disconnect an interface, destroying it and its peer on the other node.
     * 
     * @param iface The interface to disconnect.
     */
    void disconnect(Interface iface);
    
    Kernel createRouterKernel();
    UserKernel createUserKernel();
    
    void start();
    
    Logger logger();
    
    interface NodeBuilder {
        NodeBuilder name(String name);
        NodeBuilder kernel(Kernel kernel);
        NodeBuilder connections(Node ... nodes);
        Node create();
    }
}
