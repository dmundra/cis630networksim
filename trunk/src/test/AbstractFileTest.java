package test;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.BeforeSuite;

/**
 * Abstract base for tests, but uses generates log data into a file
 *
 * @author Daniel Mundra
 */
public abstract class AbstractFileTest {
    @BeforeSuite
    public void configureLogging() {
        final Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers())
            root.removeHandler(handler);
        try {
			root.addHandler(new TestFileHandler("KernelImplTest.log", false));
			root.setLevel(Level.ALL);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
