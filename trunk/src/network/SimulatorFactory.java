package network;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Entry point to the network simulator. Use {@link #instance()} to find an
 * implementation.
 * 
 * @see #instance()
 * @see #createSimulator()
 * @see java.util.ServiceLoader
 *
 * @author Luke Maurer
 */
public abstract class SimulatorFactory {
    private static final ServiceLoader<SimulatorFactory> serviceLoader =
        ServiceLoader.load(SimulatorFactory.class);
    
    public static SimulatorFactory instance() {
        final Iterator<SimulatorFactory> iter = serviceLoader.iterator();
        if (!iter.hasNext())
            throw new RuntimeException("No SimulatorFactory found in classpath");
        else
            return iter.next();
    }
    
    public abstract Simulator createSimulator();
}
