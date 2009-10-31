package network.routing;


/**
 * this class holds the information needed for a rip algorithm 
 * @author Andy
 *
 */

public class RIPEntry {

	/**the address that this entry is all about*/
	private final int address;
	/**the current count for the # of hops away it is*/
	private int cost;
	/**the link, i.e. the address we go to in 1 hop*/
	private int link;
	
	
	/**
	 * we'lll take in the address we are interested in.
	 * @param address is the address this entry is interested in.
	 */
	public RIPEntry(int address) {
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
