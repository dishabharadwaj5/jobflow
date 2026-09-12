package com.disha.jobflow;

import java.util.concurrent.PriorityBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.disha.jobflow.model.Job;
import com.disha.jobflow.repository.JobRepository;
import com.disha.jobflow.service.JobService;

class JobServiceTest {

    private JobService createService() {

        JobRepository repository = new JobRepository();

        PriorityBlockingQueue<Job> queue =
                new PriorityBlockingQueue<>(
                        10,
                        (job1, job2) -> {
                            int priority1 = getPriority(job1);
                            int priority2 = getPriority(job2);

                            return Integer.compare(priority2, priority1);
                        }
                );

        return new JobService(repository, queue);
    }

    private static int getPriority(Job job) {

        return switch (job.getPriority().toUpperCase()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    @Test
    void shouldCreateJobWithQueuedStatus() {

        JobService service = createService();

        Job job = new Job();
        job.setName("Test Job");
        job.setPriority("HIGH");

        Job created = service.createJob(job);

        assertNotNull(created.getId());
        assertEquals("Test Job", created.getName());
        assertEquals("HIGH", created.getPriority());
        assertEquals("QUEUED", created.getStatus());
    }

    @Test
    void shouldStoreJobInRepository() {

        JobService service = createService();

        Job job = new Job();
        job.setName("Generate Report");
        job.setPriority("MEDIUM");

        service.createJob(job);

        assertEquals(1, service.getAllJobs().size());
        assertEquals(
                "Generate Report",
                service.getAllJobs().get(0).getName()
        );
    }

    @Test
    void shouldProcessHighPriorityJobFirst() {

        JobService service = createService();

        Job low = new Job();
        low.setName("Low Job");
        low.setPriority("LOW");

        Job high = new Job();
        high.setName("High Job");
        high.setPriority("HIGH");

        Job medium = new Job();
        medium.setName("Medium Job");
        medium.setPriority("MEDIUM");

        service.createJob(low);
        service.createJob(high);
        service.createJob(medium);

        Job firstJob = service.getNextJob();

        assertNotNull(firstJob);
        assertEquals("High Job", firstJob.getName());
    }
}