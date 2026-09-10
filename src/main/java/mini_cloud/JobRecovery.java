package mini_cloud;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobRecovery {

    private static final String PROCESSING_KEY = "job:processing";
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;
    private final JobQueue jobQueue;

    public JobRecovery(
            StringRedisTemplate redisTemplate,
            JobQueue jobQueue
    ) {
        this.redisTemplate = redisTemplate;
        this.jobQueue = jobQueue;
    }

    @Scheduled(fixedDelay = 2000)
    public void recoverJobs() {

        List<String> processingJobs =
                redisTemplate.opsForList()
                        .range(PROCESSING_KEY, 0, -1);

        if (processingJobs == null) {
            return;
        }

        for (String jobId : processingJobs) {

            String leaseKey = "job:lease:" + jobId;

            Boolean leaseExists =
                    redisTemplate.hasKey(leaseKey);

            if (Boolean.FALSE.equals(leaseExists)) {

                System.out.println(
                        "Recovering abandoned job: " + jobId
                );

                jobQueue.markedComplete(jobId);
                jobQueue.enqueue(jobId);
            }
        }
    }
}
