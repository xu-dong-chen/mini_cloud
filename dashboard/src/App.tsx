import { useEffect, useState } from "react";
import "./App.css";

type Job = {
  id: string;
  type: string;
  status: string;
  priority: number;
  attempts: number;
  createdAt: number;
  startedAt: number;
  result: string | null;
};

type Metrics = {
  completed: number;
  failed: number;
  retried: number;
  totalExecutionTimeMs: number;
  averageExecutionTimeMs: number;
};

function App() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {

    const fetchJobs = () => {
      fetch("http://localhost:8080/jobs")
        .then((response) => {
          if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
          }

          return response.json();
        })
        .then((data) => {
          setJobs(data);
          setError(null);
        })
        .catch((error) => {
          setError(error.message);
        });
      fetch("http://localhost:8080/metrics")
        .then((response) => {
          if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
          }

          return response.json();
        })
        .then((data) => {
          setMetrics(data);
        })
        .catch((error) => {
          setError(error.message);
        });
    };

    fetchJobs();

    const interval = setInterval(fetchJobs, 2000);

    return () => clearInterval(interval);

  }, []);

  return (
    <div>
      <h1>Mini Cloud Dashboard</h1>
      <h2>Metrics</h2>

      {metrics && (
        <div>
          <p>Completed: {metrics.completed}</p>
          <p>Failed: {metrics.failed}</p>
          <p>Retried: {metrics.retried}</p>
          <p>
            Average execution time: {metrics.averageExecutionTimeMs} ms
          </p>
        </div>
      )}

      {error && <p>Error: {error}</p>}

      <h2>Jobs</h2>

      {jobs.length === 0 ? (
        <p>No jobs found.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Type</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Attempts</th>
            </tr>
          </thead>

          <tbody>
            {jobs.map((job) => (
              <tr key={job.id}>
                <td>{job.id}</td>
                <td>{job.type}</td>
                <td>{job.status}</td>
                <td>{job.priority}</td>
                <td>{job.attempts}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default App;