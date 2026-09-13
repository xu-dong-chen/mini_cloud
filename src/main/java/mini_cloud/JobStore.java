package mini_cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobStore{

    private final StringRedisTemplate redisTemplate;

    public JobStore(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public void save(Job job){
        
        String key = "job:" + job.getId();

        String value = job.getType() + "|" + 
                        job.getStatus() + "|" + 
                        job.getPriority() + "|" + 
                        job.getAttempts() + "|" +
                        job.getCreatedAt() + "|" +
                        job.getStartedAt() + "|" +
                        (job.getResult() == null ? "": job.getResult()); 
        redisTemplate.opsForValue().set(key,value);

    }

    public Job findById(UUID id) {

        String key = "job:" + id;

        String value = redisTemplate.opsForValue().get(key);

        if (value == null){
            return null;
        }

        String[] parts = value.split("\\|", -1);

        String type = parts[0];
        String status = parts[1];
        int priority = Integer.parseInt(parts[2]);
        int attempts = Integer.parseInt(parts[3]);
        long createdAt = Long.parseLong(parts[4]);
        long startedAt = Long.parseLong(parts[5]);
        String result = parts[6].isEmpty() ? null : parts[6]; // result is null if there was originally nothing
        
        
        return new Job(id, type, status, result, attempts, createdAt, startedAt, priority);
    }

    public List<Job> findAll() {

        Set<String> keys = redisTemplate.keys("job:*");

        if (keys == null) {
            return new ArrayList<>();
        }

        List<Job> jobs = new ArrayList<>();

        for (String key : keys) {

            // Ignore queue/processing/lease keys
            if (!key.startsWith("job:") ||
                key.equals("job:queue:high") ||
                key.equals("job:queue:medium") ||
                key.equals("job:queue:low") ||
                key.equals("job:processing") ||
                key.startsWith("job:lease:")) {
                continue;
            }

            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                continue;
            }

            String[] parts = value.split("\\|", -1);

            UUID id = UUID.fromString(key.substring("job:".length()));

            String type = parts[0];
            String status = parts[1];
            int priority = Integer.parseInt(parts[2]);
            int attempts = Integer.parseInt(parts[3]);
            long createdAt = Long.parseLong(parts[4]);
            long startedAt = Long.parseLong(parts[5]);
            String result = parts.length > 6 ? parts[6] : null;

            jobs.add(new Job(id, type, status, result, attempts, createdAt, startedAt, priority));
        }

        return jobs;
    }

    // function that updates the status of the job (Type|Status|priority|attempts|createdAt|startedAt|result)
    public void updateStatus(UUID id, String status, String result) {
        
        Job job = findById(id);

        if (job == null){
            return;
        }

        job.setStatus(status);
        job.setResult(result);

        // Uses the existing function save to do the rest
        save(job);
    }

    // function used for retries
    public void incrementAttempts(UUID id){

        Job job = findById(id);

        if (job == null){
            return;
        }

        job.Attempted();
        save(job);
    }

    // function to check for idempotency
    public boolean isCompleted(UUID id) {

        Job job = findById(id);

        if (job == null) {
            return false;
        }

        return "COMPLETED".equals(job.getStatus());
    }

    // for checking if a worker is already running that job
    public boolean isRunning(UUID id) {

        Job job = findById(id);

        if (job == null) {
            return false;
        }

        return "RUNNING".equals(job.getStatus());
    }

    // marks a job as running
    public void markRunning(UUID id) {

        Job job = findById(id);

        if (job == null) {
            return;
        }

        job.setStatus("RUNNING");
        job.setStartedAt(System.currentTimeMillis());

        save(job);
    }

    // checks if the job has ran for longer than the allowed time
    public boolean hasTimedOut(UUID id, long timeoutMs) {

        Job job = findById(id);

        if (job == null) {
            return false;
        }

        if (!"RUNNING".equals(job.getStatus())) {
            return false;
        }

        long runningTime =
                System.currentTimeMillis() - job.getStartedAt();

        return runningTime > timeoutMs;
    }

    public boolean canRetry(UUID id) {
        Job job = findById(id);

        if (job == null) {
            return false;
        }

        return job.getAttempts() < 3;
    }
}