package test;

import java.util.logging.Logger;

import network.KnownPort;
import network.Message;
import network.Node;
import network.OperatingSystem;
import network.Process;
import network.Simulator;
import network.SimulatorFactory;
import network.impl.kernel.KernelImpl;
import network.impl.UserKernelImpl;

import org.testng.annotations.Test;

public class KernelUserTest extends AbstractFileTest {

    @Test
    public void test() {
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
        
        userkernel1.setProcess(new Process() {
			
			@Override
			public void run(OperatingSystem os) throws InterruptedException {
				
				System.out.println("Test Process 1");	
				
				Message m = os.receive(KnownPort.KERNEL_WHO.ordinal());
				System.out.println("got a message: " + m);
				
				os.send(new Message<String>(os.address(), m.source, KnownPort.KERNEL_WHO.ordinal(), KnownPort.KERNEL_WHO.ordinal(), "hi: " + os.address()));
				
				//int sourcePort = os.receive().sourcePort;
				
				//System.out.println("SourcePort: " + (sourcePort == KnownPort.KERNEL_WHO.ordinal()));
			}
			
		});
        
//    	userkernel1.setProcess(new Process() {
//			
//			@Override
//			public void run(OperatingSystem os) throws InterruptedException {
//				System.out.println("Test Process 2");	
//				//int sourcePort = os.receive().sourcePort;
//				
//				//System.out.println("SourcePort: " + (sourcePort == KnownPort.KERNEL_WHO.ordinal()));
//			}
//			
//		});
        
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            return;
        }      
        
    }

}
