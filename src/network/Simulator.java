package network;

import java.util.logging.Logger;

/**
 * The main control interface to the simulation. From here, nodes are created,
 * destroyed, and manipulated at will. Use this power wisely.
 *
 * @author Luke Maurer
 */
public interface Simulator {
    /**
     * Begin constructing a node.
     * 
     * @return A {@link NodeBuilder} object to parameterize and create the new
     *      node.
     */
    NodeBuilder buildNode();
    
    /**
     * Begin constructing a node, assigning it a specific address.
     * 
     * @param address The address to assign the node.
     * @return A {@link NodeBuilder} object to parameterize and create the new
     *      node.
     * @throws IllegalArgumentException If the given address is already in use.
     */
    NodeBuilder buildNode(int address);
    
    /**
     * Shut down the given node completely and remove it from the simulation.
     * The node should be discarded at this point. 
     * 
     * @param node The node to destroy.
     * 
     * @throws IllegalArgumentException If the node is not a part of this
     * simulation (i.e. was not created from this object's {@link #buildNode()}
     * method).
     */
    void destroy(Node node);
    
    /**
     * Retrieve the node at the given address.
     * 
     * @param address The address of the node to get.
     * @return The node at the given address.
     */
    Node nodeAt(int address);
    
    /**
     * Connect one node to another. This actually creates a new
     * {@link Interface} object with a new address on each node; nodes can have
     * arbitrarily many interfaces.
     * 
     * @param a One of the nodes to connect.
     * @param b The other node.
     * @return The new interface on <tt>a</tt>, connecting it to <tt>b</tt>.
     * @throws IllegalArgumentException If either {@code a} or {@code b} is not
     *      a part of this simulation.
     */
    Interface connect(Node a, Node b);
    
    /**
     * Disconnect an interface, destroying it and its peer on the other node.
     * 
     * @param iface The interface to disconnect.
     * @throws IllegalArgumentException If {@code iface} is not a part of this
     *      simulation.
     */
    void disconnect(Interface iface);
    
    /**
     * Create an instance of this simulator's default kernel for router nodes.
     * 
     * @return A new {@link Kernel} object with routing capabilities.
     */
    Kernel createRouterKernel();
    
    /**
     * Create an instance of this simulator's default kernel for hosts.
     * 
     * @return A new {@link UserKernel} object.
     * 
     * @see UserKernel
     * @see Process
     */
    UserKernel createUserKernel();
    
    /**
     * Create an instance of this simulator's default kernel for hosts,
     * preloaded with the given process.
     * 
     * @param process The process to load into the new kernel.
     * @return A new {@link UserKernel} object containing the given process.
     * 
     * @see UserKernel
     * @see Process
     */
    UserKernel createUserKernel(Process process);
    
    /**
     * Begin the simulation.
     */
    void start();
    
    /**
     * End the simulation, shutting down all nodes and cleaning up all
     * resources allocated.
     */
    void destroy();
    
    /**
     * Get the logger used by the simulator for top-level messages.
     * 
     * @return The simulator's own logger.
     */
    Logger logger();
    
    /**
     * A constructor object for {@link Node} objects. 
     *
     * @see Simulator#buildNode()
     *
     * @author Luke Maurer
     */
    interface NodeBuilder {
        /**
         * Give the node a name. By default, an autogenerated name is used.
         * 
         * @param name Any string to be used as a name. Used mostly for display
         *      purposes; may also affect logger names. Need not be unique.
         * @return This node builder.
         */
        NodeBuilder name(String name);
        
        /**
         * Specify the kernel to use for this node.
         * 
         * @param kernel The node's kernel. MUST NOT be shared with any other
         * node.
         * @return This node builder.
         */
        NodeBuilder kernel(Kernel kernel);
        
        /**
         * Connect this node to all the given nodes initially. Cumultative with
         * other calls to this method.
         * 
         * @param nodes The nodes to connect to.
         * @return This node builder.
         */
        NodeBuilder connections(Node ... nodes);
        
        /**
         * Create the node as parameterized. This node builder MUST NOT be
         * reused.
         * 
         * @return The node created by this builder.
         */
        Node create();
    }
}