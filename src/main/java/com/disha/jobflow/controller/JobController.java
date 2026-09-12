package com.disha.jobflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.disha.jobflow.model.Job;
import com.disha.jobflow.service.JobService;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:5173")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job) {

        try {

            Job createdJob = jobService.createJob(job);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdJob);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public List<Job> getAllJobs() {

        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable String id) {

        Job job = jobService.getJobById(id);

        if (job == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelJob(
            @PathVariable String id) {

        Job job = jobService.getJobById(id);

        if (job == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        boolean cancelled = jobService.cancelJob(id);

        if (!cancelled) {

            return ResponseEntity
                    .badRequest()
                    .body("Job cannot be cancelled in its current state.");
        }

        return ResponseEntity.ok(
                jobService.getJobById(id)
        );
    }
}