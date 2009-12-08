package network.impl.kernel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.AbstractKernel;
import network.Interface;
import network.KnownPort;
import network.Message;
import network.Interface.DisconnectedException;
import network.protocols.RIP;

/**
 * KernelImpl for routers. The code below has two important features
 * that is running the RIP algorithm to check for new nodes and
 * a check message system to pass along messages that arrive in the
 * router from one node and are going to another node.
 * 
 * @author Anthony Wittig
 */
public class KernelImpl extends AbstractKernel {
    private static final long
        RIP_TASK_DELAY = 1,
        RIP_TASK_PERIOD = 1000,
        CHECK_TASK_DELAY = 1,
        CHECK_TASK_PEROD = 1;
    
    private ConcurrentMap<Integer, KernelNode> routingTable = new ConcurrentHashMap<Integer, KernelNode>();
	/** a list of the messages that need to be processed by the routing table */
	private ConcurrentMap<Interface, Message<RIP.Datagram>> toRoute = new ConcurrentHashMap<Interface, Message<RIP.Datagram>>();
	public static boolean printRipTable = false;
	private volatile ScheduledExecutorService checkNeighbors;
	
	/**
	 * Shutdown router
	 */
	public void shutDown() throws InterruptedException {
	    checkNeighbors.shutdownNow();
	    while (!checkNeighbors.awaitTermination(1, TimeUnit.SECONDS))
	        continue;
	}

	/**
	 * Start up router. Set RIP algorithm to run every second. Router will check messages every 50ms
	 */
	public void start() {
		// Runs the RIP algorithm every RIP_TASK_PERIOD milliseconds.
		checkNeighbors = Executors.newScheduledThreadPool(1);
		checkNeighbors.scheduleWithFixedDelay(
		        new RIPTask(), RIP_TASK_DELAY, RIP_TASK_PERIOD, TimeUnit.MILLISECONDS);
		checkNeighbors.scheduleWithFixedDelay(
		        new CheckMessages(), CHECK_TASK_DELAY, CHECK_TASK_PEROD, TimeUnit.MILLISECONDS);
	}

	/**
	 * Class that checks to see if we have any messages to process in the
	 * interfaces
	 */
	class CheckMessages implements Runnable {

		public void createDiscError(Message<?> rm) {
			logger().log(Level.WARNING, "Interface disconnected or route not found: {0}", rm);
		}
		
		@Override
		public void run() {
			// we want to check all of the interfaces to see if they have any
			// messages for us to process:
		    for (Interface iface : interfaces()) {
				try {
					Message<?> receivedMessage = iface.receive(50, TimeUnit.MILLISECONDS);
					
					// This is for case a message timeout
					if(receivedMessage == null){
						// no messages for us, ignore.
					} else if(receivedMessage.sentToPort(KnownPort.KERNEL_WHO)){
					    final Message<RIP.Datagram> message =
					        receivedMessage.asType(RIP.Datagram.class);
					    
					    // rip message:
						// see if it's from a router:
						if (message.data.entries != null) {
							toRoute.put(iface, message);
						} else {
							// this is just from a regular computer, add it to the list:
							RIP.Datagram fakeDatagram =
							    new RIP.Datagram(new RIP.Datagram.Entry[] {
						            new RIP.Datagram.Entry(receivedMessage.source, (byte) 0)
						        });
							Message<RIP.Datagram> fakeMessage = new Message<RIP.Datagram>(receivedMessage.source, receivedMessage.destination, receivedMessage.sourcePort, receivedMessage.destinationPort, fakeDatagram);
							toRoute.put(iface, fakeMessage);
						}
					} else {
						
						int dest = receivedMessage.destination;						
						KernelNode destNode = routingTable.get(dest);						
						
						if(destNode != null) {
							
							Interface sendIface = destNode.getLink();
							
							try {
								sendIface.send(receivedMessage);
							} catch (DisconnectedException e) {
							    createDiscError(receivedMessage);
							}
						} else {
							createDiscError(receivedMessage);
						}						
						
					}

				} catch (InterruptedException e) {
					// We're shutting down
				    Thread.currentThread().interrupt();
				    return;
				}
			}
		}

	}
	
	/**
	 * Class that runs the RIP algorithm to find all neighbors and add them to
	 * Kernel's routing table.
	 */
	class RIPTask implements Runnable {
		// TODO Clean up and fix RIP algorithm (current implementation of rip
		// might be n^n...)
		public void run() {			
			for (Interface i : interfaces()) {
				try {
					final KernelNode[] nodes =
					    routingTable.values().toArray(new KernelNode[0]);
				    final int count = nodes.length;
				    
				    final RIP.Datagram datagram =
				        new RIP.Datagram(new RIP.Datagram.Entry[count]);
				    
				    for (int ix = 0; ix < count; ix++)
				        datagram.entries[ix] =
				            new RIP.Datagram.Entry(nodes[ix].getAddress(),
				                    nodes[ix].getCost());
				    
					Message<RIP.Datagram> findNeighbors = new Message<RIP.Datagram>(
							address(), 255, KnownPort.KERNEL_WHO,
							KnownPort.KERNEL_WHO, datagram);
					i.send(findNeighbors);

				} catch (DisconnectedException e) {
					// TODO Handle disconnects according to RIP.
				} catch (InterruptedException e) {
				    Thread.currentThread().interrupt();
					return;
				}
			}
			
			//see if anyone sent us a back their info
			for (Map.Entry<Interface, Message<RIP.Datagram>> pair : toRoute
					.entrySet()) {
			    final Interface iface = pair.getKey();
			    final Message<RIP.Datagram> message =
			        pair.getValue().asType(RIP.Datagram.class);
			    
			    KernelNode neighbor = routingTable.get(message.source);
			    if (neighbor == null) {
			        final KernelNode newNeighbor =
			            new KernelNode(message.source, iface, (byte) 0, (byte) 1);
			        routingTable.putIfAbsent(message.source, newNeighbor);
			    } else {
			        neighbor.update(iface, (byte) 0, (byte) 1);
			    }
				
				//now see if our updated node has any information that is better than what we have:
				for (RIP.Datagram.Entry entry : message.data.entries){					
					checkAndAdd(entry, iface);
				}				

			}			

			// Print routing table
			if(printRipTable) {
				String toPrint = (name() + " - Check routing table:");
							
				for (KernelNode node : routingTable.values()) {
					toPrint += ("\n\t" + node);
				}
							
				Logger log = logger().getLogger("network.impl.kernel.KernelImpl");
				
				log.info(toPrint);
			}

		}
		
		/**
		 * checks to see if the information should be added to our routing table
		 * @param info the info to check (and add if needed)
		 */
		private void checkAndAdd(RIP.Datagram.Entry info, Interface iface){
			final KernelNode node = routingTable.get(info.destination);
			if (node == null) {
			    if (info.metric != Byte.MAX_VALUE && info.destination != address()) {
                    final KernelNode newNode =
                        new KernelNode(info.destination, iface, info.metric, (byte) 1);
                    routingTable.putIfAbsent(info.destination, newNode);
			    }
			} else {
			    node.update(iface, info.metric, (byte) 1);
			}
		}
	}
}
