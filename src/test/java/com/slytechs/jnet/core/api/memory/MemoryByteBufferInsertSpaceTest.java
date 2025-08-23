package com.slytechs.jnet.core.api.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.Arena;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for MemoryBuffer insertSpace operation. Tests single-segment and
 * multi-segment scenarios with proper reference counting.
 */
class MemoryByteBufferInsertSpaceTest {

	private static final int SEGMENT_SIZE = 256;
	private static final int SEGMENT_COUNT = 32;
	private static final int DEFAULT_HEADROOM = 32;
	private static final int DATA_SIZE = SEGMENT_SIZE - 2 * DEFAULT_HEADROOM; // 192 bytes for data

	private Arena arena;
	private MemoryPool<MemoryBuffer> pool;
	private BufferMetrics metrics;

	@BeforeEach
	void setUp() {
		arena = Arena.ofAuto();
		pool = new MemoryPool<>(
				"TestPool",
				SEGMENT_SIZE, SEGMENT_COUNT,
				DEFAULT_HEADROOM, DEFAULT_HEADROOM,
				Arena.ofAuto(),
				MemoryBuffer::new);
		metrics = pool.getBufferMetrics();
		metrics.reset();
	}

	@AfterEach
	void tearDown() {
		// Pool cleanup happens automatically when arena closes
	}

	// ==================== Single Segment Tests ====================

	@Test
	void testInsertSpaceAtBeginningWithHeadroom() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill with test data
		byte[] testData = new byte[50];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip(); // pos=0, limit=50

		// Insert space at beginning
		buffer.position(0);
		buffer.insertSpace(10);

		// Verify space was inserted
		assertEquals(0, buffer.position());
		assertEquals(60, buffer.limit());

		// Verify no data movement (used headroom)
		assertEquals(0, metrics.getBytesMovedTotal());
		assertEquals(1, metrics.getBoundsAdjustments());

		// Skip the gap and verify original data
		buffer.position(10);
		byte[] result = new byte[50];
		buffer.get(result);
		assertArrayEquals(testData, result);

		// Verify buffer will be released
		assertEquals(1, buffer.refCount());
		buffer.decrementRef();
		// Buffer is now released, don't access it
	}

	@Test
	void testInsertSpaceAtBeginningWithoutHeadroom() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Expand to use all headroom
		buffer.activeBytesStart(0);
		buffer.clear();

		// Fill with test data
		byte[] testData = new byte[50];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip();

		// Insert space at beginning (will move data right)
		buffer.position(0);
		buffer.insertSpace(10);

		// Verify space was inserted
		assertEquals(0, buffer.position());
		assertEquals(60, buffer.limit());

		// Verify data was moved right
		assertEquals(50, metrics.getBytesMovedRight());
		assertEquals(1, metrics.getBoundsAdjustments());

		// Skip the gap and verify original data
		buffer.position(10);
		byte[] result = new byte[50];
		buffer.get(result);
		assertArrayEquals(testData, result);

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}

	@Test
	void testInsertSpaceInMiddleOptimalLeft() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill with test data
		byte[] testData = new byte[100];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip();

		// Insert space at position 30 (30 bytes before, 70 after)
		// Should move left side (30 bytes) into headroom
		buffer.position(30);
		buffer.insertSpace(20);

		// Verify space was inserted
		assertEquals(50, buffer.position()); // Position after gap
		assertEquals(120, buffer.limit());

		// Verify left side was moved (optimal)
		assertEquals(30, metrics.getBytesMovedLeft());
		assertEquals(1, metrics.getBoundsAdjustments());

		// Verify data integrity
		buffer.position(0);
		byte[] beforeGap = new byte[30];
		buffer.get(beforeGap);
		for (int i = 0; i < 30; i++) {
			assertEquals((byte) i, beforeGap[i]);
		}

		buffer.position(50); // After gap
		byte[] afterGap = new byte[70];
		buffer.get(afterGap);
		for (int i = 0; i < 70; i++) {
			assertEquals((byte) (i + 30), afterGap[i]);
		}

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}

	@Test
	void testInsertSpaceInMiddleOptimalRight() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill with test data
		byte[] testData = new byte[100];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip();

		// Insert space at position 70 (70 bytes before, 30 after)
		// Should move right side (30 bytes) into tailroom
		buffer.position(70);
		buffer.insertSpace(20);

		// Verify space was inserted
		assertEquals(70, buffer.position()); // Position at gap start
		assertEquals(120, buffer.limit());

		// Verify right side was moved (optimal)
		assertEquals(30, metrics.getBytesMovedRight());
		assertEquals(1, metrics.getBoundsAdjustments());

		// Verify data integrity
		buffer.position(0);
		byte[] beforeGap = new byte[70];
		buffer.get(beforeGap);
		for (int i = 0; i < 70; i++) {
			assertEquals((byte) i, beforeGap[i]);
		}

		buffer.position(90); // After gap
		byte[] afterGap = new byte[30];
		buffer.get(afterGap);
		for (int i = 0; i < 30; i++) {
			assertEquals((byte) (i + 70), afterGap[i]);
		}

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}

	@Test
	void testInsertSpaceAtEnd() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill with test data
		byte[] testData = new byte[50];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip();

		// Insert space at end
		buffer.position(50);
		buffer.insertSpace(30);

		// Verify space was inserted
		assertEquals(50, buffer.position());
		assertEquals(80, buffer.limit());

		// Verify no data movement (just expanded into tailroom)
		assertEquals(0, metrics.getBytesMovedTotal());
		assertEquals(1, metrics.getBoundsAdjustments());

		// Verify original data
		buffer.position(0);
		byte[] result = new byte[50];
		buffer.get(result);
		assertArrayEquals(testData, result);

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}

	// ==================== Multi-Segment Tests ====================

	@Test
	void testInsertSpaceWithOverflowToNewSegment() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill buffer near capacity
		int dataSize = DATA_SIZE - 10; // Leave only 10 bytes tailroom
		byte[] testData = new byte[dataSize];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) (i % 256);
		}
		buffer.put(testData);
		buffer.flip();
		
		// After buffer.put(testData); buffer.flip();
		buffer.position(162);
		byte checkByte = buffer.get();
		System.out.printf("After write, byte at position 162: expected %d, got %d%n", 
		    (byte)162, checkByte);
		
		buffer.position(162); // Reset position for the test
		// Try to insert 50 bytes at position near end
		// This will require allocating a new segment
		buffer.position(dataSize - 20);
		buffer.insertSpace(50);

		// Verify new segment was allocated
		assertEquals(1, metrics.getSegmentsAllocated());
		assertTrue(buffer.hasNextSegment());

		// Verify data integrity
		buffer.position(0);

		// Check data before insertion point
		byte[] beforeInsert = new byte[dataSize - 20];
		buffer.get(beforeInsert);
		for (int i = 0; i < beforeInsert.length; i++) {
			assertEquals((byte) (i % 256), beforeInsert[i]);
		}

		// Skip the gap
		buffer.position(buffer.position() + 50);

		// Check data after insertion point
		byte[] afterInsert = new byte[20];
		buffer.get(afterInsert);
		for (int i = 0; i < afterInsert.length; i++) {
			assertEquals((byte) ((dataSize - 20 + i) % 256), afterInsert[i]);
		}

		// Release buffer and linked segment
		MemoryBuffer nextSeg = (MemoryBuffer) buffer.nextSegment();
		assertEquals(1, buffer.refCount());
		buffer.decrementRef();
		assertEquals(2, nextSeg.refCount());
		nextSeg.decrementRef();
	}

	@Test
	void testInsertSpaceWithPushToNextSegmentHeadroom() {
	    MemoryBuffer buffer1 = pool.allocate();
	    MemoryBuffer buffer2 = pool.allocate();
	    assertNotNull(buffer1);
	    assertNotNull(buffer2);

	    // Link segments
	    buffer1.setNextMemory(buffer2);

	    // Fill first buffer to use up most tailroom
	    // We need to ensure there's less than 20 bytes of total room
	    // Buffer has DEFAULT_HEADROOM (32) at start and end
	    // We need to fill it so tailroom < 20
	    
	    // Expand active bytes to use headroom and most of tailroom
	    buffer1.activeBytesStart(0);  // Use all headroom
	    buffer1.activeBytesEnd(SEGMENT_SIZE - 10);  // Leave only 10 bytes tailroom
	    buffer1.clear();  // Reset position/limit for the expanded region
	    
	    // Fill the expanded buffer with data
	    int fillSize = (int)(buffer1.limit() - 10);  // Leave 10 bytes at end
	    byte[] data1 = new byte[fillSize];
	    for (int i = 0; i < data1.length; i++) {
	        data1[i] = (byte) i;
	    }
	    buffer1.put(data1);
	    buffer1.flip();

	    // Fill second buffer partially (leaving headroom available)
	    byte[] data2 = new byte[50];
	    for (int i = 0; i < data2.length; i++) {
	        data2[i] = (byte) (i + 100);
	    }
	    buffer2.put(data2);
	    buffer2.flip();

	    // Position near end where we have only 10 bytes room total
	    // (0 headroom + 10 tailroom = 10 bytes available, need 20)
	    buffer1.position(fillSize - 10);
	    buffer1.insertSpace(20);

	    // Verify no new segment allocated (used next segment's headroom)
	    assertEquals(0, metrics.getSegmentsAllocated());

	    // Verify data was pushed to next segment
	    assertTrue(metrics.getBytesCopiedCrossSegment() > 0);

	    // Unlink before release
	    buffer1.setNextMemory(null);
	    
	    // Release buffers
	    assertEquals(1, buffer1.refCount());
	    buffer1.decrementRef();
	    assertEquals(1, buffer2.refCount());
	    buffer2.decrementRef();
	}

	@Test
	void testInsertSpaceChainedSegments() {
		MemoryBuffer buffer1 = pool.allocate();
		MemoryBuffer buffer2 = pool.allocate();
		MemoryBuffer buffer3 = pool.allocate();
		assertNotNull(buffer1);
		assertNotNull(buffer2);
		assertNotNull(buffer3);

		// Create chain
		buffer1.setNextMemory(buffer2);
		buffer2.setNextMemory(buffer3);

		// Fill all buffers
		for (int i = 0; i < DATA_SIZE; i++) {
			buffer1.put((byte) i);
		}
		for (int i = 0; i < DATA_SIZE; i++) {
			buffer2.put((byte) (i + 100));
		}
		for (int i = 0; i < DATA_SIZE; i++) {
			buffer3.put((byte) (i + 200));
		}

		// Position in middle of chain (second segment)
		buffer1.chainPosition(DATA_SIZE + 50);

		// Insert space
		buffer1.insertSpace(30);

		// Verify operation succeeded
		assertEquals(DATA_SIZE + 50, buffer1.position());

		// Release all buffers
		assertEquals(1, buffer1.refCount());
		buffer1.decrementRef();
		assertEquals(2, buffer2.refCount());
		buffer2.decrementRef();
		assertEquals(2, buffer3.refCount());
		buffer3.decrementRef();
	}

	@Test
	void testInsertSpaceLargeGapRequiringMultipleSegments() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Fill buffer
		byte[] testData = new byte[100];
		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}
		buffer.put(testData);
		buffer.flip();

		// Try to insert very large space (requires new segment)
		buffer.position(50);
		buffer.insertSpace(SEGMENT_SIZE); // Larger than any single segment room
		var debugString = buffer.toString();
		System.out.println(debugString);
		
		// Verify new segment was allocated
		assertTrue(metrics.getSegmentsAllocated() > 0);
		assertTrue(buffer.hasNextSegment());

		// Verify we can still read original data around the gap
		buffer.position(0);
		byte[] beforeGap = new byte[50];
		buffer.get(beforeGap);
		for (int i = 0; i < 50; i++) {
			assertEquals((byte) i, beforeGap[i]);
		}

		// Clean up allocated segments
		Memory current = buffer;
		while (current != null) {
			Memory next = current.nextSegment();
			assertEquals(current == buffer ? 1 : 2, ((MemoryBuffer) current).refCount());
			((MemoryBuffer) current).decrementRef();
			current = next;
		}
	}

	@Test
	void testInsertSpaceErrorHandling() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);

		// Test negative size
		buffer.insertSpace(-10);
		assertFalse(buffer.hasError()); // Should be no-op, not error

		// Test zero size
		buffer.insertSpace(0);
		assertFalse(buffer.hasError()); // Should be no-op, not error

		// Fill buffer completely (no room left)
		buffer.activeBytesStart(0);
		buffer.activeBytesEnd(SEGMENT_SIZE);
		buffer.clear();

		// Try to insert when no room and no pool
		MemoryBuffer standaloneBuffer = new MemoryBuffer(
				arena.allocate(100), 0, 100);
		standaloneBuffer.activeBytesStart(0);
		standaloneBuffer.activeBytesEnd(100);
		standaloneBuffer.position(50);
		standaloneBuffer.insertSpace(10);
		assertTrue(standaloneBuffer.hasError());

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}

	@Test
	void testInsertSpaceMetricsTracking() {
		MemoryBuffer buffer = pool.allocate();
		assertNotNull(buffer);
		metrics.reset();

		// Perform various insert operations
		buffer.put(new byte[100]);
		buffer.flip();

		// Insert at beginning (uses headroom)
		buffer.position(0);
		buffer.insertSpace(10);
		assertEquals(1, metrics.getInsertSpaceOps());

		// Insert in middle (moves data)
		buffer.position(50);
		buffer.insertSpace(20);
		assertEquals(2, metrics.getInsertSpaceOps());
		assertTrue(metrics.getBytesMovedTotal() > 0);

		// Insert at end (uses tailroom)
		buffer.position(buffer.limit());
		buffer.insertSpace(30);
		assertEquals(3, metrics.getInsertSpaceOps());

		// Verify metrics
		assertTrue(metrics.getBoundsAdjustments() > 0);

		// Release buffer
		assertEquals(1, buffer.refCount());  // Check before release
		buffer.decrementRef();
		// Don't access buffer after release - it's invalid
	}
}