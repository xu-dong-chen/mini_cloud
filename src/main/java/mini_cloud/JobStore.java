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

        String value = job.getType() + "|" + job.getStatus() + "|" + (job.getResult() == null ? "": job.getResult()); 
        redisTemplate.opsForValue().set(key,value);

    }

    public Job findById(UUID id) {

        String key = "job:" + id;

        String value = redisTemplate.opsForValue().get(key);

        if (value == null){
            return null;
        }

        String[] parts = value.split("\\|", -1);

        Job job = new Job(parts[0]);

        job.setStatus(parts[1]);

        if (!parts[2].isEmpty()){
            job.setResult(parts[2]);
        }
        
        return job;
    }

    // function that updates the status of the job (Type|Status|result)
    public void updateStatus(UUID id, String status, String result) {
        String key = "job:" + id;

        String current = redisTemplate.opsForValue().get(key);

        if (current == null){
            return;
        }

        String[] parts = current.split("\\|", -1);

        String type = parts[0]; 

        String value = type + "|" +
                       status + "|" +
                       (result == null ? "" : result);

        redisTemplate.opsForValue().set(key,value);
    }
}