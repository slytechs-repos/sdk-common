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
import java.lang.foreign.ValueLayout;
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
 * Corner case and abnormal usage tests for BoundView.
 * 
 * Tests edge conditions, extreme values, concurrent access patterns, recursive
 * binding scenarios, and other unusual but possible usage patterns.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("BoundView Corner Cases")
class BoundViewCornerCasesTest {

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
			BoundView view = new BoundView() {};

			// Bind at the very end of memory
			view.bind(baseMemory, 1024, 0);

			assertEquals(1024, view.start());
			assertEquals(0, view.length());
		}

		@Test
		@DisplayName("Bind with zero length slice")
		void testZeroLengthSlice() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory, 500, 0);

			assertEquals(0, view.length());
			assertEquals(500, view.start());
		}

		@Test
		@DisplayName("Bind entire memory as slice")
		void testBindEntireMemoryAsSlice() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory, 0, 1024);

			assertEquals(1024, view.length());
			assertEquals(0, view.start() - baseMemory.start());
		}

		@Test
		@DisplayName("Very small memory segment")
		void testVerySmallSegment() {
			MemorySegment tiny = arena.allocate(1);
			Memory tinyMemory = Memory.of(tiny, 0, 1);

			BoundView view = new BoundView() {};
			view.bind(tinyMemory);

			assertEquals(1, view.length());
		}
	}

	// ==================== Recursive and Nested Binding Tests ====================

	@Nested
	@DisplayName("Recursive and Nested Bindings")
	class RecursiveBindingTests {

		@Test
		@DisplayName("Multiple views to same memory")
		void testMultipleViewsToSameMemory() {
			BoundView view1 = new BoundView() {};
			BoundView view2 = new BoundView() {};
			BoundView view3 = new BoundView() {};

			// All bind to same base memory at different offsets
			view1.bind(baseMemory, 0, 100);
			view2.bind(baseMemory, 100, 100);
			view3.bind(baseMemory, 200, 100);

			// Views don't affect refcount in new model
			assertEquals(1, baseMemory.refCount());

			// Each has independent view
			assertEquals(100, view1.length());
			assertEquals(100, view2.length());
			assertEquals(100, view3.length());

			// Cleanup (no refcount effects)
			view1.unbind();
			view2.unbind();
			view3.unbind();

			assertEquals(1, baseMemory.refCount());
		}

		@Test
		@DisplayName("Nested view binding patterns")
		void testNestedViewBinding() {
			BoundView packet = new BoundView() {};
			BoundView header = new BoundView() {};
			BoundView field = new BoundView() {};

			// Bind to same memory at different offsets/lengths
			packet.bind(baseMemory);
			header.bind(baseMemory, 0, 14);
			field.bind(baseMemory, 6, 6);

			// Verify bounds
			assertEquals(1024, packet.length());
			assertEquals(14, header.length());
			assertEquals(6, field.length());

			// Cleanup
			field.unbind();
			header.unbind();
			packet.unbind();
		}
	}

	// ==================== State Transition Tests ====================

	@Nested
	@DisplayName("Abnormal State Transitions")
	class StateTransitionTests {

		@Test
		@DisplayName("Unbind already unbound is safe")
		void testUnbindAlreadyUnbound() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory);
			view.unbind();

			// Second unbind should be safe (no-op)
			assertDoesNotThrow(() -> view.unbind());
			assertFalse(view.isBound());
		}

		@Test
		@DisplayName("Operations on never-bound view")
		void testNeverBoundView() {
			BoundView view = new BoundView() {};

			assertThrows(IllegalStateException.class, () -> view.start());
			assertThrows(IllegalStateException.class, () -> view.length());
			assertThrows(IllegalStateException.class, () -> view.segment());
			assertThrows(IllegalStateException.class, () -> view.view());
		}

		@Test
		@DisplayName("Rapid bind/unbind cycles")
		void testRapidBindUnbindCycles() {
			BoundView view = new BoundView() {};

			for (int i = 0; i < 1000; i++) {
				view.bind(baseMemory, i % 1024, 1);
				assertEquals(i % 1024, view.start() - baseMemory.start());
				view.unbind();
				assertFalse(view.isBound());
			}

			// Should still be usable
			view.bind(baseMemory);
			assertTrue(view.isBound());
		}

		@Test
		@DisplayName("Rebinding replaces previous binding")
		void testRebindingReplaces() {
			BoundView view = new BoundView() {};

			view.bind(baseMemory, 0, 100);
			assertEquals(100, view.length());

			// Rebind to different parameters
			view.bind(baseMemory, 200, 300);
			assertEquals(300, view.length());
			assertEquals(200, view.start() - baseMemory.start());
		}
	}

	// ==================== Reference Counting Corner Cases ====================

	@Nested
	@DisplayName("Reference Counting Edge Cases")
	class ReferenceCountingEdgeCases {

		@Test
		@DisplayName("RefCount with multiple views")
		void testRefCountWithMultipleViews() {
			BoundView view1 = new BoundView() {};
			BoundView view2 = new BoundView() {};
			BoundView view3 = new BoundView() {};

			assertEquals(1, baseMemory.refCount());

			view1.bind(baseMemory);
			view2.bind(baseMemory);
			view3.bind(baseMemory);

			// Views are lightweight - no refcount changes
			assertEquals(1, baseMemory.refCount());

			view1.unbind();
			view2.unbind();
			view3.unbind();

			assertEquals(1, baseMemory.refCount());
		}

		@Test
		@DisplayName("Rebinding manages refcount correctly")
		void testRebindingRefCount() {
			Memory memory2 = Memory.of(arena.allocate(512), 0, 512);
			BoundView view = new BoundView() {};

			view.bind(baseMemory);
			// Views don't increment refcount
			assertEquals(1, baseMemory.refCount());
			assertEquals(1, memory2.refCount());

			// Rebind to different memory
			view.bind(memory2);

			// No refcount changes from views
			assertEquals(1, baseMemory.refCount());
			assertEquals(1, memory2.refCount());
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
			BoundView view = new BoundView() {};
			view.bind(baseMemory);

			int threads = 10;
			CountDownLatch latch = new CountDownLatch(threads);
			AtomicInteger errors = new AtomicInteger(0);

			ExecutorService executor = Executors.newFixedThreadPool(threads);

			for (int i = 0; i < threads; i++) {
				executor.submit(() -> {
					try {
						for (int j = 0; j < 1000; j++) {
							assertEquals(1024, view.length());
							assertTrue(view.isBound());
							assertNotNull(view.segment());
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
			view.unbind();
		}

		@Test
		@DisplayName("Multiple views to same memory concurrent access")
		void testMultipleViewsConcurrent() throws InterruptedException {
			int numViews = 10;
			List<BoundView> views = new ArrayList<>();
			CountDownLatch bindLatch = new CountDownLatch(numViews);
			CountDownLatch unbindLatch = new CountDownLatch(numViews);

			ExecutorService executor = Executors.newFixedThreadPool(numViews);

			// Concurrent binding
			for (int i = 0; i < numViews; i++) {
				final int offset = i * 100;
				executor.submit(() -> {
					BoundView view = new BoundView() {};
					view.bind(baseMemory, offset, 50);
					synchronized (views) {
						views.add(view);
					}
					bindLatch.countDown();
				});
			}

			bindLatch.await();

			// Views don't affect refcount
			assertEquals(1, baseMemory.refCount());

			// Concurrent unbinding
			for (BoundView view : views) {
				executor.submit(() -> {
					view.unbind();
					unbindLatch.countDown();
				});
			}

			unbindLatch.await();
			executor.shutdown();

			assertEquals(1, baseMemory.refCount());
		}
	}

	// ==================== Memory Access Corner Cases ====================

	@Nested
	@DisplayName("Memory Access Edge Cases")
	class MemoryAccessEdgeCases {

		@Test
		@DisplayName("Access at boundary offsets")
		void testAccessAtBoundaryOffsets() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory, 0, 100);

			MemorySegment viewSeg = view.segment();

			// Write at start
			viewSeg.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);

			// Write at end - 1 (last valid byte)
			viewSeg.set(ValueLayout.JAVA_BYTE, 99, (byte) 2);

			// Verify
			assertEquals(1, viewSeg.get(ValueLayout.JAVA_BYTE, 0));
			assertEquals(2, viewSeg.get(ValueLayout.JAVA_BYTE, 99));
		}

		@Test
		@DisplayName("Memory operations near boundaries")
		void testMemoryOperationsNearBoundaries() {
			// Write pattern to base memory
			for (int i = 0; i < 1024; i += 4) {
				baseMemory.segment().set(ValueLayout.JAVA_INT, i, i);
			}

			BoundView view = new BoundView() {};
			view.bind(baseMemory, 100, 200);

			// view.segment() returns the base segment
			// view.start() gives us the offset where our view starts
			// We need to read relative to view.start()

			MemorySegment seg = view.segment();
			long offset = view.start();

			// Read at start of view
			int startValue = seg.get(ValueLayout.JAVA_INT, offset);
			assertEquals(100, startValue);

			// Read at end of view (last valid int position)
			int endValue = seg.get(ValueLayout.JAVA_INT, offset + 196);
			assertEquals(296, endValue);
		}
	}

	// ==================== View Optimization Tests ====================

	@Nested
	@DisplayName("View Optimization Patterns")
	class ViewOptimizationTests {

		@Test
		@DisplayName("Direct binding shares view")
		void testDirectBindingOptimization() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory);

			// Direct binding (offset 0) should share the source's view
			assertSame(baseMemory.view(), view.view());
		}

		@Test
		@DisplayName("Mapped binding uses separate view")
		void testMappedBindingUsesSeparateView() {
			BoundView view = new BoundView() {};
			view.bind(baseMemory, 100, 200);

			// Mapped binding should use mappedView
			assertNotSame(baseMemory.view(), view.view());
			assertSame(view.mappedView, view.view());
		}

		@Test
		@DisplayName("Switching between direct and mapped")
		void testSwitchingBindingModes() {
			BoundView view = new BoundView() {};

			// Start with direct
			view.bind(baseMemory);
			assertSame(baseMemory.view(), view.view());

			// Switch to mapped
			view.bind(baseMemory, 100, 200);
			assertSame(view.mappedView, view.view());

			// Back to direct
			view.bind(baseMemory);
			assertSame(baseMemory.view(), view.view());
		}
	}
}