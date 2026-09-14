package mini_cloud;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class MetricsController {

    private final WorkerMetrics metrics;

    public MetricsController(WorkerMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {

        return new MetricsResponse(
            metrics.getCompleted(),
            metrics.getFailed(),
            metrics.getRetried(),
            metrics.getTotalExecutionTimeMs(),
            metrics.getAverageExecutionTimeMs()
        );
    }

    public record MetricsResponse(
            int completed,
            int failed,
            int retried,
            long totalExecutionTimeMs,
            long averageExecutionTimeMs
    ) {}
}