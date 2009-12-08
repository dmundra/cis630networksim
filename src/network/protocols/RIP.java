package network.protocols;

import java.io.Serializable;
import java.util.Arrays;

import network.KnownPort;

public final class RIP {
    public static final KnownPort PORT = KnownPort.KERNEL_WHO;
    
    public static final class Datagram implements Serializable {
        public static final class Entry implements Serializable {
            public final int destination;
            public final byte metric;
            
            public Entry(int destination, byte metric) {
                this.destination = destination;
                this.metric = metric;
            }
            
            @Override
            public String toString() {
                return String.format("%04x metric %d", destination, metric);
            }
            
            private static final long serialVersionUID = 1L;
        }
        
        public final Entry[] entries;

        public Datagram(Entry[] entries) {
            this.entries = entries;
        }
        
        public static Datagram notARouter() {
            return new Datagram(null);
        }
        
        @Override
        public String toString() {
            return "RIP " +
                (entries != null ? Arrays.toString(entries) :
                    "(not a router)");
        }
        
        private static final long serialVersionUID = 1L;
    }
}
