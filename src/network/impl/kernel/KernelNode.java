package network.impl.kernel;

import network.Interface;

/**
 * An entry in the routing table.
 * @author Anthony Wittig
 */
class KernelNode {
	/**
	 * The address that this entry is all about
	 */
	private final int address;
	
	/**
	 * The current count for the # of hops away it is
	 */
	private byte cost;
	
	/**
	 * Which interface leads to the node
	 */
	private Interface link;
	
	private static byte addCosts(byte fromNeighbor, byte linkCost) {
	    return (byte) Math.min(fromNeighbor + linkCost, Byte.MAX_VALUE);
	}
	
	/**
	 * We'll take in the address we are interested in.
	 * @param address is the address this entry is interested in.
	 */
	KernelNode(int address, Interface link, byte costFromNeighbor, byte linkCost) {
		this.address = address;
		this.cost = addCosts(costFromNeighbor, linkCost);
		this.link = link;
	}
	
	byte getCost() {
		return cost;
	}

	Interface getLink() {
		return link;
	}
	
	synchronized void clear() {
	    this.cost = Byte.MAX_VALUE;
	}

	synchronized void update(Interface link, byte costFromNeighbor, byte linkCost) {
	    final byte cost = addCosts(costFromNeighbor, linkCost);
	    if (cost < this.cost) {
	        this.link = link;
	        this.cost = cost;
	    } else if (this.link == link)
	        this.cost = cost;
	}
	
	int getAddress() {
		return address;
	}
	
	public String toString() {
		return "KernelNode: address:" + address + " cost:" + cost + " link:" + link;
	}	
}
