package network.impl.kernel;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Each Kernel has a Node which contains the address data, no of hops, link.
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	private HashMap<Integer, Node> routingTable = new HashMap<Integer, Node>(); 

	public HashMap<Integer, Node> getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(HashMap<Integer, Node> routingTable) {
		this.routingTable = routingTable;
	}

	/**
	 * The address that this entry is all about
	 */
	private final int address;
	
	/**
	 * The current count for the # of hops away it is
	 */
	private int cost;
	
	/**
	 * The link, i.e. the address we go to in 1 hop
	 */
	private int link;
	
	/**
	 * We'll take in the address we are interested in.
	 * @param address is the address this entry is interested in.
	 */
	public Node(int address) {
		this.address = address;
		this.cost = 0;
		this.link = -1;
	}
	
	/**
	 * returns a shallow copy (routing table is same a cloner)
	 * 
	 * @return a shallow copy
	 */
	@Override
	public Node clone(){
		Node clone = new Node(address);
		clone.cost = cost;
		clone.link = link;
		clone.routingTable = routingTable;
		return clone;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public int getLink() {
		return link;
	}

	public void setLink(int link) {
		this.link = link;
	}

	public int getAddress() {
		return address;
	}
	
}
