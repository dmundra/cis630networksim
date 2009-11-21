package network;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public abstract class AbstractKernel implements Kernel {
    private int address;
    private String name;
    private Logger logger;
    
    private final CopyOnWriteArrayList<Interface> ifaces =
        new CopyOnWriteArrayList<Interface>();
    
    private volatile Thread mainThread;
    
    public final void setAddress(int address) {
        if (this.address != 0)
            throw new IllegalStateException("Can only set the address once");
        
        this.address = address; 
    }
    
    public final int address() {
        return address;
    }
    
    public final void setName(String name) {
        if (this.name != null)
            throw new IllegalStateException("Can only set the name once");
        
        this.name = name;
    }
    
    protected final String name() {
        return name;
    }
    
    public final void setLogger(Logger logger) {
        if (this.logger != null)
            throw new IllegalStateException("Can only set the logger once");
        
        this.logger = logger;
    }
    
    protected final Logger logger() {
        return logger;
    }
    
    public final void setMainThread(Thread mainThread) {
        this.mainThread = mainThread;
    }
    
    protected final Thread mainThread() {
        return mainThread;
    }
    
    public void interfaceAdded(Interface iface) {
        ifaces.add(iface);
    }
    
    public void interfaceConnected(Interface iface) {
        // Do nothing by default
    }
    
    public void interfaceDisconnected(Interface iface) {
        // Do nothing by default
    }
    
    protected CopyOnWriteArrayList<Interface> interfaces() {
        return ifaces;
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s @ %x]", this.getClass().getSimpleName(),
                name, address);
    }
}
