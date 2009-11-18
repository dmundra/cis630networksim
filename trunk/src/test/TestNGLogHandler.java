package test;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.testng.Reporter;

/**
 * Simple log handler which simply shunts off each log record to TestNG's
 * primitive logger.
 *
 * @author Luke Maurer
 */
public class TestNGLogHandler extends Handler {
    {
        // Should be overridden by SimulatorImpl, but if we don't set it here
        // we might get an NPE if it isn't
        setFormatter(new SimpleFormatter());
    }
    
    public void close() { }
    
    public void flush() { }
    
    public void publish(LogRecord record) {
        Reporter.log(getFormatter().format(record), true);
    }
    
}
