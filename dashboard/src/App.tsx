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

  const [jobType, setJobType] = useState("SLEEP");
  const [priority, setPriority] = useState(1);
  const [submitting, setSubmitting] = useState(false);

  const [workers, setWorkers] = useState(
    [1, 2, 3, 4, 5, 6].map((id) => ({
      id,
      enabled: id === 1,
    }))
  );

  const submitJob = async () => { // function for user to submit jobs
      setSubmitting(true);

      try {
        const response = await fetch("http://localhost:8080/jobs", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            type: jobType,
            priority: priority,
          }),
        });

        if (!response.ok) {
          throw new Error(`HTTP error: ${response.status}`);
        }

      } catch (error) {
        setError(error instanceof Error ? error.message : "Failed to submit job");
      } finally {
        setSubmitting(false);
      }
    };

  const toggleWorker = (workerId: number) => { // function to toggle workers on and off
    setWorkers((currentWorkers) =>
      currentWorkers.map((worker) =>
        worker.id === workerId
          ? { ...worker, enabled: !worker.enabled }
          : worker
      )
    );
  };

  useEffect(() => {

    const fetchJobs = () => { // function to get the jobs
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
      fetch("http://localhost:8080/metrics") // for metrics
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

    const interval = setInterval(fetchJobs, 2000); // interval to update the jobs

    return () => clearInterval(interval);
    }, []);

    const getStatusClass = (status: string) => { // gets the class of the job
      switch (status) { 
        case "COMPLETED": 
          return "status completed"; 
        
        case "RUNNING": 
          return "status running"; 
          
        case "FAILED": 
          return "status failed"; 
          
        case "QUEUED": 
          return "status queued"; 
          
        default: 
          return "status"; } 
        }; 
        
    const getPriorityLabel = (priority: number) => { // gets the priority of the job
      switch (priority) { 
        case 3: 
          return "High"; 
          
        case 2: 
          return "Medium"; 
          
        case 1: 
          return "Low"; 
          
        default: 
          return priority.toString(); 
      } 
    };

  return (
    <div className="dashboard"> 
      <header className="header"> 
        <div>
          <h1>Mini Cloud</h1>
          <p>Distributed Job Processing Dashboard</p>
        </div>

        <div className="system-status">
          <span className="online-dot"></span>
          System Online
        </div>
      </header>

      <section className="metrics">
        <div className="metric-card">
          <span className="metric-title">Completed</span>
          <strong>{metrics?.completed ?? 0}</strong>
        </div>

        <div className="metric-card">
          <span className="metric-title">Failed</span>
          <strong>{metrics?.failed ?? 0}</strong>
        </div>

        <div className="metric-card">
          <span className="metric-title">Retried</span>
          <strong>{metrics?.retried ?? 0}</strong>
        </div>

        <div className="metric-card">
          <span className="metric-title">Avg Execution</span>
          <strong>
            {metrics?.averageExecutionTimeMs ?? 0} ms
          </strong>
        </div>
      </section>

      <section className="panel">

        <div className="section-header">
          <div>
            <h2>Workers</h2>
            <p>Active compute workers</p>
          </div>

          <span className="worker-count">
            {workers.filter((worker) => worker.enabled).length} / 6 workers active
          </span>
        </div>
        
        <div className="workers"> {/* Gets all the workers */}

          {workers.map((worker) => (

            <div
              className={`worker-card ${
                worker.enabled ? "worker-enabled" : "worker-disabled" 
              }`}
              key={worker.id}
            >

              <div className="worker-info">

                <div className="worker-icon">
                  W
                </div>

                <div>
                  <strong>Worker {worker.id}</strong>

                  <span
                    className={
                      worker.enabled
                        ? "worker-online"
                        : "worker-offline"
                    }
                  >
                    <span className="small-dot"></span>

                    {worker.enabled ? "Online" : "Offline"}
                  </span>
                </div>

              </div>

              <button
                className="disable-button"
                onClick={() => toggleWorker(worker.id)}
              >
                {worker.enabled ? "Disable" : "Enable"}
              </button>

            </div>

          ))}

        </div>

      </section>

      {/* Where the user can add jobs */}
      <section className="panel">
        <div className="section-header">
          <div>
            <h2>Submit Job</h2>
            <p>Create a new job for the worker pool</p>
          </div>
        </div>

        <div className="submit-form">
          <div className="form-group">
            <label>Job Type</label> {/* Can add different jobs here if wanted */}

            <select
              value={jobType}
              onChange={(e) => setJobType(e.target.value)}
            >
              <option value="SLEEP">SLEEP</option>
            </select>
          </div>

          <div className="form-group">
            <label>Priority</label>

            <select
              value={priority}
              onChange={(e) => setPriority(Number(e.target.value))}
            >
              <option value={3}>High</option>
              <option value={2}>Medium</option>
              <option value={1}>Low</option>
            </select>
          </div>

          <button
            className="submit-button"
            onClick={submitJob}
            disabled={submitting}
          >
            {submitting ? "Submitting..." : "Submit Job"}
          </button>
        </div>
      </section>

      {error && (
        <div className="error">
          Error: {error}
        </div>
      )}

      <section className="panel"> {/* The table of the jobs */}
        <div className="section-header">
          <div>
            <h2>Jobs</h2>
            <p>Recent jobs processed by the cluster</p>
          </div>

          <span className="job-count">
            {jobs.length} jobs
          </span>
        </div>

        {jobs.length === 0 ? (
          <p className="empty">
            No jobs found.
          </p>
        ) : (
          <div className="table-container">
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
                    <td className="job-id">
                      {job.id}
                    </td>

                    <td>
                      {job.type}
                    </td>

                    <td>
                      <span className={getStatusClass(job.status)}>
                        {job.status}
                      </span>
                    </td>

                    <td>
                      <span
                        className={`priority priority-${job.priority}`}
                      >
                        {getPriorityLabel(job.priority)}
                      </span>
                    </td>

                    <td>
                      {job.attempts}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
  }

  export default App;

