package test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import network.AbstractKernel;
import network.Interface;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.Interface.DisconnectedException;

public class CountingTest {
	final Random random = new Random();

	private final class CountingKernel extends AbstractKernel {
		private boolean first;
		Interface iface;

		public void start() throws Exception {
			try {
				iface = interfaces().get(0);
			} catch (IndexOutOfBoundsException e) {
				logger().warning("No interfaces; exiting immediately");
				return;
			}

			while (!Thread.interrupted()) {
				if (first) {
					send(0);
					first = false;
					continue;
				}

				final Integer received = receive();
				Thread.sleep(500);
				send(received == null ? 0 : received + 1);
			}
		}

		private void send(int n) throws DisconnectedException,
				InterruptedException {
			logger().info("Sending " + n);
			iface.send(new Message<Integer>(0, 0, 0, 0, n));
		}

		private Integer receive() throws InterruptedException {
			final Message<?> msg = iface.receive(random.nextInt(500) + 500,
					TimeUnit.MILLISECONDS);

			if (msg == null) {
				logger().info("Timed out");
				return null;
			} else {
				final Integer ans = msg.dataAs(Integer.class);
				logger().info("Receiving " + ans);
				return ans;
			}
		}

		public void shutDown() {
			mainThread().interrupt();
		}
	}

	@Test
	public void test() {
		final Simulator sim = SimulatorFactory.instance().createSimulator();

		final Node a = sim.buildNode().name("A").kernel(new CountingKernel())
				.create();

		final Node b = sim.buildNode().name("B").kernel(new CountingKernel())
				.connections(a).create();

		((CountingKernel) a.kernel()).first = true;

		sim.start();

		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			return;
		}
	}
}
