package ru.ifmo.ctddev.malimonov.iterativeparallelism;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by heat_wave on 5/11/15.
 */
class Task<E, T> implements Runnable{
    private final Result<T> result;
    private T value;
    private final List<? extends E> list;
    private final BiFunction<E, T, T> biFunction;
    private final int threadNumber;

    public Task(Result<T> result, List<? extends E> list, BiFunction<E, T, T> biFunction, int threadNumber, T value) {
        this.result = result;
        this.list = list;
        this.biFunction = biFunction;
        this.threadNumber = threadNumber;
        this.value = value;
    }

    @Override
    public void run() {
        for (E e : list) {
            value = biFunction.apply(e, value);
        }
        result.modify(value, threadNumber);
    }
}
