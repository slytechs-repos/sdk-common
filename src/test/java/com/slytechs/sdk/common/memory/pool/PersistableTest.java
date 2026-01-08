/*
 * Apache License, Version 2.0
 * 
 * Copyright 2005-2025 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.memory.pool;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.memory.BoundView;
import com.slytechs.sdk.common.memory.Memory;
import com.slytechs.sdk.common.memory.ScopedMemory;

/**
 * Comprehensive test suite for the {@link Persistable} interface.
 * 
 * <p>
 * Tests cover all default method implementations using a minimal test
 * implementation, verifying correct behavior for scoped vs fixed memory,
 * pooled vs non-pooled objects, and all copy/persist/duplicate operations.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@DisplayName("Persistable Interface Tests")
class PersistableTest {

    /** Test data pattern for verification. */
    private static final byte[] TEST_DATA = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    
    /** Test data size. */
    private static final int DATA_SIZE = TEST_DATA.length;

    /**
     * Minimal Persistable implementation for testing.
     */
    static class TestPersistable extends BoundView implements Persistable<TestPersistable> {
        
        private final PoolEntry poolEntry = new PoolEntry() {
            @Override
            protected void onRecycle() {
                if (!isPooled()) {
                    unbind();
                } else {
                    boundMemory().unbindIfScoped();
                }
            }
        };

        @Override
        public TestPersistable newUnbound() {
            return new TestPersistable();
        }

        @Override
        public PoolEntry poolEntry() {
            return poolEntry;
        }
        
        /**
         * Writes test data to the bound memory.
         */
        void writeTestData(byte[] data) {
            assert isBound() : "Must be bound before writing";
            MemorySegment seg = boundMemory().segment();
            for (int i = 0; i < data.length && i < seg.byteSize(); i++) {
                seg.set(java.lang.foreign.ValueLayout.JAVA_BYTE, i, data[i]);
            }
        }
        
        /**
         * Reads data from bound memory for verification.
         */
        byte[] readData(int length) {
            assert isBound() : "Must be bound before reading";
            byte[] data = new byte[length];
            MemorySegment seg = boundMemory().segment();
            for (int i = 0; i < length && i < seg.byteSize(); i++) {
                data[i] = seg.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i);
            }
            return data;
        }
    }

    /**
     * Creates a scoped (non-persistent) test object with data.
     */
    private TestPersistable createScopedWithData() {
        TestPersistable obj = new TestPersistable();
        ScopedMemory scoped = new ScopedMemory();
        
        // Bind scoped to backing fixed memory (simulating native buffer)
        MemorySegment backing = Arena.ofAuto().allocate(DATA_SIZE, 8);
        scoped.bind(backing, 0, DATA_SIZE);
        
        obj.bind(scoped);
        obj.writeTestData(TEST_DATA);
        return obj;
    }

    /**
     * Creates a fixed (persistent) test object with data.
     */
    private TestPersistable createFixedWithData() {
        TestPersistable obj = new TestPersistable();
        Memory fixed = Memory.of(DATA_SIZE);
        obj.bind(fixed);
        obj.writeTestData(TEST_DATA);
        return obj;
    }

    /**
     * Creates a pool of fixed-memory test objects.
     */
    private Pool<TestPersistable> createFixedPool(int capacity) {
        PoolSettings settings = new PoolSettings()
                .capacity(capacity)
                .segmentSize(9000)
                .preallocate(true);
        
        return new FreeListPool<>(settings, this::createFixedWithData);
    }

    @Nested
    @DisplayName("isPersistent() Tests")
    class IsPersistentTests {

        @Test
        @DisplayName("Unbound object is not persistent")
        void unboundIsNotPersistent() {
            TestPersistable obj = new TestPersistable();
            assertFalse(obj.isPersistent());
        }

        @Test
        @DisplayName("Scoped memory object is not persistent")
        void scopedIsNotPersistent() {
            TestPersistable obj = createScopedWithData();
            assertFalse(obj.isPersistent());
        }

        @Test
        @DisplayName("Fixed memory object is persistent")
        void fixedIsPersistent() {
            TestPersistable obj = createFixedWithData();
            assertTrue(obj.isPersistent());
        }
    }

    @Nested
    @DisplayName("persist() Tests")
    class PersistTests {

        @Test
        @DisplayName("Persist on fixed returns same instance")
        void persistOnFixedReturnsSame() {
            TestPersistable fixed = createFixedWithData();
            TestPersistable result = fixed.persist();
            
            assertSame(fixed, result, "Should return same instance for fixed memory");
        }

        @Test
        @DisplayName("Persist on scoped returns new copy")
        void persistOnScopedReturnsNewCopy() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable result = scoped.persist();
            
            assertNotSame(scoped, result, "Should return new instance for scoped memory");
            assertTrue(result.isPersistent(), "Result should be persistent");
        }

        @Test
        @DisplayName("Persist on scoped copies data correctly")
        void persistOnScopedCopiesData() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable result = scoped.persist();
            
            assertArrayEquals(TEST_DATA, result.readData(DATA_SIZE), "Data should be copied");
        }

        @Test
        @DisplayName("Persist result is not pooled by default")
        void persistResultIsNotPooled() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable result = scoped.persist();
            
            assertFalse(result.poolEntry().isPooled(), "Default persist should not be pooled");
        }
    }

    @Nested
    @DisplayName("persistTo(T target) Tests")
    class PersistToTargetTests {

        @Test
        @DisplayName("PersistTo on fixed returns same instance, target unused")
        void persistToOnFixedReturnsSame() {
            TestPersistable fixed = createFixedWithData();
            TestPersistable target = createFixedWithData();
            
            TestPersistable result = fixed.persistTo(target);
            
            assertSame(fixed, result, "Should return same instance for fixed memory");
        }

        @Test
        @DisplayName("PersistTo on scoped copies to target and returns target")
        void persistToOnScopedCopiesToTarget() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable target = createFixedWithData();
            // Clear target data first
            target.writeTestData(new byte[DATA_SIZE]);
            
            TestPersistable result = scoped.persistTo(target);
            
            assertSame(target, result, "Should return target instance");
            assertArrayEquals(TEST_DATA, result.readData(DATA_SIZE), "Data should be copied to target");
        }
    }

    @Nested
    @DisplayName("persistTo(Pool) Tests")
    class PersistToPoolTests {

        @Test
        @DisplayName("PersistTo pool on fixed returns same instance")
        void persistToPoolOnFixedReturnsSame() {
            TestPersistable fixed = createFixedWithData();
            Pool<TestPersistable> pool = createFixedPool(10);
            
            TestPersistable result = fixed.persistTo(pool);
            
            assertSame(fixed, result, "Should return same instance for fixed memory");
            
            pool.close();
        }

        @Test
        @DisplayName("PersistTo pool on scoped returns pooled copy")
        void persistToPoolOnScopedReturnsPooledCopy() {
            TestPersistable scoped = createScopedWithData();
            Pool<TestPersistable> pool = createFixedPool(10);
            
            TestPersistable result = scoped.persistTo(pool);
            
            assertNotSame(scoped, result, "Should return new instance");
            assertTrue(result.poolEntry().isPooled(), "Result should be pooled");
            assertArrayEquals(TEST_DATA, result.readData(DATA_SIZE), "Data should be copied");
            
            pool.close();
        }

        @Test
        @DisplayName("PersistTo pool result can be recycled back to pool")
        void persistToPoolResultCanBeRecycled() {
            TestPersistable scoped = createScopedWithData();
            Pool<TestPersistable> pool = createFixedPool(10);
            
            long availableBefore = pool.available();
            TestPersistable result = scoped.persistTo(pool);
            long availableAfterAllocate = pool.available();
            
            result.recycle();
            long availableAfterRecycle = pool.available();
            
            assertEquals(availableBefore - 1, availableAfterAllocate, "Pool should have one less after allocate");
            assertEquals(availableBefore, availableAfterRecycle, "Pool should be restored after recycle");
            
            pool.close();
        }
    }

    @Nested
    @DisplayName("copy() Tests")
    class CopyTests {

        @Test
        @DisplayName("Copy creates new instance")
        void copyCreatesNewInstance() {
            TestPersistable original = createFixedWithData();
            TestPersistable copy = original.copy();
            
            assertNotSame(original, copy, "Copy should be new instance");
        }

        @Test
        @DisplayName("Copy is persistent")
        void copyIsPersistent() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable copy = scoped.copy();
            
            assertTrue(copy.isPersistent(), "Copy should be persistent");
        }

        @Test
        @DisplayName("Copy contains same data")
        void copyContainsSameData() {
            TestPersistable original = createFixedWithData();
            TestPersistable copy = original.copy();
            
            assertArrayEquals(TEST_DATA, copy.readData(DATA_SIZE), "Copy should contain same data");
        }

        @Test
        @DisplayName("Copy is independent - modifying copy doesn't affect original")
        void copyIsIndependent() {
            TestPersistable original = createFixedWithData();
            TestPersistable copy = original.copy();
            
            // Modify copy
            copy.writeTestData(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                           (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
            
            // Original unchanged
            assertArrayEquals(TEST_DATA, original.readData(DATA_SIZE), "Original should be unchanged");
        }

        @Test
        @DisplayName("Copy is not pooled")
        void copyIsNotPooled() {
            TestPersistable original = createFixedWithData();
            TestPersistable copy = original.copy();
            
            assertFalse(copy.poolEntry().isPooled(), "Copy should not be pooled");
        }
    }

    @Nested
    @DisplayName("copyTo(T target) Tests")
    class CopyToTargetTests {

        @Test
        @DisplayName("CopyTo copies data to target")
        void copyToTargetCopiesData() {
            TestPersistable source = createFixedWithData();
            TestPersistable target = createFixedWithData();
            target.writeTestData(new byte[DATA_SIZE]); // Clear target
            
            TestPersistable result = source.copyTo(target);
            
            assertSame(target, result, "Should return target");
            assertArrayEquals(TEST_DATA, target.readData(DATA_SIZE), "Target should contain source data");
        }

        @Test
        @DisplayName("CopyTo works from scoped to fixed")
        void copyToFromScopedToFixed() {
            TestPersistable scoped = createScopedWithData();
            TestPersistable fixed = createFixedWithData();
            fixed.writeTestData(new byte[DATA_SIZE]); // Clear
            
            scoped.copyTo(fixed);
            
            assertArrayEquals(TEST_DATA, fixed.readData(DATA_SIZE), "Data should be copied");
        }
    }

    @Nested
    @DisplayName("copyTo(Pool) Tests")
    class CopyToPoolTests {

        @Test
        @DisplayName("CopyTo pool allocates and copies")
        void copyToPoolAllocatesAndCopies() {
            TestPersistable source = createFixedWithData();
            Pool<TestPersistable> pool = createFixedPool(10);
            
            TestPersistable result = source.copyTo(pool);
            
            assertNotSame(source, result, "Should return new instance from pool");
            assertTrue(result.poolEntry().isPooled(), "Result should be pooled");
            assertArrayEquals(TEST_DATA, result.readData(DATA_SIZE), "Data should be copied");
            
            pool.close();
        }

        @Test
        @DisplayName("CopyTo pool result can be recycled")
        void copyToPoolResultCanBeRecycled() {
            TestPersistable source = createFixedWithData();
            Pool<TestPersistable> pool = createFixedPool(10);
            
            long availableBefore = pool.available();
            TestPersistable result = source.copyTo(pool);
            
            result.recycle();
            
            assertEquals(availableBefore, pool.available(), "Pool should be restored after recycle");
            
            pool.close();
        }
    }

    @Nested
    @DisplayName("duplicate() Tests")
    class DuplicateTests {

        @Test
        @DisplayName("Duplicate creates new instance")
        void duplicateCreatesNewInstance() {
            TestPersistable original = createFixedWithData();
            TestPersistable dup = original.duplicate();
            
            assertNotSame(original, dup, "Duplicate should be new instance");
        }

        @Test
        @DisplayName("Duplicate shares memory - sees same data")
        void duplicateSharesMemory() {
            TestPersistable original = createFixedWithData();
            TestPersistable dup = original.duplicate();
            
            assertArrayEquals(TEST_DATA, dup.readData(DATA_SIZE), "Duplicate should see same data");
        }

        @Test
        @DisplayName("Duplicate shares memory - modifications visible to both")
        void duplicateModificationsShared() {
            TestPersistable original = createFixedWithData();
            TestPersistable dup = original.duplicate();
            
            byte[] newData = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD,
                              (byte) 0xEE, (byte) 0xFF, (byte) 0x11, (byte) 0x22};
            original.writeTestData(newData);
            
            assertArrayEquals(newData, dup.readData(DATA_SIZE), 
                    "Duplicate should see modifications to original");
        }

        @Test
        @DisplayName("Duplicate increments reference count")
        void duplicateIncrementsRefCount() {
            TestPersistable original = createFixedWithData();
            int refCountBefore = original.boundMemory().refCount();
            
            TestPersistable dup = original.duplicate();
            
            int refCountAfter = original.boundMemory().refCount();
            assertEquals(refCountBefore + 1, refCountAfter, "Ref count should be incremented");
            
            // Cleanup
            dup.boundMemory().decrementRef();
        }
    }

    @Nested
    @DisplayName("duplicate(T target) Tests")
    class DuplicateToTargetTests {

        @Test
        @DisplayName("Duplicate to target binds to shared memory")
        void duplicateToTargetBindsShared() {
            TestPersistable original = createFixedWithData();
            TestPersistable target = new TestPersistable();
            ScopedMemory scoped = new ScopedMemory();
            target.bind(scoped);
            
            original.duplicate(target);
            
            assertArrayEquals(TEST_DATA, target.readData(DATA_SIZE), 
                    "Target should see original's data");
        }

        @Test
        @DisplayName("Duplicate to target increments ref count")
        void duplicateToTargetIncrementsRef() {
            TestPersistable original = createFixedWithData();
            TestPersistable target = new TestPersistable();
            ScopedMemory scoped = new ScopedMemory();
            target.bind(scoped);
            
            int refCountBefore = original.boundMemory().refCount();
            original.duplicate(target);
            int refCountAfter = original.boundMemory().refCount();
            
            assertEquals(refCountBefore + 1, refCountAfter, "Ref count should be incremented");
            
            // Cleanup
            original.boundMemory().decrementRef();
        }
    }

    @Nested
    @DisplayName("newUnbound() Tests")
    class NewUnboundTests {

        @Test
        @DisplayName("newUnbound returns unbound instance")
        void newUnboundReturnsUnbound() {
            TestPersistable original = createFixedWithData();
            TestPersistable unbound = original.newUnbound();
            
            assertFalse(unbound.isBound(), "newUnbound should return unbound instance");
        }

        @Test
        @DisplayName("newUnbound returns correct type")
        void newUnboundReturnsCorrectType() {
            TestPersistable original = createFixedWithData();
            TestPersistable unbound = original.newUnbound();
            
            assertInstanceOf(TestPersistable.class, unbound, "Should return correct type");
        }

        @Test
        @DisplayName("newUnbound is not persistent")
        void newUnboundIsNotPersistent() {
            TestPersistable original = createFixedWithData();
            TestPersistable unbound = original.newUnbound();
            
            assertFalse(unbound.isPersistent(), "Unbound should not be persistent");
        }
    }

    @Nested
    @DisplayName("Recycle Integration Tests")
    class RecycleIntegrationTests {

        @Test
        @DisplayName("Recycle on non-pooled is no-op")
        void recycleOnNonPooledIsNoOp() {
            TestPersistable obj = createFixedWithData();
            
            // Should not throw
            assertDoesNotThrow(() -> obj.recycle());
        }

        @Test
        @DisplayName("Recycle on pooled returns to pool")
        void recycleOnPooledReturnsToPool() {
            Pool<TestPersistable> pool = createFixedPool(10);
            
            long availableBefore = pool.available();
            TestPersistable obj = pool.allocate();
            long availableAfterAllocate = pool.available();
            
            obj.recycle();
            long availableAfterRecycle = pool.available();
            
            assertEquals(availableBefore - 1, availableAfterAllocate);
            assertEquals(availableBefore, availableAfterRecycle);
            
            pool.close();
        }

        @Test
        @DisplayName("Copy then recycle - copy remains valid")
        void copyThenRecycleCopyRemainsValid() {
            Pool<TestPersistable> pool = createFixedPool(10);
            TestPersistable original = pool.allocate();
            original.writeTestData(TEST_DATA);
            
            TestPersistable copy = original.copy();
            original.recycle();
            
            // Copy should still be valid and contain data
            assertArrayEquals(TEST_DATA, copy.readData(DATA_SIZE), 
                    "Copy should remain valid after original recycled");
            
            pool.close();
        }

        @Test
        @DisplayName("Persist workflow - scoped to queue to recycle")
        void persistWorkflowComplete() {
            // Simulate capture -> persist -> queue -> process -> recycle
            Pool<TestPersistable> persistPool = createFixedPool(10);
            
            // "Capture" - scoped packet
            TestPersistable captured = createScopedWithData();
            assertFalse(captured.isPersistent());
            
            // Persist to pool
            TestPersistable keeper = captured.persistTo(persistPool);
            assertTrue(keeper.isPersistent());
            assertTrue(keeper.poolEntry().isPooled());
            assertArrayEquals(TEST_DATA, keeper.readData(DATA_SIZE));
            
            // "Process"
            byte[] processed = keeper.readData(DATA_SIZE);
            assertArrayEquals(TEST_DATA, processed);
            
            // Recycle
            long availableBefore = persistPool.available();
            keeper.recycle();
            assertEquals(availableBefore + 1, persistPool.available());
            
            persistPool.close();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Multiple duplicates share same memory")
        void multipleDuplicatesShareMemory() {
            TestPersistable original = createFixedWithData();
            TestPersistable dup1 = original.duplicate();
            TestPersistable dup2 = original.duplicate();
            TestPersistable dup3 = original.duplicate();
            
            // All should see same data
            assertArrayEquals(TEST_DATA, dup1.readData(DATA_SIZE));
            assertArrayEquals(TEST_DATA, dup2.readData(DATA_SIZE));
            assertArrayEquals(TEST_DATA, dup3.readData(DATA_SIZE));
            
            // Modify via original
            byte[] newData = {(byte) 0x99, (byte) 0x88, (byte) 0x77, (byte) 0x66,
                              (byte) 0x55, (byte) 0x44, (byte) 0x33, (byte) 0x22};
            original.writeTestData(newData);
            
            // All duplicates see change
            assertArrayEquals(newData, dup1.readData(DATA_SIZE));
            assertArrayEquals(newData, dup2.readData(DATA_SIZE));
            assertArrayEquals(newData, dup3.readData(DATA_SIZE));
            
            // Ref count should be original + 3
            assertEquals(4, original.boundMemory().refCount());
            
            // Cleanup
            dup1.boundMemory().decrementRef();
            dup2.boundMemory().decrementRef();
            dup3.boundMemory().decrementRef();
        }

        @Test
        @DisplayName("Copy of copy is independent")
        void copyOfCopyIsIndependent() {
            TestPersistable original = createFixedWithData();
            TestPersistable copy1 = original.copy();
            TestPersistable copy2 = copy1.copy();
            
            // Modify copy1
            copy1.writeTestData(new byte[DATA_SIZE]);
            
            // copy2 should still have original data
            assertArrayEquals(TEST_DATA, copy2.readData(DATA_SIZE));
            
            // original unchanged
            assertArrayEquals(TEST_DATA, original.readData(DATA_SIZE));
        }

        @Test
        @DisplayName("Persist already persistent is identity")
        void persistAlreadyPersistentIsIdentity() {
            TestPersistable fixed = createFixedWithData();
            
            TestPersistable p1 = fixed.persist();
            TestPersistable p2 = p1.persist();
            TestPersistable p3 = p2.persist();
            
            assertSame(fixed, p1);
            assertSame(fixed, p2);
            assertSame(fixed, p3);
        }
    }
}