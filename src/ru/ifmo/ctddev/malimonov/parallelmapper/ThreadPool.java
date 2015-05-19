package ru.ifmo.ctddev.malimonov.parallelmapper;

/**
 * Created by heat_wave on 5/12/15.
 */

class ThreadPool implements Runnable {

    private final JobQueue jobQueue;

    ThreadPool(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                jobQueue.next().process();
            }
        } catch (InterruptedException e) {
            //do nothing
        }
    }
}