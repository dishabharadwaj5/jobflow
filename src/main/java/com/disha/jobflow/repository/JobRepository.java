package com.disha.jobflow.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.disha.jobflow.model.Job;

@Repository
public class JobRepository {

    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<>();

    public void save(Job job) {
        jobs.put(job.getId(), job);
    }

    public List<Job> findAll() {
        return new ArrayList<>(jobs.values());
    }

    public Job findById(String id) {
        return jobs.get(id);
    }

    public boolean deleteById(String id) {
        return jobs.remove(id) != null;
    }

    public int count() {
        return jobs.size();
    }
}