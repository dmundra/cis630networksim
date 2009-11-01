package network;
import java.util.List;

/**
 * A machine on the network. May be either a router or a leaf node.
 * 
 * @see Simulator#buildNode()
 *
 * @author Luke Maurer
 */
public interface Node {
    /**
     * A purely descriptive name. Need not be unique, much less useful; only
     * used for display.
     * 
     * @return A name for the node.
     */
    String name();
    
    /**
     * The address of the node, analogous to an IP address. In this simulation,
     * this identifies one node, no matter how many interfaces it has, whereas
     * of course in real life each interface has its own address.
     * 
     * @return The node's address. Currently it's just a number, with no
     * structure of its own, but this may change.
     */
    int address();
    
    /**
     * Retrieve this node's kernel. See the {@link Kernel} interface for more
     * on its role.
     * 
     * @return The kernel specified for this node at creation time.
     */
    Kernel kernel();
    
    /**
     * Get all of this node's network interfaces.
     * 
     * @return An immutable list view of the node's interfaces.
     */
    List<? extends Interface> interfaces();
}
