package ru.ifmo.ctddev.malimonov.parallelmapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by heat_wave on 5/12/15.
 */

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> threadList;
    private JobQueue jobQueue;


    public ParallelMapperImpl(int threads) {
        threadList = new ArrayList<>();
        jobQueue = new JobQueue();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(new ThreadPool(jobQueue));
            threadList.add(thread);
            thread.start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        List<Job<? super T, ? extends R>> jobList = new ArrayList<>();
        for (T arg : list) {
            Job<? super T, ? extends R> job = new Job<>(arg, function);
            jobQueue.add(job);
            jobList.add(job);
        }
        List<R> result = new ArrayList<>();
        for (Job<? super T, ? extends R> job : jobList) {
            result.add(job.getResult());
        }
        return result;
    }

    @Override
    public void close() throws InterruptedException {
        threadList.forEach(java.lang.Thread::interrupt);
    }
}