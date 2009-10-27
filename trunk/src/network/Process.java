package network;

/**
 * The software loaded into a node. Can be installed using
 * {@link UserKernel#setProcess(Process)}.
 *
 * @author Luke Maurer
 */
public interface Process {
    void run(OperatingSystem os) throws InterruptedException;
}
