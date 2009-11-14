package network.impl.kernel;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import network.AbstractKernel;
import network.Interface;
import network.KnownPort;
import network.Message;
import network.Interface.DisconnectedException;

public class KernelImpl extends AbstractKernel {

	/** this is a map of our interface to unique integer */
	private ConcurrentHashMap<Interface, Integer> interfaceList = new ConcurrentHashMap<Interface, Integer>();
	private ConcurrentHashMap<Integer, KernelNode> routingTable = new ConcurrentHashMap<Integer, KernelNode>();
	/** a list of the messages that need to be processed by the routing table */
	private ConcurrentHashMap<Integer, Message<KernelNode>> toRoute = new ConcurrentHashMap<Integer, Message<KernelNode>>();
	/**for debugging*/
	private static boolean isDebug = true;
	
	@Override
	public void interfaceAdded(Interface iface) {

		int i = interfaceList.size() - 1;
		synchronized (interfaceList) {
			for (Integer j : interfaceList.values()) {
				i = Math.max(i, j);
			}

			// we should have a unique i
			interfaceList.put(iface, i);
		}
	}

	@Override
	public void interfaceConnected(Interface iface) {
		// TODO Auto-generated method stub

	}

	@Override
	public void interfaceDisconnected(Interface iface) {

		// want to sync so that we don't remove while adding
		synchronized (interfaceList) {
			interfaceList.remove(iface);
		}
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// Runs the RIP algorithm every 50 seconds for now.
		Timer checkNeighbors = new Timer("checkNeighbors");
		checkNeighbors.schedule(new RIPTask(), 2000, 1000);
		checkNeighbors.schedule(new CheckMessages(), 4000, 1000);
	}

	/**
	 * Class that checks to see if we have any messages to process in the
	 * interfaces
	 */
	class CheckMessages extends TimerTask {

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			// we want to check all of the interfaces to see if they have any
			// messages for us to process:
			for (Map.Entry<Interface, Integer> pair : interfaceList.entrySet()) {
				try {
					// TODO: do our normal work if it isn't a rip message...
					Message recievedMessage = pair.getKey().receive();
					if (recievedMessage.data instanceof KernelNode) {
						Message<KernelNode> rm2 = recievedMessage
								.asType(KernelNode.class);
						toRoute.put(pair.getValue(), rm2);
						// }else if(...){
					} else {
						// other cases did not work
						throw new RuntimeException("Can't handle the case");
					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Class that runs the RIP algorithm to find all neighbors and add them to
	 * Kernel's routing table.
	 */
	class RIPTask extends TimerTask {
		// TODO Clean up and fix RIP algorithm (current implementation of rip
		// might be n^n...)
		public void run() {
			for (Interface i : interfaceList.keySet()) {
				try {
					KernelNode kernelNode = new KernelNode(address());
					kernelNode.setRoutingTable(routingTable);
					Message<KernelNode> findNeighbors = new Message<KernelNode>(
							address(), 255, KnownPort.KERNEL_WHO.ordinal(),
							KnownPort.KERNEL_WHO.ordinal(), kernelNode);
					i.send(findNeighbors);

				} catch (DisconnectedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			
			//see if anyone sent us a back their info
			for (Map.Entry<Integer, Message<KernelNode>> pair : toRoute
					.entrySet()) {
				

				// Message<KernelNode> recievedMessage =
				// pair.getKey().receive().asType(KernelNode.class);
				KernelNode neighbor = pair.getValue().data.partialClone();
				neighbor.setCost(1);
				neighbor.setLink(pair.getKey());
				//replace current info:
				routingTable.put(pair.getValue().source, neighbor);
				
				//now see if our updated node has any information that is better than what we have:
				for(Map.Entry<Integer, KernelNode> sub : pair.getValue().data.getRoutingTable().entrySet()){
					
					checkAndAdd(sub, pair.getValue().source);
				}
				
				

			}
			
			

			String toPrint = (name() + "- Check routing table:");
			
			for (Map.Entry<Integer, KernelNode> pair : routingTable.entrySet()) {
				toPrint += ("\n\tName:" + pair.getValue().getAddress() + " Size:" + pair.getValue().getRoutingTable().size());
			}
			
			if(isDebug && name().equals("15")) System.out.println(toPrint);

		}
		
		/**
		 * checks to see if the information should be added to our routing table
		 * @param toAdd the info to check (and add if needed)
		 */
		private void checkAndAdd(Map.Entry<Integer, KernelNode> info, int link){
//			KernelNode toAdd = info.getValue().clone();
//			toAdd.setCost(toAdd.getCost() + 1);
//			toAdd.setLink(link);
			
			
			boolean checkNewGuy = false;
			// see if the address is in our table:
			if (routingTable.containsKey(info.getKey())) {
				// the address is already here, so see if we are
				// better:
				if (routingTable.get(info.getKey()).getCost() > info.getValue()
						.getCost()) {
					// looks like we have a better way to get there:
					KernelNode toAdd = info.getValue().partialClone();
					toAdd.setCost(toAdd.getCost() + 1);
					toAdd.setLink(link);
					routingTable.replace(info.getKey(), toAdd);
					checkNewGuy = true;
				}
			} else {
				// see if the address is not us:
				if (address() != info.getKey()) {
					//if(isDebug) System.err.println(name() + " address, adding address == (" + address() + ", " + info.getKey() + ")");
					// this might throw exception at runtime... I
					// think that concurrentHashMap can do this
					// though
					KernelNode toAdd = info.getValue().partialClone();
					toAdd.setCost(toAdd.getCost() + 1);
					toAdd.setLink(link);
					routingTable.put(info.getKey(), toAdd);
					checkNewGuy = true;
					if(isDebug && name().equals("15") && info.getKey() == (17)) System.out.println("Debug- " + info.getKey() + " : " + info.getValue().getRoutingTable().toString());
				}
			}
			
			if(checkNewGuy){
				for(Map.Entry<Integer, KernelNode> info2 : info.getValue().getRoutingTable().entrySet()){
					
					checkAndAdd(info2, link);
				}
			}
		}
	}
}
