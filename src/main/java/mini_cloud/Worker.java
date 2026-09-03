package mini_cloud;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Worker class that executes the jobs
@Component
public class Worker implements CommandLineRunner {

    private final JobQueue jobQueue;
    private final JobStore jobStore;

    public Worker(JobQueue jobQueue, JobStore jobStore) {
        this.jobQueue = jobQueue;
        this.jobStore = jobStore;
    }

    @Override
    public void run(String... args) {

        System.out.println("Worker started.");

        while (true) {

            String jobId = jobQueue.dequeue(); // Gets a job from the queue

            // Checks if there was a job
            if (jobId == null) {
                continue;
            }

            // Gets the type of job
            String jobType = jobStore.findById(
                    java.util.UUID.fromString(jobId)
            );

            System.out.println(
                    "Worker received job: " + jobId +
                    " type=" + jobType
            );
        }
    }
}