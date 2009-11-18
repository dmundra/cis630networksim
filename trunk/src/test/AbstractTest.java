package test;

import java.util.logging.Handler;
import java.util.logging.Logger;

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
    }
}
