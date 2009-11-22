package network;

public enum KnownPort {
    UNKNOWN, RIP, HTTP, KERNEL_WHO;
    
    public int number() {
        return ordinal();
    }
    
    public boolean is(int number) {
        return ordinal() == number;
    }
    
    public static KnownPort withNumber(int number) {
        final KnownPort[] all = KnownPort.values();
        if (number < 0 || number > all.length)
            return null;
        else
            return all[number];
    }
    
    public static void formatPort(int portNum, StringBuilder out) {
        final KnownPort port = withNumber(portNum);
        out.append(port == null ? portNum : port);
    }
    
    public static String formatPort(int portNum) {
        final StringBuilder builder = new StringBuilder();
        formatPort(portNum, builder);
        return builder.toString();
    }
}
