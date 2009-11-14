package test;

import java.util.Random;

import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.impl.kernel.KernelImpl;

import org.testng.annotations.Test;

public class KernelImplTest {
    final Random random = new Random();
    
    
    
    @Test
    public void test() {
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        final Node a = sim.buildNode()
            .name("A")
            .kernel(new KernelImpl())
            .create();
        
        final Node b = sim.buildNode()
            .name("B")
            .kernel(new KernelImpl())
            .connections(a)
            .create();
        
        //((CountingKernel) a.kernel()).first = true;
        
        sim.start();
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            return;
        }
    }
}
