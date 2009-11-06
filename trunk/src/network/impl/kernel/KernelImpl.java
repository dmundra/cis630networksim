package network.impl.kernel;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import network.AbstractKernel;
import network.Interface;
import network.KnownPort;
import network.Message;
import network.Interface.DisconnectedException;

public class KernelImpl extends AbstractKernel {

	private CopyOnWriteArrayList<Interface> interfaceList = new CopyOnWriteArrayList<Interface>();
	private HashMap<Integer, Node> routingTable = new HashMap<Integer, Node>();
	
	@Override
	public void interfaceAdded(Interface iface) {
		interfaceList.add(iface);		
	}
	
	@Override
	public void interfaceConnected(Interface iface) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void interfaceDisconnected(Interface iface) {
		interfaceList.remove(iface);		
	}
	
	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void start() {
		while(true) {
			// Runs the RIP algorithm every 50 seconds for now.
			Timer checkNeighbors = new Timer("checkNeighbors");			
			checkNeighbors.schedule(new RIPTask(), 50000);			
		}
	}
	
	/**
	 * Class that runs the RIP algorithm to find all neighbors and add them to Kernel's
	 * routing table.
	 */
	class RIPTask extends TimerTask {		
		// TODO Clean up and add RIP algorithm
		public void run() {
			for(Interface i : interfaceList) {
				try {
					Node kernelNode = new Node(address());
					Message<Node> findNeighbors = new Message<Node>(address(), 255, KnownPort.KERNEL_WHO.ordinal(), KnownPort.KERNEL_WHO.ordinal(), kernelNode);
					i.send(findNeighbors);				
					
				} catch (DisconnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			for(Interface i : interfaceList) {
				try {
					Message<Node> recievedMessage = i.receive().asType(Node.class);
					
					routingTable.put(recievedMessage.source, recievedMessage.data);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		}		
	}
}
