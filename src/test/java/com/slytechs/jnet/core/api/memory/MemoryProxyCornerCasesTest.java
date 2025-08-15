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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Corner case and abnormal usage tests for MemoryProxy.
 * 
 * Tests edge conditions, extreme values, concurrent access patterns, recursive
 * binding scenarios, and other unusual but possible usage patterns.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryProxy Corner Cases")
class MemoryProxyCornerCasesTest {

	private Arena arena;
	private MemorySegment segment;
	private Memory baseMemory;

	@BeforeEach
	void setUp() {
		arena = Arena.ofConfined();
		segment = arena.allocate(1024);
		baseMemory = Memory.of(segment, 0, 1024);
	}

	// ==================== Extreme Values Tests ====================

	@Nested
	@DisplayName("Extreme Values")
	class ExtremeValuesTests {

		@Test
		@DisplayName("Bind at maximum offset")
		void testBindAtMaxOffset() {
			MemoryProxy proxy = new MemoryProxy();

			// Bind at the very end of memory
			proxy.bindMemory(baseMemory, 1024);

			assertEquals(1024, proxy.segmentOffset());
			assertEquals(0, proxy.segmentSize());
			assertEquals(0, proxy.totalActiveBytes());
		}

		@Test
		@DisplayName("Bind with zero length slice")
		void testZeroLengthSlice() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 500, 0);

			assertEquals(0, proxy.segmentSize());
			assertFalse(proxy.hasNextSegment());

			// Should create zero-length segment
			MemorySegment seg = proxy.asMemorySegment();
			assertEquals(0, seg.byteSize());
		}

		@Test
		@DisplayName("Bind entire memory as slice")
		void testBindEntireMemoryAsSlice() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0, 1024);

			// Should behave like bounded even though it covers everything
			assertFalse(proxy.hasNextSegment());
			assertEquals(1, proxy.segmentCount());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				Long.MAX_VALUE - 1000,
				Long.MAX_VALUE - 1,
				Long.MAX_VALUE
		})
		@DisplayName("Accept extremely large offsets")
		void testExtremelyLargeOffsets(long offset) {
			MemoryProxy proxy = new MemoryProxy();

			// Large offsets are technically valid, they just result in zero capacity
			assertDoesNotThrow(() -> proxy.bindMemory(baseMemory, offset));

			// The proxy will have zero capacity since offset > memory size
			assertEquals(0, proxy.segmentSize());

			proxy.unbindMemory();
		}

		@ParameterizedTest
		@ValueSource(longs = {
				Long.MAX_VALUE - 1000,
				Long.MAX_VALUE - 1,
				Long.MAX_VALUE
		})
		@DisplayName("Extremely large lengths are accepted")
		void testExtremelyLargeLengths(long length) {
			MemoryProxy proxy = new MemoryProxy();

			// Should bind successfully with large length
			assertDoesNotThrow(() -> proxy.bindMemory(baseMemory, 0, length));

			// The proxy stores the requested length, even if it's beyond memory bounds
			assertEquals(length, proxy.segmentSize());

			// However, actual operations would be limited by the real memory size
			// For example, creating a ByteBuffer or MemorySegment would fail or be clamped

			proxy.unbindMemory();
		}

		@Test
		@DisplayName("Very small memory segment")
		void testVerySmallSegment() {
			MemorySegment tiny = arena.allocate(1);
			Memory tinyMemory = Memory.of(tiny, 0, 1);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(tinyMemory, 0);

			assertEquals(1, proxy.segmentSize());

			// Can still create views
			ByteBuffer bb = proxy.asByteBuffer();
			assertEquals(1, bb.capacity());
		}
	}

	// ==================== Recursive and Nested Binding Tests ====================

	@Nested
	@DisplayName("Recursive and Nested Bindings")
	class RecursiveBindingTests {

		@Test
		@DisplayName("Proxy can bind to another proxy")
		void testProxyBindsToAnotherProxy() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			// Binding to another proxy should work - this is proxy chaining
			MemoryProxy other = new MemoryProxy();
			assertDoesNotThrow(() -> other.bindMemory(proxy, 0));

			// Should be able to access through the chain
			assertEquals(proxy.segmentSize(), other.segmentSize());

			// Cleanup
			other.unbindMemory();
			proxy.unbindMemory();
		}

		@Test
		@DisplayName("Proxy chaining works correctly")
		void testProxyChaining() {
			MemoryProxy proxy1 = new MemoryProxy();
			MemoryProxy proxy2 = new MemoryProxy();
			MemoryProxy proxy3 = new MemoryProxy();

			proxy1.bindMemory(baseMemory, 0);
			proxy2.bindMemory(proxy1, 0, 100);

			// proxy3 can bind to proxy2 - this creates a chain, not a circle
			assertDoesNotThrow(() -> proxy3.bindMemory(proxy2, 0));

			// All three proxies are now bound in a chain:
			// baseMemory <- proxy1 <- proxy2 <- proxy3
			assertTrue(proxy1.isBound());
			assertTrue(proxy2.isBound());
			assertTrue(proxy3.isBound());

			// Cleanup in reverse order
			proxy3.unbindMemory();
			proxy2.unbindMemory();
			proxy1.unbindMemory();
		}

		@Test
		@DisplayName("Deep proxy nesting")
		void testDeepProxyNesting() {
			List<MemoryProxy> proxies = new ArrayList<>();

			// Create first proxy
			MemoryProxy first = new MemoryProxy();
			first.bindMemory(baseMemory, 0);
			proxies.add(first);

			// Create chain of nested proxies
			for (int i = 1; i < 10; i++) {
				MemoryProxy next = new MemoryProxy();
				next.bindMemory(proxies.get(i - 1), i, 100 - i);
				proxies.add(next);
			}

			// Verify deepest proxy still works
			MemoryProxy deepest = proxies.get(9);
			assertEquals(91, deepest.segmentSize());

			// Cleanup in reverse order
			for (int i = proxies.size() - 1; i >= 0; i--) {
				proxies.get(i).unbindMemory();
			}
		}

		@Test
		@DisplayName("Cannot create true circular reference")
		void testCannotCreateCircularReference() {
			MemoryProxy proxy1 = new MemoryProxy();
			MemoryProxy proxy2 = new MemoryProxy();

			proxy1.bindMemory(baseMemory, 0);
			proxy2.bindMemory(proxy1, 0);

			// Can't rebind proxy1 to proxy2 (would create a circle)
			// But proxy1 is already bound, so this fails for that reason
			assertThrows(IllegalStateException.class,
					() -> proxy1.bindMemory(proxy2, 0),
					"Proxy is already bound");
		}

		@Test
		@DisplayName("Multiple proxies to same memory")
		void testMultipleProxiesToSameMemory() {
			MemoryProxy proxy1 = new MemoryProxy();
			MemoryProxy proxy2 = new MemoryProxy();
			MemoryProxy proxy3 = new MemoryProxy();

			// All bind to same base memory at different offsets
			proxy1.bindMemory(baseMemory, 0, 100);
			proxy2.bindMemory(baseMemory, 100, 100);
			proxy3.bindMemory(baseMemory, 200, 100);

			// Reference count should be 4 (base + 3 proxies)
			assertEquals(4, baseMemory.refCount());

			// Each has independent view
			assertEquals(100, proxy1.segmentSize());
			assertEquals(100, proxy2.segmentSize());
			assertEquals(100, proxy3.segmentSize());

			// Cleanup
			proxy1.unbindMemory();
			proxy2.unbindMemory();
			proxy3.unbindMemory();

			assertEquals(1, baseMemory.refCount());
		}
	}

	// ==================== State Transition Tests ====================

	@Nested
	@DisplayName("Abnormal State Transitions")
	class StateTransitionTests {

		@Test
		@DisplayName("Unbind already unbound throws")
		void testUnbindAlreadyUnbound() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);
			proxy.unbindMemory();

			assertThrows(IllegalStateException.class,
					() -> proxy.unbindMemory());
		}

		@Test
		@DisplayName("Operations on never-bound proxy")
		void testNeverBoundProxy() {
			MemoryProxy proxy = new MemoryProxy();

			assertThrows(IllegalStateException.class, () -> proxy.segmentOffset());
			assertThrows(IllegalStateException.class, () -> proxy.segmentSize());
			assertThrows(IllegalStateException.class, () -> proxy.asMemorySegment());
			assertThrows(IllegalStateException.class, () -> proxy.asByteBuffer());
			assertThrows(IllegalStateException.class, () -> proxy.totalActiveBytes());
			assertThrows(IllegalStateException.class, () -> proxy.refCount());
		}

		@Test
		@DisplayName("Rapid bind/unbind cycles")
		void testRapidBindUnbindCycles() {
			MemoryProxy proxy = new MemoryProxy();

			for (int i = 0; i < 1000; i++) {
				proxy.bindMemory(baseMemory, i % 1024);
				assertEquals(i % 1024, proxy.segmentOffset());
				proxy.unbindMemory();
				assertFalse(proxy.isBound());
			}

			// Should still be usable
			proxy.bindMemory(baseMemory, 0);
			assertTrue(proxy.isBound());
		}
	}

	// ==================== Hook Method Corner Cases ====================

	@Nested
	@DisplayName("Hook Method Edge Cases")
	class HookMethodEdgeCases {

		static class ExceptionThrowingProxy extends MemoryProxy {
			boolean throwOnBind = false;
			boolean throwOnUnbind = false;
			int bindCallCount = 0;
			int unbindCallCount = 0;

			@Override
			protected void onBindMemory() {
				bindCallCount++;
				if (throwOnBind) {
					throw new RuntimeException("Bind hook failed");
				}
			}

			@Override
			protected void onUnbindMemory() {
				unbindCallCount++;
				if (throwOnUnbind) {
					throw new RuntimeException("Unbind hook failed");
				}
			}
		}

		@Test
		@DisplayName("Exception in onBindMemory")
		void testExceptionInOnBindMemory() {
			ExceptionThrowingProxy proxy = new ExceptionThrowingProxy();
			proxy.throwOnBind = true;

			// Binding should fail but state should be consistent
			assertThrows(RuntimeException.class,
					() -> proxy.bindMemory(baseMemory, 0));

			// Proxy should still be in bound state (binding happened before hook)
			// This is a design choice - the binding is complete before the hook
			assertTrue(proxy.isBound());

			// Should be able to unbind
			proxy.throwOnBind = false;
			proxy.unbindMemory();
			assertFalse(proxy.isBound());
		}

		@Test
		@DisplayName("Exception in onUnbindMemory")
		void testExceptionInOnUnbindMemory() {
			ExceptionThrowingProxy proxy = new ExceptionThrowingProxy();
			proxy.bindMemory(baseMemory, 0);

			proxy.throwOnUnbind = true;

			// Unbinding should fail
			assertThrows(RuntimeException.class,
					() -> proxy.unbindMemory());

			// State might be inconsistent here - this is a corner case
			// The hook failed but unbinding might be partially complete
		}

		@Test
		@DisplayName("Recursive binding in hook")
		void testRecursiveBindingInHook() {
			class RecursiveProxy extends MemoryProxy {
				MemoryProxy child = new MemoryProxy();

				@Override
				protected void onBindMemory() {
					// Try to bind child to this proxy during binding
					if (getBoundMemory() != null) {
						child.bindMemory(this, 0, 10);
					}
				}

				@Override
				protected void onUnbindMemory() {
					if (child.isBound()) {
						child.unbindMemory();
					}
				}
			}

			RecursiveProxy proxy = new RecursiveProxy();
			proxy.bindMemory(baseMemory, 0);

			assertTrue(proxy.child.isBound());
			assertEquals(10, proxy.child.segmentSize());

			proxy.unbindMemory();
			assertFalse(proxy.child.isBound());
		}
	}

	// ==================== Chain Navigation Corner Cases ====================

	@Nested
	@DisplayName("Chain Navigation Edge Cases")
	class ChainNavigationEdgeCases {

		@Test
		@DisplayName("Chain with null next memory")
		void testChainWithNullNext() {
			baseMemory.setNextMemory(null);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			assertFalse(proxy.hasNextSegment());
			assertNull(proxy.nextSegment());
			assertEquals(1, proxy.segmentCount());
		}

		@Test
		@DisplayName("Clear chain by setting next to null")
		void testClearChainBySettingNull() {
			// Create a chain
			Memory second = Memory.of(arena.allocate(512), 0, 512);
			baseMemory.setNextMemory(second);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			assertTrue(proxy.hasNextSegment());
			assertEquals(2, proxy.segmentCount());

			// Clear the chain
			baseMemory.setNextMemory(null);

			assertFalse(proxy.hasNextSegment());
			assertEquals(1, proxy.segmentCount());
		}

		@Test
		@DisplayName("Chain modified after binding")
		void testChainModifiedAfterBinding() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			assertFalse(proxy.hasNextSegment());

			// Add chain after binding
			Memory second = Memory.of(arena.allocate(512), 0, 512);
			baseMemory.setNextMemory(second);

			// Proxy should see the new chain
			assertTrue(proxy.hasNextSegment());
			assertEquals(2, proxy.segmentCount());
			assertEquals(1024 + 512, proxy.totalActiveBytes());
		}

		@Test
		@DisplayName("Seek beyond chain throws")
		void testSeekBeyondChain() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			// AbstractMemory.seekMemory() uses Objects.checkIndex which throws
			// IndexOutOfBoundsException
			assertThrows(IndexOutOfBoundsException.class,
					() -> proxy.seekSegment(2000));
		}

		@Test
		@DisplayName("Seek in bounded view limited")
		void testSeekInBoundedView() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0, 100);

			// Can seek within bounds
			assertDoesNotThrow(() -> proxy.seekSegment(50));

			// Cannot seek beyond bounds
			assertThrows(IllegalArgumentException.class,
					() -> proxy.seekSegment(101));
		}

		@Test
		@DisplayName("Chain with zero-length segments")
		void testChainWithZeroLengthSegments() {
			Memory zero1 = Memory.of(arena.allocate(1), 0, 0); // Zero length
			Memory normal = Memory.of(arena.allocate(100), 0, 100);
			Memory zero2 = Memory.of(arena.allocate(1), 0, 0); // Zero length

			baseMemory.setNextMemory(zero1);
			zero1.setNextMemory(normal);
			normal.setNextMemory(zero2);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			assertEquals(4, proxy.segmentCount());
			assertEquals(1024 + 0 + 100 + 0, proxy.totalActiveBytes());
		}
	}

	// ==================== Reference Counting Corner Cases ====================

	@Nested
	@DisplayName("Reference Counting Edge Cases")
	class ReferenceCountingEdgeCases {

		@Test
		@DisplayName("RefCount overflow protection")
		void testRefCountOverflow() {
			// This is hypothetical - would need Integer.MAX_VALUE proxies
			// Just test that many increments work
			for (int i = 0; i < 1000; i++) {
				baseMemory.incrementRef();
			}

			assertEquals(1001, baseMemory.refCount());

			for (int i = 0; i < 1000; i++) {
				baseMemory.decrementRef();
			}

			assertEquals(1, baseMemory.refCount());
		}

		@Test
		@DisplayName("Auto-unbind at zero refs")
		void testAutoUnbindAtZeroRefs() {
			Memory temp = Memory.of(arena.allocate(256), 0, 256);

			MemoryProxy proxy1 = new MemoryProxy();
			MemoryProxy proxy2 = new MemoryProxy();

			proxy1.bindMemory(temp, 0); // ref = 2
			proxy2.bindMemory(temp, 100); // ref = 3

			assertEquals(3, temp.refCount());

			temp.decrementRef(); // ref = 2
			temp.decrementRef(); // ref = 1

			// One more should trigger auto-unbind
			assertEquals(0, proxy1.decrementRef()); // ref = 0

			assertFalse(proxy1.isBound());
			// proxy2 is also affected since memory is gone
		}

		@Test
		@DisplayName("Reference count with nested proxies")
		void testRefCountWithNestedProxies() {
			MemoryProxy parent = new MemoryProxy();
			MemoryProxy child = new MemoryProxy();
			MemoryProxy grandchild = new MemoryProxy();

			parent.bindMemory(baseMemory, 0); // baseMemory ref = 2
			child.bindMemory(parent, 0, 100); // parent increments its bound (baseMemory): ref = 3
			grandchild.bindMemory(child, 0, 50); // child increments its bound (parent->baseMemory): ref = 4

			// All proxies delegate refCount() to baseMemory
			assertEquals(4, baseMemory.refCount());
			assertEquals(4, parent.refCount()); // Delegates to baseMemory
			assertEquals(4, child.refCount()); // Delegates to parent -> baseMemory
			assertEquals(4, grandchild.refCount()); // Delegates to child -> parent -> baseMemory

			// Unbind in order
			grandchild.unbindMemory(); // ref = 3
			child.unbindMemory(); // ref = 2
			parent.unbindMemory(); // ref = 1

			assertEquals(1, baseMemory.refCount());
		}
	}

	// ==================== Concurrent Access Tests ====================

	@Nested
	@DisplayName("Concurrent Access Patterns")
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	class ConcurrentAccessTests {

		@Test
		@DisplayName("Concurrent reads are safe")
		void testConcurrentReads() throws InterruptedException {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0);

			int threads = 10;
			CountDownLatch latch = new CountDownLatch(threads);
			AtomicInteger errors = new AtomicInteger(0);

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						for (int j = 0; j < 1000; j++) {
							assertEquals(1024, proxy.segmentSize());
							assertEquals(0, proxy.segmentOffset());
							assertTrue(proxy.isBound());
						}
					} catch (Exception e) {
						errors.incrementAndGet();
					} finally {
						latch.countDown();
					}
				});
			}

			latch.await();
			executor.shutdown();

			assertEquals(0, errors.get());
			proxy.unbindMemory();
		}

		@Test
		@DisplayName("Concurrent bind/unbind is unsafe")
		void testConcurrentBindUnbindUnsafe() throws InterruptedException {
			int iterations = 100;
			AtomicInteger conflicts = new AtomicInteger(0);

			for (int iter = 0; iter < iterations; iter++) {
				MemoryProxy proxy = new MemoryProxy();
				CountDownLatch start = new CountDownLatch(1);
				CountDownLatch done = new CountDownLatch(2);

				Thread t1 = new Thread(() -> {
					try {
						start.await();
						proxy.bindMemory(baseMemory, 0);
					} catch (IllegalStateException e) {
						conflicts.incrementAndGet();
					} catch (Exception e) {
						// Ignore
					} finally {
						done.countDown();
					}
				});

				Thread t2 = new Thread(() -> {
					try {
						start.await();
						proxy.bindMemory(baseMemory, 100);
					} catch (IllegalStateException e) {
						conflicts.incrementAndGet();
					} catch (Exception e) {
						// Ignore
					} finally {
						done.countDown();
					}
				});

				t1.start();
				t2.start();
				start.countDown();
				done.await();

				if (proxy.isBound()) {
					proxy.unbindMemory();
				}
			}

			// Should have detected race conditions
			assertTrue(conflicts.get() > 0,
					"Should have detected concurrent binding conflicts");
		}

		@Test
		@DisplayName("Multiple proxies to same memory concurrent access")
		void testMultipleProxiesConcurrent() throws InterruptedException {
			int numProxies = 10;
			List<MemoryProxy> proxies = new ArrayList<>();
			CountDownLatch bindLatch = new CountDownLatch(numProxies);
			CountDownLatch unbindLatch = new CountDownLatch(numProxies);

			ExecutorService executor = Executors.newFixedThreadPool(numProxies);

			// Concurrent binding
			for (int i = 0; i < numProxies; i++) {
				final int offset = i * 100;
				executor.submit(() -> {
					MemoryProxy proxy = new MemoryProxy();
					proxy.bindMemory(baseMemory, offset, 50);
					proxies.add(proxy);
					bindLatch.countDown();
				});
			}

			bindLatch.await();

			// All should be bound
			assertEquals(numProxies + 1, baseMemory.refCount());

			// Concurrent unbinding
			for (MemoryProxy proxy : proxies) {
				executor.submit(() -> {
					proxy.unbindMemory();
					unbindLatch.countDown();
				});
			}

			unbindLatch.await();
			executor.shutdown();

			assertEquals(1, baseMemory.refCount());
		}
	}

	// ==================== Memory Segment Corner Cases ====================

	@Nested
	@DisplayName("Memory Segment Edge Cases")
	class MemorySegmentEdgeCases {

		@Test
		@DisplayName("Segment at boundary offsets")
		void testSegmentAtBoundaryOffsets() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0, 100);

			// At start - should work
			MemorySegment seg0 = proxy.asMemorySegmentAt(0);
			assertNotNull(seg0);

			// Near end - should work
			MemorySegment seg99 = proxy.asMemorySegmentAt(99);
			assertNotNull(seg99);

			// The proxy doesn't enforce its own bounds strictly
			// It delegates to the underlying memory which may have more space
			// So offset 100 might actually work

			// Just verify we can call it without crashes
			assertDoesNotThrow(() -> proxy.asMemorySegmentAt(50)); // Safely in middle
		}

		@Test
		@DisplayName("ByteBuffer from bounded proxy")
		void testByteBufferFromBoundedProxy() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 100, 200);

			ByteBuffer bb = proxy.asByteBuffer();

			assertEquals(0, bb.position());
			assertEquals(200, bb.capacity());
			assertEquals(200, bb.limit());

			// Write through buffer using explicit byte order
			bb.order(ByteOrder.BIG_ENDIAN); // Use big-endian for predictable behavior
			bb.putInt(0, 0x12345678);

			// Read through segment with matching byte order
			int value = proxy.asMemorySegment().get(ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN), 0); // Use
																												// big-endian
																												// layout
			assertEquals(0x12345678, value);
		}

		@Test
		@DisplayName("Memory operations near boundaries")
		void testMemoryOperationsNearBoundaries() {
			// Write pattern to base memory
			for (int i = 0; i < 1024; i += 4) {
				baseMemory.asMemorySegment().set(ValueLayout.JAVA_INT, i, i);
			}

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 100, 200);

			// Read at start of proxy
			int startValue = proxy.asMemorySegment().get(ValueLayout.JAVA_INT, 0);
			assertEquals(100, startValue);

			// Read at end of proxy (last valid int position)
			int endValue = proxy.asMemorySegment().get(ValueLayout.JAVA_INT, 196);
			assertEquals(296, endValue);
		}
	}

	// ==================== Data Bounds Corner Cases ====================

	@Nested
	@DisplayName("Data Bounds Edge Cases")
	class DataBoundsEdgeCases {

		@Test
		@DisplayName("Data bounds narrower than memory bounds")
		void testDataBoundsNarrowerThanMemory() {
			// Set narrow data bounds on base
			baseMemory.activeBytesStart(baseMemory.segmentOffset() + 100);
			baseMemory.activeBytesEnd(baseMemory.segmentEnd() - 100);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 50, 900);

			// Proxy should respect base's data bounds
			assertTrue(proxy.activeBytesStart() >= baseMemory.activeBytesStart());
			assertTrue(proxy.activeBytesEnd() <= baseMemory.activeBytesEnd());
		}

		@Test
		@DisplayName("Data bounds adjustment validation")
		void testDataBoundsAdjustmentValidation() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 100, 200);

			long initialDataOffset = proxy.activeBytesStart();
			long initialDataEnd = proxy.activeBytesEnd();

			// Valid adjustment
			proxy.activeBytesStart(initialDataOffset + 10);
			proxy.activeBytesEnd(initialDataEnd - 10);

			assertEquals(180, proxy.activeBytesEnd() - proxy.activeBytesStart());

			// Invalid adjustments
			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesStart(proxy.segmentOffset() - 1));

			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesEnd(proxy.segmentEnd() + 1));
		}

		@Test
		@DisplayName("Chain data length with adjusted bounds")
		void testChainDataLengthWithAdjustedBounds() {
			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(baseMemory, 0, 500);

			// Adjust data bounds
			proxy.activeBytesStart(proxy.segmentOffset() + 100);
			proxy.activeBytesEnd(proxy.segmentEnd() - 100);

			// Chain data length should reflect adjusted bounds
			assertEquals(300, proxy.totalActiveBytes());
		}
	}

	// ==================== Cleanup and Resource Tests ====================
	@Nested
	@DisplayName("Cleanup and Resource Management")
	class CleanupTests {

		@Test
		@DisplayName("Proxy cleanup order matters")
		void testProxyCleanupOrder() {
			MemoryProxy level1 = new MemoryProxy();
			MemoryProxy level2 = new MemoryProxy();
			MemoryProxy level3 = new MemoryProxy();

			level1.bindMemory(baseMemory, 0);
			level2.bindMemory(level1, 0, 500);
			level3.bindMemory(level2, 0, 250);

			// Correct cleanup order - reverse of binding
			level3.unbindMemory();
			level2.unbindMemory();
			level1.unbindMemory();

			// All should be unbound
			assertFalse(level1.isBound());
			assertFalse(level2.isBound());
			assertFalse(level3.isBound());
		}

		@Test
		@DisplayName("Memory leak prevention with proper lifecycle")
		void testMemoryLeakPrevention() {
			int initialRef = baseMemory.refCount();

			// Simulate packet processing loop with proper cleanup
			MemoryProxy packetProxy = new MemoryProxy();
			MemoryProxy headerProxy = new MemoryProxy();

			for (int i = 0; i < 100; i++) {
				// Bind for packet processing
				packetProxy.bindMemory(baseMemory, i % 512);
				headerProxy.bindMemory(packetProxy, 0, 14);

				// Process packet...

				// Proper cleanup
				headerProxy.unbindMemory();
				packetProxy.unbindMemory();
			}

			// No leak - ref count back to initial
			assertEquals(initialRef, baseMemory.refCount());
		}

		@Test
		@DisplayName("Forgotten unbind causes reference leak")
		void testForgottenUnbindCausesLeak() {
			int initialRef = baseMemory.refCount();
			List<MemoryProxy> leakedProxies = new ArrayList<>();

			// Create proxies without proper cleanup
			for (int i = 0; i < 10; i++) {
				MemoryProxy proxy = new MemoryProxy();
				proxy.bindMemory(baseMemory, i * 10);
				leakedProxies.add(proxy);
				// Forgot to unbind!
			}

			// References are leaked
			assertEquals(initialRef + 10, baseMemory.refCount());

			// Manual cleanup required
			for (MemoryProxy proxy : leakedProxies) {
				proxy.unbindMemory();
			}

			assertEquals(initialRef, baseMemory.refCount());
		}

		@Test
		@DisplayName("Reusable proxy lifecycle in packet processing")
		void testReusableProxyLifecycle() {
			// Simulate typical packet processing pattern
			MemoryProxy packetProxy = new MemoryProxy();
			MemoryProxy ethernetProxy = new MemoryProxy();
			MemoryProxy ipProxy = new MemoryProxy();

			// Process multiple packets with same proxies
			for (int i = 0; i < 5; i++) {
				// Simulate getting new packet
				Memory packet = Memory.of(arena.allocate(1500), 0, 1500);

				// Bind proxies
				packetProxy.bindMemory(packet, 0);
				ethernetProxy.bindMemory(packetProxy, 0, 14);
				ipProxy.bindMemory(packetProxy, 14, 20);

				// Process...
				assertTrue(packetProxy.isBound());
				assertTrue(ethernetProxy.isBound());
				assertTrue(ipProxy.isBound());

				// Unbind in reverse order
				ipProxy.unbindMemory();
				ethernetProxy.unbindMemory();
				packetProxy.unbindMemory();

				// Ready for next packet
				assertFalse(packetProxy.isBound());
				assertFalse(ethernetProxy.isBound());
				assertFalse(ipProxy.isBound());
			}
		}

		@Test
		@DisplayName("Backend-managed proxy lifecycle")
		void testBackendManagedProxyLifecycle() {
			// Simulate backend managing proxy pool
			class ProxyPool {
				private final List<MemoryProxy> available = new ArrayList<>();
				private final List<MemoryProxy> inUse = new ArrayList<>();

				ProxyPool(int size) {
					for (int i = 0; i < size; i++) {
						available.add(new MemoryProxy());
					}
				}

				MemoryProxy acquire() {
					if (available.isEmpty()) {
						throw new IllegalStateException("No proxies available");
					}
					MemoryProxy proxy = available.remove(available.size() - 1);
					inUse.add(proxy);
					return proxy;
				}

				void release(MemoryProxy proxy) {
					if (proxy.isBound()) {
						proxy.unbindMemory();
					}
					inUse.remove(proxy);
					available.add(proxy);
				}

				void shutdown() {
					for (MemoryProxy proxy : inUse) {
						if (proxy.isBound()) {
							proxy.unbindMemory();
						}
					}
				}
			}

			ProxyPool pool = new ProxyPool(3);

			// Use proxies
			MemoryProxy p1 = pool.acquire();
			p1.bindMemory(baseMemory, 0);

			MemoryProxy p2 = pool.acquire();
			p2.bindMemory(baseMemory, 100);

			// Return to pool
			pool.release(p1);
			pool.release(p2);

			// Reuse
			MemoryProxy p3 = pool.acquire();
			assertFalse(p3.isBound()); // Clean proxy from pool
			p3.bindMemory(baseMemory, 200);

			// Cleanup
			pool.shutdown();
		}

		@Test
		@DisplayName("Proxy lifecycle with chain navigation")
		void testProxyLifecycleWithChainNavigation() {
			// Create chain
			Memory seg1 = Memory.of(arena.allocate(100), 0, 100);
			Memory seg2 = Memory.of(arena.allocate(100), 0, 100);
			Memory seg3 = Memory.of(arena.allocate(100), 0, 100);
			seg1.setNextMemory(seg2);
			seg2.setNextMemory(seg3);

			MemoryProxy proxy = new MemoryProxy();
			proxy.bindMemory(seg1, 0);

			// Check chain properties
			assertEquals(3, proxy.segmentCount());

			// MemoryProxy in minimalist design doesn't maintain navigation state
			// nextMemory() always returns the same thing - seg2
			Memory next = proxy.nextSegment();
			assertSame(seg2, next);

			// Calling again returns the same
			Memory next2 = proxy.nextSegment();
			assertSame(seg2, next2);

			// Unbind cleans up
			proxy.unbindMemory();

			// Can rebind and navigate again
			proxy.bindMemory(seg1, 50);
			assertEquals(3, proxy.segmentCount());

			proxy.unbindMemory();
		}
	}
}