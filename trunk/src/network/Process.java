package network;

/**
 * The software loaded into a node. Can be installed using
 * {@link UserKernel#setProcess(Process)}.
 *
 * @author Luke Maurer
 */
public interface Process {
    /**
     * Run the process, using the given {@link OperatingSystem} object to make
     * system calls. The process should be designed to shut down quickly if the
     * thread is ever interrupted; this means both letting
     * InterruptedExceptions propagate when thrown by blocking methods and
     * checking {@link Thread#interrupted()} periodically.
     * <p>
     * This method will be run in its own thread.
     * 
     * @param os The operating system running the process.
     * @throws InterruptedException If interrupted.
     * @throws IllegalStateException If the process is already running.
     */
    void run(OperatingSystem os) throws InterruptedException;
}
