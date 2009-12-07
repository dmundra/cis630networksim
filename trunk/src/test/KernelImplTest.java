package test;

import java.util.ArrayList;
import java.util.Random;

import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.impl.kernel.KernelImpl;

import org.testng.annotations.Test;

/**
 * Stress program to test 50 routers connected to each other randomly.
 * The program will test the RIP algorithm that is executed by each router.
 * @author Daniel Mundra
 *
 */
public class KernelImplTest extends AbstractTest {
    final Random random = new Random();   
    final int TEST_NODES = 50;
    
    @Test
    public void test() {
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        ArrayList<Node> lst = new ArrayList<Node>();
        KernelImpl.printRipTable = true;
        
        final Node a = sim.buildNode()
            .name("1")
            .kernel(new KernelImpl())
            .create();
        
        lst.add(a);
        
        Random r = new Random();
        
        for(int i = 1; i <= TEST_NODES; i++) {
            final Node temp = sim.buildNode()
            .name("" + (i+1))
            .kernel(new KernelImpl())
            .connections(lst.get(r.nextInt(i))/*lst.get(i-1)*/)
            .create();
            lst.add(temp);
        }
        
        sim.start();
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            return;
        }
    }
}
