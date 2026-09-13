package mini_cloud;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// Queue for incoming jobs (FIFO)
@Component
public class JobQueue {

    private static final long LEASE_SECONDS = 10;

    private static final String LOW_QUEUE_KEY = "job:queue:low";
    private static final String MED_QUEUE_KEY = "job:queue:medium";
    private static final String HIGH_QUEUE_KEY = "job:queue:high";
    private static final String PROCESSING_KEY = "job:processing"; // used to prevent jobs lost when worker fails
    
    private final StringRedisTemplate redisTemplate;
    private final JobStore jobStore;

    public JobQueue(
            StringRedisTemplate redisTemplate,
            JobStore jobStore
    ) {
        this.redisTemplate = redisTemplate;
        this.jobStore = jobStore;
    }

    // Adds a job to the right of the list and sorts it by priority with 3 different queues
    public void enqueue(String jobId) {

        Job job = jobStore.findById(UUID.fromString(jobId));

        if (job == null) {
            return;
        }

        String queueKey;

        switch (job.getPriority()) {
            case 3:
                queueKey = HIGH_QUEUE_KEY;
                break;
            case 2:
                queueKey = MED_QUEUE_KEY;
                break;
            default:
                queueKey = LOW_QUEUE_KEY;
                break;
        }

        redisTemplate.opsForList().rightPush(queueKey, jobId);
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

        // extends the lease for a fixed amount of time
        redisTemplate.expire(leaseKey,Duration.ofSeconds(LEASE_SECONDS));
    }

    public void releaseLease(String jobId){
        String leaseKey = "job:lease:" + jobId;
        redisTemplate.delete(leaseKey);
    }

    // function to essentially dequeue the jobs based on priority
    public String claimJob() {

        String jobId;

        // try high priority jobs first
        jobId = redisTemplate.opsForList().move(
                HIGH_QUEUE_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                PROCESSING_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT
        );

        if (jobId != null) {
            return jobId;
        }

        // for medium priority jobs
        jobId = redisTemplate.opsForList().move(
                MED_QUEUE_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                PROCESSING_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT
        );

        if (jobId != null) {
            return jobId;
        }

        // finally low priority jobs
        return redisTemplate.opsForList().move(
                LOW_QUEUE_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT,
                PROCESSING_KEY,
                org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT
        );
    }

    // function to allow a job to retry itself
    public void retry(String jobId){
        markedComplete(jobId);
        releaseLease(jobId);
        enqueue(jobId);
    }

    public boolean ownsLease(String jobId, int workerId) {

        String leaseKey = "job:lease:" + jobId;

        String owner = redisTemplate.opsForValue().get(leaseKey);

        return ("worker-" + workerId).equals(owner);
    }
}