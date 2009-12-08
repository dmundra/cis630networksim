package test;

import network.Interface;
import network.Message;
import network.Node;
import network.Simulator;
import network.SimulatorFactory;
import network.protocols.HTTP;
import network.software.HTTPServer;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Simulates a HTTP server client model.
 * @author Luke Maurer
 *
 */
public class HTTPServerTest extends AbstractTest {
    private static final String LOCATION = "/index.html", CONTENT = "Hello world!";
    
    @Test
    public void test() throws Exception {
        final Simulator sim = SimulatorFactory.instance().createSimulator();
        
        final HTTPServer service = new HTTPServer();
        
        final Node server = sim.buildNode(1)
            .name("Server")
            .kernel(sim.createUserKernel(service))
            .create();
        
        final Node client = sim.buildNode(2)
            .name("Client")
            .kernel(new TrivialKernel())
            .connections(server)
            .create();
        
        service.addFile(LOCATION, CONTENT);
        
        sim.start();
        
        final Interface iface = client.interfaces().get(0);
        
        iface.send(new Message<HTTP.Request>(1, 2, -1, HTTP.PORT.number(),
                new HTTP.Request(HTTP.Request.Method.GET, LOCATION)));
        
        final HTTP.Response response =
            iface.receive().dataAs(HTTP.Response.class);
        
        Assert.assertEquals(response.status, HTTP.Response.Status.OK);
        Assert.assertEquals(response.content, CONTENT);
    }
}
