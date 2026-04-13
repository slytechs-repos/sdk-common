/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.memory.pool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BucketPool Test Suite")
class BucketPoolTest {

    static class TestPoolable implements Poolable {
        private final PoolEntry poolEntry = new PoolEntry() {
            @Override
            protected void onRecycle() {
                recycleCount++;
            }
        };

        int recycleCount = 0;

        @Override
        public PoolEntry poolEntry() {
            return poolEntry;
        }
    }

    @Nested
    @DisplayName("BucketPool Construction")
    class BucketPoolConstruction {

        @Test
        @DisplayName("Should create pool with specified bucket sizes")
        void testConstruction() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            long[] sizes = {256, 1024, 4096, 9000};

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, sizes, TestPoolable::new);

            assertEquals(4, pool.bucketCount());
            assertArrayEquals(sizes, pool.sizes());

            pool.close();
        }

        @Test
        @DisplayName("Should reject empty sizes array")
        void testEmptySizes() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{}, TestPoolable::new));
        }

        @Test
        @DisplayName("Should reject null sizes array")
        void testNullSizes() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, null, TestPoolable::new));
        }

        @Test
        @DisplayName("Should reject zero size")
        void testZeroSize() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{0, 100}, TestPoolable::new));
        }

        @Test
        @DisplayName("Should reject negative size")
        void testNegativeSize() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{-1, 100}, TestPoolable::new));
        }

        @Test
        @DisplayName("Should reject non-ascending sizes")
        void testNonAscendingSizes() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{100, 50, 200}, TestPoolable::new));
        }

        @Test
        @DisplayName("Should reject duplicate sizes")
        void testDuplicateSizes() {
            PoolSettings settings = new PoolSettings().capacity(10);

            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{100, 100, 200}, TestPoolable::new));
        }

        @Test
        @DisplayName("Should accept single bucket")
        void testSingleBucket() {
            PoolSettings settings = new PoolSettings().capacity(10);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{1024}, TestPoolable::new);

            assertEquals(1, pool.bucketCount());
            assertEquals(1024, pool.bucketSize(0));

            pool.close();
        }
    }

    @Nested
    @DisplayName("BucketPool Bucket Selection")
    class BucketPoolBucketSelection {

        BucketPool<TestPoolable> pool;
        long[] sizes = {256, 1024, 4096, 9000};

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            pool = new BucketPool<>(settings, sizes, TestPoolable::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Should select smallest fitting bucket")
        void testSmallestFit() {
            // Request 100 bytes - should use 256 bucket
            TestPoolable obj = pool.allocate(100);
            assertNotNull(obj);
            assertEquals(4, pool.bucket(0).available()); // 256 bucket used

            // Request 500 bytes - should use 1024 bucket
            TestPoolable obj2 = pool.allocate(500);
            assertNotNull(obj2);
            assertEquals(4, pool.bucket(1).available()); // 1024 bucket used

            // Request 2000 bytes - should use 4096 bucket
            TestPoolable obj3 = pool.allocate(2000);
            assertNotNull(obj3);
            assertEquals(4, pool.bucket(2).available()); // 4096 bucket used

            // Request 5000 bytes - should use 9000 bucket
            TestPoolable obj4 = pool.allocate(5000);
            assertNotNull(obj4);
            assertEquals(4, pool.bucket(3).available()); // 9000 bucket used
        }

        @Test
        @DisplayName("Should select exact match bucket")
        void testExactMatch() {
            TestPoolable obj = pool.allocate(256);
            assertNotNull(obj);
            assertEquals(4, pool.bucket(0).available());

            TestPoolable obj2 = pool.allocate(1024);
            assertNotNull(obj2);
            assertEquals(4, pool.bucket(1).available());

            TestPoolable obj3 = pool.allocate(4096);
            assertNotNull(obj3);
            assertEquals(4, pool.bucket(2).available());

            TestPoolable obj4 = pool.allocate(9000);
            assertNotNull(obj4);
            assertEquals(4, pool.bucket(3).available());
        }

        @Test
        @DisplayName("Should return null for size exceeding all buckets")
        void testSizeTooLarge() {
            TestPoolable obj = pool.allocate(10000);
            assertNull(obj);

            // All buckets should still be full
            assertEquals(5, pool.bucket(0).available());
            assertEquals(5, pool.bucket(1).available());
            assertEquals(5, pool.bucket(2).available());
            assertEquals(5, pool.bucket(3).available());
        }

        @Test
        @DisplayName("Should handle zero size allocation")
        void testZeroSizeAllocation() {
            // Zero size should use smallest bucket
            TestPoolable obj = pool.allocate(0);
            assertNotNull(obj);
            assertEquals(4, pool.bucket(0).available());
        }

        @Test
        @DisplayName("Default allocate should use smallest bucket")
        void testDefaultAllocate() {
            TestPoolable obj = pool.allocate();
            assertNotNull(obj);
            assertEquals(4, pool.bucket(0).available());
        }
    }

    @Nested
    @DisplayName("BucketPool Capacity")
    class BucketPoolCapacity {

        BucketPool<TestPoolable> pool;

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            pool = new BucketPool<>(settings, new long[]{256, 1024, 4096}, TestPoolable::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Total capacity should be sum of all buckets")
        void testTotalCapacity() {
            // 3 buckets * 10 each = 30
            assertEquals(30, pool.capacity());
        }

        @Test
        @DisplayName("Total available should be sum of all buckets")
        void testTotalAvailable() {
            assertEquals(30, pool.available());

            pool.allocate(100);  // from 256 bucket
            pool.allocate(500);  // from 1024 bucket
            pool.allocate(2000); // from 4096 bucket

            assertEquals(27, pool.available());
        }

        @Test
        @DisplayName("minCapacity should be sum of all bucket minCapacities")
        void testMinCapacity() {
            assertEquals(30, pool.minCapacity());
        }

        @Test
        @DisplayName("maxCapacity should be sum of all bucket maxCapacities")
        void testMaxCapacity() {
            assertEquals(30, pool.maxCapacity());
        }

        @Test
        @DisplayName("maxByteSize should be largest bucket size")
        void testMaxByteSize() {
            assertEquals(4096, pool.maxByteSize());
        }

        @Test
        @DisplayName("Per-bucket capacity should be accessible")
        void testPerBucketCapacity() {
            assertEquals(10, pool.bucket(0).capacity());
            assertEquals(10, pool.bucket(1).capacity());
            assertEquals(10, pool.bucket(2).capacity());
        }

        @Test
        @DisplayName("Per-bucket available should be accessible")
        void testPerBucketAvailable() {
            pool.allocate(100);
            pool.allocate(100);

            assertEquals(8, pool.bucket(0).available());
            assertEquals(10, pool.bucket(1).available());
            assertEquals(10, pool.bucket(2).available());
        }
    }

    @Nested
    @DisplayName("BucketPool Recycle")
    class BucketPoolRecycle {

        BucketPool<TestPoolable> pool;

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            pool = new BucketPool<>(settings, new long[]{256, 1024, 4096}, TestPoolable::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Recycle should return to correct bucket")
        void testRecycleToCorrectBucket() {
            TestPoolable obj256 = pool.allocate(100);
            TestPoolable obj1024 = pool.allocate(500);
            TestPoolable obj4096 = pool.allocate(2000);

            assertEquals(4, pool.bucket(0).available());
            assertEquals(4, pool.bucket(1).available());
            assertEquals(4, pool.bucket(2).available());

            obj1024.recycle();
            assertEquals(4, pool.bucket(0).available());
            assertEquals(5, pool.bucket(1).available());
            assertEquals(4, pool.bucket(2).available());

            obj256.recycle();
            assertEquals(5, pool.bucket(0).available());
            assertEquals(5, pool.bucket(1).available());
            assertEquals(4, pool.bucket(2).available());

            obj4096.recycle();
            assertEquals(5, pool.bucket(0).available());
            assertEquals(5, pool.bucket(1).available());
            assertEquals(5, pool.bucket(2).available());
        }

        @Test
        @DisplayName("Recycle should call onRecycle callback")
        void testRecycleCallback() {
            TestPoolable obj = pool.allocate(100);
            assertEquals(0, obj.recycleCount);

            obj.recycle();
            assertEquals(1, obj.recycleCount);
        }

        @Test
        @DisplayName("Multiple recycles should work")
        void testMultipleRecycles() {
            TestPoolable obj = pool.allocate(100);

            obj.recycle();
            assertEquals(5, pool.bucket(0).available());

            // Allocate same object again
            TestPoolable obj2 = pool.allocate(100);
            obj2.recycle();
            assertEquals(5, pool.bucket(0).available());
        }
    }

    @Nested
    @DisplayName("BucketPool Exhaustion")
    class BucketPoolExhaustion {

        BucketPool<TestPoolable> pool;

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(3)
                    .maxCapacity(3);

            pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Should return null when specific bucket exhausted")
        void testBucketExhaustion() {
            // Exhaust 256 bucket
            pool.allocate(100);
            pool.allocate(100);
            pool.allocate(100);

            assertEquals(0, pool.bucket(0).available());
            assertEquals(3, pool.bucket(1).available());

            // Next 256-fitting allocation should fail (doesn't spill to larger)
            TestPoolable obj = pool.allocate(100);
            assertNull(obj);
        }

        @Test
        @DisplayName("Should track exhaustions in metrics")
        void testExhaustionMetrics() {
            // Exhaust 256 bucket
            pool.allocate(100);
            pool.allocate(100);
            pool.allocate(100);

            // Try more allocations
            pool.allocate(100);
            pool.allocate(100);

            assertTrue(pool.metrics().exhaustions() >= 2);
        }

        @Test
        @DisplayName("Large size should not spill to smaller bucket")
        void testNoSpillToSmaller() {
            // Exhaust 1024 bucket
            pool.allocate(500);
            pool.allocate(500);
            pool.allocate(500);

            assertEquals(3, pool.bucket(0).available()); // 256 bucket untouched
            assertEquals(0, pool.bucket(1).available()); // 1024 exhausted

            // Request 500 should fail, not use 256 bucket
            TestPoolable obj = pool.allocate(500);
            assertNull(obj);
            assertEquals(3, pool.bucket(0).available()); // Still untouched
        }
    }

    @Nested
    @DisplayName("BucketPool Metrics")
    class BucketPoolMetrics {

        BucketPool<TestPoolable> pool;

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            pool = new BucketPool<>(settings, new long[]{256, 1024, 4096}, TestPoolable::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Should track total allocations")
        void testTotalAllocations() {
            pool.allocate(100);
            pool.allocate(500);
            pool.allocate(2000);
            pool.allocate(100);

            assertEquals(4, pool.metrics().allocations());
        }

        @Test
        @DisplayName("Should track total releases")
        void testTotalReleases() {
            TestPoolable obj1 = pool.allocate(100);
            TestPoolable obj2 = pool.allocate(500);
            pool.allocate(2000);

            obj1.recycle();
            obj2.recycle();

            assertEquals(2, pool.metrics().releases());
        }

        @Test
        @DisplayName("Should track per-bucket allocations")
        void testPerBucketAllocations() {
            pool.allocate(100);
            pool.allocate(100);
            pool.allocate(500);
            pool.allocate(2000);
            pool.allocate(2000);
            pool.allocate(2000);

            PoolMetrics metrics = pool.metrics();
            assertEquals(2, metrics.allocations(0)); // 256 bucket
            assertEquals(1, metrics.allocations(1)); // 1024 bucket
            assertEquals(3, metrics.allocations(2)); // 4096 bucket
        }

        @Test
        @DisplayName("Should report bucket count")
        void testBucketCount() {
            assertEquals(3, pool.metrics().bucketCount());
        }

        @Test
        @DisplayName("Should report bucket sizes")
        void testBucketSizes() {
            PoolMetrics metrics = pool.metrics();
            assertEquals(256, metrics.bucketSize(0));
            assertEquals(1024, metrics.bucketSize(1));
            assertEquals(4096, metrics.bucketSize(2));
        }

        @Test
        @DisplayName("Should report per-bucket capacity")
        void testBucketCapacity() {
            PoolMetrics metrics = pool.metrics();
            assertEquals(10, metrics.bucketCapacity(0));
            assertEquals(10, metrics.bucketCapacity(1));
            assertEquals(10, metrics.bucketCapacity(2));
        }

        @Test
        @DisplayName("Should report per-bucket available")
        void testBucketAvailable() {
            pool.allocate(100);
            pool.allocate(100);
            pool.allocate(500);

            PoolMetrics metrics = pool.metrics();
            assertEquals(8, metrics.bucketAvailable(0));
            assertEquals(9, metrics.bucketAvailable(1));
            assertEquals(10, metrics.bucketAvailable(2));
        }
    }

    @Nested
    @DisplayName("BucketPool Dynamic Sizing")
    class BucketPoolDynamicSizing {

        @Test
        @DisplayName("Should grow all buckets proportionally")
        void testGrow() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(50);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);

            assertEquals(10, pool.capacity()); // 2 * 5

            long grown = pool.grow(10); // 5 per bucket
            assertTrue(grown > 0);
            assertTrue(pool.capacity() > 10);

            pool.close();
        }

        @Test
        @DisplayName("Should contract all buckets proportionally")
        void testContract() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(50);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);

            pool.grow(20); // Grow significantly
            long capacityBefore = pool.capacity();

            long contracted = pool.contractUnused(10);
            assertTrue(contracted > 0);
            assertTrue(pool.capacity() < capacityBefore);

            pool.close();
        }
    }

    @Nested
    @DisplayName("BucketPool Lifecycle")
    class BucketPoolLifecycle {

        @Test
        @DisplayName("Close should close all buckets")
        void testClose() {
            PoolSettings settings = new PoolSettings().capacity(5);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);
            pool.close();

            assertTrue(pool.isClosed());
            assertTrue(pool.bucket(0).isClosed());
            assertTrue(pool.bucket(1).isClosed());
        }

        @Test
        @DisplayName("Allocate on closed pool should return null")
        void testAllocateOnClosed() {
            PoolSettings settings = new PoolSettings().capacity(5);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);
            pool.close();

            assertNull(pool.allocate(100));
            assertNull(pool.allocate(500));
        }

        @Test
        @DisplayName("Double close should be safe")
        void testDoubleClose() {
            PoolSettings settings = new PoolSettings().capacity(5);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);
            pool.close();

            assertDoesNotThrow(pool::close);
        }
    }

    @Nested
    @DisplayName("BucketPool Concurrency")
    class BucketPoolConcurrency {

        @Test
        @DisplayName("Should handle concurrent allocations from different buckets")
        void testConcurrentDifferentBuckets() throws InterruptedException {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(100)
                    .maxCapacity(100);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024, 4096}, TestPoolable::new);

            int threadsPerBucket = 10;
            int allocationsPerThread = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadsPerBucket * 3);
            AtomicInteger successCount = new AtomicInteger();

            ExecutorService executor = Executors.newFixedThreadPool(threadsPerBucket * 3);

            // Threads for 256 bucket
            for (int i = 0; i < threadsPerBucket; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < allocationsPerThread; j++) {
                            if (pool.allocate(100) != null) {
                                successCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Threads for 1024 bucket
            for (int i = 0; i < threadsPerBucket; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < allocationsPerThread; j++) {
                            if (pool.allocate(500) != null) {
                                successCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Threads for 4096 bucket
            for (int i = 0; i < threadsPerBucket; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < allocationsPerThread; j++) {
                            if (pool.allocate(2000) != null) {
                                successCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            // Each bucket has 100, 10 threads * 10 allocations = 100 per bucket
            assertEquals(300, successCount.get());
            assertEquals(0, pool.available());

            executor.shutdown();
            pool.close();
        }

        @Test
        @DisplayName("Should handle concurrent allocate and recycle")
        void testConcurrentAllocateRecycle() throws InterruptedException {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, new long[]{256, 1024}, TestPoolable::new);

            int iterations = 500;
            CountDownLatch doneLatch = new CountDownLatch(4);
            AtomicInteger allocations = new AtomicInteger();

            // Two threads for each bucket size
            for (int size : new int[]{100, 500}) {
                for (int t = 0; t < 2; t++) {
                    final int reqSize = size;
                    new Thread(() -> {
                        for (int i = 0; i < iterations; i++) {
                            TestPoolable obj = pool.allocate(reqSize);
                            if (obj != null) {
                                allocations.incrementAndGet();
                                obj.recycle();
                            }
                        }
                        doneLatch.countDown();
                    }).start();
                }
            }

            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            assertEquals(20, pool.available()); // All returned

            pool.close();
        }
    }

    @Nested
    @DisplayName("BucketPool Realistic Sizes")
    class BucketPoolRealisticSizes {

        @Test
        @DisplayName("Should work with network packet sizes")
        void testNetworkPacketSizes() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(100)
                    .maxCapacity(1000);

            // Realistic packet sizes: min ethernet, standard MTU, jumbo, TSO segments
            long[] sizes = {64, 1518, 9000, 16384, 65536};

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, sizes, TestPoolable::new);

            assertEquals(5, pool.bucketCount());
            assertEquals(65536, pool.maxByteSize());

            // Allocate various sizes
            assertNotNull(pool.allocate(60));    // -> 64 bucket
            assertNotNull(pool.allocate(1500));  // -> 1518 bucket
            assertNotNull(pool.allocate(8000));  // -> 9000 bucket
            assertNotNull(pool.allocate(10000)); // -> 16384 bucket
            assertNotNull(pool.allocate(32000)); // -> 65536 bucket

            pool.close();
        }

        @Test
        @DisplayName("Should handle power-of-two sizes")
        void testPowerOfTwoSizes() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            long[] sizes = {256, 512, 1024, 2048, 4096, 8192};

            BucketPool<TestPoolable> pool = new BucketPool<>(settings, sizes, TestPoolable::new);

            assertEquals(6, pool.bucketCount());

            // Test boundary allocations
            assertNotNull(pool.allocate(256));  // exact fit
            assertNotNull(pool.allocate(257));  // -> 512
            assertNotNull(pool.allocate(1000)); // -> 1024
            assertNotNull(pool.allocate(1025)); // -> 2048

            pool.close();
        }
    }
}