package cn.ilikui.learning;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyLabTest {
    @Test
    void atomicCounterRetainsAllConcurrentIncrements() throws Exception {
        int workers = 8;
        int incrementsPerWorker = 10_000;
        AtomicInteger counter = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    for (int j = 0; j < incrementsPerWorker; j++) {
                        counter.incrementAndGet();
                    }
                }));
            }
            for (Future<?> future : futures) future.get(5, TimeUnit.SECONDS);
            assertEquals(workers * incrementsPerWorker, counter.get());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void atomicIncrementValidatesArguments() {
        AtomicInteger counter = new AtomicInteger(3);
        assertEquals(3, ConcurrencyLab.incrementAtomically(counter, 0));
        assertEquals(5, ConcurrencyLab.incrementAtomically(counter, 2));
        assertThrows(NullPointerException.class, () -> ConcurrencyLab.incrementAtomically(null, 1));
        assertThrows(IllegalArgumentException.class, () -> ConcurrencyLab.incrementAtomically(counter, -1));
    }
}