package ru.ifmo.ctddev.malimonov.parallelmapper;

import com.sun.xml.internal.fastinfoset.algorithm.IEEE754FloatingPointEncodingAlgorithm;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by heat_wave on 5/12/15.
 */
public class IterativeParallelism implements ScalarIP {
    private ParallelMapper parallelMapper;

    public IterativeParallelism() {
    }

    public IterativeParallelism(int threads) {
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T> List<List<? extends T>> cut(int threads, List<? extends T> list) {
        if (threads < 1) {
            threads = 1;
        } else if (threads > list.size()) {
            threads = list.size();
        }
        int chunkSize = (int) Math.ceil(1d * list.size() / threads);
        int left = 0;
        int right = Math.min(left + chunkSize, list.size());
        List<List<? extends T>> parts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            parts.add(list.subList(left, right));
            chunkSize = (int) Math.ceil(1d * (list.size() - right) / (threads - i - 1));
            left = right;
            right = Math.min(list.size(), right + chunkSize);
        }
        return parts;
    }

    private static class SubListWorker<T, R> implements Runnable {

        private List<? extends T> subList;
        private Function<List<? extends T>, R> function;
        private R result;

        private SubListWorker(List<? extends T> subList, Function<List<? extends T>, R> function) {
            this.subList = subList;
            this.function = function;
        }

        public R getResult() {
            return result;
        }

        @Override
        public void run() {
            result = function.apply(subList);
        }
    }

    private <T, R> R doParallel(int threads, List<? extends T> list, Function<List<? extends T>, R> function, Function<List<R>, R> merger) throws InterruptedException {
        List<List<? extends T>> subLists = cut(threads, list);
        if (parallelMapper != null) {
            List<R> results = parallelMapper.map(function, subLists);
            return merger.apply(results);
        } else {
            List<Thread> threadList = new ArrayList<>();
            List<SubListWorker<T, R>> workers = new ArrayList<>();
            for (List<? extends T> part : subLists) {
                SubListWorker<T, R> subListWorker = new SubListWorker<>(part, function);
                workers.add(subListWorker);
                Thread thread = new Thread(subListWorker);
                threadList.add(thread);
                thread.start();
            }
            for (Thread thread : threadList) {
                thread.join();
            }
            return merger.apply(workers.stream().map(SubListWorker::getResult).collect(Collectors.toList()));
        }
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return doParallel(threads, list, job -> Collections.min(job, comparator), results -> Collections.min(results, comparator));
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return doParallel(threads, list, job -> Collections.max(job, comparator), results -> Collections.max(results, comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return doParallel(threads, list, job -> job.stream().allMatch(predicate), results -> results.stream().allMatch(Predicate.isEqual(true)));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return doParallel(threads, list,
                job -> job.stream().anyMatch(predicate), results -> results.stream().anyMatch(Predicate.isEqual(true)));
    }

}