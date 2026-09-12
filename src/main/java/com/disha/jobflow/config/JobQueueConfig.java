package com.disha.jobflow.config;

import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.disha.jobflow.model.Job;

@Configuration
public class JobQueueConfig {

    @Bean
    public PriorityBlockingQueue<Job> jobQueue() {
        return new PriorityBlockingQueue<>(20, (job1, job2) -> {
            int priorityComparison = Integer.compare(
                    getPriority(job2), getPriority(job1));

            if (priorityComparison != 0) {
                return priorityComparison;
            }

            return Long.compare(job1.getCreatedAt(), job2.getCreatedAt());
        });
    }

    private int getPriority(Job job) {
        return switch (job.getPriority().toUpperCase()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }
}
