package com.disha.jobflow.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.disha.jobflow.service.JobService;
import com.disha.jobflow.worker.JobWorker;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "http://localhost:5173")
public class SystemController {

    private final JobService jobService;
    private final JobWorker jobWorker;

    public SystemController(JobService jobService, JobWorker jobWorker) {
        this.jobService = jobService;
        this.jobWorker = jobWorker;
    }

    @GetMapping
    public Map<String, Object> getSystemStatus() {

        List<JobWorker.WorkerSnapshot> workers =
                jobWorker.getWorkerSnapshots();

        int total = jobService.getAllJobs().size();
        int completed = jobWorker.getCompletedJobs();
        int failed = jobWorker.getFailedJobs();
        int processed = completed + failed;

        int successRate = processed == 0
                ? 0
                : Math.round((completed * 100f) / processed);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("workerCount", JobWorker.WORKER_COUNT);
        response.put("activeWorkers", jobWorker.getActiveWorkerCount());
        response.put(
                "idleWorkers",
                JobWorker.WORKER_COUNT - jobWorker.getActiveWorkerCount()
        );

        response.put("queueDepth", jobService.getQueueSize());
        response.put("totalJobs", total);
        response.put("completedJobs", completed);
        response.put("failedJobs", failed);
        response.put("processedJobs", jobWorker.getProcessedJobs());
        response.put("retries", jobWorker.getTotalRetries());
        response.put("successRate", successRate);
        response.put("workers", workers);

        return response;
    }
}