package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import network.AbstractProcess;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.UserKernel;
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
    private static final int DEFAULT_GOAL = 100;
    private static final long TIMEOUT = 7000;
    
    abstract class TestCase {
        private CountDownLatch goalSignal;
        final Simulator sim =
            SimulatorFactory.instance().createSimulator();
        
        int goal() {
            return DEFAULT_GOAL;
        }
        
        int goalCount() {
            return 1;
        }
        
        int waitTimeMillis() {
            return 2000;
        }
        
        abstract void setup();
        
        final void run() throws Exception {
            setup();
            goalSignal = new CountDownLatch(goalCount());
            sim.start();
            goalSignal.await();
            sim.destroy();
        }
        
        int messageReportingInterval() {
            return 1;
        }
        
        Node counter(String name, int address, int peer, boolean start,
                Node ... neighbors) {
            return sim.buildNode(address)
                .name(name)
                .kernel(sim.createUserKernel(new CountingProcess(peer, start)))
                .connections(neighbors)
                .create();
        }
        
        Node router(String name, int address, Node ... neighbors) {
            return sim.buildNode(address)
                .name(name)
                .connections(neighbors)
                .create();
        }
        
        class CountingProcess extends AbstractProcess {
            private static final int PORT = 42;
            
            private final int peerAddr;
            private final boolean first;
            
            CountingProcess(int peerAddr, boolean first) {
                this.peerAddr = peerAddr;
                this.first = first;
            }
    
            private void send(int num) throws InterruptedException {
                if (num % messageReportingInterval() == 0)
                    os().logger().log(Level.INFO, "Sending: {0}", num);
                try {
                    os().send(peerAddr, PORT, PORT, num);
                } catch (DisconnectedException e) {
                    os().logger().info("Oops. I've been disconnected.");
                }
                
            }
            
            protected void run() throws InterruptedException {
            	Thread.sleep(waitTimeMillis());
            	
                if (first)
                    send(1);
                
                while (true) {
                    Message<Integer> message =
                        os().receive(PORT).asType(Integer.class);
                    
                    final int num = message.data;
                    if (num % messageReportingInterval() == 0)
                        os().logger().log(Level.INFO, "Received: {0}", num);
                    if (num == goal()) {
                        os().logger().info("Reached goal; signaling");
                        goalSignal.countDown();
                    }
                    
                    send(num + 1);
                    
                    if (num >= goal())
                        break;
                }
            }
        }
    }

    @Test(description="Test with two nodes directly connected",
            timeOut=TIMEOUT)
    public void test() throws Exception {
        new TestCase() {
            void setup() {
                final Node a = counter("A", 1, 2, true);
                final Node b = counter("B", 2, 1, false, a);
            }
        }.run();
    }
    
    @Test(description="Test with three nodes connected by a router",
            timeOut=TIMEOUT)
    public void test3Nodes() throws Exception {
        new TestCase() {
            void setup() {
                final Node a = counter("A", 1, 2, true);
                final Node b = counter("B", 2, 3, false);
                final Node c = counter("C", 3, 1, false);
                
                final Node router = router("Router", 4, a, b, c);        
            }
        }.run();
    }
    
    @Test(description="Test with 5 nodes connected by two routers",
            timeOut=TIMEOUT*2)
    public void test5Nodes() throws Exception {
        new TestCase() {
            int waitTimeMillis() {
                return 3000;
            }
            
            void setup() {
                final Node a = counter("A", 1, 2, true);
                final Node b = counter("B", 2, 3, false);
                final Node c = counter("C", 3, 4, false);
                final Node d = counter("D", 4, 5, false);
                final Node e = counter("E", 5, 1, false);
                
                final Node router1 = router("Router 1", 6, a, b, c);
                final Node router2 = router("Router 2", 7, router1, d, e);        
            }
        }.run();
    }
    
    private static int[][] partitionAndPermute(
            int from, int count, int ... sizes) {
        {
            int sum = 0;
            for (int size : sizes)
                sum += size;
            Assert.assertEquals(count, sum, "Total partition size");
        }
        
        final int[] nums = new int[count];
        for (int n = from, ix = 0; ix < count; ix++, n++)
            nums[ix] = n;
        
        Collections.shuffle(Arrays.asList(nums), new Random(0));
        
        final int[][] ans = new int[sizes.length][];
        for (int partIx = 0, ix = 0; partIx < sizes.length; partIx++) {
            ans[partIx] = Arrays.copyOfRange(nums, ix, ix + sizes[partIx]);
            ix += sizes[partIx];
        }
        
        // Make sure this works ...
        final BitSet present = new BitSet();
        for (int[] part : ans)
            for (int n : part)
                present.set(n);
        
        for (int n = from; n < from + count; n++)
            assert present.get(n);
        
        return ans;
    }
    
    @Test(description="Test with 18 nodes, in three groups, connected by 7 routers",
            timeOut=TIMEOUT*4)
    public void test18Nodes() throws Exception {
        new TestCase() {
            Node[] nodes(int ... addrs) {
                final Node[] ans = new Node[addrs.length];
                for (int ix = 0; ix < addrs.length; ix++)
                    ans[ix] = sim.nodeAt(addrs[ix]);
                return ans;
            }
            
            int goalCount() {
                return 3;
            }
            
            int waitTimeMillis() {
                return 10000;
            }
            
            void setup() {
                // Scramble the numbers from 1 to 18 and split them into three
                // partitions of sizes 5, 6, and 7
                final int groups[][] = partitionAndPermute(1, 18, 5, 6, 7);
                
                for (int[] group : groups) {
                    for (int ix = 0; ix < group.length; ix++) {
                        final int addr = group[ix];
                        final int peerAddr = group[(ix + 1) % group.length];
                        
                        counter(String.valueOf((char) ('A' + (addr - 1))),
                                addr, peerAddr, ix == 0);
                    }
                }
                
                router("Router 1", 19, nodes(1, 2, 3, 4));
                router("Router 2", 20, nodes(5, 6, 7, 8, 9));
                router("Router 3", 21, nodes(19, 20));
                router("Router 4", 22, nodes(21));
                router("Router 5", 23, nodes(10, 11, 22));
                router("Router 6", 24, nodes(12, 13, 14, 15, 22));
                router("Router 7", 25, nodes(16, 17, 18, 22));
            }
        }.run();
    }
    
    // The following test is insane. It'll start about 250 threads.
//    @Test(description="Test with randomly generated nodes",
//            timeOut=TIMEOUT*4*0)
    public void testRandomNodes() throws Exception {
        new TestCase() {
            private static final int NODE_COUNT = 100;
            private static final int GROUP_SIZE = 6;
            private static final long SEED = 0L;
            
            private final Random random = new Random(SEED);
            private volatile int groupCount = -1;
            
            int waitTimeMillis() {
                return 20000;
            };
            
            int goalCount() {
                return groupCount;
            }
            
            void setup() {
                // Generate a random graph, starting with a simple graph of
                // four nodes with a hub in the middle
                
                final Graph graph = new Graph(new BitSet[0], NODE_COUNT);
                graph.addNode();
                graph.addNode(0);
                graph.addNode(0);
                graph.addNode(0);
                
                graph.growTo(NODE_COUNT, random);
                
                final BitSet[] lowerAdjacencies = graph.lowerAdjacencies();
                
                int counterNumber = 1, routerNumber = 1, ix = 0;
                
                final Node[] nodes = new Node[NODE_COUNT];
                final List<Node> counters =
                    new ArrayList<Node>();
                final List<String> edges = new ArrayList<String>();
                
                for (BitSet ixsToConnect : lowerAdjacencies) {
                    final Simulator.NodeBuilder builder =
                        sim.buildNode();
                    
                    for (int neighborIx = ixsToConnect.nextSetBit(0);
                            neighborIx != -1;
                            neighborIx = ixsToConnect.nextSetBit(neighborIx + 1)) {
                        builder.connections(nodes[neighborIx]);
                        edges.add((ix + 1) + " -> " + (neighborIx + 1));
                    }
                    
                    final boolean isRouter = graph.degree(ix) > 1;
                    
                    if (isRouter)
                        builder.name("Router " + routerNumber++);
                    else
                        builder.name("Counter " + counterNumber++).kernel(sim.createUserKernel());
                    
                    final Node node = builder.create();
                    
                    if (!isRouter)
                        counters.add(node);
                    
                    nodes[ix++] = node;
                }
                
                sim.logger().info(edges.toString());
                
                final int counterCount = counters.size();
                for (int counterIx = 0; counterIx < counterCount; counterIx++) {
                    final Node node = counters.get(counterIx);
                    final Node peer;
                    if ((counterIx + 1) % GROUP_SIZE == 0)
                        peer = counters.get((counterIx + 1) - GROUP_SIZE);
                    else if (counterIx + 1 == counterCount)
                        peer = counters.get(
                                (counterIx + 1) - (counterIx + 1) % GROUP_SIZE);
                    else
                        peer = counters.get(counterIx + 1);
                    
                    ((UserKernel) node.kernel()).setProcess(
                            new CountingProcess(peer.address(), counterIx % GROUP_SIZE == 1));
                }
                
                groupCount = counterCount / GROUP_SIZE +
                    (counterCount % GROUP_SIZE == 0 ? 0 : 1);
            };
        }.run();
    }
}
