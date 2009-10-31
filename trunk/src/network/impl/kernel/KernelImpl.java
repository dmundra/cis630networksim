package network.impl.kernel;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import network.Interface;
import network.Kernel;
import network.KnownPort;
import network.Message;
import network.Interface.DisconnectedException;

public class KernelImpl implements Kernel {

	private CopyOnWriteArrayList<Interface> interfaceList = new CopyOnWriteArrayList<Interface>();
	private HashMap<Integer, Node> routingTable = new HashMap<Integer, Node>();
	private int address = -1;
	
	@Override
	public void interfaceAdded(Interface iface) {
		interfaceList.add(iface);		
	}

	@Override
	public void interfaceConnected(Interface iface) {
		// TODO
	}

	@Override
	public void interfaceDisconnected(Interface iface) {
		interfaceList.remove(iface);		
	}

	@Override
	public void shutDown() {
		
	}

	@Override
	public void start() {
		for(Interface i : interfaceList) {
			try {				
				Message<Node> findNeighbors = new Message<Node>(address, -1, KnownPort.KERNEL_WHO.ordinal(), KnownPort.KERNEL_WHO.ordinal(), new Node(address));
				i.send(findNeighbors);
			} catch (DisconnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setAddress(int address) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

}
