package test;

import java.util.ArrayList;
import java.util.Random;

import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.impl.kernel.KernelImpl;

import org.testng.annotations.Test;

public class KernelImplTest extends AbstractFileTest {
    final Random random = new Random();    
    
    @Test
    public void test() {
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        ArrayList<Node> lst = new ArrayList<Node>();
        
        final Node a = sim.buildNode()
            .name("1")
            .kernel(new KernelImpl())
            .create();
//        
//        final Node b = sim.buildNode()
//            .name("B")
//            .kernel(new KernelImpl())
//            .connections(a)
//            .create();
//        
//        final Node c = sim.buildNode()
//        .name("C")
//        .kernel(new KernelImpl())
//        .connections(a)
//        .create();
        
        lst.add(a);
        
        Random r = new Random();
        
        for(int i = 1; i <= 4; i++) {
            final Node temp = sim.buildNode()
            .name("" + (i+1))
            .kernel(new KernelImpl())
            .connections(lst.get(r.nextInt(i))/*lst.get(i-1)*/)
            .create();
            lst.add(temp);
        }
        
        //((CountingKernel) a.kernel()).first = true;
        
        sim.start();
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            return;
        }
    }
    
    public static void main(String args[]) {
    	new KernelImplTest().test();
    }
}
