package network;
import java.io.Serializable;

public final class Message<T extends Serializable> implements Serializable {
    public final int source, sourcePort;
    public final int destination, destinationPort;
    public final T data;
    
    public Message(int source, int destination,
            int sourcePort, int destinationPort, T data) {
        this.source = source;
        this.sourcePort = sourcePort;
        this.destination = destination;
        this.destinationPort = destinationPort;
        this.data = data;
    }
    
    @SuppressWarnings("unchecked")
    public <U extends Serializable> Message<U> asType(Class<U> clazz) {
        // Throw exception if wrong class
        clazz.cast(data);
        
        return (Message<U>) this;
    }
    
    public <U extends Serializable> U dataAs(Class<U> clazz) {
        return clazz.cast(data);
    }
    
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Message [");
        
        builder.append(Integer.toHexString(source)).append(':');
        KnownPort.formatPort(sourcePort, builder);
        
        builder.append(" => ").append(Integer.toHexString(destination));
        
        builder.append(Integer.toHexString(destination)).append(':');
        KnownPort.formatPort(sourcePort, builder);
        
        builder.append("] ").append(data);
        
        return builder.toString();
    }
    
    private static final long serialVersionUID = 1L;
}
