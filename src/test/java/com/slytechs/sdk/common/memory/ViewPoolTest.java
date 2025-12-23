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
package com.slytechs.sdk.common.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive test suite for ViewPool.
 * 
 * Tests view pooling with dynamic capacity, min/max bounds, automatic
 * unbinding, and concurrent operations.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("ViewPool Tests")
class ViewPoolTest {

	private static final int MIN_CAPACITY = 5;
	private static final int MAX_CAPACITY = 20;

	private ViewPool<TestView> pool;
	private Arena arena;

	// Test view class
	static class TestView extends BoundView {
		boolean customFlag = false;
		int bindCount = 0;

		@Override
		public void onBind() {
			bindCount++;
			customFlag = true;
		}

		@Override
		public void onUnbind() {
			customFlag = false;
		}

		@Override
		void onRecycle() {
			bindCount = 0;
			customFlag = false;
		}
	}

	@BeforeEach
	void setUp() {
		pool = new ViewPool<>("test-pool", TestView.class, MIN_CAPACITY, MAX_CAPACITY);
		arena = Arena.ofConfined();
	}

	// ==================== Basic Pool Operations ====================

	@Nested
	@DisplayName("Basic Pool Operations")
	class BasicPoolOperations {

		@Test
		@DisplayName("Pool initializes with minimum capacity")
		void testPoolInitialization() {
			assertEquals(MAX_CAPACITY, pool.capacity());
			assertEquals(MIN_CAPACITY, pool.available());
			assertEquals("test-pool", pool.getMetrics().getName());
		}

		@Test
		@DisplayName("Allocate from pre-allocated views")
		void testAllocatePreAllocated() {
			List<TestView> views = new ArrayList<>();

			// Allocate minimum capacity
			for (int i = 0; i < MIN_CAPACITY; i++) {
				TestView view = pool.allocate();
				assertNotNull(view);
				assertFalse(view.isBound());
				views.add(view);
			}

			assertEquals(0, pool.available());

			// Next allocation should create new (up to max)
			TestView extra = pool.allocate();
			assertNotNull(extra);

			// Release all
			views.forEach(pool::release);
			pool.release(extra);
		}

		@Test
		@DisplayName("Dynamic expansion up to maximum")
		void testDynamicExpansion() {
			List<TestView> views = new ArrayList<>();

			// Allocate up to maximum
			for (int i = 0; i < MAX_CAPACITY; i++) {
				TestView view = pool.allocate();
				assertNotNull(view, "Should allocate up to max capacity");
				views.add(view);
			}

			// Pool exhausted
			assertNull(pool.allocate());
			assertEquals(0, pool.available());

			// Release all
			views.forEach(pool::release);

			// Should have more than minimum available now
			assertTrue(pool.available() >= MIN_CAPACITY);
		}

		@Test
		@DisplayName("View is recycled properly")
		void testViewRecycling() {
			TestView view = pool.allocate();
			MemorySegment segment = arena.allocate(256);
			Memory memory = Memory.of(segment, 0, 256);

			// Use the view
			view.bind(memory);
			assertTrue(view.customFlag);
			assertEquals(1, view.bindCount);

			// Release back to pool
			pool.release(view);

			// Should be unbound and reset
			assertFalse(view.isBound());
			assertEquals(0, view.bindCount);
			assertFalse(view.customFlag);

			// Allocate again - might get same view
			TestView recycled = pool.allocate();
			assertNotNull(recycled);
			assertFalse(recycled.isBound());
			assertEquals(0, recycled.bindCount);
		}
	}

	// ==================== View Lifecycle ====================

	@Nested
	@DisplayName("View Lifecycle")
	class ViewLifecycleTests {

		@Test
		@DisplayName("Views unbind when released")
		void testUnbindOnRelease() {
			TestView view = pool.allocate();
			MemorySegment segment = arena.allocate(512);
			Memory memory = Memory.of(segment, 0, 512);

			view.bind(memory);
			assertTrue(view.isBound());

			pool.release(view);

			assertFalse(view.isBound());
			assertFalse(view.customFlag);
		}

		@Test
		@DisplayName("Bound views can be rebound")
		void testRebinding() {
			TestView view = pool.allocate();
			MemorySegment seg1 = arena.allocate(256);
			MemorySegment seg2 = arena.allocate(256);
			Memory mem1 = Memory.of(seg1, 0, 256);
			Memory mem2 = Memory.of(seg2, 0, 256);

			view.bind(mem1);
			assertEquals(1, view.bindCount);

			view.bind(mem2);
			assertEquals(2, view.bindCount);

			pool.release(view);
			assertEquals(0, view.bindCount); // Reset on recycle
		}

		@Test
		@DisplayName("Multiple views can bind to same memory")
		void testMultipleViewsSameMemory() {
			MemorySegment segment = arena.allocate(1024);
			Memory memory = Memory.of(segment, 0, 1024);

			TestView view1 = pool.allocate();
			TestView view2 = pool.allocate();
			TestView view3 = pool.allocate();

			// All bind to same memory at different offsets
			view1.bind(memory, 0, 100);
			view2.bind(memory, 100, 100);
			view3.bind(memory, 200, 100);

			assertTrue(view1.isBound());
			assertTrue(view2.isBound());
			assertTrue(view3.isBound());

			// Views don't affect memory refcount
			assertEquals(1, memory.refCount());

			// Release all
			pool.release(view1);
			pool.release(view2);
			pool.release(view3);

			assertFalse(view1.isBound());
			assertFalse(view2.isBound());
			assertFalse(view3.isBound());
		}
	}

	// ==================== Pool Compaction ====================

	@Nested
	@DisplayName("Pool Compaction")
	class PoolCompactionTests {

		@Test
		@DisplayName("Compact removes excess views")
		void testCompaction() {
			List<TestView> views = new ArrayList<>();

			// Allocate maximum
			for (int i = 0; i < MAX_CAPACITY; i++) {
				views.add(pool.allocate());
			}

			// Release all
			views.forEach(pool::release);

			// Should have MAX_CAPACITY available
			assertTrue(pool.available() >= MIN_CAPACITY);

			// Compact to minimum
			pool.compact();

			// Should have around MIN_CAPACITY available
			assertTrue(pool.available() >= MIN_CAPACITY);
			assertTrue(pool.available() <= MIN_CAPACITY + 5); // Some tolerance
		}

		@Test
		@DisplayName("Compact preserves minimum capacity")
		void testCompactPreservesMinimum() {
			// Start with minimum
			assertEquals(MIN_CAPACITY, pool.available());

			// Compact should not go below minimum
			pool.compact();

			assertTrue(pool.available() >= MIN_CAPACITY);
		}
	}

	// ==================== Concurrent Operations ====================

	@Nested
	@DisplayName("Concurrent Operations")
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	class ConcurrentOperationsTests {

		@Test
		@DisplayName("Concurrent allocation and release")
		void testConcurrentAllocationRelease() throws InterruptedException {
			int threads = 10;
			int iterations = 100;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threads);
			AtomicInteger successCount = new AtomicInteger();

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();

						for (int j = 0; j < iterations; j++) {
							TestView view = pool.allocate();
							if (view != null) {
								// Bind to some memory
								try (Arena localArena = Arena.ofConfined()) {
									MemorySegment seg = localArena.allocate(256);
									Memory mem = Memory.of(seg, 0, 256);
									view.bind(mem);

									// Do some work
									assertTrue(view.isBound());

									// Release
									pool.release(view);
									successCount.incrementAndGet();
								}
							}

							if (j % 10 == 0) {
								Thread.yield();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			doneLatch.await();
			executor.shutdown();

			assertTrue(successCount.get() > 0);
			assertTrue(pool.available() >= MIN_CAPACITY);
		}

		@Test
		@DisplayName("Pool exhaustion under contention")
		void testPoolExhaustion() throws InterruptedException {
			int threads = 30; // More than MAX_CAPACITY
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch holdLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threads);
			AtomicInteger allocatedCount = new AtomicInteger();
			AtomicInteger nullCount = new AtomicInteger();

			List<TestView> allocated = new ArrayList<>();
			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						TestView view = pool.allocate();
						if (view != null) {
							synchronized (allocated) {
								allocated.add(view);
							}
							allocatedCount.incrementAndGet();
							holdLatch.await(); // Hold the view
						} else {
							nullCount.incrementAndGet();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			Thread.sleep(100); // Let allocations happen

			// Should allocate up to MAX_CAPACITY
			assertTrue(allocatedCount.get() <= MAX_CAPACITY);
			assertTrue(nullCount.get() > 0); // Some should fail

			holdLatch.countDown();
			doneLatch.await();

			// Release all
			synchronized (allocated) {
				allocated.forEach(pool::release);
			}

			executor.shutdown();

			assertTrue(pool.available() >= MIN_CAPACITY);
		}
	}

	// ==================== Edge Cases ====================

	@Nested
	@DisplayName("Edge Cases")
	class EdgeCasesTests {

		@Test
		@DisplayName("Release null view is safe")
		void testReleaseNull() {
			assertDoesNotThrow(() -> pool.release(null));
		}

		@Test
		@DisplayName("Release view from different pool is ignored")
		void testReleaseForeignView() {
			ViewPool<TestView> otherPool = new ViewPool<>("other", TestView.class, 1, 5);
			TestView view = otherPool.allocate();

			// Should not crash or affect our pool
			pool.release(view);
			assertEquals(MIN_CAPACITY, pool.available());

			// View should still be allocated in its own pool
			assertNotNull(view);
		}

		@Test
		@DisplayName("Pool with capacity of 1")
		void testSingleCapacityPool() {
			ViewPool<TestView> singlePool = new ViewPool<>("single", TestView.class, 1, 1);

			TestView view = singlePool.allocate();
			assertNotNull(view);

			assertNull(singlePool.allocate()); // Exhausted

			singlePool.release(view);
			assertNotNull(singlePool.allocate()); // Available again
		}

		@Test
		@DisplayName("Factory-based pool creation")
		void testFactoryPool() {
			AtomicInteger createdCount = new AtomicInteger();

			ViewPool<TestView> factoryPool = new ViewPool<TestView>("factory",
					() -> {
						createdCount.incrementAndGet();
						return new TestView();
					},
					2, 10);

			assertEquals(2, createdCount.get()); // Min capacity pre-allocated

			// Allocate more
			List<TestView> views = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				views.add(factoryPool.allocate());
			}

			assertEquals(5, createdCount.get()); // Created on demand

			views.forEach(factoryPool::release);
		}
	}
}