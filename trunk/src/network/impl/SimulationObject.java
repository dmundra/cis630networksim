package network.impl;

import network.Interface;
import network.Node;

/**
 * Convenient base class holding each object's back pointer to the simulator,
 * and supporting the {@link SimulatorImpl#checkOwnership(Object)} method.
 * 
 * @param <I> The interface (such as {@link Node} or {@link Interface})
 * implemented by this class.
 *
 * @author Luke Maurer
 */
abstract class SimulationObject<I> {
    final SimulatorImpl sim;
    
    SimulationObject(SimulatorImpl sim) {
        this.sim = sim;
    }
    
    @Override
    // This way I won't forget to write toString() methods :-)
    public abstract String toString();
}
