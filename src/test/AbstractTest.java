package test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import network.Simulator;
import network.SimulatorFactory;

import org.testng.annotations.BeforeSuite;

/**
 * Abstract base for tests, including general-purpose setup methods.
 *
 * @author Luke Maurer
 */
public abstract class AbstractTest {
    @BeforeSuite
    public void configureLogging() {
        final Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers())
            root.removeHandler(handler);
        root.addHandler(new TestNGLogHandler());
        Logger.getLogger("network").setLevel(Level.INFO);
    }
    
    private Simulator simulator;
    protected synchronized Simulator simulator() {
        if (simulator == null)
            simulator = SimulatorFactory.instance().createSimulator();
        return simulator;
    }
    
    protected Simulator sim() {
        return simulator();
    }
}
