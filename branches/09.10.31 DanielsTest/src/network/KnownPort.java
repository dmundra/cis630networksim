package network;

public enum KnownPort {
    RESERVED, RIP, HTTP, KERNEL_WHO;
    
    public static void formatPort(int portNum, StringBuilder out) {
        final KnownPort[] all = KnownPort.values();
        if (portNum < 0 || portNum > all.length)
            out.append(portNum);
        else
            out.append(all[portNum]);
    }
    
    public static String formatPort(int portNum) {
        final StringBuilder builder = new StringBuilder();
        formatPort(portNum, builder);
        return builder.toString();
    }
}
