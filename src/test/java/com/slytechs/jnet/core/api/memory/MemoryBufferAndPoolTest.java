package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MemoryByteBufferAndPoolTest {

	private MemoryPool<MemoryByteBuffer> createPool() {
		return new MemoryPool<>(64L, 3L, Arena.ofConfined(),
				(owningPool, segment, offset, length) -> new MemoryByteBuffer(owningPool, segment, offset, length));
	}

	@Test
	void testMemoryByteBufferMutableOffsets() {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment segment = arena.allocate(100);
			MemoryByteBuffer buffer = new MemoryByteBuffer(Memory.of(segment, 0));

			Assertions.assertEquals(0, buffer.memoryDataOffset());
			Assertions.assertEquals(100, buffer.memoryDataEnd());
			Assertions.assertEquals(100, buffer.memoryDataLength());

			buffer.memoryDataOffset(10);
			Assertions.assertEquals(10, buffer.memoryDataOffset());
			Assertions.assertEquals(100, buffer.memoryDataEnd());
			Assertions.assertEquals(90, buffer.memoryDataLength());

			buffer.memoryDataEnd(90);
			Assertions.assertEquals(10, buffer.memoryDataOffset());
			Assertions.assertEquals(90, buffer.memoryDataEnd());
			Assertions.assertEquals(80, buffer.memoryDataLength());

			Assertions.assertTrue(buffer.hasMemoryLeadingSpace());
			Assertions.assertEquals(10, buffer.memoryLeadingSpace());
			Assertions.assertTrue(buffer.hasMemoryTrailingSpace());
			Assertions.assertEquals(10, buffer.memoryTrailingSpace());
			Assertions.assertTrue(buffer.hasMemoryDataRemaining());
		}
	}

	@Test
	void testMemoryByteBufferFromPool() {
		MemoryPool<MemoryByteBuffer> pool = createPool();
		MemoryByteBuffer buffer = pool.allocate();

		Assertions.assertEquals(64, buffer.memoryCapacity());
		Assertions.assertEquals(0, buffer.memoryDataOffset());
		Assertions.assertEquals(64, buffer.memoryDataEnd());
		Assertions.assertEquals(64, buffer.memoryDataLength());
		Assertions.assertEquals(1, buffer.refCount());
		Assertions.assertEquals(pool, buffer.getOwningPool());

		buffer.decrementRef();
		Assertions.assertEquals(1, pool.getFreeListSize());
	}

	@Test
	void testMemoryPoolAllocationAndRelease() {
		MemoryPool<MemoryByteBuffer> pool = createPool();
		Assertions.assertEquals(3, pool.getFreeListSize());

		MemoryByteBuffer buf1 = pool.allocate();
		MemoryByteBuffer buf2 = pool.allocate();
		MemoryByteBuffer buf3 = pool.allocate();
		Assertions.assertEquals(0, pool.getFreeListSize());

		Assertions.assertThrows(OutOfMemoryError.class, pool::allocate);

		buf1.decrementRef();
		Assertions.assertEquals(1, pool.getFreeListSize());

		MemoryByteBuffer buf1Realloc = pool.allocate();
		Assertions.assertEquals(buf1, buf1Realloc);
		Assertions.assertEquals(1, buf1Realloc.refCount());
		Assertions.assertEquals(0, pool.getFreeListSize());

		buf2.decrementRef();
		buf3.decrementRef();
		buf1Realloc.decrementRef();
		Assertions.assertEquals(3, pool.getFreeListSize());
	}

	@Test
	void testMemoryByteBufferResetForReuse() {
		MemoryPool<MemoryByteBuffer> pool = createPool();
		MemoryByteBuffer buffer = pool.allocate();

		buffer.memoryDataOffset(10);
		buffer.memoryDataEnd(50);
		Assertions.assertEquals(10, buffer.memoryDataOffset());
		Assertions.assertEquals(50, buffer.memoryDataEnd());

		buffer.decrementRef();
		MemoryByteBuffer reused = pool.allocate();
		Assertions.assertEquals(buffer, reused);
		Assertions.assertEquals(0, reused.memoryDataOffset());
		Assertions.assertEquals(64, reused.memoryDataEnd());
		Assertions.assertEquals(64, reused.memoryDataLength());
	}
}