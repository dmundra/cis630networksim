package test;

import network.Interface;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;

public class SimpleTestRun {
    public static void main(String ... args) {
        // Get a simulator
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        // Build a bunch of nodes
        final Node
            a = sim.buildNode().name("A").create(),
            b = sim.buildNode().name("B").create(),
            c = sim.buildNode().name("C").create(),
            d = sim.buildNode().name("D").create(),
            e = sim.buildNode().name("E").create(),
            f = sim.buildNode().name("F").create();
        
        final Node router1 =
            sim.buildNode()
                .name("Router1")
                .connections(a, b, c)
                .create();
        
        final Node router2 =
            sim.buildNode()
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
