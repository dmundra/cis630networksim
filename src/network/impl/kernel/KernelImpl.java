package network.impl.kernel;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import network.AbstractKernel;
import network.Interface;
import network.KnownPort;
import network.Message;
import network.Interface.DisconnectedException;

public class KernelImpl extends AbstractKernel {
    private ConcurrentHashMap<Integer, KernelNode> routingTable = new ConcurrentHashMap<Integer, KernelNode>();
	/** a list of the messages that need to be processed by the routing table */
	private ConcurrentHashMap<Integer, Message<KernelNode>> toRoute = new ConcurrentHashMap<Integer, Message<KernelNode>>();
	public static boolean printRipTable = false;
	private volatile Timer checkNeighbors;
	
	/**
	 * Shutdown router
	 */
	public void shutDown() throws InterruptedException {
	    checkNeighbors.cancel();
	    
        logger().info("Shutting down");
        
        Thread.sleep(100);
	}

	/**
	 * Start up router. Set RIP algorithm to run every second. Router will check messages every 50ms
	 */
	public void start() {
		// Runs the RIP algorithm every 50 seconds for now.
		checkNeighbors = new Timer("checkNeighbors");
		checkNeighbors.schedule(new RIPTask(), 200, 1000);
		checkNeighbors.schedule(new CheckMessages(), 2000, 50);
	}

	/**
	 * Class that checks to see if we have any messages to process in the
	 * interfaces
	 */
	class CheckMessages extends TimerTask {

		public void createDiscError(Message<?> rm) {
			logger().info(rm.toString());
			logger().info(routingTable.toString());
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			// we want to check all of the interfaces to see if they have any
			// messages for us to process:
		    for (Interface iface : interfaces()) {
				try {
					Message receivedMessage = iface.receive(1, TimeUnit.MILLISECONDS);
					
					// This is for case a message timeout
					if(receivedMessage == null){
						// no messages for us, ignore.
					} else if(receivedMessage.destinationPort == KnownPort.KERNEL_WHO.ordinal()){
						// rip message:
						// see if it's from a router:
						if (receivedMessage.data instanceof KernelNode) {
							Message<KernelNode> rm2 = receivedMessage
									.asType(KernelNode.class);
							toRoute.put(iface.index(), rm2);
						}else if(receivedMessage.data instanceof String){
							// this is just from a regular computer, add it to the list:
							KernelNode kn = new KernelNode(receivedMessage.source);
							Message<KernelNode> mKN = new Message<KernelNode>(receivedMessage.source, receivedMessage.destination, receivedMessage.sourcePort, receivedMessage.destinationPort, kn);
							toRoute.put(iface.index(), mKN);
						}
					} else {
						
						int dest = receivedMessage.destination;						
						KernelNode destNode = routingTable.get(dest);						
						
						if(destNode != null) {
							
							Interface sendIface = interfaces().get(destNode.getLink());
							
							try {
								sendIface.send(receivedMessage);
							} catch (DisconnectedException e) {
								//interfaces().remove(sendIface);
								//e.printStackTrace();
							}
						} else {
							createDiscError(receivedMessage);
						}						
						
					}

				} catch (InterruptedException e) {
					// We're shutting down
				    return;
				}
			}
		}

	}
	
	/**
	 * Class that runs the RIP algorithm to find all neighbors and add them to
	 * Kernel's routing table.
	 */
	class RIPTask extends TimerTask {
		@SuppressWarnings("static-access")
		public void run() {			
			for (Interface i : interfaces()) {
				try {
					KernelNode kernelNode = new KernelNode(address());
					kernelNode.setRoutingTable(routingTable);
					Message<KernelNode> findNeighbors = new Message<KernelNode>(
							address(), 255, KnownPort.KERNEL_WHO.ordinal(),
							KnownPort.KERNEL_WHO.ordinal(), kernelNode);
					i.send(findNeighbors);

				} catch (DisconnectedException e) {
					//e.printStackTrace();
				} catch (InterruptedException e) {
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
					checkAndAdd(sub, pair.getKey());
				}				

			}			

			// Print routing table
			if(printRipTable) {
				String toPrint = (name() + " - Check routing table:");
							
				for (Map.Entry<Integer, KernelNode> pair : routingTable.entrySet()) {
					toPrint += ("\n\tName:" + pair.getValue().getAddress() + " Size:" + pair.getValue().getRoutingTable().size() + "");
				}
							
				Logger log = logger().getLogger("network.impl.kernel.KernelImpl");
				
				log.info(toPrint);
			}

		}
		
		/**
		 * checks to see if the information should be added to our routing table
		 * @param toAdd the info to check (and add if needed)
		 */
		private void checkAndAdd(Map.Entry<Integer, KernelNode> info, int link){
			
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
					//if(isDebug && name().equals("15") && info.getKey() == (18)) System.out.println("Debug- " + info.getKey() + " : " + info.getValue().getRoutingTable().toString());
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
