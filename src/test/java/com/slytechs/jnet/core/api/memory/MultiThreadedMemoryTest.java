package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

class MultiThreadedMemoryTest {
    
    /**
     * Helper method to create and properly shutdown an executor.
     */
    private void shutdownExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("WARNING: Executor did not terminate cleanly");
                executor.shutdownNow();
                Thread.sleep(100);
            }
        }
    }
    
    /**
     * Creates a fresh pool with a new Arena for complete isolation.
     */
    private MemoryPool<MemoryByteBuffer> createPool() {
        Arena arena = Arena.ofConfined();
        return new MemoryPool<>("memory-pool", 64L, 10L,
                arena, (owningPool, segment, offset, end, dataOffset, dataEnd) -> 
                    new MemoryByteBuffer(owningPool, segment, offset, end, dataOffset, dataEnd));
    }

    @Test
    void testSingleAllocation() {
        MemoryPool<MemoryByteBuffer> pool = createPool();

        MemoryByteBuffer buffer = pool.allocate();

        Assertions.assertNotNull(buffer, "Should allocate successfully");
        Assertions.assertEquals(1, buffer.refCount(), "Should have refcount=1");
        Assertions.assertEquals(9, pool.getFreeListSize(), "Should have 9 free");

        buffer.decrementRef();
        Assertions.assertEquals(10, pool.getFreeListSize(), "Should have 10 free");
    }

    @RepeatedTest(value = 10, name = "Concurrent allocation/release test {currentRepetition}/{totalRepetitions}")
    void testConcurrentPoolAllocationRelease(RepetitionInfo repetitionInfo) throws InterruptedException {
        // Log which iteration we're on for debugging
        if (repetitionInfo.getCurrentRepetition() == 1) {
            System.out.println("Starting concurrent allocation/release stress test...");
        }
        
        MemoryPool<MemoryByteBuffer> pool = createPool();
        Assertions.assertEquals(10, pool.getFreeListSize());

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch allocatedLatch = new CountDownLatch(threadCount);
            CountDownLatch holdLatch = new CountDownLatch(1);
            CountDownLatch releasedLatch = new CountDownLatch(threadCount);
            
            AtomicInteger allocatedCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    MemoryByteBuffer buffer = null;
                    try {
                        startLatch.await();
                        
                        buffer = pool.allocate();
                        if (buffer != null) {
                            allocatedCount.incrementAndGet();
                        }
                        
                        allocatedLatch.countDown();
                        holdLatch.await();
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        if (buffer != null) {
                            buffer.decrementRef();
                        }
                        releasedLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            allocatedLatch.await();
            
            int actuallyAllocated = allocatedCount.get();
            Assertions.assertEquals(5, actuallyAllocated, 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": Should allocate exactly 5");
            Assertions.assertEquals(5, pool.getFreeListSize(), 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": Should have 5 free after 5 allocations");

            holdLatch.countDown();
            releasedLatch.await();
            
            Thread.sleep(50);
            
            Assertions.assertEquals(10, pool.getFreeListSize(), 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": Should have all 10 back after releases");
                
        } finally {
            shutdownExecutor(executor);
        }
    }

    @RepeatedTest(value = 5, name = "Pool exhaustion test {currentRepetition}/{totalRepetitions}")
    void testPoolExhaustionUnderContention(RepetitionInfo repetitionInfo) throws InterruptedException {
        MemoryPool<MemoryByteBuffer> pool = createPool();
        int threadCount = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        MemoryByteBuffer buffer = pool.allocate();

                        if (buffer != null) {
                            successCount.incrementAndGet();
                            Thread.sleep(10);
                            buffer.decrementRef();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            Assertions.assertTrue(completed, 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": All threads should complete");

            Assertions.assertTrue(successCount.get() <= 10,
                "Iteration " + repetitionInfo.getCurrentRepetition() + 
                ": Cannot allocate more than pool size: " + successCount.get());
            Assertions.assertEquals(threadCount, successCount.get() + failureCount.get(),
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": All threads should complete");
            Assertions.assertTrue(failureCount.get() > 0,
                "Iteration " + repetitionInfo.getCurrentRepetition() + 
                ": Should have some allocation failures with 20 threads and pool of 10");
            
            Thread.sleep(100);
            
            Assertions.assertEquals(10, pool.getFreeListSize(),
                "Iteration " + repetitionInfo.getCurrentRepetition() + 
                ": All buffers should be returned to pool");
                    
        } finally {
            shutdownExecutor(executor);
        }
    }

    @RepeatedTest(value = 5, name = "Chain operations test {currentRepetition}/{totalRepetitions}")
    void testConcurrentChainOperations() throws InterruptedException {
        MemoryPool<MemoryByteBuffer> pool = createPool();
        
        MemoryByteBuffer buf1 = pool.allocate();
        MemoryByteBuffer buf2 = pool.allocate();
        Assertions.assertNotNull(buf1);
        Assertions.assertNotNull(buf2);
        
        buf1.setNextMemory(buf2);

        MemoryProxy chain = new MemoryProxy();
        chain.bindMemory(buf1, 0);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 100; j++) {
                            chain.incrementRef();
                            Assertions.assertEquals(2, chain.segmentCount());
                            chain.seekSegment(10);
                            chain.decrementRef();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            Assertions.assertTrue(completed, "All threads should complete");

            Assertions.assertEquals(2, buf1.refCount());
            Assertions.assertEquals(2, buf2.refCount());

            chain.unbindMemory();
            buf1.setNextMemory(null);
            buf1.decrementRef();
            buf2.decrementRef();

            Assertions.assertEquals(10, pool.getFreeListSize());
            
        } finally {
            shutdownExecutor(executor);
        }
    }

    @RepeatedTest(value = 10, name = "Reference counting test {currentRepetition}/{totalRepetitions}")
    void testConcurrentRefCount(RepetitionInfo repetitionInfo) throws InterruptedException {
        MemoryPool<MemoryByteBuffer> pool = createPool();
        MemoryByteBuffer buffer = pool.allocate();
        Assertions.assertNotNull(buffer);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            CyclicBarrier startBarrier = new CyclicBarrier(threadCount);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startBarrier.await(); // All threads start together
                        for (int j = 0; j < 100; j++) {
                            buffer.incrementRef();
                            // Simulate work
                            Thread.sleep(1);
                            buffer.decrementRef();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            Assertions.assertTrue(completed, 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": All threads should complete");
            Assertions.assertEquals(0, errorCount.get(), 
                "Iteration " + repetitionInfo.getCurrentRepetition() + ": No errors should occur");

            Assertions.assertEquals(1, buffer.refCount());
            buffer.decrementRef();
            
            Assertions.assertEquals(10, pool.getFreeListSize());
            
        } finally {
            shutdownExecutor(executor);
        }
    }
}