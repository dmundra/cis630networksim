package network.impl.kernel;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each Kernel has a Node which contains the address data, no of hops, link.
 */
public class KernelNode implements Serializable {

	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<Integer, KernelNode> routingTable = new ConcurrentHashMap<Integer, KernelNode>(); 

	public ConcurrentHashMap<Integer, KernelNode> getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(ConcurrentHashMap<Integer, KernelNode> routingTable) {
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
	public KernelNode(int address) {
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
	public KernelNode clone(){
		KernelNode clone = partialClone();
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
	
	public String toString()	{
		return "KernelNode: address:" + address + " cost:" + cost + " link:" + link + " routing table:" + routingTable.toString() + "";
	}

	public KernelNode partialClone() {
		KernelNode clone = new KernelNode(address);
		clone.cost = cost;
		clone.link = link;
		return clone;
	}
	
}
