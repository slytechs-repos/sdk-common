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

import java.lang.foreign.ValueLayout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.memory.FixedMemory;

@DisplayName("MemoryPool Test Suite")
class MemoryPoolTest {

    @Nested
    @DisplayName("MemoryPoolSettings")
    class MemoryPoolSettingsTest {

        @Test
        @DisplayName("Default settings should have expected values")
        void testDefaultSettings() {
            MemoryPoolSettings settings = new MemoryPoolSettings();
            assertEquals(64, settings.minCapacity());
            assertEquals(1024, settings.maxCapacity());
            assertEquals(9000, settings.segmentSize()); // Default jumbo frame
            assertEquals(0, settings.headroom());
            assertEquals(0, settings.tailroom());
        }

        @Test
        @DisplayName("Settings should be chainable")
        void testChainableSettings() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(100)
                    .maxCapacity(500)
                    .segmentSize(4096)
                    .headroom(64)
                    .tailroom(32);

            assertEquals(100, settings.minCapacity());
            assertEquals(500, settings.maxCapacity());
            assertEquals(4096, settings.segmentSize());
            assertEquals(64, settings.headroom());
            assertEquals(32, settings.tailroom());
        }

        @Test
        @DisplayName("Usable size should account for headroom and tailroom")
        void testUsableSize() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .segmentSize(1000)
                    .headroom(100)
                    .tailroom(50);

            assertEquals(850, settings.usableSize());
        }

        @Test
        @DisplayName("Validation should fail when headroom + tailroom >= segmentSize")
        void testValidationFails() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .segmentSize(100)
                    .headroom(50)
                    .tailroom(50);

            assertThrows(IllegalStateException.class, settings::validate);
        }

        @Test
        @DisplayName("Validation should pass with valid configuration")
        void testValidationPasses() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .segmentSize(100)
                    .headroom(40)
                    .tailroom(40);

            assertDoesNotThrow(settings::validate);
        }
    }

    @Nested
    @DisplayName("MemoryPool Basic Operations")
    class MemoryPoolBasicOperations {

        MemoryPool pool;

        @BeforeEach
        void setUp() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10)
                    .segmentSize(1024)
                    .headroom(0)
                    .tailroom(0);
            pool = new MemoryPool(settings);
        }

        @AfterEach
        void tearDown() {
            if (pool != null && !pool.isClosed()) {
                pool.close();
            }
        }

        @Test
        @DisplayName("Pool should preallocate minCapacity segments")
        void testPreallocation() {
            assertEquals(10, pool.available());
            assertEquals(10, pool.capacity());
        }

        @Test
        @DisplayName("Allocate should return non-null memory")
        void testAllocate() {
            FixedMemory memory = pool.allocate();
            assertNotNull(memory);
            assertEquals(9, pool.available());
        }

        @Test
        @DisplayName("Allocated memory should have correct segment size")
        void testAllocatedSegmentSize() {
            FixedMemory memory = pool.allocate();
            assertEquals(1024, memory.segment().byteSize());
        }

        @Test
        @DisplayName("Recycle should return memory to pool")
        void testRecycle() {
            FixedMemory memory = pool.allocate();
            assertEquals(9, pool.available());

            memory.poolRecycle();
            assertEquals(10, pool.available());
        }

        @Test
        @DisplayName("Allocate should return null when pool exhausted")
        void testExhaustion() {
            for (int i = 0; i < 10; i++) {
                assertNotNull(pool.allocate());
            }

            assertNull(pool.allocate());
            assertEquals(0, pool.available());
        }

        @Test
        @DisplayName("Memory can be reused after recycle")
        void testReuse() {
            FixedMemory memory1 = pool.allocate();
            memory1.segment().set(ValueLayout.JAVA_INT, 0, 12345);
            memory1.poolRecycle();

            FixedMemory memory2 = pool.allocate();
            assertNotNull(memory2);
            assertEquals(1024, memory2.segment().byteSize());
        }

        @Test
        @DisplayName("Pool release should work")
        void testPoolRelease() {
            FixedMemory memory = pool.allocate();
            assertEquals(9, pool.available());

            pool.release(memory);
            assertEquals(10, pool.available());
        }
    }

    @Nested
    @DisplayName("MemoryPool Headroom/Tailroom")
    class MemoryPoolHeadroomTailroom {

        @Test
        @DisplayName("Allocated memory should have start at headroom")
        void testHeadroomStart() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(64)
                    .tailroom(0);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            assertEquals(64, memory.start());
            assertEquals(1024, memory.end());

            pool.close();
        }

        @Test
        @DisplayName("Allocated memory should have end accounting for tailroom")
        void testTailroomEnd() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(0)
                    .tailroom(32);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            assertEquals(0, memory.start());
            assertEquals(992, memory.end());

            pool.close();
        }

        @Test
        @DisplayName("Both headroom and tailroom should work together")
        void testHeadroomAndTailroom() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(64)
                    .tailroom(32);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            assertEquals(64, memory.start());
            assertEquals(992, memory.end());
            assertEquals(928, memory.end() - memory.start());

            pool.close();
        }

        @Test
        @DisplayName("Bounds should reset on recycle")
        void testBoundsResetOnRecycle() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(64)
                    .tailroom(32);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            // Modify bounds
            memory.start(100);
            memory.end(500);

            // Recycle
            memory.poolRecycle();

            // Allocate again - should get reset bounds
            FixedMemory memory2 = pool.allocate();
            assertEquals(64, memory2.start());
            assertEquals(992, memory2.end());

            pool.close();
        }

        @Test
        @DisplayName("Headroom space should be accessible for prepending")
        void testHeadroomAccessible() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(64)
                    .tailroom(0);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            // Can write in headroom area if we adjust start
            memory.start(0);
            memory.segment().set(ValueLayout.JAVA_LONG, 0, 0xDEADBEEFL);
            assertEquals(0xDEADBEEFL, memory.segment().get(ValueLayout.JAVA_LONG, 0));

            pool.close();
        }

        @Test
        @DisplayName("Tailroom space should be accessible for appending")
        void testTailroomAccessible() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(1024)
                    .headroom(0)
                    .tailroom(32);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            // Can write in tailroom area if we adjust end
            memory.end(1024);
            memory.segment().set(ValueLayout.JAVA_INT, 1020, 0xCAFEBABE);
            assertEquals(0xCAFEBABE, memory.segment().get(ValueLayout.JAVA_INT, 1020));

            pool.close();
        }
    }

    @Nested
    @DisplayName("MemoryPool Dynamic Sizing")
    class MemoryPoolDynamicSizing {

        @Test
        @DisplayName("Pool should grow when exhausted")
        void testAutoGrow() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(20)
                    .segmentSize(256);

            MemoryPool pool = new MemoryPool(settings);
            assertEquals(5, pool.capacity());

            // Exhaust initial capacity
            for (int i = 0; i < 5; i++) {
                pool.allocate();
            }

            // Next allocate should trigger growth
            FixedMemory memory = pool.allocate();
            assertNotNull(memory);
            assertTrue(pool.capacity() > 5);

            pool.close();
        }

        @Test
        @DisplayName("Contract should reduce capacity and free slab memory")
        void testContract() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(100)
                    .segmentSize(256);

            MemoryPool pool = new MemoryPool(settings);
            pool.grow(20);
            assertEquals(25, pool.capacity());

            long contracted = pool.contractUnused(10);
            assertEquals(10, contracted);
            assertEquals(15, pool.capacity());

            pool.close();
        }
    }

    @Nested
    @DisplayName("MemoryPool Lifecycle")
    class MemoryPoolLifecycle {

        @Test
        @DisplayName("Close should release all memory")
        void testClose() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(256);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            pool.close();

            assertTrue(pool.isClosed());
            // Segment should be invalid after slab arena closes
            assertFalse(memory.segment().scope().isAlive());
        }

        @Test
        @DisplayName("Pool info methods should work")
        void testInfoMethods() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(10)
                    .maxCapacity(100)
                    .segmentSize(512)
                    .headroom(16)
                    .tailroom(8);

            MemoryPool pool = new MemoryPool(settings);

            assertEquals(10, pool.minCapacity());
            assertEquals(100, pool.maxCapacity());
            assertEquals(10, pool.capacity());
            assertEquals(512, pool.segmentSize());
            assertEquals(16, pool.headroom());
            assertEquals(8, pool.tailroom());
            assertEquals(488, pool.usableSize());

            pool.close();
        }

        @Test
        @DisplayName("Metrics should be accessible")
        void testMetrics() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(10)
                    .maxCapacity(10)
                    .segmentSize(256);

            MemoryPool pool = new MemoryPool(settings);

            pool.allocate();
            pool.allocate();
            FixedMemory m3 = pool.allocate();
            m3.poolRecycle();

            assertEquals(3, pool.metrics().allocations());
            assertEquals(1, pool.metrics().releases());

            pool.close();
        }
    }

    @Nested
    @DisplayName("MemoryPool Large Segments")
    class MemoryPoolLargeSegments {

        @Test
        @DisplayName("Should handle jumbo frame size segments")
        void testJumboFrames() {
            MemoryPoolSettings settings = new MemoryPoolSettings()
                    .minCapacity(5)
                    .maxCapacity(5)
                    .segmentSize(9000)
                    .headroom(64)
                    .tailroom(4);

            MemoryPool pool = new MemoryPool(settings);
            FixedMemory memory = pool.allocate();

            assertEquals(9000, memory.segment().byteSize());
            assertEquals(64, memory.start());
            assertEquals(8996, memory.end());
            assertEquals(8932, pool.usableSize());

            pool.close();
        }
    }
}