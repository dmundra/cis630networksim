package network;

public class NodeNotRunningException extends IllegalStateException {
    public NodeNotRunningException(Node node) {
        this("Node not started or has shut down: " + node.name());
    }
    public NodeNotRunningException(String message) {
        super(message);
    }
}
