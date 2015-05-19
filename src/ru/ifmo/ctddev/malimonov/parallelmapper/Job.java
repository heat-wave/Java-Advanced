package ru.ifmo.ctddev.malimonov.parallelmapper;

/**
 * Created by heat_wave on 5/12/15.
 */

import java.util.function.Function;

class Job<T, R> {

    private T arg;
    private Function<? super T, ? extends R> function;
    private R result;

    Job(T arg, Function<? super T, ? extends R> function) {
        this.arg = arg;
        this.function = function;
    }

    public synchronized void process() {
        result = function.apply(arg);
        notifyAll();
    }

    public synchronized R getResult() throws InterruptedException {
        while (result == null) {
            wait();
        }
        return result;
    }

}