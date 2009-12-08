package test;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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
@Test(sequential=true)
@SuppressWarnings("unused")
public class CountingProcessTest extends AbstractTest {
    private static final int GOAL = 100;
    private static final long TIMEOUT = 7000;
    
    private volatile CountDownLatch goalSignal;
    private volatile Simulator sim;
    
    private class CountingProcess extends AbstractProcess {
        private static final int WAIT = 4000;

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
        	Thread.sleep(WAIT);
        	
            if (first)
                send(1);
            
            while (true) {
                Message<Integer> message =
                    os().receive(PORT).asType(Integer.class);
                
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

    @BeforeMethod
    public void setup() {
        sim = SimulatorFactory.instance().createSimulator();
        goalSignal = new CountDownLatch(1);
    }
    
    @AfterMethod
    public void shutDown() {
        sim.destroy();
        sim = null;
        goalSignal = null;
    }
    
    @Test(description="Test with two nodes directly connected",
            timeOut=TIMEOUT)
    public void test() throws InterruptedException {
        final Node a = counter("A", 1, 2, true);
        final Node b = counter("B", 2, 1, false, a);
        
        sim.start();
        goalSignal.await();
    }
    
    @Test(description="Test with three nodes connected by a router",
            timeOut=TIMEOUT)
    public void test3Nodes() throws InterruptedException {
        final Node a = counter("A", 1, 2, true);
        final Node b = counter("B", 2, 3, false);
        final Node c = counter("C", 3, 1, false);
        
        final Node router = router("Router", 4, a, b, c);
        
        sim.start();
        goalSignal.await();
    }
    
    @Test(description="Test with 5 nodes connected by two routers",
            timeOut=TIMEOUT*2)
    public void test5Nodes() throws InterruptedException {
        final Node a = counter("A", 1, 2, true);
        final Node b = counter("B", 2, 3, false);
        final Node c = counter("C", 3, 4, false);
        final Node d = counter("D", 4, 5, false);
        final Node e = counter("E", 5, 1, false);
        
        final Node router1 = router("Router 1", 6, a, b, c);
        final Node router2 = router("Router 2", 7, router1, d, e);
        
        sim.start();
        goalSignal.await();
    }
    
    private Node counter(String name, int address, int peer, boolean start,
            Node ... neighbors) {
        return sim.buildNode(address)
            .name(name)
            .kernel(sim.createUserKernel(new CountingProcess(peer, start)))
            .connections(neighbors)
            .create();
    }
    
    private Node router(String name, int address, Node ... neighbors) {
        return sim.buildNode(address)
            .name(name)
            .connections(neighbors)
            .create();
    }
}
