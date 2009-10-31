package test;

import network.Interface;
import network.Kernel;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;

class SimpleTestRun {
    private static class TrivialKernel implements Kernel {
        public void interfaceAdded(Interface iface) { }
        public void interfaceConnected(Interface iface) { }
        public void interfaceDisconnected(Interface iface) { }
        
        public void start() { }
        public void shutDown() { }
    }
    
    public static void main(String ... args) {
        // Get a simulator
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        // Build a bunch of nodes
        final Node
            a = sim.buildNode().kernel(new TrivialKernel()).name("A").create(),
            b = sim.buildNode().kernel(new TrivialKernel()).name("B").create(),
            c = sim.buildNode().kernel(new TrivialKernel()).name("C").create(),
            d = sim.buildNode().kernel(new TrivialKernel()).name("D").create(),
            e = sim.buildNode().kernel(new TrivialKernel()).name("E").create(),
            f = sim.buildNode().kernel(new TrivialKernel()).name("F").create();
        
        final Node router1 =
            sim.buildNode()
                .kernel(new TrivialKernel())
                .name("Router1")
                .connections(a, b, c)
                .create();
        
        final Node router2 =
            sim.buildNode()
                .kernel(new TrivialKernel())
                .name("Router2")
                .connections(router1, d, e, f)
                .create();
        
        // Now, um ... do something?
        for (Interface iface : router1.interfaces())
            System.out.println(iface);
        for (Interface iface : router2.interfaces())
            System.out.println(iface);
    }
}
