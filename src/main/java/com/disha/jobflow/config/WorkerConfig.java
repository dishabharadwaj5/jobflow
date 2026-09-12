package com.disha.jobflow.config;

import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.disha.jobflow.model.Job;
import com.disha.jobflow.repository.JobRepository;
import com.disha.jobflow.worker.JobWorker;

@Configuration
public class WorkerConfig {

    @Bean
    public JobWorker jobWorker(
            PriorityBlockingQueue<Job> jobQueue,
            JobRepository jobRepository) {
        return new JobWorker(jobQueue, jobRepository);
    }
}
