package ru.ifmo.ctddev.malimonov.iterativeparallelism;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by heat_wave on 5/11/15.
 */
class Result<T> {
    private T value;
    private List<T> results;
    private BiFunction<T, T, T> biFunction;

    public Result(T value, int size, BiFunction<T, T, T> biFunction) {
        this.value = value;
        this.results = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            results.add(null);
        }
        this.biFunction = biFunction;
    }

    public T getValue() {
        for (T result : results) {
            value = biFunction.apply(value, result);
        }
        return value;
    }

    public synchronized void modify(T t, int threadNum) {
        results.set(threadNum, t);
    }
}
