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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive test suite for ScopedMemoryPool.
 * 
 * Tests scoped memory lifecycle, binding/unbinding, scope tracking, and
 * concurrent operations.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("ScopedMemoryPool Tests")
class ScopedMemoryPoolTest {

	private static final int DEFAULT_CAPACITY = 10;
	private static final long DEFAULT_HEADROOM = 128L;

	private ScopedMemoryPool<ScopedMemory> pool;
	private Arena arena;

	@BeforeEach
	void setUp() {
		pool = new ScopedMemoryPool<>("test-pool", ScopedMemory.class,
				DEFAULT_CAPACITY, DEFAULT_HEADROOM);
		arena = Arena.ofConfined();
	}

	// ==================== Basic Pool Operations ====================

	@Nested
	@DisplayName("Basic Pool Operations")
	class BasicPoolOperations {

		@Test
		@DisplayName("Pool initializes with wrapper objects")
		void testPoolInitialization() {
			assertEquals(DEFAULT_CAPACITY, pool.capacity());
			assertEquals(DEFAULT_CAPACITY, pool.available());

			// All wrappers should be pre-allocated
			List<ScopedMemory> allocated = new ArrayList<>();
			for (int i = 0; i < DEFAULT_CAPACITY; i++) {
				ScopedMemory mem = pool.allocate();
				assertNotNull(mem);
				assertFalse(mem.isBound()); // Not bound to native memory yet
				allocated.add(mem);
			}

			// Pool exhausted
			assertNull(pool.allocate());

			// Release all
			allocated.forEach(m -> m.decrementRef());
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@Test
		@DisplayName("Allocate with native segment binding")
		void testAllocateWithSegment() {
			MemorySegment segment = arena.allocate(1024);

			ScopedMemory memory = pool.allocate(segment, 0, 1024);
			assertNotNull(memory);
			assertTrue(memory.isBound());
			assertEquals(segment, memory.segment());
			assertEquals(1024, memory.byteSize());

			memory.decrementRef();
			assertFalse(memory.isBound()); // Should unbind on return to pool
		}

		@Test
		@DisplayName("Allocate unbound then bind later")
		void testAllocateUnboundThenBind() {
			ScopedMemory memory = pool.allocate();
			assertNotNull(memory);
			assertFalse(memory.isBound());

			// Bind to native segment later
			MemorySegment segment = arena.allocate(512);
			memory.bind(segment, 0, 512);

			assertTrue(memory.isBound());
			assertEquals(512, memory.byteSize());

			memory.decrementRef();
		}
	}

	// ==================== Scope Management ====================

	@Nested
	@DisplayName("Scope Management")
	class ScopeManagementTests {

		@Test
		@DisplayName("Scope ID changes on rebinding")
		void testScopeIdChanges() {
			ScopedMemory memory = pool.allocate();
			MemorySegment seg1 = arena.allocate(256);
			MemorySegment seg2 = arena.allocate(256);

			// First binding
			memory.bind(seg1, 0, 256);
			long scope1 = memory.scopeId();
			assertTrue(scope1 > 0);

			// Rebinding changes scope
			memory.bind(seg2, 0, 256);
			long scope2 = memory.scopeId();
			assertTrue(scope2 > scope1);

			// Unbinding clears scope
			memory.unbind();
			assertEquals(0, memory.scopeId());

			memory.decrementRef();
		}

		@Test
		@DisplayName("Scope ID used for staleness detection")
		void testStalenessDetection() {
			ScopedMemory memory = pool.allocate();
			MemorySegment segment = arena.allocate(512);

			memory.bind(segment, 0, 512);
			long originalScope = memory.scopeId();

			// Simulate a view caching the scope
			long cachedScope = originalScope;

			// Rebind to new segment
			MemorySegment newSegment = arena.allocate(512);
			memory.bind(newSegment, 0, 512);

			// Cached scope is now stale
			assertNotEquals(cachedScope, memory.scopeId());

			memory.decrementRef();
		}
	}

	// ==================== Lifecycle Management ====================

	@Nested
	@DisplayName("Lifecycle Management")
	class LifecycleTests {

		@Test
		@DisplayName("Memory unbinds when returned to pool")
		void testUnbindOnReturn() {
			MemorySegment segment = arena.allocate(512);
			ScopedMemory memory = pool.allocate(segment, 0, 512);

			assertTrue(memory.isBound());
			assertEquals(1, memory.refCount());

			memory.decrementRef(); // Returns to pool

			assertFalse(memory.isBound());
			assertEquals(0, memory.scopeId());
		}

		@Test
		@DisplayName("Pinning prevents automatic release")
		void testPinning() {
			MemorySegment segment = arena.allocate(512);
			ScopedMemory memory = pool.allocate(segment, 0, 512);

			assertEquals(1, memory.refCount());

			memory.pin(); // Increment ref
			assertEquals(2, memory.refCount());

			memory.decrementRef(); // User release
			assertEquals(1, memory.refCount());
			assertTrue(memory.isBound()); // Still bound due to pin

			memory.unpin(); // Remove pin
			assertEquals(0, memory.refCount());
			assertFalse(memory.isBound()); // Now returns to pool
		}

		@Test
		@DisplayName("Duplicate creates independent copy")
		void testDuplicate() {
			MemorySegment segment = arena.allocate(512); // Larger segment
			ScopedMemory memory = pool.allocate(segment, 0, 512);

			// Expand data region
			memory.end(memory.start() + 100);

			// Write a byte instead of an int to avoid alignment issues
			byte testValue = (byte) 0x42;
			memory.segment().set(ValueLayout.JAVA_BYTE, memory.start(), testValue);

			FixedMemory copy = memory.duplicate();
			assertNotNull(copy);
			assertEquals(100, copy.byteSize());

			// Verify byte was copied
			byte copiedValue = copy.segment().get(ValueLayout.JAVA_BYTE, 0);
			assertEquals(testValue, copiedValue);

			// Copy is independent
			memory.decrementRef();
			assertEquals(1, copy.refCount());

			copy.decrementRef();
		}
	}

	// ==================== Data Operations ====================

	@Nested
	@DisplayName("Data Operations")
	class DataOperationsTests {

		@Test
		@DisplayName("Read/write through scoped memory")
		void testReadWriteOperations() {
			MemorySegment segment = arena.allocate(1024);
			ScopedMemory memory = pool.allocate(segment, 0, 1024);

			// Write data
			memory.segment().set(ValueLayout.JAVA_LONG, memory.start(), 0xDEADBEEFL);
			memory.segment().set(ValueLayout.JAVA_INT, memory.start() + 8, 42);

			// Read data
			long longVal = memory.segment().get(ValueLayout.JAVA_LONG, memory.start());
			int intVal = memory.segment().get(ValueLayout.JAVA_INT, memory.start() + 8);

			assertEquals(0xDEADBEEFL, longVal);
			assertEquals(42, intVal);

			memory.decrementRef();
		}

		@Test
		@DisplayName("Headroom and tailroom operations")
		void testHeadroomTailroom() {
			MemorySegment segment = arena.allocate(1024);
			// Bind to entire segment to have room for headroom/tailroom
			ScopedMemory memory = pool.allocate(segment, 0, 1024);

			// Pool has DEFAULT_HEADROOM of 128, so data starts there
			assertEquals(DEFAULT_HEADROOM, memory.headroom());
			assertEquals(1024 - DEFAULT_HEADROOM, memory.tailroom());
			assertEquals(0, memory.length()); // Empty initially

			// Expand into headroom
			memory.start(memory.start() - 50);
			assertEquals(DEFAULT_HEADROOM - 50, memory.headroom());
			assertEquals(50, memory.length());

			// Expand into tailroom
			memory.end(memory.end() + 200);
			assertEquals(250, memory.length());

			memory.decrementRef();
		}
	}

	// ==================== Concurrent Operations ====================

	@Nested
	@DisplayName("Concurrent Operations")
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	class ConcurrentOperationsTests {

		@Test
		@DisplayName("Concurrent allocation and binding")
		void testConcurrentAllocationBinding() throws InterruptedException {
			int threads = 5;
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(threads);
			AtomicInteger successCount = new AtomicInteger();

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();

						for (int j = 0; j < 100; j++) {
							// Allocate unbound
							ScopedMemory memory = pool.allocate();
							if (memory != null) {
								// Create segment in thread-local arena
								try (Arena localArena = Arena.ofConfined()) {
									MemorySegment segment = localArena.allocate(256);
									memory.bind(segment, 0, 256);

									// Write thread-specific pattern
									memory.segment().set(ValueLayout.JAVA_INT,
											memory.start(),
											threadId * 1000 + j);

									memory.unbind();
								}

								memory.decrementRef();
								successCount.incrementAndGet();
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
			assertEquals(DEFAULT_CAPACITY, pool.available());
		}

		@Test
		@DisplayName("Scope tracking under concurrent rebinding")
		void testConcurrentScopeTracking() throws InterruptedException {
			ScopedMemory memory = pool.allocate();
			AtomicLong maxScopeId = new AtomicLong(0);
			int threads = 3;
			CountDownLatch doneLatch = new CountDownLatch(threads);

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try (Arena localArena = Arena.ofConfined()) {
						for (int j = 0; j < 100; j++) {
							MemorySegment segment = localArena.allocate(128);

							synchronized (memory) {
								memory.bind(segment, 0, 128);
								long scopeId = memory.scopeId();
								maxScopeId.updateAndGet(v -> Math.max(v, scopeId));
								memory.unbind();
							}
						}
					} finally {
						doneLatch.countDown();
					}
				});
			}

			doneLatch.await();
			executor.shutdown();

			// Verify scope IDs were incrementing
			assertTrue(maxScopeId.get() >= threads * 100);

			memory.decrementRef();
		}
	}

	// ==================== Factory-based Pool ====================

	@Nested
	@DisplayName("Factory-based Pool")
	class FactoryBasedPoolTests {

		@Test
		@DisplayName("Pool with custom factory")
		void testCustomFactory() {
			// Custom scoped memory subclass
			class CustomScopedMemory extends ScopedMemory {
				boolean customFlag = false;

				@Override
				protected void onBind() {
					customFlag = true;
				}
			}

			ScopedMemoryPool<CustomScopedMemory> customPool = new ScopedMemoryPool<>("custom", CustomScopedMemory::new,
					5, 64L);

			CustomScopedMemory memory = customPool.allocate();
			assertNotNull(memory);
			assertFalse(memory.customFlag);

			MemorySegment segment = arena.allocate(256);
			memory.bind(segment, 0, 256);

			assertTrue(memory.customFlag); // onBind was called

			memory.decrementRef();
		}
	}

	// ==================== Edge Cases ====================

	@Nested
	@DisplayName("Edge Cases")
	class EdgeCasesTests {

		@Test
		@DisplayName("Multiple bind/unbind cycles")
		void testMultipleBindUnbindCycles() {
			ScopedMemory memory = pool.allocate();

			for (int i = 0; i < 100; i++) {
				MemorySegment segment = arena.allocate(256);
				memory.bind(segment, 0, 256);
				assertTrue(memory.isBound());
				assertTrue(memory.scopeId() > 0);

				memory.unbind();
				assertFalse(memory.isBound());
				assertEquals(0, memory.scopeId());
			}

			memory.decrementRef();
		}

		@Test
		@DisplayName("Binding to zero-size segment")
		void testZeroSizeBinding() {
			ScopedMemory memory = pool.allocate();
			MemorySegment segment = arena.allocate(256);

			memory.bind(segment, 100, 0); // Zero length

			assertTrue(memory.isBound());
			assertEquals(0, memory.length());
			assertEquals(100, memory.start());

			memory.decrementRef();
		}

		@Test
		@DisplayName("Pool exhaustion and recovery")
		void testPoolExhaustionRecovery() {
			// Allocate all
			List<ScopedMemory> memories = new ArrayList<>();
			for (int i = 0; i < DEFAULT_CAPACITY; i++) {
				memories.add(pool.allocate());
			}

			// Exhausted
			assertNull(pool.allocate());
			assertEquals(0, pool.available());

			// Release half
			for (int i = 0; i < DEFAULT_CAPACITY / 2; i++) {
				memories.get(i).decrementRef();
			}

			assertEquals(DEFAULT_CAPACITY / 2, pool.available());

			// Can allocate again
			assertNotNull(pool.allocate());

			// Cleanup
			memories.subList(DEFAULT_CAPACITY / 2, DEFAULT_CAPACITY)
					.forEach(Memory::decrementRef);
		}
	}
}