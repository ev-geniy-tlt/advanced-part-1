package com.java.advanced;

import java.math.BigInteger;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class SumUtils {
    public static BigInteger sum(BigInteger start, BigInteger finish) {
        BigInteger sum = ZERO;
        for (BigInteger i = start; i.compareTo(finish) <= 0; i = i.add(ONE)) {
            sum = sum.add(i);
        }
        return sum;
    }
}
