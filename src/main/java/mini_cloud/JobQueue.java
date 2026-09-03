package mini_cloud;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// Queue for incoming jobs (FIFO)
@Component
public class JobQueue {

    private static final String QUEUE_KEY = "job:queue";

    private final StringRedisTemplate redisTemplate;

    public JobQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Adds a job to the right of the list
    public void enqueue(String jobId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, jobId);
    }

    // Removes the job from the left of the list
    public String dequeue() {
        return redisTemplate.opsForList()
                .leftPop(QUEUE_KEY, Duration.ofSeconds(5));
    }
}