package test;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class TestFileHandler extends FileHandler {

	public TestFileHandler(String arg0, boolean arg1) throws IOException,
			SecurityException {
		super(arg0, arg1);
	}
	
	{		
        // Should be overridden by SimulatorImpl, but if we don't set it here
        // we might get an NPE if it isn't
        setFormatter(new SimpleFormatter());
	}
}
