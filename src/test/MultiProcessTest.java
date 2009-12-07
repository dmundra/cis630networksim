package test;

import java.util.concurrent.TimeUnit;

import network.AbstractProcess;
import network.Node;
import network.software.MultiProcess;

import org.testng.annotations.Test;

public class MultiProcessTest extends AbstractTest {
    private static final int PROCESS_COUNT = 5;
    
    @Test
    public void test() throws Exception {
        final MultiProcess process = new MultiProcess();
        
        for (int ix = 0; ix < PROCESS_COUNT; ix++) {
            final int num = ix + 1;
            process.add(new AbstractProcess() {
                protected void run() throws InterruptedException {
                    os().logger().info("Running process " + num);
                    
                    try {
                        synchronized (this) {
                            while (true)
                                wait();
                        }
                    } catch (InterruptedException e) { }
                    
                    os().logger().info("Leaving subprocess " + num);
                }
            });
        }
        
		final Node a = sim().buildNode().name("A").kernel(sim().createUserKernel(process)).create();
		@SuppressWarnings("unused")
		final Node router1 = sim().buildNode().name("Router1").connections(a).create();
        
        sim().start();
        
        TimeUnit.SECONDS.sleep(1);
        
        sim().destroy();
    }
}
