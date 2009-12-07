/**
 * 
 */
package test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import network.AbstractProcess;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.OperatingSystem.DisconnectedException;

import org.testng.annotations.Test;

/**
 * Test for a two phase protocol system in a token ring system
 * 
 * @author Daniel Mundra
 * 
 */
public class TokenRingTwoPhaseTest extends AbstractTest {
	private static final long TIMEOUT = 0;
    private static final Random rand = new Random();
    private static int successCounts;
	private static int counter;

	private volatile CountDownLatch goalSignal;

	/**
	 * Token Ring Two Phase Process that will send and receive tokens
	 * @author Daniel Mundra
	 *
	 */
	private class TokenRingTwoPhaseProcess extends AbstractProcess {
		private static final int PORT = 35;			
		// Counter to run the token toCommits a number of times
		private static final int GOAL = 5;
		private static final int ACCEPT_COMMIT = 10;
		private static final int GRAB_TOKEN = 8;
		private final int peerAddr;
		private boolean iWantToCommit;

		/**
		 * Create process with peer address and whether it is first
		 * @param peerAddr - address of the next node in the ring
		 * @param first - whether you are the first one in the token
		 */
		TokenRingTwoPhaseProcess(int peerAddr, boolean iWantToCommit) {
			this.peerAddr = peerAddr;
			this.iWantToCommit = iWantToCommit;
		}

		/**
		 * Send token and msg with it
		 * @param token - token to be sent
		 * @throws InterruptedException
		 */
		private void send(RTuple token) throws InterruptedException {
	
			try {
				os().send(peerAddr, PORT, PORT, token);
			} catch (DisconnectedException e) {
				os().logger().info("Oops. I've been disconnected.");
			}
		}

		/**
		 * Send Accept token to agree to commit or abort
		 * @param token - token to be sent
		 * @param accept - agree or disagree to a commit or abort
		 * @throws InterruptedException
		 */
		private void sendAccept(RTuple token, boolean accept) throws InterruptedException {

			token.accepts.add(accept);
			
			try {
				os().send(peerAddr, PORT, PORT, token);
			} catch (DisconnectedException e) {
				os().logger().info("Oops. I've been disconnected.");
			}
		}

		/**
		 * Run method thats runs the token ring two phase process
		 */
		protected void run() throws InterruptedException {
			Thread.sleep(5000);
			
			// If you are the first you will send the commit request
			if (iWantToCommit) {
				os().logger().log(Level.INFO, "Message: {0}", "I " + os().address() + " Want To Commit");
				RTuple token = RTuple.get(os().address(), true, false, false);
				send(token);
			}

			while (true) {
				// Receive token
				Message<RTuple> message = os().receive(PORT).asType(
						RTuple.class);

				final RTuple token = message.data;
				
                // Randomly sleep
				if (!iWantToCommit) {
                    Thread.sleep(1000 * rand.nextInt(4));
                }
				
				// If you are first that means you send the commit request
				// If any one aborted you will abort and send to everyone that you have aborted
				// If every one accepted then you send a token of success to everyone
				// If you have got a acknowledgment of success you are done
				// If you have got a acknowledgment of abort you are done
				if(iWantToCommit) {					
					if(token.accepts.contains(false)) {
						os().logger().log(Level.INFO, "Someone disagreed, abort: {0}", token);
						RTuple newToken = RTuple.get(os().address(), false, false, false);
						send(newToken);
					} else {
						if(token.toCommit) {
							os().logger().log(Level.INFO, "Everyone agreed, success: {0}", token);
							RTuple newToken = RTuple.get(os().address(), false, true, false);
							send(newToken);
						} else if(token.success) {
							successCounts++;
							counter++;
							
							os().logger().log(Level.INFO, "Message: {0}", "Successfully committed");							
							
							if(counter>=GOAL) {
								os().logger().info("Reached goal; signaling");
								goalSignal.countDown();
							} else {
								os().logger().info("Token is up for grabs");
								this.iWantToCommit = false;
								RTuple newToken = RTuple.get(os().address(), false, false, true);
								send(newToken);
							}
						} else {
							counter++;
							os().logger().log(Level.INFO, "Message: {0}", "Aborted!");	
							os().logger().info("Reached goal; signaling");
							
							if(counter>=GOAL) {
								os().logger().info("Reached goal; signaling");
								goalSignal.countDown();
							} else {
								os().logger().info("Token is up for grabs");
								this.iWantToCommit = false;
								RTuple newToken = RTuple.get(os().address(), false, false, true);
								send(newToken);							
							}
						}
					}
				// If you are not first then you have to accept the commit and send an agreement
				// or send an acknowledgment to success or abort
				} else {
					if(token.toCommit) {
						if(ACCEPT_COMMIT <= (rand.nextInt(10)+1)) {
							sendAccept(token, false);
						} else {
							sendAccept(token, true);
						}
					} else if(token.success) {
						os().logger().log(Level.INFO, "Message: {0}", "Good for you, releasing locks");
						send(token);
					} else if(token.upForGrabs) {
						if(GRAB_TOKEN <= (rand.nextInt(10)+1)) {
							os().logger().log(Level.INFO, "Message: {0}", "I " + os().address() + " Want To Commit");
							this.iWantToCommit = true;
							RTuple newToken = RTuple.get(os().address(), true, false, false);
							send(newToken);
						} else {
							os().logger().info("Token is up for grabs");
							send(token);
						}
					} else {
						os().logger().log(Level.INFO, "Message: {0}", "Oh " + token.address + " aborted. Continue on!");
						send(token);
					}
				}
			}
		}
	}

	@Test(description = "Test with 5 nodes connected by two routers", timeOut = TIMEOUT)
	public void test5Nodes() throws InterruptedException {
		goalSignal = new CountDownLatch(1);
		counter = 0;

		final Simulator sim = SimulatorFactory.instance().createSimulator();

		final Node a = sim.buildNode(1).name("A").kernel(
				sim.createUserKernel(new TokenRingTwoPhaseProcess(2, true))).create();

		final Node b = sim.buildNode(2).name("B").kernel(
				sim.createUserKernel(new TokenRingTwoPhaseProcess(3, false))).create();

		final Node c = sim.buildNode(3).name("C").kernel(
				sim.createUserKernel(new TokenRingTwoPhaseProcess(4, false))).create();

		final Node d = sim.buildNode(4).name("D").kernel(
				sim.createUserKernel(new TokenRingTwoPhaseProcess(5, false))).create();

		final Node e = sim.buildNode(5).name("E").kernel(
				sim.createUserKernel(new TokenRingTwoPhaseProcess(1, false))).create();

		final Node router1 = sim.buildNode().name("Router1").connections(a, b,
				c).create();

		@SuppressWarnings("unused")
		final Node router2 = sim.buildNode().name("Router2").connections(
				router1, d, e).create();

		sim.start();
		goalSignal.await();
		
		sim.logger().info("Number of two phase successes: " + successCounts);

		sim.destroy();
	}
	
	/**
	 * Class that will contain who wants to commit and who has accepted it
	 * @author Daniel Mundra
	 *
	 */
    public static class RTuple implements Serializable {

		private static final long serialVersionUID = 4763254L;
		
		int address;
        boolean toCommit;
        boolean success;
        boolean upForGrabs;
        ArrayList<Boolean> accepts; 

        public static RTuple get(int address, boolean toCommit, boolean success, boolean upForGrabs) {
            RTuple ret = new RTuple();
            ret.address = address;
            ret.toCommit = toCommit;
            ret.success = success;
            ret.upForGrabs = upForGrabs;
            ret.accepts = new ArrayList<Boolean>();
            return ret;
        }

        public String toString() {
            return "address:" + address + " Commit:" + toCommit + " Success:" + success + " UpForGrabs:" + upForGrabs + " Accepts:" + accepts;
        }
    }
}
