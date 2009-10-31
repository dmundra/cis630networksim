package network.impl.kernel;

import java.io.Serializable;

/**
 * Each Kernel has a Node which contains the address data, no of hops, link.
 * @author Anthony Wittig
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;

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
