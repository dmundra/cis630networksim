package network;

public abstract class AbstractProcess implements Process {
    private OperatingSystem os;
    
    public final void run(OperatingSystem os) throws InterruptedException {
        this.os = os;
        run();
    }
    
    protected final OperatingSystem os() {
        return os;
    }
    
    protected abstract void run() throws InterruptedException;
}
