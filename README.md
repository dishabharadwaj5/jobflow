# JobFlow

A concurrent, priority-based background job processing system built with **Java, Spring Boot, and React**.

JobFlow demonstrates how backend systems can process asynchronous workloads using a thread-safe priority queue, multiple concurrent workers, automatic retries, exponential backoff, job cancellation, and real-time system monitoring.

## Features

- Priority-based job scheduling: HIGH, MEDIUM, LOW
- 3 concurrent background workers
- Thread-safe `PriorityBlockingQueue`
- Automatic retry handling
- Exponential backoff between retries
- Failure simulation and recovery
- Job cancellation
- Real-time worker and queue monitoring
- Processing, success, failure, and retry metrics
- REST API
- React dashboard
- Unit and integration tests

## Architecture

```text
                 React Dashboard
                        |
                        v
                Spring Boot REST API
                        |
                        v
                    JobService
                        |
                        v
             PriorityBlockingQueue
                        |
              +---------+---------+
              |         |         |
              v         v         v
           Worker 1  Worker 2  Worker 3
              |         |         |
              +---------+---------+
                        |
                        v
                  Job Processing
                   /     |      \
                  /      |       \
            Success    Retry    Failure
                         |
                         v
                  Exponential Backoff
````

## How It Works

When a job is submitted, it is validated and added to a thread-safe priority queue.

Workers continuously consume jobs from the queue. Higher-priority jobs are processed before lower-priority jobs when available.

Multiple workers operate concurrently, allowing several jobs to be processed at the same time.

If a job fails, the system can retry it using exponential backoff:

```text
Attempt 1
   |
 Failure
   |
 Wait
   |
Attempt 2
   |
 Failure
   |
 Wait longer
   |
Attempt 3
   |
Success / Permanent Failure
```

This models common patterns used in asynchronous backend processing systems.

## Tech Stack

### Backend

* Java 25
* Spring Boot
* Spring WebMVC
* Maven
* JUnit

### Frontend

* React
* Vite
* JavaScript
* CSS

## REST API

| Method | Endpoint                | Description               |
| ------ | ----------------------- | ------------------------- |
| `GET`  | `/api/jobs`             | Get all jobs              |
| `POST` | `/api/jobs`             | Create a job              |
| `GET`  | `/api/jobs/{id}`        | Get a job by ID           |
| `POST` | `/api/jobs/{id}/cancel` | Cancel a job              |
| `GET`  | `/api/system`           | Get worker/system metrics |

### Create Job

```json
{
  "name": "Generate Report",
  "priority": "HIGH"
}
```

## Running the Backend

From the project root:

```powershell
.\mvnw.cmd clean
.\mvnw.cmd -DforkCount=0 test
.\mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

## Running the Frontend

Open another terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Open the Vite URL shown in the terminal, normally:

```text
http://localhost:5173
```

## Dashboard

The React dashboard provides real-time visibility into:

* Total jobs
* Queued jobs
* Running jobs
* Completed jobs
* Failed jobs
* Retry counts
* Worker activity
* Queue activity

The demo scenario can be used to generate multiple jobs and observe concurrent processing and retry behavior.
<img width="1528" height="873" alt="Screenshot 2026-09-12 124311" src="https://github.com/user-attachments/assets/7d80694e-165b-4c4d-bb19-cde014f7a72d" />
<img width="1522" height="802" alt="Screenshot 2026-09-12 124320" src="https://github.com/user-attachments/assets/024e33e4-34fa-49fb-9e19-6261f885de4c" />
<img width="1520" height="725" alt="Screenshot 2026-09-12 124342" src="https://github.com/user-attachments/assets/4631bce9-d6fc-484e-bcb6-2b5cd36db037" />


## Project Structure

```text
jobflow/
├── src/
│   ├── main/
│   │   ├── java/com/disha/jobflow/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── worker/
│   │   └── resources/
│   └── test/
│       └── java/com/disha/jobflow/
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── vite.config.js
│
├── pom.xml
├── mvnw
└── mvnw.cmd
```

## Engineering Concepts

JobFlow demonstrates practical backend engineering concepts including:

* Producer-consumer architecture
* Concurrent worker execution
* Thread-safe data structures
* Priority scheduling
* Retry policies
* Exponential backoff
* REST API design
* Backend/frontend integration
* System monitoring
* Unit and integration testing

## Future Improvements

* Persistent database storage
* Redis-backed distributed queues
* Horizontal worker scaling
* Authentication and authorization
* Scheduled jobs
* Distributed tracing
* Docker deployment

## Author

**Disha Bharadwaj**

Built as a backend-focused systems project to explore concurrent job processing, queue-based architecture, and scalable service design.

````

