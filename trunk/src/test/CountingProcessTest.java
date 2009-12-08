package test;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.testng.annotations.Test;

import network.AbstractProcess;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.OperatingSystem.DisconnectedException;

/**
 * Counting test that simulates send and received messages between nodes using
 * processes and nodes being connected to routers.
 * 
 * @author Luke Maurer
 * 
 */
public class CountingProcessTest extends AbstractTest {
	private static final int GOAL = 100;
	private static final long TIMEOUT = 0;

	private volatile CountDownLatch goalSignal;

	private class CountingProcess extends AbstractProcess {
		private static final int PORT = 42;

		private final int peerAddr;
		private final boolean first;

		CountingProcess(int peerAddr, boolean first) {
			this.peerAddr = peerAddr;
			this.first = first;
		}

		private void send(int num) throws InterruptedException {
			os().logger().log(Level.INFO, "Sending: {0}", num);
			try {
				os().send(peerAddr, PORT, PORT, num);
			} catch (DisconnectedException e) {
				os().logger().info("Oops. I've been disconnected.");
			}

		}

		protected void run() throws InterruptedException {
			Thread.sleep(3000);

			if (first)
				send(1);

			while (true) {
				Message<Integer> message = os().receive(PORT).asType(
						Integer.class);

				final int num = message.data;
				os().logger().log(Level.INFO, "Received: {0}", num);
				if (num == GOAL) {
					os().logger().info("Reached goal; signaling");
					goalSignal.countDown();
				}

				send(num + 1);
			}
		}
	}

	// @Test(description="Test with two nodes directly connected",
	// timeOut=TIMEOUT)
	public void test() throws InterruptedException {
		goalSignal = new CountDownLatch(1);

		final Simulator sim = SimulatorFactory.instance().createSimulator();

		final Node a = sim.buildNode(1).name("A").kernel(
				sim.createUserKernel(new CountingProcess(2, true))).create();

		final Node b = sim.buildNode(2).name("B").kernel(
				sim.createUserKernel(new CountingProcess(1, false)))
				.connections(a).create();

		sim.start();
		goalSignal.await();

		sim.destroy(a);
		sim.destroy(b);
	}

	// @Test(description="Test with three nodes connected by a router",
	// timeOut=TIMEOUT)
	public void test3Nodes() throws InterruptedException {
		goalSignal = new CountDownLatch(1);

		final Simulator sim = SimulatorFactory.instance().createSimulator();

		final Node a = sim.buildNode(1).name("A").kernel(
				sim.createUserKernel(new CountingProcess(2, true))).create();

		final Node b = sim.buildNode(2).name("B").kernel(
				sim.createUserKernel(new CountingProcess(3, false))).create();

		final Node c = sim.buildNode(3).name("C").kernel(
				sim.createUserKernel(new CountingProcess(1, false))).create();

		final Node router = sim.buildNode().name("Router").connections(a, b, c)
				.create();

		sim.start();
		goalSignal.await();

		sim.destroy(a);
		sim.destroy(b);
		sim.destroy(c);
		sim.destroy(router);
	}

	@Test(description = "Test with 5 nodes connected by two routers", timeOut = TIMEOUT)
	public void test5Nodes() throws InterruptedException {
		goalSignal = new CountDownLatch(1);

		final Simulator sim = SimulatorFactory.instance().createSimulator();

		final Node a = sim.buildNode(1).name("A").kernel(
				sim.createUserKernel(new CountingProcess(2, true))).create();

		final Node b = sim.buildNode(2).name("B").kernel(
				sim.createUserKernel(new CountingProcess(3, false))).create();

		final Node c = sim.buildNode(3).name("C").kernel(
				sim.createUserKernel(new CountingProcess(4, false))).create();

		final Node d = sim.buildNode(4).name("D").kernel(
				sim.createUserKernel(new CountingProcess(5, false))).create();

		final Node e = sim.buildNode(5).name("E").kernel(
				sim.createUserKernel(new CountingProcess(1, false))).create();

		final Node router1 = sim.buildNode().name("Router1").connections(a, b,
				c).create();

		@SuppressWarnings("unused")
		final Node router2 = sim.buildNode().name("Router2").connections(
				router1, d, e).create();

		sim.start();
		goalSignal.await();

		sim.destroy();
	}
}