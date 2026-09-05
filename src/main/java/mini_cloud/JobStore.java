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

    public String findById(UUID id) {

        String key = "job:" + id;
        
        return redisTemplate.opsForValue().get(key);
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