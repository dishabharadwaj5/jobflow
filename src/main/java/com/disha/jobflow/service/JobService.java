package com.disha.jobflow.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.stereotype.Service;

import com.disha.jobflow.model.Job;
import com.disha.jobflow.repository.JobRepository;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final PriorityBlockingQueue<Job> jobQueue;

    public JobService(JobRepository jobRepository, PriorityBlockingQueue<Job> jobQueue) {
        this.jobRepository = jobRepository;
        this.jobQueue = jobQueue;
    }

    public Job createJob(Job job) {
        if (job.getName() == null || job.getName().isBlank()) {
            throw new IllegalArgumentException("Job name is required");
        }

        if (job.getPriority() == null || job.getPriority().isBlank()) {
            job.setPriority("MEDIUM");
        }

        String priority = job.getPriority().toUpperCase();
        if (!priority.equals("HIGH") && !priority.equals("MEDIUM") && !priority.equals("LOW")) {
            throw new IllegalArgumentException("Priority must be HIGH, MEDIUM or LOW");
        }

        job.setPriority(priority);
        job.setId(UUID.randomUUID().toString());
        job.setRetryCount(0);
        job.setStatus("QUEUED");
        job.setCreatedAt(System.currentTimeMillis());
        job.setStartedAt(0);
        job.setCompletedAt(0);
        job.setProgress(0);
        job.setWorkerId(null);
        job.setNextRetryAt(0);
        job.setExecutionDurationMs(0);

        jobRepository.save(job);
        jobQueue.offer(job);
        return job;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparingLong(Job::getCreatedAt).reversed())
                .toList();
    }

    public Job getJobById(String id) {
        return jobRepository.findById(id);
    }

    public boolean cancelJob(String id) {
        Job job = jobRepository.findById(id);
        if (job == null) return false;

        String status = job.getStatus();
        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
            return false;
        }

        job.setStatus("CANCELLED");
        job.setProgress(0);
        jobQueue.remove(job);
        jobRepository.save(job);
        return true;
    }

    public int getQueueSize() {
        return jobQueue.size();
    }

    public Job getNextJob() {
        return jobQueue.poll();
    }
}
