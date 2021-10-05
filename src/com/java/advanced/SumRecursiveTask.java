package com.java.advanced;

import java.math.BigInteger;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Level;
import java.util.logging.Logger;

class SumRecursiveTask extends RecursiveTask<BigInteger> {
    private static final Logger logger = Logger.getLogger(SumRecursiveTask.class.getName());

    private static final BigInteger THRESHOLD = BigInteger.valueOf(1_000_000);
    private final BigInteger start;
    private final BigInteger finish;

    public SumRecursiveTask(BigInteger start, BigInteger finish) {
        logger.setLevel(Level.WARNING); //to enable/disable debug
        logger.info(String.format("New task created, %s, %s", start, finish));
        this.start = start;
        this.finish = finish;
    }

    @Override
    protected BigInteger compute() {
        BigInteger length = finish.subtract(start);
        BigInteger sum;
        if (length.compareTo(THRESHOLD) > 0) {
            logger.info(String.format("%s: Waiting for tasks results", Thread.currentThread().getName()));
            SumRecursiveTask left = new SumRecursiveTask(start, start.add(length.divide(BigInteger.valueOf(2))));
            SumRecursiveTask right = new SumRecursiveTask(finish.subtract(length.divide(BigInteger.valueOf(2))), finish);
            ForkJoinTask.invokeAll(left, right);
            sum = left.join().add(right.join());
        } else {
            logger.info(String.format("%s: Calculating sum for start = %s, finish = %s", Thread.currentThread().getName(), start, finish));
            sum = SumUtils.sum(start, finish);
        }
        return sum;
    }
}
