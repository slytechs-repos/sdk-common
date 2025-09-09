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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for BoundView operations.
 * 
 * Tests the view architecture with direct and mapped binding modes,
 * reference counting, and error handling.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("BoundView Tests")
class BoundViewTest {

    private static final long SEGMENT_SIZE = 1024;
    private Arena arena;
    private MemorySegment segment;
    private Memory baseMemory;
    private BoundView boundView;

    @BeforeEach
    void setUp() {
        arena = Arena.ofConfined();
        segment = arena.allocate(SEGMENT_SIZE);
        baseMemory = Memory.of(segment, 0, SEGMENT_SIZE);
        boundView = new BoundView() {}; // Anonymous subclass for testing
    }

    // ==================== Basic View State Tests ====================

    @Nested
    @DisplayName("View State Management")
    class ViewStateTests {

        @Test
        @DisplayName("Initial state is unbound")
        void testInitialState() {
            assertFalse(boundView.isBound());
            assertThrows(IllegalStateException.class, () -> boundView.view());
        }

        @Test
        @DisplayName("State after binding")
        void testStateAfterBinding() {
            boundView.bind(baseMemory);

            assertTrue(boundView.isBound());
            assertNotNull(boundView.view());
            assertEquals(baseMemory.segment(), boundView.segment());
        }

        @Test
        @DisplayName("State after unbinding")
        void testStateAfterUnbinding() {
            boundView.bind(baseMemory);
            boundView.unbind();

            assertFalse(boundView.isBound());
            assertNull(boundView.view);
        }

        @Test
        @DisplayName("Rebinding replaces previous binding")
        void testRebinding() {
            boundView.bind(baseMemory);
            int firstRefCount = baseMemory.refCount();
            
            // Rebind to different offset
            boundView.bind(baseMemory, 100, 200);
            
            // Should have released and reacquired
            assertEquals(firstRefCount, baseMemory.refCount());
            assertTrue(boundView.isBound());
            assertEquals(100, boundView.start() - baseMemory.start());
        }

        @Test
        @DisplayName("Can rebind after unbinding")
        void testRebindAfterUnbind() {
            boundView.bind(baseMemory);
            boundView.unbind();

            // Should be able to bind again
            assertDoesNotThrow(() -> boundView.bind(baseMemory, 100, 200));
            assertTrue(boundView.isBound());
        }

        @Test
        @DisplayName("Operations fail when unbound")
        void testOperationsFailWhenUnbound() {
            assertThrows(IllegalStateException.class, () -> boundView.segment());
            assertThrows(IllegalStateException.class, () -> boundView.start());
            assertThrows(IllegalStateException.class, () -> boundView.length());
            assertThrows(IllegalStateException.class, () -> boundView.end());
        }
    }

    // ==================== Direct Binding Tests ====================

    @Nested
    @DisplayName("Direct Binding (Offset 0)")
    class DirectBindingTests {

        @Test
        @DisplayName("Direct binding shares view")
        void testDirectBindingSharesView() {
            boundView.bind(baseMemory);

            // Should share the same view object
            assertSame(baseMemory.view(), boundView.view());
        }

        @Test
        @DisplayName("Direct binding uses memory boundaries")
        void testDirectBindingBoundaries() {
            boundView.bind(baseMemory);

            assertEquals(baseMemory.start(), boundView.start());
            assertEquals(baseMemory.length(), boundView.length());
            assertEquals(baseMemory.segment(), boundView.segment());
        }

        @Test
        @DisplayName("Direct binding with adjusted memory bounds")
        void testDirectBindingWithAdjustedBounds() {
            // Adjust memory's active data region
            baseMemory.start(100);
            baseMemory.end(500);

            boundView.bind(baseMemory);

            assertEquals(100, boundView.start());
            assertEquals(400, boundView.length()); // 500 - 100
        }
    }

    // ==================== Mapped Binding Tests ====================

    @Nested
    @DisplayName("Mapped Binding (With Offset)")
    class MappedBindingTests {

        @Test
        @DisplayName("Mapped binding uses mappedView")
        void testMappedBindingUsesMappedView() {
            boundView.bind(baseMemory, 100, 200);

            // Should use the mapped view
            assertSame(boundView.mappedView, boundView.view());
            assertNotSame(baseMemory.view(), boundView.view());
        }

        @Test
        @DisplayName("Mapped binding with offset")
        void testMappedBindingWithOffset() {
            boundView.bind(baseMemory, 100, 200);

            assertEquals(baseMemory.start() + 100, boundView.start());
            assertEquals(200, boundView.length());
        }

        @Test
        @DisplayName("Mapped binding at various offsets")
        void testMappedBindingVariousOffsets() {
            long[] offsets = {50, 100, 200, 500};
            long[] lengths = {100, 200, 300, 400};

            for (int i = 0; i < offsets.length; i++) {
                boundView.bind(baseMemory, offsets[i], lengths[i]);
                
                assertEquals(baseMemory.start() + offsets[i], boundView.start());
                assertEquals(lengths[i], boundView.length());
                
                boundView.unbind();
            }
        }

        @Test
        @DisplayName("Mapped binding of size 0")
        void testMappedZeroSize() {
            boundView.bind(baseMemory, 100, 0);

            assertEquals(baseMemory.start() + 100, boundView.start());
            assertEquals(0, boundView.length());
        }
    }

    // ==================== Reference Counting Tests ====================

    @Nested
    @DisplayName("Reference Counting")
    class ReferenceCountingTests {

        @Test
        @DisplayName("Binding increments ref count")
        void testBindingIncrementsRef() {
            int initialRef = baseMemory.refCount();

            boundView.bind(baseMemory);

            assertEquals(initialRef + 1, baseMemory.refCount());
        }

        @Test
        @DisplayName("Unbinding decrements ref count")
        void testUnbindingDecrementsRef() {
            boundView.bind(baseMemory);
            int boundRef = baseMemory.refCount();

            boundView.unbind();

            assertEquals(boundRef - 1, baseMemory.refCount());
        }

        @Test
        @DisplayName("Rebinding manages ref count correctly")
        void testRebindingRefCount() {
            Memory memory2 = Memory.of(arena.allocate(512), 0, 512);
            
            boundView.bind(baseMemory);
            int mem1Ref = baseMemory.refCount();
            int mem2Ref = memory2.refCount();
            
            // Rebind to different memory
            boundView.bind(memory2);
            
            assertEquals(mem1Ref - 1, baseMemory.refCount());
            assertEquals(mem2Ref + 1, memory2.refCount());
        }

        @Test
        @DisplayName("Multiple unbind safe")
        void testMultipleUnbindSafe() {
            boundView.bind(baseMemory);
            boundView.unbind();
            
            // Second unbind should be safe (no-op)
            assertDoesNotThrow(() -> boundView.unbind());
            assertFalse(boundView.isBound());
        }
    }

    // ==================== Error Condition Tests ====================

    @Nested
    @DisplayName("Error Conditions")
    class ErrorConditionTests {

        @Test
        @DisplayName("Null memory binding throws NPE")
        void testNullMemoryBinding() {
            assertThrows(NullPointerException.class,
                    () -> boundView.bind(null));

            assertThrows(NullPointerException.class,
                    () -> boundView.bind(null, 0, 100));
        }

        @Test
        @DisplayName("Negative offset throws")
        void testNegativeOffset() {
            assertThrows(IllegalArgumentException.class,
                    () -> boundView.bind(baseMemory, -1, 100));
        }

        @Test
        @DisplayName("Negative length throws")
        void testNegativeLength() {
            assertThrows(IllegalArgumentException.class,
                    () -> boundView.bind(baseMemory, 0, -1));
        }

        @Test
        @DisplayName("Offset beyond memory throws")
        void testOffsetBeyondMemory() {
            assertThrows(IllegalArgumentException.class,
                    () -> boundView.bind(baseMemory, SEGMENT_SIZE + 1, 10));
        }

        @Test
        @DisplayName("Length beyond memory throws")
        void testLengthBeyondMemory() {
            assertThrows(IllegalArgumentException.class,
                    () -> boundView.bind(baseMemory, 0, SEGMENT_SIZE + 1));
        }

        @Test
        @DisplayName("Offset plus length beyond memory throws")
        void testOffsetPlusLengthBeyondMemory() {
            assertThrows(IllegalArgumentException.class,
                    () -> boundView.bind(baseMemory, 500, 600));
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("Multiple rebinds with different modes")
        void testMultipleRebinds() {
            // Direct binding
            boundView.bind(baseMemory);
            assertEquals(SEGMENT_SIZE, boundView.length());
            assertSame(baseMemory.view(), boundView.view());

            // Mapped binding
            boundView.bind(baseMemory, 200, 150);
            assertEquals(150, boundView.length());
            assertSame(boundView.mappedView, boundView.view());

            // Back to direct
            boundView.bind(baseMemory);
            assertEquals(SEGMENT_SIZE, boundView.length());
            assertSame(baseMemory.view(), boundView.view());
        }

        @Test
        @DisplayName("Nested view binding")
        void testNestedViewBinding() {
            // Packet view
            BoundView packet = new BoundView() {};
            packet.bind(baseMemory);

            // Header view binds with offset
            BoundView header = new BoundView() {};
            header.bind(baseMemory, 0, 14);

            // Sub-element binds further
            BoundView subElement = new BoundView() {};
            subElement.bind(baseMemory, 6, 6);

            // Verify bounds
            assertEquals(SEGMENT_SIZE, packet.length());
            assertEquals(14, header.length());
            assertEquals(6, subElement.length());

            // Cleanup
            subElement.unbind();
            header.unbind();
            packet.unbind();
        }

        @Test
        @DisplayName("Data access through view")
        void testDataAccessThroughView() {
            // Write data to base memory
            baseMemory.segment().set(ValueLayout.JAVA_INT, 100, 0x12345678);

            // Bind view and read through it
            boundView.bind(baseMemory, 100, 4);
            int value = boundView.segment().get(ValueLayout.JAVA_INT, 
                                              boundView.start() - baseMemory.start());

            assertEquals(0x12345678, value);
        }

        @Test
        @DisplayName("ToString provides useful info")
        void testToString() {
            String unbound = boundView.toString();
            assertTrue(unbound.toLowerCase().contains("unbound"));

            boundView.bind(baseMemory);
            String bound = boundView.toString();
            assertTrue(bound.contains(String.valueOf(boundView.start())));
            assertTrue(bound.contains(String.valueOf(boundView.length())));
        }
    }
}