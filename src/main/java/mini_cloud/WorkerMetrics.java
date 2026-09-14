package mini_cloud;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetrics {

    // uses redis to ensure all workers contribute to the metrics 
    private static final String COMPLETED = "metrics:completed";
    private static final String FAILED = "metrics:failed";
    private static final String RETRIED = "metrics:retried";
    private static final String TOTAL_EXECUTION_TIME = "metrics:totalExecutionTimeMs";

    private final RedisTemplate<String, String> redisTemplate;

    public WorkerMetrics(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordCompleted(long executionTimeMs) {
        redisTemplate.opsForValue().increment(COMPLETED);
        redisTemplate.opsForValue().increment(TOTAL_EXECUTION_TIME, executionTimeMs);
    }

    public void recordFailed() {
        redisTemplate.opsForValue().increment(FAILED);
    }

    public void recordRetried() {
        redisTemplate.opsForValue().increment(RETRIED);
    }

    public long getCompleted() {
        return getValue(COMPLETED);
    }

    public long getFailed() {
        return getValue(FAILED);
    }

    public long getRetried() {
        return getValue(RETRIED);
    }

    public long getTotalExecutionTimeMs() {
        return getValue(TOTAL_EXECUTION_TIME);
    }

    public long getAverageExecutionTimeMs() {
        long count = getCompleted();

        if (count == 0) {
            return 0;
        }

        return getTotalExecutionTimeMs() / count;
    }

    // used by all get metric function to reduce redundant code by using the key as the parameter 
    private long getValue(String key) {
        String value = redisTemplate.opsForValue().get(key); // gets the specific metric 

        if (value == null) {
            return 0;
        }

        return Long.parseLong(value);
    }
}