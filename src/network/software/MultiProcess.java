package network.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import network.AbstractProcess;
import network.Process;

public class MultiProcess extends AbstractProcess {
    private volatile ExecutorService executor;
    private final AtomicReference<List<Process>> pending =
        new AtomicReference<List<Process>>(
                Collections.synchronizedList(new ArrayList<Process>()));
    
    private class ProcessRunner implements Runnable {
        private final Process process;
        
        private ProcessRunner(Process process) {
            this.process = process;
        }
        
        public void run() {
            try {
                process.run(os());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    public void add(Process process) {
        final List<Process> pending = this.pending.get();
        
        if (pending != null)
            pending.add(process);
        else
            execute(process);
    }

    private void execute(Process process) {
        try {
            executor.execute(new ProcessRunner(process));
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException("Shutting down", e);
        }
    }
    
    protected void run() throws InterruptedException {
        executor = Executors.newCachedThreadPool();
        final List<Process> pending = this.pending.getAndSet(null);
        
        for (Process process : pending)
            execute(process);
        
        try {
            while (true)
                synchronized (this) {
                    wait();
                }
        } catch (InterruptedException e) { }
        
        executor.shutdownNow();
        while (!executor.awaitTermination(5, TimeUnit.SECONDS))
            continue;
    }
}