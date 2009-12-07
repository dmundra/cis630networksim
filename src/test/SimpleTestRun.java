package test;

import static org.testng.Assert.assertNotNull;
import network.AbstractKernel;
import network.Interface;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;

import org.testng.annotations.Test;

/**
 * First test we ran to check the underlying implementation
 * @author Luke Maurer
 *
 */
public class SimpleTestRun {
    private static class TrivialKernel extends AbstractKernel {
        public void interfaceAdded(Interface iface) { }
        public void interfaceConnected(Interface iface) { }
        public void interfaceDisconnected(Interface iface) { }
        
        public void start() { }
        public void shutDown() { }
    }
    
    @Test
    public void test1() {
        // Get a simulator
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        assertNotNull(sim);
        
        // Build a bunch of nodes
        final Node
            a = sim.buildNode().kernel(new TrivialKernel()).name("A").create(),
            b = sim.buildNode().kernel(new TrivialKernel()).name("B").create(),
            c = sim.buildNode().kernel(new TrivialKernel()).name("C").create(),
            d = sim.buildNode().kernel(new TrivialKernel()).name("D").create(),
            e = sim.buildNode().kernel(new TrivialKernel()).name("E").create(),
            f = sim.buildNode().kernel(new TrivialKernel()).name("F").create();
        
        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertNotNull(d);
        assertNotNull(e);
        assertNotNull(f);
        
        final Node router1 =
            sim.buildNode()
                .kernel(new TrivialKernel())
                .name("Router1")
                .connections(a, b, c)
                .create();
        
        assertNotNull(router1);
        
        final Node router2 =
            sim.buildNode()
                .kernel(new TrivialKernel())
                .name("Router2")
                .connections(router1, d, e, f)
                .create();
        
        assertNotNull(router2);
        
        // Now, um ... do something?
        for (Interface iface : router1.interfaces()) {
            System.out.println(iface);
            assertNotNull(iface);
        }
        for (Interface iface : router2.interfaces()) {
            System.out.println(iface);
            assertNotNull(iface);
        }
    }
    
    public static void main(String args[]) {
    	new SimpleTestRun().test1();
    }
}
