package mini_cloud;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/jobs")
public class JobController {

    // Controls list of jobs and the queue
    private final JobStore jobs;
    private final JobQueue jobQueue;

    public JobController(JobStore jobs, JobQueue jobQueue){ // jobs created by Spring as JobStore is @Component
        this.jobs = jobs; 
        this.jobQueue = jobQueue;
    }

    @PostMapping
    public Job createJob(@RequestBody CreateJobRequest request) {

        Job job = new Job(request.type(), request.priority);

        jobs.save(job);
        jobQueue.enqueue(job.getId().toString());

        return job;
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable UUID id) {

        return jobs.findById(id);
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobs.findAll();
    }

    public record CreateJobRequest(String type, int priority) {
    }
}