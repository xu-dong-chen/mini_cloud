package mini_cloud;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Worker class that executes the jobs
@Component
public class Worker implements CommandLineRunner {

    private final JobQueue jobQueue;
    private final JobStore jobStore;

    private static final int NumberOfWorkers = 3;

    public Worker(JobQueue jobQueue, JobStore jobStore) {
        this.jobQueue = jobQueue;
        this.jobStore = jobStore;
    }

    @Override
    public void run(String... args) {

        System.out.println("starting " + NumberOfWorkers + " workers");

        for(int i = 1; i <= NumberOfWorkers; i++){
            int workerId = i;

            Thread thread = new Thread(
                () -> workerLoop(workerId),
                "worker-" + workerId
            );

            thread.start();
        }
    }

    private void workerLoop(int workerId){
        System.out.println("Worker " + workerId + " started");

        while (true) {

            String jobId = jobQueue.dequeue(); // Gets a job from the queue

            // Checks if there was a job
            if (jobId == null) {
                continue;
            }

            UUID id = UUID.fromString(jobId);

            Job job = jobStore.findById(id);
            
            if (job == null){
                System.out.println("Worker " + workerId + " could not find job" + jobId);
                continue;
            }

            System.out.println(
                    "Worker received job: " + job.getId() +
                    " type = " + job.getType() +
                    " status = " + job.getStatus()
            );

            executeJob(job, workerId);
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
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    jobStore.updateStatus(
                            job.getId(),
                            "Failed",
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
}