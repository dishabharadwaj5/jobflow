package com.disha.jobflow.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.disha.jobflow.model.Job;
import com.disha.jobflow.repository.JobRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class JobWorker {

    public static final int WORKER_COUNT = 3;
    public static final int MAX_RETRIES = 3;
    private static final int PROCESSING_TIME_MS = 6000;
    private static final int PROGRESS_STEP_MS = 500;

    private final PriorityBlockingQueue<Job> jobQueue;
    private final JobRepository jobRepository;
    private final Map<Integer, Job> activeJobs = new ConcurrentHashMap<>();
    private final AtomicInteger completedJobs = new AtomicInteger();
    private final AtomicInteger failedJobs = new AtomicInteger();
    private final AtomicInteger totalRetries = new AtomicInteger();
    private final AtomicInteger processedJobs = new AtomicInteger();

    private ExecutorService executorService;
    private ScheduledExecutorService retryScheduler;

    public JobWorker(PriorityBlockingQueue<Job> jobQueue, JobRepository jobRepository) {
        this.jobQueue = jobQueue;
        this.jobRepository = jobRepository;
    }

    @PostConstruct
    public void startWorkers() {
        executorService = Executors.newFixedThreadPool(WORKER_COUNT);
        retryScheduler = Executors.newScheduledThreadPool(1);

        for (int i = 1; i <= WORKER_COUNT; i++) {
            final int workerId = i;
            executorService.submit(() -> processJobs(workerId));
        }

        System.out.println("JobFlow started with " + WORKER_COUNT + " concurrent priority workers.");
    }

    private void processJobs(int workerId) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Job job = jobQueue.take();

                if (!"QUEUED".equals(job.getStatus())) {
                    continue;
                }

                activeJobs.put(workerId, job);
                processJob(job, workerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Worker " + workerId + " error: " + e.getMessage());
            } finally {
                activeJobs.remove(workerId);
            }
        }
    }

    private void processJob(Job job, int workerId) throws InterruptedException {
        job.setStatus("RUNNING");
        job.setWorkerId(workerId);
        job.setStartedAt(System.currentTimeMillis());
        job.setCompletedAt(0);
        job.setProgress(0);
        job.setNextRetryAt(0);
        jobRepository.save(job);

        System.out.println("Worker " + workerId + " processing " + job.getName()
                + " [" + job.getPriority() + "] attempt " + (job.getRetryCount() + 1));

        int steps = PROCESSING_TIME_MS / PROGRESS_STEP_MS;
        for (int step = 1; step <= steps; step++) {
            Thread.sleep(PROGRESS_STEP_MS);

            if ("CANCELLED".equals(job.getStatus())) {
                job.setWorkerId(null);
                jobRepository.save(job);
                return;
            }

            job.setProgress(Math.min(100, (step * 100) / steps));
            jobRepository.save(job);
        }

        // A name containing "fail" intentionally exercises the retry path.
        if (job.getName().toLowerCase().contains("fail")) {
            handleFailure(job, workerId);
            return;
        }

        long completedAt = System.currentTimeMillis();
        job.setStatus("COMPLETED");
        job.setProgress(100);
        job.setCompletedAt(completedAt);
        job.setWorkerId(null);
        job.setExecutionDurationMs(completedAt - job.getStartedAt());
        jobRepository.save(job);

        processedJobs.incrementAndGet();
        completedJobs.incrementAndGet();
        System.out.println("Worker " + workerId + " completed " + job.getName());
    }

    private void handleFailure(Job job, int workerId) {
        if (job.getRetryCount() < MAX_RETRIES) {
            int retryNumber = job.getRetryCount() + 1;
            long backoffMs = 1000L * (1L << (retryNumber - 1));

            job.setRetryCount(retryNumber);
            job.setStatus("RETRYING");
            job.setProgress(100);
            job.setWorkerId(null);
            job.setStartedAt(0);
            job.setCompletedAt(0);
            job.setExecutionDurationMs(0);
            job.setNextRetryAt(System.currentTimeMillis() + backoffMs);
            jobRepository.save(job);

            totalRetries.incrementAndGet();
            processedJobs.incrementAndGet();

            retryScheduler.schedule(() -> {
                if ("RETRYING".equals(job.getStatus())) {
                    job.setStatus("QUEUED");
                    job.setProgress(0);
                    job.setNextRetryAt(0);
                    jobRepository.save(job);
                    jobQueue.offer(job);
                }
            }, backoffMs, TimeUnit.MILLISECONDS);

            System.out.println("Worker " + workerId + " failed " + job.getName()
                    + " | retry " + retryNumber + "/" + MAX_RETRIES
                    + " | backoff " + backoffMs + "ms");
        } else {
            long completedAt = System.currentTimeMillis();
            job.setStatus("FAILED");
            job.setProgress(100);
            job.setCompletedAt(completedAt);
            job.setWorkerId(null);
            job.setNextRetryAt(0);
            job.setExecutionDurationMs(job.getStartedAt() == 0 ? 0 : completedAt - job.getStartedAt());
            jobRepository.save(job);

            processedJobs.incrementAndGet();
            failedJobs.incrementAndGet();
            System.out.println("Worker " + workerId + " permanently failed " + job.getName());
        }
    }

    public List<WorkerSnapshot> getWorkerSnapshots() {
        List<WorkerSnapshot> snapshots = new ArrayList<>();
        for (int workerId = 1; workerId <= WORKER_COUNT; workerId++) {
            Job job = activeJobs.get(workerId);
            snapshots.add(new WorkerSnapshot(workerId, job));
        }
        return snapshots;
    }

    public int getActiveWorkerCount() {
        return activeJobs.size();
    }

    public int getCompletedJobs() {
        return completedJobs.get();
    }

    public int getFailedJobs() {
        return failedJobs.get();
    }

    public int getTotalRetries() {
        return totalRetries.get();
    }

    public int getProcessedJobs() {
        return processedJobs.get();
    }

    public record WorkerSnapshot(int workerId, Job job) {
        public String getStatus() {
            return job == null ? "IDLE" : job.getStatus();
        }

        public String getJobId() {
            return job == null ? null : job.getId();
        }

        public String getJobName() {
            return job == null ? null : job.getName();
        }

        public Integer getProgress() {
            return job == null ? 0 : job.getProgress();
        }
    }

    @PreDestroy
    public void stopWorkers() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (retryScheduler != null) {
            retryScheduler.shutdownNow();
        }
    }
}
