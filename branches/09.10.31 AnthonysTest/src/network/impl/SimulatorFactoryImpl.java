package network.impl;

import network.Simulator;
import network.SimulatorFactory;

public class SimulatorFactoryImpl extends SimulatorFactory {
    public Simulator createSimulator() {
        return new SimulatorImpl();
    }
}
