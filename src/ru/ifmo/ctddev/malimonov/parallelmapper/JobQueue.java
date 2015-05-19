package ru.ifmo.ctddev.malimonov.parallelmapper;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by heat_wave on 5/12/15.
 */
class JobQueue {

    private final Queue<Job<?, ?>> jobQueue;

    JobQueue() {
        jobQueue = new LinkedList<>();
    }

    public synchronized void add(Job<?, ?> job) {
        jobQueue.add(job);
        notifyAll();
    }

    public synchronized Job<?, ?> next() throws InterruptedException {
        while (jobQueue.isEmpty()) {
            wait();
        }
        return jobQueue.poll();
    }

}
