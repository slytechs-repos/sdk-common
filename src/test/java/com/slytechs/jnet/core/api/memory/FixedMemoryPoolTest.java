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
package com.slytechs.jnet.core.api.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive test suite for FixedMemoryPool normal usage patterns.
 * 
 * Tests expected behavior under typical usage scenarios including allocation,
 * release, automatic return to pool, and concurrent operations.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("FixedMemoryPool Tests")
class FixedMemoryPoolTest {

	private static final int DEFAULT_CAPACITY = 10;
	private static final long DEFAULT_SEGMENT_SIZE = 1024L;
	private static final long DEFAULT_HEADROOM = 128L;

	private FixedMemoryPool pool;

	@BeforeEach
	void setUp() {
		pool = new FixedMemoryPool("test-pool", DEFAULT_CAPACITY,
				DEFAULT_SEGMENT_SIZE, DEFAULT_HEADROOM);
	}

	// ==================== Basic Pool Operations ====================

	@Nested
	@DisplayName("Basic Pool Operations")
	class BasicPoolOperations {

		@Test
		@DisplayName("Pool initializes with correct capacity")
		void testPoolInitialization() {
			assertEquals(DEFAULT_CAPACITY, pool.capacity());
			assertEquals(DEFAULT_CAPACITY, pool.available());
			assertEquals("test-pool", pool.getMetrics().getName());
		}

		@Test
		@DisplayName("Single allocation reduces available count")
		void testSingleAllocation() {
			FixedMemory memory = pool.allocate();

			assertNotNull(memory);
			assertEquals(1, memory.refCount());
			assertEquals(DEFAULT_CAPACITY - 1, pool.available());
		}

		@Test
		@DisplayName("Memory returns to pool when refcount reaches zero")
		void testAutomaticReturn() {
			FixedMemory memory = pool.allocate();
			assertEquals(DEFAULT_CAPACITY - 1, pool.available());

			memory.decrementRef(); // refCount: 1 -> 0

			// Memory should automatically return to pool
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@Test
		@DisplayName("Allocate all pool capacity")
		void testAllocateFullCapacity() {
			List<FixedMemory> memories = new ArrayList<>();

			// Allocate all
			for (int i = 0; i < DEFAULT_CAPACITY; i++) {
				FixedMemory mem = pool.allocate();
				assertNotNull(mem, "Allocation " + i + " should succeed");
				memories.add(mem);
			}

			assertEquals(0, pool.available());

			// Next allocation should return null
			assertNull(pool.allocate());

			// Release all
			memories.forEach(Memory::decrementRef);
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@Test
		@DisplayName("Pool exhaustion returns null")
		void testPoolExhaustion() {
			// Allocate all
			List<FixedMemory> memories = new ArrayList<>();
			for (int i = 0; i < DEFAULT_CAPACITY; i++) {
				memories.add(pool.allocate());
			}

			// Pool exhausted
			assertNull(pool.allocate());
			assertNull(pool.allocate()); // Multiple attempts

			// Release one and try again
			memories.get(0).decrementRef();
			assertNotNull(pool.allocate());
		}
	}

	// ==================== Memory Lifecycle Tests ====================

	@Nested
	@DisplayName("Memory Lifecycle")
	class MemoryLifecycleTests {

		@Test
		@DisplayName("Allocated memory has correct initial state")
		void testAllocatedMemoryState() {
			FixedMemory memory = pool.allocate();

			assertEquals(1, memory.refCount());
			assertEquals(DEFAULT_SEGMENT_SIZE, memory.byteSize());
			assertEquals(DEFAULT_HEADROOM, memory.headroom());
			assertEquals(DEFAULT_SEGMENT_SIZE - DEFAULT_HEADROOM, memory.tailroom());
			assertEquals(0, memory.length()); // Empty initially
		}

		@Test
		@DisplayName("Memory is recycled correctly")
		void testMemoryRecycling() {
			FixedMemory memory = pool.allocate();

			// Modify the memory
			memory.end(memory.start() + 100);
			assertEquals(100, memory.length());

			// Return to pool
			memory.decrementRef();

			// Allocate again - should get recycled memory
			FixedMemory recycled = pool.allocate();
			assertNotNull(recycled);

			// Should be reset
			assertEquals(0, recycled.length());
			assertEquals(DEFAULT_HEADROOM, recycled.headroom());
		}

		@Test
		@DisplayName("Reference counting prevents premature return")
		void testReferenceCountingPreventsReturn() {
			FixedMemory memory = pool.allocate();
			assertEquals(DEFAULT_CAPACITY - 1, pool.available());

			memory.incrementRef(); // refCount: 1 -> 2
			memory.decrementRef(); // refCount: 2 -> 1

			// Should NOT return to pool yet
			assertEquals(DEFAULT_CAPACITY - 1, pool.available());

			memory.decrementRef(); // refCount: 1 -> 0

			// NOW it returns
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@Test
		@DisplayName("Memory chain lifecycle")
		void testMemoryChainLifecycle() {
			FixedMemory head = pool.allocate();
			FixedMemory tail = pool.allocate();

			assertEquals(DEFAULT_CAPACITY - 2, pool.available());

			// Create chain
			head.nextSegment(tail); // tail refCount: 1 -> 2

			// Release user references
			tail.decrementRef(); // tail refCount: 2 -> 1
			head.decrementRef(); // head refCount: 1 -> 0, triggers cascade

			// Both should return to pool
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}
	}

	// ==================== Headroom and Data Management ====================

	@Nested
	@DisplayName("Headroom and Data Management")
	class HeadroomDataTests {

		@Test
		@DisplayName("Allocate with specific data size")
		void testAllocateWithDataSize() {
			long dataSize = 512L;
			FixedMemory memory = pool.allocate(dataSize);

			assertNotNull(memory);
			assertEquals(dataSize, memory.length());
			assertEquals(DEFAULT_HEADROOM, memory.headroom());
			assertEquals(DEFAULT_SEGMENT_SIZE - DEFAULT_HEADROOM - dataSize,
					memory.tailroom());
		}

		@Test
		@DisplayName("Allocate with data size too large returns null")
		void testAllocateDataSizeTooLarge() {
			long tooLarge = DEFAULT_SEGMENT_SIZE - DEFAULT_HEADROOM + 1;
			FixedMemory memory = pool.allocate(tooLarge);

			assertNull(memory);
			assertEquals(DEFAULT_CAPACITY, pool.available()); // No allocation occurred
		}

		@Test
		@DisplayName("Headroom operations")
		void testHeadroomOperations() {
			FixedMemory memory = pool.allocate(100);

			// Initial state
			assertEquals(DEFAULT_HEADROOM, memory.headroom());
			assertEquals(100, memory.length());

			// Use headroom to prepend
			memory.start(memory.start() - 10);
			assertEquals(DEFAULT_HEADROOM - 10, memory.headroom());
			assertEquals(110, memory.length());

			// Use tailroom to append
			memory.end(memory.end() + 50);
			assertEquals(160, memory.length());
		}
	}

	// ==================== Concurrent Operations ====================

	@Nested
	@DisplayName("Concurrent Operations")
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	class ConcurrentOperationsTests {

		@Test
		@DisplayName("Concurrent allocations are thread-safe")
		void testConcurrentAllocations() throws InterruptedException {
			int threads = 10;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threads);
			AtomicInteger successCount = new AtomicInteger();

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						FixedMemory memory = pool.allocate();
						if (memory != null) {
							successCount.incrementAndGet();
							Thread.sleep(10); // Hold briefly
							memory.decrementRef();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			doneLatch.await();
			executor.shutdown();

			assertEquals(DEFAULT_CAPACITY, successCount.get());
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@RepeatedTest(5)
		@DisplayName("Rapid allocate/release cycles")
		void testRapidAllocateReleaseCycles() throws InterruptedException {
		    int threads = 5;
		    int iterations = 100;
		    CyclicBarrier barrier = new CyclicBarrier(threads);
		    CountDownLatch doneLatch = new CountDownLatch(threads);
		    AtomicInteger errorCount = new AtomicInteger();
		    AtomicInteger allocationCount = new AtomicInteger();
		    
		    ExecutorService executor = Executors.newFixedThreadPool(threads);
		    
		    for (int i = 0; i < threads; i++) {
		        final int threadId = i;
		        executor.submit(() -> {
		            try {
		                barrier.await();
		                for (int j = 0; j < iterations; j++) {
		                    FixedMemory memory = null;
		                    try {
		                        memory = pool.allocate();
		                        if (memory != null) {
		                            allocationCount.incrementAndGet();
		                            
		                            // IMPORTANT: All operations must happen before decrementRef
		                            // Once we decrement, the memory can be reused immediately
		                            
		                            // First, ensure memory is in valid state
		                            if (memory.start() >= 0 && memory.segment() != null) {
		                                // Write a simple pattern
		                                memory.segment().set(ValueLayout.JAVA_INT, 
		                                                   memory.start(), 
		                                                   threadId * 1000 + j);
		                            }
		                            
		                            // Now safe to release
		                            memory.decrementRef();
		                            memory = null; // Clear reference to avoid use-after-free
		                        }
		                    } catch (Exception e) {
		                        System.err.println("Thread " + threadId + 
		                            " error at iteration " + j + ": " + e.getMessage());
		                        e.printStackTrace();
		                        errorCount.incrementAndGet();
		                        
		                        // Clean up if we still hold a reference
		                        if (memory != null) {
		                            try {
		                                memory.decrementRef();
		                            } catch (Exception ignored) {}
		                        }
		                    }
		                    
		                    // Small yield to allow other threads
		                    if (j % 100 == 0) {
		                        Thread.yield();
		                    }
		                }
		            } catch (Exception e) {
		                System.err.println("Thread " + threadId + " barrier error: " + e);
		                e.printStackTrace();
		                errorCount.incrementAndGet();
		            } finally {
		                doneLatch.countDown();
		            }
		        });
		    }
		    
		    boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
		    executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);
		    
		    System.out.println("Allocations performed: " + allocationCount.get());
		    System.out.println("Errors encountered: " + errorCount.get());
		    
		    assertTrue(completed, "All threads should complete");
		    assertEquals(0, errorCount.get(), "No errors should occur");
		    
		    // Wait a bit for any async operations to complete
		    Thread.sleep(100);
		    
		    assertEquals(DEFAULT_CAPACITY, pool.available(), "All memory should be returned");
		}
		@Test
		@DisplayName("Producer-consumer pattern")
		void testProducerConsumerPattern() throws InterruptedException {
			ExecutorService executor = Executors.newFixedThreadPool(4);
			CountDownLatch doneLatch = new CountDownLatch(2);
			AtomicInteger produced = new AtomicInteger();
			AtomicInteger consumed = new AtomicInteger();
			List<FixedMemory> queue = new ArrayList<>();

			// Producer
			executor.submit(() -> {
				try {
					for (int i = 0; i < 100; i++) {
						FixedMemory memory = pool.allocate();
						if (memory != null) {
							memory.segment().set(ValueLayout.JAVA_INT,
									memory.start(), i);
							synchronized (queue) {
								queue.add(memory);
								queue.notify();
							}
							produced.incrementAndGet();
						}
						Thread.sleep(1);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			});

			// Consumer
			executor.submit(() -> {
				try {
					while (consumed.get() < 100) {
						FixedMemory memory = null;
						synchronized (queue) {
							while (queue.isEmpty() && consumed.get() < 100) {
								queue.wait(100);
							}
							if (!queue.isEmpty()) {
								memory = queue.remove(0);
							}
						}
						if (memory != null) {
							// Process
							int value = memory.segment().get(ValueLayout.JAVA_INT,
									memory.start());
							memory.decrementRef();
							consumed.incrementAndGet();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			});

			doneLatch.await();
			executor.shutdown();

			assertTrue(produced.get() >= consumed.get());
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}
	}

	// ==================== Pool Metrics ====================

	@Nested
	@DisplayName("Pool Metrics")
	class PoolMetricsTests {

		@Test
		@DisplayName("Metrics track allocations correctly")
		void testAllocationMetrics() {
			PoolMetrics metrics = pool.getMetrics();
			int initialAllocations = metrics.getAllocations();

			// Allocate some memories
			List<FixedMemory> memories = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				memories.add(pool.allocate());
			}

			assertEquals(initialAllocations + 5, metrics.getAllocations());

			// Release them
			memories.forEach(Memory::decrementRef);
			assertEquals(initialAllocations + 5, metrics.getReleases());
		}

		@Test
		@DisplayName("Metrics track exhaustion")
		void testExhaustionMetrics() {
			PoolMetrics metrics = pool.getMetrics();
			int initialExhaustions = metrics.getExhaustions();

			// Allocate all
			List<FixedMemory> memories = new ArrayList<>();
			for (int i = 0; i < DEFAULT_CAPACITY; i++) {
				memories.add(pool.allocate());
			}

			// Try to allocate when exhausted
			assertNull(pool.allocate());
			assertNull(pool.allocate());

			assertEquals(initialExhaustions + 2, metrics.getExhaustions());

			// Cleanup
			memories.forEach(Memory::decrementRef);
		}
	}

	// ==================== Edge Cases ====================

	@Nested
	@DisplayName("Edge Cases")
	class EdgeCasesTests {

		@Test
		@DisplayName("Pool with single capacity")
		void testSingleCapacityPool() {
			FixedMemoryPool singlePool = new FixedMemoryPool("single", 1, 256L, 32L);

			FixedMemory memory = singlePool.allocate();
			assertNotNull(memory);
			assertNull(singlePool.allocate()); // Exhausted

			memory.decrementRef();
			assertNotNull(singlePool.allocate()); // Available again
		}

		@Test
		@DisplayName("Pool with zero headroom")
		void testZeroHeadroomPool() {
			FixedMemoryPool noHeadroomPool = new FixedMemoryPool("no-headroom",
					5, 512L, 0L);

			FixedMemory memory = noHeadroomPool.allocate();
			assertNotNull(memory);
			assertEquals(0, memory.headroom());
			assertEquals(512L, memory.tailroom());
		}

		@Test
		@DisplayName("Multiple pools don't interfere")
		void testMultiplePools() {
			FixedMemoryPool pool1 = new FixedMemoryPool("pool1", 5, 256L, 32L);
			FixedMemoryPool pool2 = new FixedMemoryPool("pool2", 5, 512L, 64L);

			FixedMemory mem1 = pool1.allocate();
			FixedMemory mem2 = pool2.allocate();

			assertEquals(256L, mem1.byteSize());
			assertEquals(512L, mem2.byteSize());

			mem1.decrementRef();
			mem2.decrementRef();

			assertEquals(5, pool1.available());
			assertEquals(5, pool2.available());
		}
	}
}