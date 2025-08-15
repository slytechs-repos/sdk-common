package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MemoryApiTest {

    @Test
    void testMemoryOfFullSegment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 0);

            Assertions.assertEquals(100, memory.segmentSize());
            Assertions.assertEquals(0, memory.segmentOffset());
            Assertions.assertEquals(100, memory.segmentEnd());
            Assertions.assertEquals(0, memory.activeBytesStart());
            Assertions.assertEquals(100, memory.activeBytesEnd());
            Assertions.assertEquals(100, memory.activeBytesLength());
            Assertions.assertEquals(100, memory.totalSegmentSize());
            Assertions.assertEquals(100, memory.totalActiveBytes());
            Assertions.assertEquals(1, memory.refCount());
            Assertions.assertFalse(memory.hasNextSegment());
            Assertions.assertNull(memory.nextSegment());
            Assertions.assertFalse(memory.isNull());
            Assertions.assertFalse(memory.isPointer());
        }
    }

    @Test
    void testMemoryOfSlicedSegment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 10, 80);

            Assertions.assertEquals(80, memory.segmentSize());
            Assertions.assertEquals(10, memory.segmentOffset());
            Assertions.assertEquals(90, memory.segmentEnd());
            Assertions.assertEquals(10, memory.activeBytesStart());
            Assertions.assertEquals(90, memory.activeBytesEnd());
            Assertions.assertEquals(80, memory.activeBytesLength());
            Assertions.assertEquals(80, memory.totalSegmentSize());
            Assertions.assertEquals(80, memory.totalActiveBytes());
            Assertions.assertEquals(1, memory.refCount());
            Assertions.assertFalse(memory.hasNextSegment());
            Assertions.assertNull(memory.nextSegment());
        }
    }

    @Test
    void testMemoryAsByteBuffer() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 0);

            ByteBuffer buffer = memory.asByteBuffer();
            Assertions.assertEquals(100, buffer.capacity());
            Assertions.assertEquals(0, buffer.position());
            Assertions.assertEquals(100, buffer.limit());
        }
    }

    @Test
    void testMemoryAsMemorySegment() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 0);

            MemorySegment returnedSegment = memory.asMemorySegment();
            Assertions.assertEquals(segment, returnedSegment);
        }
    }

    @Test
    void testMemoryRefCountIncrementAndDecrement() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 0);

            Assertions.assertEquals(1, memory.refCount());
            Assertions.assertEquals(2, memory.incrementRef());
            Assertions.assertEquals(2, memory.refCount());
            Assertions.assertEquals(1, memory.decrementRef());
            Assertions.assertEquals(1, memory.refCount());
        }
    }

    @Test
    void testMemoryClose() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(100);
            Memory memory = Memory.of(segment, 0);

            Assertions.assertEquals(1, memory.refCount());
            memory.decrementRef(); // Reduces to 0 and calls close()
            Assertions.assertThrows(IllegalStateException.class, memory::segmentSize);
        }
    }

    @Test
    void testMemoryNullCheck() {
        Assertions.assertTrue(Memory.isNull(null));
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nullSegment = MemorySegment.ofAddress(0);
            Assertions.assertTrue(Memory.isNull(nullSegment));
        }
    }

    @Test
    void testMemoryPointerCheck() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pointer = MemorySegment.ofAddress(12345);
            Memory memory = Memory.of(pointer, 0, 0);
            Assertions.assertTrue(memory.isPointer());
            Assertions.assertEquals(0, memory.segmentSize());
        }
    }
}