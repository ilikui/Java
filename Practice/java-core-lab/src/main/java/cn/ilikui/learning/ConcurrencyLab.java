package cn.ilikui.learning;

import java.util.concurrent.atomic.AtomicInteger;

/** Minimal concurrency example for comparing atomic updates. */
public final class ConcurrencyLab {
    private ConcurrencyLab() { }

    /** A plain increment is not an atomic read-modify-write operation. */
    public static int incrementPlain(int value) {
        return value + 1;
    }

    public static int incrementAtomically(AtomicInteger counter, int times) {
        if (counter == null) {
            throw new NullPointerException("counter must not be null");
        }
        if (times < 0) {
            throw new IllegalArgumentException("times must be >= 0");
        }
        for (int i = 0; i < times; i++) {
            counter.incrementAndGet();
        }
        return counter.get();
    }

    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger();
        incrementAtomically(counter, 100_000);
        System.out.println("原子计数结果：" + counter.get());
    }
}