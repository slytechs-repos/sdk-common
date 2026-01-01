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

import java.util.ArrayList;
import java.util.List;
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

@DisplayName("Pool Test Suite")
class PoolTest {

    static class TestObject implements Poolable {
        private final PoolEntry poolEntry = new PoolEntry() {
            @Override
            protected void onAllocate() {
                allocateCount.incrementAndGet();
            }

            @Override
            protected void onRecycle() {
                recycleCount.incrementAndGet();
                data = null;
            }
        };

        final AtomicInteger allocateCount = new AtomicInteger();
        final AtomicInteger recycleCount = new AtomicInteger();
        String data;

        @Override
        public PoolEntry poolEntry() {
            return poolEntry;
        }
    }

    static class SizedTestObject implements Poolable {
        private final PoolEntry poolEntry = new PoolEntry();
        final long size;

        SizedTestObject() {
            this.size = 0;
        }

        SizedTestObject(long size) {
            this.size = size;
        }

        @Override
        public PoolEntry poolEntry() {
            return poolEntry;
        }
    }

    @Nested
    @DisplayName("FreeListPool Basic Operations")
    class FreeListPoolBasicOperations {

        Pool<TestObject> pool;
        PoolSettings settings;

        @BeforeEach
        void setUp() {
            settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);
            pool = new FreeListPool<>(settings, TestObject::new);
        }

        @AfterEach
        void tearDown() {
            if (pool != null && !pool.isClosed()) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Pool should preallocate minCapacity objects")
        void testPreallocation() {
            assertEquals(10, pool.available());
            assertEquals(10, pool.capacity());
        }

        @Test
        @DisplayName("Allocate should return non-null object")
        void testAllocate() {
            TestObject obj = pool.allocate();
            assertNotNull(obj);
            assertEquals(9, pool.available());
        }

        @Test
        @DisplayName("Allocate should call onAllocate callback")
        void testAllocateCallback() {
            TestObject obj = pool.allocate();
            assertEquals(1, obj.allocateCount.get());
        }

        @Test
        @DisplayName("Recycle should return object to pool")
        void testRecycle() {
            TestObject obj = pool.allocate();
            assertEquals(9, pool.available());

            obj.recycle();
            assertEquals(10, pool.available());
        }

        @Test
        @DisplayName("Recycle should call onRecycle callback")
        void testRecycleCallback() {
            TestObject obj = pool.allocate();
            obj.data = "test";

            obj.recycle();
            assertEquals(1, obj.recycleCount.get());
            assertNull(obj.data);
        }

        @Test
        @DisplayName("Allocate should return null when pool exhausted")
        void testExhaustion() {
            List<TestObject> allocated = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                allocated.add(pool.allocate());
            }

            assertNull(pool.allocate());
            assertEquals(0, pool.available());
            assertEquals(1, pool.metrics().exhaustions());
        }

        @Test
        @DisplayName("Objects can be reused after recycle")
        void testReuse() {
            TestObject obj1 = pool.allocate();
            obj1.data = "first";
            obj1.recycle();

            TestObject obj2 = pool.allocate();
            assertNull(obj2.data);
            assertEquals(2, obj2.allocateCount.get());
        }

        @Test
        @DisplayName("Pool release should work same as recycle")
        void testPoolRelease() {
            TestObject obj = pool.allocate();
            assertEquals(9, pool.available());

            pool.release(obj);
            assertEquals(10, pool.available());
        }

        @Test
        @DisplayName("Release null should be no-op")
        void testReleaseNull() {
            pool.release(null);
            assertEquals(10, pool.available());
        }
    }

    @Nested
    @DisplayName("FreeListPool Dynamic Sizing")
    class FreeListPoolDynamicSizing {

        @Test
        @DisplayName("Pool should grow when exhausted up to maxCapacity")
        void testAutoGrow() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(20);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            assertEquals(5, pool.capacity());

            // Exhaust initial capacity
            for (int i = 0; i < 5; i++) {
                pool.allocate();
            }
            assertEquals(0, pool.available());

            // Next allocate should trigger growth
            TestObject obj = pool.allocate();
            assertNotNull(obj);
            assertTrue(pool.capacity() > 5);

            pool.close();
        }

        @Test
        @DisplayName("Pool should not grow beyond maxCapacity")
        void testMaxCapacityLimit() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(10);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            // Allocate all
            List<TestObject> allocated = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                TestObject obj = pool.allocate();
                if (obj != null) {
                    allocated.add(obj);
                }
            }

            assertEquals(10, allocated.size());
            assertEquals(10, pool.capacity());
            assertTrue(pool.metrics().exhaustions() > 0);

            pool.close();
        }

        @Test
        @DisplayName("Manual grow should work")
        void testManualGrow() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            assertEquals(5, pool.capacity());

            long grown = pool.grow(10);
            assertEquals(10, grown);
            assertEquals(15, pool.capacity());

            pool.close();
        }

        @Test
        @DisplayName("Contract should reduce capacity")
        void testContract() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            pool.grow(20); // Now at 25
            assertEquals(25, pool.capacity());

            long contracted = pool.contractUnused(10);
            assertEquals(10, contracted);
            assertEquals(15, pool.capacity());

            pool.close();
        }

        @Test
        @DisplayName("Contract should not go below minCapacity")
        void testContractMinCapacity() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            pool.grow(10); // Now at 20
            assertEquals(20, pool.capacity());

            // Try to contract 15 - should only contract 10 (down to min)
            long contracted = pool.contractUnused(15);
            assertEquals(10, contracted);
            assertEquals(10, pool.capacity());

            pool.close();
        }
    }

    @Nested
    @DisplayName("Pool Lifecycle")
    class PoolLifecycle {

        @Test
        @DisplayName("Close should mark pool as closed")
        void testClose() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            pool.close();

            assertTrue(pool.isClosed());
        }

        @Test
        @DisplayName("Allocate on closed pool should return null")
        void testAllocateOnClosed() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            pool.close();

            assertNull(pool.allocate());
        }

        @Test
        @DisplayName("Double close should be safe")
        void testDoubleClose() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);
            pool.close();

            assertDoesNotThrow(pool::close);
        }
    }

    @Nested
    @DisplayName("Pool Metrics")
    class PoolMetricsTest {

        @Test
        @DisplayName("Metrics should track allocations")
        void testAllocationMetrics() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            pool.allocate();
            pool.allocate();
            pool.allocate();

            assertEquals(3, pool.metrics().allocations());

            pool.close();
        }

        @Test
        @DisplayName("Metrics should track releases")
        void testReleaseMetrics() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            TestObject obj1 = pool.allocate();
            pool.allocate();
            obj1.recycle();

            assertEquals(2, pool.metrics().allocations());
            assertEquals(1, pool.metrics().releases());

            pool.close();
        }

        @Test
        @DisplayName("Metrics should track exhaustions")
        void testExhaustionMetrics() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(2)
                    .maxCapacity(2);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            pool.allocate();
            pool.allocate();
            pool.allocate(); // exhaustion
            pool.allocate(); // exhaustion

            assertEquals(2, pool.metrics().exhaustions());

            pool.close();
        }

        @Test
        @DisplayName("Metrics should track growth events")
        void testGrowthMetrics() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            pool.grow(10);
            pool.grow(10);

            assertEquals(3, pool.metrics().growthEvents()); // 1 initial + 2 manual

            pool.close();
        }

        @Test
        @DisplayName("Metrics should track contractions")
        void testContractionMetrics() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            pool.grow(20);
            pool.contractUnused(5);
            pool.contractUnused(5);

            assertEquals(2, pool.metrics().contractions());
            assertEquals(10, pool.metrics().evictions());

            pool.close();
        }
    }

    @Nested
    @DisplayName("Pool Concurrency")
    class PoolConcurrency {

        @Test
        @DisplayName("Pool should handle concurrent allocations")
        void testConcurrentAllocations() throws InterruptedException {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(100)
                    .maxCapacity(100);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            int threadCount = 10;
            int allocationsPerThread = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < allocationsPerThread; j++) {
                            TestObject obj = pool.allocate();
                            if (obj != null) {
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
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

            assertEquals(100, successCount.get());
            assertEquals(0, pool.available());

            executor.shutdown();
            pool.close();
        }

        @Test
        @DisplayName("Pool should handle concurrent allocate and recycle")
        void testConcurrentAllocateRecycle() throws InterruptedException {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10);

            Pool<TestObject> pool = new FreeListPool<>(settings, TestObject::new);

            int iterations = 1000;
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger allocations = new AtomicInteger();
            AtomicInteger recycles = new AtomicInteger();

            Thread allocator = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    TestObject obj = pool.allocate();
                    if (obj != null) {
                        allocations.incrementAndGet();
                        obj.recycle();
                        recycles.incrementAndGet();
                    }
                }
                doneLatch.countDown();
            });

            Thread recycler = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    TestObject obj = pool.allocate();
                    if (obj != null) {
                        allocations.incrementAndGet();
                        obj.recycle();
                        recycles.incrementAndGet();
                    }
                }
                doneLatch.countDown();
            });

            allocator.start();
            recycler.start();

            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            assertEquals(allocations.get(), recycles.get());
            assertEquals(10, pool.available());

            pool.close();
        }
    }

    @Nested
    @DisplayName("Pool Non-Pooled Objects")
    class PoolNonPooledObjects {

        @Test
        @DisplayName("Recycle on non-pooled object should be no-op")
        void testRecycleNonPooled() {
            TestObject obj = new TestObject();
            assertFalse(obj.poolEntry().isPooled());

            assertDoesNotThrow(obj::recycle);
        }

        @Test
        @DisplayName("Non-pooled object should have null owning pool")
        void testNonPooledOwner() {
            TestObject obj = new TestObject();
            assertNull(obj.poolEntry().owningPool());
        }
    }

    @Nested
    @DisplayName("PoolSettings")
    class PoolSettingsTest {

        @Test
        @DisplayName("Default settings should have expected values")
        void testDefaultSettings() {
            PoolSettings settings = new PoolSettings();
            assertEquals(64, settings.minCapacity());
            assertEquals(1024, settings.maxCapacity());
            assertEquals(0, settings.segmentSize());
            assertFalse(settings.contractionEnabled());
        }

        @Test
        @DisplayName("Settings should be chainable")
        void testChainableSettings() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(100)
                    .maxCapacity(500)
                    .segmentSize(1024)
                    .contractionEnabled(true);

            assertEquals(100, settings.minCapacity());
            assertEquals(500, settings.maxCapacity());
            assertEquals(1024, settings.segmentSize());
            assertTrue(settings.contractionEnabled());
        }

        @Test
        @DisplayName("Fixed capacity should set min and max equal")
        void testFixedCapacity() {
            PoolSettings settings = new PoolSettings().capacity(100);

            assertEquals(100, settings.minCapacity());
            assertEquals(100, settings.maxCapacity());
        }
    }

    @Nested
    @DisplayName("BucketPool")
    class BucketPoolTest {

        BucketPool<SizedTestObject> bucketPool;

        @BeforeEach
        void setUp() {
            PoolSettings settings = new PoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5);

            long[] sizes = {256, 1024, 4096, 9000};

            bucketPool = new BucketPool<>(settings, sizes, SizedTestObject::new);
        }

        @AfterEach
        void tearDown() {
            if (bucketPool != null) {
                bucketPool.close();
            }
        }

        @Test
        @DisplayName("BucketPool should create buckets for each size")
        void testBucketCount() {
            assertEquals(4, bucketPool.bucketCount());
        }

        @Test
        @DisplayName("BucketPool should have correct sizes")
        void testBucketSizes() {
            assertArrayEquals(new long[]{256, 1024, 4096, 9000}, bucketPool.sizes());
        }

        @Test
        @DisplayName("Allocate should find smallest fitting bucket")
        void testAllocateSmallestFit() {
            // Note: SizedTestObject doesn't actually use size, just tests bucket selection
            SizedTestObject obj = bucketPool.allocate(100);
            assertNotNull(obj);

            SizedTestObject obj2 = bucketPool.allocate(500);
            assertNotNull(obj2);

            SizedTestObject obj3 = bucketPool.allocate(5000);
            assertNotNull(obj3);
        }

        @Test
        @DisplayName("Allocate exact size should work")
        void testAllocateExactSize() {
            SizedTestObject obj = bucketPool.allocate(256);
            assertNotNull(obj);
        }

        @Test
        @DisplayName("Allocate too large should return null")
        void testAllocateTooLarge() {
            SizedTestObject obj = bucketPool.allocate(10000);
            assertNull(obj);
        }

        @Test
        @DisplayName("Recycle should return to correct bucket")
        void testRecycleToCorrectBucket() {
            Pool<SizedTestObject> bucket0 = bucketPool.bucket(0);
            assertEquals(5, bucket0.available());

            SizedTestObject obj = bucketPool.allocate(100);
            assertEquals(4, bucket0.available());

            obj.recycle();
            assertEquals(5, bucket0.available());
        }

        @Test
        @DisplayName("Total capacity should be sum of all buckets")
        void testTotalCapacity() {
            assertEquals(20, bucketPool.capacity());
        }

        @Test
        @DisplayName("Total available should be sum of all buckets")
        void testTotalAvailable() {
            assertEquals(20, bucketPool.available());

            bucketPool.allocate(100);
            bucketPool.allocate(500);

            assertEquals(18, bucketPool.available());
        }

        @Test
        @DisplayName("Bucket metrics should be accessible via PoolMetrics interface")
        void testBucketMetrics() {
            bucketPool.allocate(100);
            bucketPool.allocate(100);

            PoolMetrics metrics = bucketPool.metrics();
            assertEquals(2, metrics.allocations(0));
        }

        @Test
        @DisplayName("Aggregated metrics should work")
        void testAggregatedMetrics() {
            bucketPool.allocate(100);
            bucketPool.allocate(500);
            bucketPool.allocate(5000);

            PoolMetrics metrics = bucketPool.metrics();
            assertEquals(3, metrics.allocations());
        }

        @Test
        @DisplayName("Sizes must be positive")
        void testInvalidSizes() {
            PoolSettings settings = new PoolSettings().capacity(5);
            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{0, 100}, SizedTestObject::new));
        }

        @Test
        @DisplayName("Sizes must be ascending")
        void testSizesNotAscending() {
            PoolSettings settings = new PoolSettings().capacity(5);
            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{100, 50}, SizedTestObject::new));
        }

        @Test
        @DisplayName("Sizes array cannot be empty")
        void testEmptySizes() {
            PoolSettings settings = new PoolSettings().capacity(5);
            assertThrows(IllegalArgumentException.class, () ->
                    new BucketPool<>(settings, new long[]{}, SizedTestObject::new));
        }
    }
}