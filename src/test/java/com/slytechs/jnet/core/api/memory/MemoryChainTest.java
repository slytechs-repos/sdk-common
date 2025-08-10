package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MemoryChainTest {

    @Test
    void testBindMemoryFull() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            Assertions.assertEquals(100, chain.memoryCapacity(), "memoryCapacity in testBindMemoryFull");
            Assertions.assertEquals(0, chain.memoryOffset(), "memoryOffset in testBindMemoryFull");
            Assertions.assertEquals(100, chain.memoryEnd(), "memoryEnd in testBindMemoryFull");
            Assertions.assertEquals(100, chain.chainCapacity(), "chainCapacity in testBindMemoryFull");
            System.out.println("testBindMemoryFull: chainDataLength=" + chain.chainDataLength());
            Assertions.assertEquals(100, chain.chainDataLength(), "chainDataLength in testBindMemoryFull");
            Assertions.assertEquals(1, chain.chainMemoryCount(), "chainMemoryCount in testBindMemoryFull");
            Assertions.assertEquals(2, baseMemory.refCount(), "refCount in testBindMemoryFull");
        }
    }

    @Test
    void testBindMemorySlice() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            System.out.println("testBindMemorySlice: baseMemory.capacity=" + baseMemory.memoryCapacity());
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 10, 80);
            System.out.println("testBindMemorySlice: chain=" + chain.toString());
            Assertions.assertEquals(80, chain.memoryCapacity(), "memoryCapacity in testBindMemorySlice");
            Assertions.assertEquals(10, chain.memoryOffset(), "memoryOffset in testBindMemorySlice");
            Assertions.assertEquals(90, chain.memoryEnd(), "memoryEnd in testBindMemorySlice");
            Assertions.assertEquals(80, chain.chainCapacity(), "chainCapacity in testBindMemorySlice");
            System.out.println("testBindMemorySlice: chainDataLength=" + chain.chainDataLength());
            Assertions.assertEquals(80, chain.chainDataLength(), "chainDataLength in testBindMemorySlice");
            Assertions.assertEquals(1, chain.chainMemoryCount(), "chainMemoryCount in testBindMemorySlice");
            Assertions.assertEquals(2, baseMemory.refCount(), "refCount in testBindMemorySlice");
        }
    }

    @Test
    void testUnbindMemory() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            Assertions.assertEquals(2, baseMemory.refCount());
            chain.unbindMemory();
            Assertions.assertEquals(1, baseMemory.refCount());
            Assertions.assertThrows(IllegalStateException.class, chain::memoryCapacity);
        }
    }

    @Test
    void testChainTraversal() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg1 = arena.allocate(100);
            MemorySegment seg2 = arena.allocate(100);
            Memory mem1 = Memory.of(seg1, 0);
            Memory mem2 = Memory.of(seg2, 0);
            mem1.setNextMemory(mem2);
            MemoryProxy chain = new MemoryProxy();
            chain.bindMemory(mem1, 0);

            Assertions.assertEquals(200, chain.chainDataLength());
            Assertions.assertEquals(2, chain.chainMemoryCount());
            Assertions.assertTrue(chain.hasNextMemory());
            System.out.println("Before nextMemory: chain=" + chain.toString());
            Memory next = chain.nextMemory();
            System.out.println("After nextMemory: chain=" + chain.toString() + ", next=" + next);
            Assertions.assertEquals(mem2, next);
            Assertions.assertFalse(chain.hasNextMemory());
            System.out.println("After traversal: hasNextMemory=" + chain.hasNextMemory());

            Memory sought = chain.seekMemory(150);
            Assertions.assertEquals(mem2, sought);
        }
    }

    @Test
    void testChainRefCount() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            Assertions.assertEquals(2, chain.refCount());
            Assertions.assertEquals(3, chain.incrementRef());
            Assertions.assertEquals(3, chain.refCount());
            Assertions.assertEquals(2, chain.decrementRef());
            Assertions.assertEquals(2, chain.refCount());
        }
    }

    @Test
    void testChainAsByteBuffer() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            ByteBuffer buffer = chain.asByteBuffer();
            Assertions.assertEquals(100, buffer.capacity());
        }
    }

    @Test
    void testChainAsMemorySegment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            MemorySegment returned = chain.asMemorySegment();
            Assertions.assertEquals(segment, returned);
        }
    }

    @Test
    void testChainClose() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory baseMemory = Memory.of(segment, 0);
            MemoryProxy chain = new MemoryProxy();

            chain.bindMemory(baseMemory, 0);
            Assertions.assertEquals(2, baseMemory.refCount());
            chain.close();
            Assertions.assertEquals(1, baseMemory.refCount());
            Assertions.assertThrows(IllegalStateException.class, chain::refCount);
        }
    }
}