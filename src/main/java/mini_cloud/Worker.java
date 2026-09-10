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

            executeJob(job, workerId);

            heartbeat.interrupt();

            jobQueue.markedComplete(jobId);
            jobQueue.releaseLease(jobId);
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

            default:
                jobStore.updateStatus(
                        job.getId(),
                        "FAILED",
                        "Unknown job type"
                );
        }

        System.out.println("Worker" + workerId + " finished job " + jobId);
    }

    // private void startHeartbeat(String jobId, int workerId, Thread[] heartbeatHolder) {

    //     Thread heartbeat = new Thread(() -> {

    //         try {
    //             while (!Thread.currentThread().isInterrupted()) {

    //                 Thread.sleep(3000);

    //                 jobQueue.renewLease(jobId, workerId);

    //                 System.out.println(
    //                         "Worker " + workerId +
    //                         " renewed lease for job " + jobId
    //                 );
    //             }

    //         } catch (InterruptedException e) {
    //             Thread.currentThread().interrupt();
    //         }

    //     }, "heartbeat-" + workerId);

    //     heartbeat.start();

    //     heartbeatHolder[0] = heartbeat;
    // }
}