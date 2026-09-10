package mini_cloud;

import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobTimeoutChecker {

    private static final String PROCESSING_KEY = "job:processing";
    private static final long JOB_TIMEOUT_MS = 15_000;

    private final StringRedisTemplate redisTemplate;
    private final JobStore jobStore;
    private final JobQueue jobQueue;

    public JobTimeoutChecker(
            StringRedisTemplate redisTemplate,
            JobStore jobStore,
            JobQueue jobQueue
    ) {
        this.redisTemplate = redisTemplate;
        this.jobStore = jobStore;
        this.jobQueue = jobQueue;
    }

    @Scheduled(fixedDelay = 2000)
    public void checkForTimeouts() {

        List<String> processingJobs =
                redisTemplate.opsForList()
                        .range(PROCESSING_KEY, 0, -1);

        if (processingJobs == null) {
            return;
        }

        for (String jobId : processingJobs) {

            try {

                if (jobStore.hasTimedOut(
                        java.util.UUID.fromString(jobId),
                        JOB_TIMEOUT_MS
                )) {

                    System.out.println(
                            "Job timed out: " + jobId
                    );

                UUID id = UUID.fromString(jobId);

                jobQueue.markedComplete(jobId);
                jobQueue.releaseLease(jobId);

                if (jobStore.canRetry(id)) {

                    System.out.println(
                        "Retrying timed out job: " + jobId
                    );

                    jobStore.updateStatus(
                        id,
                        "QUEUED",
                        "Job timed out and will be retried"
                    );

                    jobQueue.enqueue(jobId);

                } else {

                    System.out.println(
                        "Job timed out and permanently failed: " + jobId
                    );

                    jobStore.updateStatus(
                        id,
                        "FAILED",
                        "Job timed out after maximum attempts"
                    );
                }
                }

            } catch (Exception e) {

                System.out.println(
                        "Error checking timeout for job " +
                        jobId + ": " + e.getMessage()
                );
            }
        }
    }
}