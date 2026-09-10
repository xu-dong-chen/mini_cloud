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
        int workerId = Integer.parseInt(System.getProperty("worker.id", "1"));

        System.out.println("starting worker " + workerId);

        workerLoop(workerId);
    }

    private void workerLoop(int workerId){
        System.out.println("Worker " + workerId + " started");

        while (true) {

            String jobId = jobQueue.claimJob(); // Gets a job from the queue

            // Checks if there was a job
            if (jobId == null) {
                continue;
            }

            jobQueue.createLease(jobId, workerId); // creates a lease for the job and worker

            UUID id = UUID.fromString(jobId);

            jobStore.incrementAttempts(id); // jobs attempts incremented

            Job job = jobStore.findById(id);
            
            if (job == null){
                System.out.println("Worker " + workerId + " could not find job" + jobId);
                jobQueue.markedComplete(jobId);
                continue;
            }

            System.out.println(
                    "Worker received job: " + job.getId() +
                    " type = " + job.getType() +
                    " status = " + job.getStatus()
            );

            Thread heartbeat = new Thread(() -> {

                try {
                    while (!Thread.currentThread().isInterrupted()) {

                        Thread.sleep(3000);

                        jobQueue.renewLease(jobId, workerId);

                        System.out.println(
                                "Worker " + workerId +
                                " renewed lease for job " + jobId
                        );
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }, "heartbeat-" + workerId);

            heartbeat.start();

            boolean retried = false; // Check if the job needs updating

            try {
                // executes as per usual
                executeJob(job, workerId);

                jobStore.updateStatus(
                        job.getId(),
                        "COMPLETED",
                        job.getResult()
                );

            } catch (Exception e) {

                // error for a job 
                System.out.println(
                        "Worker " + workerId +
                        " failed job " + job.getId() +
                        ": " + e.getMessage()
                );

                // Check if the max attempts has been reached
                if (job.getAttempts() < 3) {

                    System.out.println(
                            "Retrying job " + job.getId() +
                            " (attempt " + job.getAttempts() + ")"
                    );

                    jobStore.updateStatus(
                            job.getId(),
                            "QUEUED",
                            e.getMessage()
                    );

                    jobQueue.retry(jobId);
                    retried = true; // do not mark as complete and release lease again 

                } else {

                    System.out.println(
                            "Job " + job.getId() +
                            " permanently failed after 3 attempts"
                    );

                    jobStore.updateStatus(
                            job.getId(),
                            "FAILED",
                            e.getMessage()
                    );

                    jobQueue.markedComplete(jobId);
                    jobQueue.releaseLease(jobId);
                }
            }

            heartbeat.interrupt();

            if (!retried) {
                jobQueue.markedComplete(jobId);
                jobQueue.releaseLease(jobId);
            }
        }
    }

    // function to execute the job
    private void executeJob(Job job, int workerId) {

        String jobId = job.getId().toString(); 
        String type = job.getType();

        System.out.println(
                "worker " + workerId +
                " executing job " +
                jobId +
                " type=" +
                type
        );

        // switch case for each type of job
        switch (type) {
            case "ADD":
                jobStore.updateStatus(
                        job.getId(),
                        "COMPLETED",
                        "42"
                );
                break;

            case "SUBTRACT":
                jobStore.updateStatus(
                        job.getId(),
                        "COMPLETED",
                        "8"
                );
                break;
            
            case "SLEEP":
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    jobStore.updateStatus(
                            job.getId(),
                            "FAILED",
                            "SLEEP interrupted"
                    );
                    break;
                }
                jobStore.updateStatus(
                            job.getId(),
                            "COMPLETED",
                            "Slept for 5 seconds"
                );
                break;

            case "TEST":
                jobStore.updateStatus(
                        job.getId(),
                        "COMPLETED",
                        "Test successful"
                );
                break;

            // For testing
            case "FAIL":
                throw new RuntimeException("Intentional test failure");

            default:
                jobStore.updateStatus(
                        job.getId(),
                        "FAILED",
                        "Unknown job type"
                );
        }

        System.out.println("Worker" + workerId + " finished job " + jobId);
    }

}