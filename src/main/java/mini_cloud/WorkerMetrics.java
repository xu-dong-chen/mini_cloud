package mini_cloud;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class WorkerMetrics {

    // atomic integer allows multiple threads safely update the counter
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger retried = new AtomicInteger();

    private final AtomicLong totalExecutionTimeMs = new AtomicLong();

    public void recordCompleted(long executionTimeMs) {
        completed.incrementAndGet();
        totalExecutionTimeMs.addAndGet(executionTimeMs);
    }

    public void recordFailed() {
        failed.incrementAndGet();
    }

    public void recordRetried() {
        retried.incrementAndGet();
    }

    public int getCompleted() {
        return completed.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public int getRetried() {
        return retried.get();
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs.get();
    }

    public long getAverageExecutionTimeMs() {

        int count = completed.get();

        if (count == 0) {
            return 0;
        }

        return totalExecutionTimeMs.get() / count;
    }
}