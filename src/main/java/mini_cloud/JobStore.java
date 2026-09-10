package mini_cloud;

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
                        job.getAttempts() + "|" +
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
        int attempts = Integer.parseInt(parts[2]);
        String result = parts[3].isEmpty() ? null : parts[3]; // result is null if there was originally nothing
        
        
        return new Job(id, type, status, result,attempts);
    }

    // function that updates the status of the job (Type|Status|attempts|result)
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

    public void incrementAttempts(UUID id){

        Job job = findById(id);

        if (job == null){
            return;
        }

        job.Attempted();
        save(job);
    }
}