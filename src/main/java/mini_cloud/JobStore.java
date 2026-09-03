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

        redisTemplate.opsForValue().set(key,job.getType());

    }

    public String findById(UUID id) {

        String key = "job:" + id;
        
        return redisTemplate.opsForValue().get(key);
    }
}