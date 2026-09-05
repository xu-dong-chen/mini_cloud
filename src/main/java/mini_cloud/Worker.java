package mini_cloud;

import java.util.UUID;

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

            UUID id = UUID.fromString(jobId);

            String jobData = jobStore.findById(id);

            System.out.println(
                    "Worker received job: " + jobId +
                    " data=" + jobData
            );

            executeJob(jobId,jobData);
        }
    }

    // function to execute the job
    private void executeJob(String jobId, String jobData) {

        UUID id = UUID.fromString(jobId);

        String[] parts = jobData.split("\\|", -1);

        String type = parts[0];

        jobStore.updateStatus(id, "RUNNING", null);

        System.out.println(
                "Executing job " +
                jobId +
                " type=" +
                type
        );

        // switch case for each type of job
        switch (type) {

            case "ADD":
                jobStore.updateStatus(
                        id,
                        "COMPLETED",
                        "42"
                );
                break;

            case "Subtract":
                jobStore.updateStatus(
                        id,
                        "COMPLETED",
                        "8"
                );
                break;

            case "test":
                jobStore.updateStatus(
                        id,
                        "COMPLETED",
                        "Test successful"
                );
                break;

            default:
                jobStore.updateStatus(
                        id,
                        "FAILED",
                        "Unknown job type"
                );
        }

        System.out.println(
                "Job " + jobId + " finished."
        );
    }
}