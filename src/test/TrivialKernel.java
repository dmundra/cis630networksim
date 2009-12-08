package test;

import network.AbstractKernel;
import network.Interface;

class TrivialKernel extends AbstractKernel {
    public void interfaceAdded(Interface iface) { }
    public void interfaceConnected(Interface iface) { }
    public void interfaceDisconnected(Interface iface) { }
    
    public void start() { }
    public void shutDown() { }
}