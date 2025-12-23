package com.slytechs.sdk.common.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

// FixedMemoryPool.java - simplified
public class FixedMemoryPool extends AbstractMemoryPool<FixedMemory> {
	private final long segmentSize;
	private final Arena arena;

	public FixedMemoryPool(
			String name,
			long capacity,
			long segmentSize,
			long defaultHeadroom) {

		super(name, capacity, defaultHeadroom);

		this.segmentSize = segmentSize;
		this.arena = Arena.ofShared();

		initializePool();
	}

	public FixedMemoryPool(String name, int capacity, long segmentSize) {
		this(name, capacity, segmentSize, 128); // Default 128 bytes headroom
	}

	private void initializePool() {
		// Allocate one large block for better memory locality
		long totalSize = capacity * segmentSize;
		MemorySegment totalMemory = arena.allocate(totalSize);

		// Slice into individual segments
		for (int i = 0; i < capacity; i++) {
			long offset = i * segmentSize;
			MemorySegment segment = totalMemory.asSlice(offset, segmentSize);

			FixedMemory memory = new FixedMemory(this, segment, 0, segmentSize);
			memory.start(defaultHeadroom);
			memory.end(defaultHeadroom); // Empty initially
			memory.refCount.set(0); // Start at 0 for pooling

			addToFreeList(memory);
		}
	}

	public FixedMemory allocate(long dataSize) {
		if (dataSize > segmentSize - defaultHeadroom) {
			return null; // Too large for pool segments
		}

		FixedMemory memory = allocate();
		if (memory != null) {
			memory.end(defaultHeadroom + dataSize);
		}
		return memory;
	}

	public long getSegmentSize() {
		return segmentSize;
	}

	public void close() {
		arena.close();
	}
}