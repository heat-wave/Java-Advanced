package ru.ifmo.ctddev.malimonov.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by heat_wave on 5/11/15.
 */
public class IterativeParallelism implements ScalarIP {

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty())
            return null;
        BiFunction<T, T, T> biFunction = (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
        return reduce(threads, biFunction, values, biFunction, () -> values.get(0));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty())
            return null;
        BiFunction<T, T, T> biFunction = (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
        return reduce(threads, biFunction, values, biFunction, () -> values.get(0));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<T, Boolean, Boolean> biFunction = (a, b) -> predicate.test(a) && b;
        return reduce(threads, biFunction, values, (a, b) -> a && b, () -> true);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        BiFunction<? super T, Boolean, Boolean> biFunction = (a, b) -> predicate.test(a) || b;
        return reduce(threads, biFunction, values, (a, b) -> a || b, () -> false);
    }

    private static <E, T> T reduce(int threads, BiFunction<E, T, T> operation, List<? extends E> list,
                                   BiFunction<T, T, T> join, Supplier<T> supplier) throws InterruptedException {

        int blockSize = (int) Math.ceil(list.size() / threads);
        Result<T> result = new Result<>(supplier.get(), threads, join);
        int left = 0;
        int right = Math.min(left + blockSize, list.size());
        ArrayList<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Runnable task = new Task<>(result, list.subList(left, right), operation, i, supplier.get());
            Thread thread = new Thread(task);
            threadList.add(thread);
            thread.start();
            blockSize = (int) Math.ceil((list.size() - right) / (double) (threads - i - 1));
            left = right;
            right = Math.min(list.size(), right + blockSize);
        }
        for (Thread thread : threadList) {
            thread.join();
        }
        return result.getValue();
    }
}
