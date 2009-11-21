package test;

import java.util.logging.Logger;

import network.KnownPort;
import network.Message;
import network.Node;
import network.OperatingSystem;
import network.Process;
import network.Simulator;
import network.SimulatorFactory;
import network.impl.UserKernelImpl;
import network.impl.kernel.KernelImpl;

import org.testng.annotations.Test;

public class KernelUserTest extends AbstractFileTest {

    @Test
    public void test() {
    	System.out.println("KernelUserTest Running: Check log for output");
    	
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        UserKernelImpl userkernel1 = new UserKernelImpl();
        UserKernelImpl userkernel2 = new UserKernelImpl();
        
        final Node router = sim.buildNode()
        .name("Router")
        .kernel(new KernelImpl())
        .create();
        
        final Node user1 = sim.buildNode()
        .name("A")
        .kernel(userkernel1)
        .connections(router)
        .create();
        
        final Node user2 = sim.buildNode()
        .name("B")
        .kernel(userkernel2)
        .connections(router)
        .create();
                
        sim.start();        
        
        // Sleep for 5 seconds so that the simulation can get started and
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            return;
        }
                  
        userkernel1.setProcess(new Process() {
			
			@Override
			public void run(OperatingSystem os) throws InterruptedException {			
				Logger log = os.logger().getLogger("test.KernelUserTest");

				Message m = os.receive(KnownPort.KERNEL_WHO.ordinal());
				log.info("Got a message: " + m);
				
				os.send(new Message<String>(os.address(), m.source, KnownPort.KERNEL_WHO.ordinal(), KnownPort.KERNEL_WHO.ordinal(), "hi: " + os.address()));
				
			}
			
		});
        
        userkernel2.setProcess(new Process() {
			
			@Override
			public void run(OperatingSystem os) throws InterruptedException {
				Logger log = os.logger().getLogger("test.KernelUserTest");
				
				Message m = os.receive(KnownPort.KERNEL_WHO.ordinal());
				log.info("Got a message: " + m);
				
				os.send(new Message<String>(os.address(), m.source, KnownPort.KERNEL_WHO.ordinal(), KnownPort.KERNEL_WHO.ordinal(), "hi: " + os.address()));
				
			}
			
		});
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            return;
        }            
    }
}
