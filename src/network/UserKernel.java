package network;

/**
 * A kernel which can load and run a {@link Process} with user code.
 *
 * @author Luke Maurer
 */
public interface UserKernel extends Kernel {
    void setProcess(Process process);
}
