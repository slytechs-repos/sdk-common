package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MultiThreadedMemoryTest {

    private MemoryPool<MemoryBuffer> createPool() {
        return new MemoryPool<>(64L, 10L, Arena.ofConfined(),
                (owningPool, segment, offset, length) -> new MemoryBuffer(owningPool, segment, offset, length));
    }

    @Test
    void testConcurrentPoolAllocationRelease() throws InterruptedException {
        MemoryPool<MemoryBuffer> pool = createPool();
        Assertions.assertEquals(10, pool.getFreeListSize());

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch allocateLatch = new CountDownLatch(threadCount);
        CountDownLatch releaseLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    MemoryBuffer buffer = pool.allocate();
                    allocateLatch.countDown();
                    try {
                        allocateLatch.await(); // Wait for all threads to allocate
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    buffer.decrementRef();
                    releaseLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        allocateLatch.await();
        Assertions.assertEquals(5, pool.getFreeListSize()); // 10 - 5 allocations
        releaseLatch.await();
        Assertions.assertEquals(10, pool.getFreeListSize()); // 5 + 5 releases
        executor.shutdown();
    }

    @Test
    void testConcurrentChainOperations() throws InterruptedException {
        MemoryPool<MemoryBuffer> pool = createPool();
        MemoryBuffer buf1 = pool.allocate();
        MemoryBuffer buf2 = pool.allocate();
        buf1.setNextMemory(buf2);

        MemoryProxy chain = new MemoryProxy();
        chain.bindMemory(buf1, 0);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        chain.incrementRef();
                        Assertions.assertEquals(2, chain.chainMemoryCount());
                        chain.seekMemory(10); // Access chain
                        chain.decrementRef();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(2, buf1.refCount()); // Original + chain
        Assertions.assertEquals(2, buf2.refCount()); // Original + chain

        chain.close();
        buf1.decrementRef();
        buf2.decrementRef();
        Assertions.assertEquals(10, pool.getFreeListSize());
    }

    @Test
    void testConcurrentRefCount() throws InterruptedException {
        MemoryPool<MemoryBuffer> pool = createPool();
        MemoryBuffer buffer = pool.allocate();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        buffer.incrementRef();
                        // Simulate work
                        Thread.sleep(1);
                        buffer.decrementRef();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(1, buffer.refCount());
        buffer.decrementRef();
        Assertions.assertEquals(10, pool.getFreeListSize());
    }
}