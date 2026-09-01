package mini_cloud;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobStore jobs;

    public JobController(JobStore jobs){ // jobs created by Spring as JobStore is @Component
        this.jobs = jobs; 
    }

    @PostMapping
    public Job createJob(@RequestBody CreateJobRequest request) {

        Job job = new Job(request.type());

        jobs.save(job);

        return job;
    }

    @GetMapping("/{id}")
    public String getJob(@PathVariable UUID id) {

        return jobs.findById(id);
    }

    public record CreateJobRequest(String type) {
    }
}