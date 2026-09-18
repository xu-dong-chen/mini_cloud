# Mini Cloud

A small distributed job-processing platform built with **Java, Spring Boot, Redis, React, TypeScript, and Docker**.

The project demonstrates how multiple workers can process jobs from a shared Redis-backed queue while using leases, retries, timeouts, and failure recovery to make job execution more reliable.

## Features

- Redis-backed job queue
- Multiple concurrent worker instances
- Worker IDs for identifying individual workers
- Job priorities
- Worker leases and lease renewal
- Recovery of abandoned jobs
- Job execution timeouts
- Automatic retry handling
- Maximum of 3 attempts per job
- Permanent failure handling
- Idempotency checks for completed jobs
- Worker execution metrics
- REST API for submitting and viewing jobs
- React/TypeScript monitoring dashboard
- Live job and metrics updates
- Worker status controls in the dashboard
- CORS support for the frontend
- Docker and Docker Compose support
- Support for running multiple worker containers

## Architecture

                    ┌─────────────────────┐
                    │   React Dashboard   │
                    │   TypeScript / UI   │
                    └──────────┬──────────┘
                               │
                               │ REST API
                               ▼
                    ┌─────────────────────┐
                    │    Spring Boot      │
                    │      Backend        │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │        Redis        │
                    │   Queue + Job Data  │
                    └──────┬───────┬──────┘
                           │       │
                 ┌─────────┘       └─────────┐
                 ▼                           ▼
          ┌─────────────┐             ┌─────────────┐
          │   Worker 1  │             │   Worker 2  │
          └─────────────┘             └─────────────┘
                 │                           │
                 └───────────┬───────────────┘
                             ▼
                    ┌─────────────────┐
                    │   Job Results   │
                    │    & Metrics    │
                    └─────────────────┘

Additional workers can be run against the same Redis instance, allowing jobs to be distributed between workers.

## Job Lifecycle

A typical job moves through the following states:

QUEUED
  │
  ▼
RUNNING
  │
  ├──────────────► COMPLETED
  │
  └──────────────► FAILED
                    │
                    └── retry while attempts < 3

Workers acquire a lease before processing a job. The lease is periodically renewed while the job is running.

If a worker disappears or loses its lease, another worker can recover the abandoned job.

## Reliability

The worker system includes several mechanisms to handle failures:

### Leases

Workers acquire a lease before processing a job and periodically renew it.

This prevents multiple workers from normally processing the same job simultaneously.

### Abandoned Jobs

If a worker stops unexpectedly while holding a job, the job can be detected as abandoned and recovered by another worker.

### Timeouts

Long-running jobs can exceed their configured execution time. A timed-out job is interrupted and can be retried.

### Retries

Failed jobs can be returned to the queue until they reach the maximum attempt count.

The current maximum is:

3 attempts

After the final failed attempt, the job is permanently marked as `FAILED`.

### Idempotency

Before executing a job, the system checks its state so that jobs already completed or permanently failed are not unnecessarily executed again.

## Metrics

The system tracks worker/job execution metrics including:

- Completed jobs
- Failed jobs
- Retried jobs
- Total execution time
- Average execution time

The dashboard periodically refreshes these values so that the system can be monitored while jobs are running.

## Dashboard

The React dashboard provides:

- Job list
- Job status
- Priority
- Attempt count
- Job submission
- Live metrics
- Worker status
- Worker enable/disable controls

The dashboard is designed around a dark UI for monitoring the system while it is running.

> Note: The current worker enable/disable controls represent worker state in the dashboard. They do not directly start or stop Docker containers.

## Example Job

A job can be submitted through the REST API:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/jobs" `
  -ContentType "application/json" `
  -Body '{"type":"SLEEP","priority":3}'
```

A successful response contains information similar to:

id        : 02635be8-2bda-48fb-b559-201510d3801b
type      : SLEEP
status    : QUEUED
priority  : 3
attempts  : 0

Jobs can then be viewed through:

GET /jobs

Metrics are available through:

GET /metrics

## Running Locally

### Requirements

- Java 21
- Maven (or the included Maven wrapper)
- Redis
- Node.js and npm
- Docker and Docker Compose (for containerised deployment)

### Backend

Build the Spring Boot application:

./mvnw package -DskipTests

The resulting JAR is created in the `target` directory.

Start the application with:

java -jar target/minicloud-0.0.1-SNAPSHOT.jar

The backend runs on:

http://localhost:8080

### Frontend

From the frontend directory, install dependencies:

npm install

Start the development server:

npm run dev

The React dashboard is normally available at:

http://localhost:5173

## Docker

The project can be run using Docker Compose.

Build and start the containers:

docker compose up --build

This starts Redis and the configured worker/application containers.

Multiple workers can share the same Redis instance, allowing jobs to be processed concurrently.

To stop the containers:

docker compose down

## Testing Multiple Workers

Running multiple worker containers allows the distributed processing behaviour to be observed.

For example:

Redis
  │
  ├── Worker 1
  ├── Worker 2
  └── Worker 3

Submitting several `SLEEP` jobs should allow different workers to acquire and process jobs.

Stopping a worker while it owns a job can also be used to test abandoned-job recovery.

## Example Failure Scenario

A worker starts processing a job:

Worker 1 executing job ... type=SLEEP
Worker 1 renewed lease for job ...

If the job times out:

Job timed out: ...
Retrying timed out job: ...

The job can then be placed back into the queue and processed by another worker.

After the maximum number of attempts:

Job ... permanently failed after 3 attempts

This demonstrates failure detection, retry behaviour, and eventual failure handling.

## Technology Stack

### Backend

- Java 21
- Spring Boot
- Spring Data Redis
- REST API
- Maven

### Queue / Storage

- Redis

### Frontend

- React
- TypeScript
- JavaScript
- CSS
- Vite

### Infrastructure

- Docker
- Docker Compose

## Project Goals

This project was built to explore practical distributed-systems concepts rather than simply creating a CRUD application.

The main areas demonstrated are:

- Distributed job processing
- Worker coordination
- Lease-based ownership
- Fault tolerance
- Retry mechanisms
- Timeout handling
- Failure recovery
- Concurrent processing
- Monitoring and metrics
- Containerisation
- Horizontal scaling

## Future Improvements

Potential future improvements include:

- Persisting worker enabled/disabled state in the backend
- Starting and stopping worker processes from the dashboard
- More job types
- Authentication and authorisation
- More detailed job history
- Better metric aggregation across workers
- Configurable retry limits and timeouts
- Health checks for workers
- Improved queue management
- Production deployment
