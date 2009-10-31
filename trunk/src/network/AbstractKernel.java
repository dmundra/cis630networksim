package network;

public abstract class AbstractKernel implements Kernel {
    private int address;
    private String name;
    
    public final void setAddress(int address) {
        this.address = address; 
    }
    
    protected final int getAddress() {
        return address;
    }
    
    public final void setName(String name) {
        this.name = name;
    }
    
    protected final String getName() {
        return name;
    }
}
