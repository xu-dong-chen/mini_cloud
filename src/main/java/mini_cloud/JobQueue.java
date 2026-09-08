package mini_cloud;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// Queue for incoming jobs (FIFO)
@Component
public class JobQueue {

    private static final long LEASE_SECONDS = 10;

    private static final String QUEUE_KEY = "job:queue";
    private static final String PROCESSING_KEY = "job:processing"; // used to prevent jobs lost when worker fails
    
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

    // Mark the job as processing
    public void markProcessing(String jobId){
        redisTemplate.opsForList().rightPush(PROCESSING_KEY, jobId);
    }

    // Mark the job as complete
    public void markedComplete(String jobId){
        redisTemplate.opsForList().remove(PROCESSING_KEY,1, jobId);
    }

    public void createLease(String jobId, int workerId){
        String leaseKey = "job:lease:" + jobId;

        redisTemplate.opsForValue().set(leaseKey, "worker-" + workerId, Duration.ofSeconds(LEASE_SECONDS));
    }

    public void renewLease(String jobId, int workerId){
        String leaseKey = "job:lease:" + jobId;

        String owner = redisTemplate.opsForValue().get(leaseKey);

        if(owner == null){
            return;
        }

        if(!owner.equals("worker-" + workerId)){
            return;
        }

        redisTemplate.expire(leaseKey,Duration.ofSeconds(LEASE_SECONDS));
    }

    public void releaseLease(String jobId){
        String leaseKey = "job:lease:" + jobId;
        redisTemplate.delete(leaseKey);
    }

    public String claimJob() {

        return redisTemplate.opsForList().move(
                QUEUE_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                PROCESSING_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT
        );
    }
}