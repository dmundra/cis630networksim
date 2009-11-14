package network.impl.kernel;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
		while (true) {
			// Runs the RIP algorithm every 50 seconds for now.
			Timer checkNeighbors = new Timer("checkNeighbors");
			checkNeighbors.schedule(new RIPTask(), 10000);
		}
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
					if(isDebug) System.out.println("rip task on interface: " + i);
					KernelNode kernelNode = new KernelNode(address());
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

			// we want to make a new routing table that will replace our current
			// when we are done:
			ConcurrentHashMap<Integer, KernelNode> routingTableNew = new ConcurrentHashMap<Integer, KernelNode>();

			for (Map.Entry<Integer, Message<KernelNode>> pair : toRoute
					.entrySet()) {

				// Message<KernelNode> recievedMessage =
				// pair.getKey().receive().asType(KernelNode.class);
				KernelNode neighbor = pair.getValue().data.clone();
				neighbor.setCost(1);
				neighbor.setLink(pair.getKey());
				routingTableNew.put(pair.getValue().source, neighbor);

			}

			// now let's fix up our routing table:
			for (boolean madeChanges = true; madeChanges;) {
				madeChanges = false;

				for (KernelNode n : routingTableNew.values()) {
					// see if the node has a routing table
					for (Map.Entry<Integer, KernelNode> pair : n
							.getRoutingTable().entrySet()) {
						KernelNode toAdd = pair.getValue().clone();
						toAdd.setCost(toAdd.getCost() + 1);
						toAdd.setLink(n.getLink());
						// see if the address is in our table:
						if (routingTableNew.containsKey(pair.getKey())) {
							// the address is already here, so see if we are
							// better:
							if (routingTableNew.get(pair.getKey()).getCost() > toAdd
									.getCost()) {
								// looks like we have a better way to get there:
								routingTableNew.replace(pair.getKey(), toAdd);
								// don't think we should set madeChanges = true;
							}
						} else {
							// see if the address is not us:
							if (address() != pair.getKey()) {
								// this might throw exception at runtime... I
								// think that concurrentHashMap can do this
								// though
								routingTableNew.put(pair.getKey(), toAdd);
								madeChanges = true;
							}
						}
					}
				}
			}

			// now start using our new routing table:
			routingTable = routingTableNew;

		}
	}
}
