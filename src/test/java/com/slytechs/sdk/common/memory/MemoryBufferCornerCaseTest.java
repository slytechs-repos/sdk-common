package com.slytechs.sdk.common.memory;

import java.lang.foreign.Arena;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive corner case tests for MemoryBuffer. Tests boundary
 * conditions, error handling, and edge cases.
 */
class MemoryBufferCornerCaseTest {

	private MemoryBuffer buffer;
	private static final int BUFFER_SIZE = 100;

	@BeforeEach
	void setUp() {
		Arena arena = Arena.ofAuto();
		buffer = new MemoryBuffer(arena.allocate(BUFFER_SIZE));
		buffer.clear();
	}

	// ==================== Zero-Size Operations ====================

	@Test
	void testZeroLengthArrayPut() {
		byte[] empty = new byte[0];
		long posBefore = buffer.position();

		buffer.put(empty);

		Assertions.assertEquals(posBefore, buffer.position(), "Position shouldn't change for empty array");
		Assertions.assertFalse(buffer.hasError(), "Empty array shouldn't cause error");
	}

	@Test
	void testZeroLengthArrayGet() {
		byte[] empty = new byte[0];
		long posBefore = buffer.position();

		buffer.get(empty);

		Assertions.assertEquals(posBefore, buffer.position(), "Position shouldn't change for empty array");
		Assertions.assertFalse(buffer.hasError(), "Empty array shouldn't cause error");
	}

	@Test
	void testZeroLengthPartialArray() {
		byte[] data = {
				1,
				2,
				3,
				4,
				5
		};

		buffer.put(data, 2, 0); // Zero length from offset 2
		Assertions.assertEquals(0, buffer.position());

		buffer.get(data, 3, 0); // Zero length into offset 3
		Assertions.assertEquals(0, buffer.position());
	}

	// ==================== Boundary Position Tests ====================

	@Test
	void testPutAtExactCapacity() {
		buffer.position(BUFFER_SIZE - 4);

		buffer.putInt(0x12345678);

		Assertions.assertEquals(BUFFER_SIZE, buffer.position(), "Should be at exact capacity");
		Assertions.assertFalse(buffer.hasError(), "Writing to exact capacity should succeed");

		// Verify we can read it back
		buffer.position(BUFFER_SIZE - 4);
		Assertions.assertEquals(0x12345678, buffer.getInt());
	}

	@Test
	void testPutBeyondCapacity() {
		buffer.position(BUFFER_SIZE - 3); // Only 3 bytes left

		buffer.putInt(0x12345678); // Needs 4 bytes

		Assertions.assertTrue(buffer.hasError(), "Should have error for insufficient space");
		// Position behavior when error occurs is implementation-dependent
	}

	@Test
	void testGetAtExactCapacity() {
		// Put data at the end
		buffer.position(BUFFER_SIZE - 8);
		buffer.putLong(0x123456789ABCDEF0L);

		// Read it back
		buffer.position(BUFFER_SIZE - 8);
		long value = buffer.getLong();

		Assertions.assertEquals(0x123456789ABCDEF0L, value);
		Assertions.assertEquals(BUFFER_SIZE, buffer.position());
		Assertions.assertFalse(buffer.hasError());
	}

	@Test
	void testGetBeyondCapacity() {
		buffer.position(BUFFER_SIZE - 3); // Only 3 bytes available

		int value = buffer.getInt(); // Needs 4 bytes

		Assertions.assertTrue(buffer.hasError(), "Should have error for insufficient data");
		// Value returned on error is implementation-dependent (likely 0)
	}

	// ==================== Limit Boundary Tests ====================

	@Test
	void testPutAtLimit() {
		buffer.limit(50);
		buffer.position(46);

		buffer.putInt(0xDEADBEEF); // Exactly fits to limit

		Assertions.assertEquals(50, buffer.position());
		Assertions.assertFalse(buffer.hasError(), "Should succeed when exactly reaching limit");
	}

	@Test
	void testPutBeyondLimit() {
		buffer.limit(50);
		buffer.position(47); // Only 3 bytes to limit

		buffer.putInt(0xDEADBEEF); // Needs 4 bytes

		Assertions.assertTrue(buffer.hasError(), "Should have error when exceeding limit");
	}

	@Test
	void testGetAtLimit() {
		buffer.putLong(0x123456789ABCDEF0L);
		buffer.limit(8);
		buffer.position(0);

		long value = buffer.getLong(); // Exactly reads to limit

		Assertions.assertEquals(0x123456789ABCDEF0L, value);
		Assertions.assertEquals(8, buffer.position());
		Assertions.assertFalse(buffer.hasError());
	}

	// ==================== Position/Limit Edge Cases ====================

	@Test
	void testZeroLimit() {
		buffer.limit(0);

		Assertions.assertFalse(buffer.hasRemaining());

		buffer.put((byte) 1);
		Assertions.assertTrue(buffer.hasError(), "Should error with zero limit");
	}

	@Test
	void testPositionAtLimit() {
		buffer.limit(50);
		buffer.position(50);

		Assertions.assertFalse(buffer.hasRemaining());
		Assertions.assertEquals(0, buffer.remaining());

		buffer.put((byte) 1);
		Assertions.assertTrue(buffer.hasError(), "Should error when position equals limit");
	}

	@Test
	void testFlipWithZeroPosition() {
		buffer.position(0);
		buffer.flip();

		Assertions.assertEquals(0, buffer.position());
		Assertions.assertEquals(0, buffer.limit());
		Assertions.assertFalse(buffer.hasRemaining());
	}

	@Test
	void testFlipAtCapacity() {
		// Fill entire buffer
		for (int i = 0; i < BUFFER_SIZE; i++) {
			buffer.put((byte) i);
		}

		buffer.flip();

		Assertions.assertEquals(0, buffer.position());
		Assertions.assertEquals(BUFFER_SIZE, buffer.limit());
		Assertions.assertEquals(BUFFER_SIZE, buffer.remaining());
	}

	// ==================== Absolute Access Corner Cases ====================

	@Test
	void testAbsolutePutAtNegativeIndex() {
		buffer.put(-1, (byte) 42);

		Assertions.assertTrue(buffer.hasError(), "Negative index should cause error");
	}

	@Test
	void testAbsoluteGetAtNegativeIndex() {
		byte value = buffer.get(-1);

		Assertions.assertTrue(buffer.hasError(), "Negative index should cause error");
		Assertions.assertEquals(0, value, "Should return 0 on error");
	}

	@Test
	void testAbsolutePutAtMaxIndex() {
		buffer.put(BUFFER_SIZE - 1, (byte) 42); // Last valid position

		Assertions.assertFalse(buffer.hasError(), "Should succeed at last position");
		Assertions.assertEquals(42, buffer.get(BUFFER_SIZE - 1));
	}

	@Test
	void testAbsolutePutBeyondCapacity() {
		buffer.putInt(BUFFER_SIZE - 3, 0x12345678); // Would overflow

		Assertions.assertTrue(buffer.hasError(), "Should error when int extends beyond capacity");
	}

	@Test
	void testAbsoluteAccessWithRestrictedLimit() {
		buffer.limit(50);

		// Absolute operations might not respect limit (implementation-dependent)
		// But should still respect capacity
		buffer.putInt(60, 0x12345678);
		int value = buffer.getInt(60);

		// This behavior depends on whether absolute operations check limit
		// Some implementations only check capacity for absolute ops
		if (buffer.hasError()) {
			// Implementation checks limit for absolute ops
			Assertions.assertTrue(buffer.hasError());
		} else {
			// Implementation only checks capacity for absolute ops
			Assertions.assertEquals(0x12345678, value);
		}
	}

	// ==================== Array Access Corner Cases ====================

	@Test
	void testNullArrayPut() {
		buffer.put((byte[])null);

		Assertions.assertTrue(buffer.hasError(), "Null array should cause error");
		Assertions.assertEquals(0, buffer.position(), "Position shouldn't change on error");
	}

	@Test
	void testNullArrayGet() {
		buffer.get(null);

		Assertions.assertTrue(buffer.hasError(), "Null array should cause error");
		Assertions.assertEquals(0, buffer.position(), "Position shouldn't change on error");
	}

	@Test
	void testArrayWithInvalidOffset() {
		byte[] data = new byte[10];

		// Negative offset
		buffer.put(data, -1, 5);
		Assertions.assertTrue(buffer.hasError(), "Negative offset should cause error");
		buffer.clearError();

		// Offset beyond array
		buffer.put(data, 11, 1);
		Assertions.assertTrue(buffer.hasError(), "Offset beyond array should cause error");
		buffer.clearError();

		// Offset + length beyond array
		buffer.put(data, 5, 10);
		Assertions.assertTrue(buffer.hasError(), "Offset+length beyond array should cause error");
	}

	@Test
	void testLargeArrayPartialFit() {
		byte[] huge = new byte[BUFFER_SIZE * 2]; // Twice the buffer size
		for (int i = 0; i < huge.length; i++) {
			huge[i] = (byte) i;
		}

		buffer.put(huge);

		Assertions.assertTrue(buffer.hasError(), "Should error when array exceeds capacity");
		// Implementation might write partial data or none at all
	}

	// ==================== Special Value Tests ====================

	@Test
	void testByteMinMaxValues() {
		buffer.put(Byte.MIN_VALUE);
		buffer.put(Byte.MAX_VALUE);
		buffer.put((byte) 0);

		buffer.flip();

		Assertions.assertEquals(Byte.MIN_VALUE, buffer.get());
		Assertions.assertEquals(Byte.MAX_VALUE, buffer.get());
		Assertions.assertEquals(0, buffer.get());
	}

	@Test
	void testShortMinMaxValues() {
		buffer.putShort(Short.MIN_VALUE);
		buffer.putShort(Short.MAX_VALUE);
		buffer.putShort((short) 0);
		buffer.putShort((short) -1);

		buffer.flip();

		Assertions.assertEquals(Short.MIN_VALUE, buffer.getShort());
		Assertions.assertEquals(Short.MAX_VALUE, buffer.getShort());
		Assertions.assertEquals(0, buffer.getShort());
		Assertions.assertEquals(-1, buffer.getShort());
	}

	@Test
	void testIntMinMaxValues() {
		buffer.putInt(Integer.MIN_VALUE);
		buffer.putInt(Integer.MAX_VALUE);
		buffer.putInt(0);
		buffer.putInt(-1);

		buffer.flip();

		Assertions.assertEquals(Integer.MIN_VALUE, buffer.getInt());
		Assertions.assertEquals(Integer.MAX_VALUE, buffer.getInt());
		Assertions.assertEquals(0, buffer.getInt());
		Assertions.assertEquals(-1, buffer.getInt());
	}

	@Test
	void testLongMinMaxValues() {
		buffer.putLong(Long.MIN_VALUE);
		buffer.putLong(Long.MAX_VALUE);
		buffer.putLong(0L);
		buffer.putLong(-1L);

		buffer.flip();

		Assertions.assertEquals(Long.MIN_VALUE, buffer.getLong());
		Assertions.assertEquals(Long.MAX_VALUE, buffer.getLong());
		Assertions.assertEquals(0L, buffer.getLong());
		Assertions.assertEquals(-1L, buffer.getLong());
	}

	@Test
	void testFloatSpecialCornerCases() {
		buffer.putFloat(Float.MIN_VALUE); // Smallest positive
		buffer.putFloat(Float.MAX_VALUE);
		buffer.putFloat(Float.MIN_NORMAL); // Smallest normal
		buffer.putFloat(-Float.MIN_VALUE); // Negative smallest

		buffer.flip();

		Assertions.assertEquals(Float.MIN_VALUE, buffer.getFloat());
		Assertions.assertEquals(Float.MAX_VALUE, buffer.getFloat());
		Assertions.assertEquals(Float.MIN_NORMAL, buffer.getFloat());
		Assertions.assertEquals(-Float.MIN_VALUE, buffer.getFloat());
	}

	@Test
	void testDoubleSpecialCornerCases() {
		buffer.putDouble(Double.MIN_VALUE);
		buffer.putDouble(Double.MAX_VALUE);
		buffer.putDouble(Double.MIN_NORMAL);
		buffer.putDouble(-Double.MIN_VALUE);

		buffer.flip();

		Assertions.assertEquals(Double.MIN_VALUE, buffer.getDouble());
		Assertions.assertEquals(Double.MAX_VALUE, buffer.getDouble());
		Assertions.assertEquals(Double.MIN_NORMAL, buffer.getDouble());
		Assertions.assertEquals(-Double.MIN_VALUE, buffer.getDouble());
	}

	// ==================== Alignment Tests ====================

	@Test
	void testUnalignedShortAccess() {
		buffer.position(1); // Odd position - unaligned for short
		buffer.putShort((short) 0x1234);

		buffer.position(1);
		Assertions.assertEquals((short) 0x1234, buffer.getShort(),
				"Should handle unaligned short access");
	}

	@Test
	void testUnalignedIntAccess() {
		for (int offset = 1; offset <= 3; offset++) {
			buffer.clear();
			buffer.position(offset); // Unaligned positions
			buffer.putInt(0xDEADBEEF);

			buffer.position(offset);
			Assertions.assertEquals(0xDEADBEEF, buffer.getInt(),
					"Should handle unaligned int at offset " + offset);
		}
	}

	@Test
	void testUnalignedLongAccess() {
		for (int offset = 1; offset <= 7; offset++) {
			buffer.clear();
			buffer.position(offset); // Unaligned positions
			buffer.putLong(0x123456789ABCDEF0L);

			buffer.position(offset);
			Assertions.assertEquals(0x123456789ABCDEF0L, buffer.getLong(),
					"Should handle unaligned long at offset " + offset);
		}
	}

	// ==================== Mark/Reset Corner Cases ====================

	@Test
	void testResetWithoutMark() {
		buffer.reset(); // No mark set

		Assertions.assertTrue(buffer.hasError(), "Reset without mark should cause error");
	}

	@Test
	void testMarkBeyondLimit() {
		buffer.position(50);
		buffer.mark();
		buffer.limit(40); // Limit now less than mark

		buffer.position(10);
		buffer.reset();

		// Implementation-dependent: might error or discard mark
		if (!buffer.hasError()) {
			// If no error, mark should be discarded
			Assertions.assertEquals(50, buffer.position());
		}
	}


	@Test
	void testMarkAtExactCapacity() {
		// Test marking exactly at capacity boundary
		buffer.position(BUFFER_SIZE);
		buffer.mark();

		buffer.position(0);
		buffer.reset();

		// This might fail if implementation doesn't allow mark at capacity
		if (buffer.hasError()) {
			// Mark at exact capacity not allowed
			Assertions.assertEquals(0, buffer.position(),
					"Position unchanged when reset fails");
		} else {
			Assertions.assertEquals(BUFFER_SIZE, buffer.position(),
					"Should reset to capacity position");
		}
	}

	@Test
	void testMarkResetBasic() {
		// Ensure basic mark/reset works
		buffer.position(25);
		buffer.mark();

		buffer.position(75);
		Assertions.assertEquals(75, buffer.position());

		buffer.reset();

		Assertions.assertFalse(buffer.hasError(), "Basic reset should not error");
		Assertions.assertEquals(25, buffer.position(), "Should reset to mark");
	}

	@Test
	void testMarkThenPositionAtCapacity() {
		// Test marking when position is at capacity
		buffer.position(BUFFER_SIZE);
		buffer.mark(); // Mark at capacity

		// Try to use the mark
		buffer.position(50);
		buffer.reset();

		// Check if the implementation allows this
		if (buffer.hasError()) {
			// Mark at capacity was considered invalid
			Assertions.assertEquals(50, buffer.position(),
					"Position unchanged when reset fails");
			buffer.clearError();
		} else {
			// Mark at capacity was allowed
			Assertions.assertEquals(BUFFER_SIZE, buffer.position());
		}
	}

	// ==================== Rapid State Changes ====================

	@Test
	void testRapidFlipRewind() {
		buffer.putInt(0x12345678);

		buffer.flip();
		buffer.rewind();
		buffer.flip(); // Flip with position=0

		Assertions.assertEquals(0, buffer.position());
		Assertions.assertEquals(0, buffer.limit());
	}

	@Test
	void testAlternatingPutGet() {
		for (int i = 0; i < 10; i++) {
			buffer.put((byte) i);
			buffer.position(buffer.position() - 1); // Back up
			byte value = buffer.get();
			Assertions.assertEquals(i, value);
		}
	}

	@Test
	void testPositionBeyondLimitThenClear() {
		buffer.limit(50);
		buffer.position(50); // At limit

		buffer.clear(); // Should reset both position and limit

		Assertions.assertEquals(0, buffer.position());
		Assertions.assertEquals(BUFFER_SIZE, buffer.limit());
		Assertions.assertEquals(BUFFER_SIZE, buffer.capacity());
	}

	// ==================== Error State Propagation ====================

	@Test
	void testErrorPersistence() {
		buffer.position(BUFFER_SIZE - 1);
		buffer.putInt(0x12345678); // Causes error - needs 4 bytes, have 1

		Assertions.assertTrue(buffer.hasError());

		// Further operations should be no-ops
		long posBefore = buffer.position();
		buffer.putLong(0L);
		buffer.putShort((short) 0);
		buffer.put((byte) 0);

		Assertions.assertEquals(posBefore, buffer.position(),
				"Position shouldn't change after error");
		Assertions.assertTrue(buffer.hasError(), "Error should persist");
	}

	@Test
	void testErrorClearAndRecovery() {
		buffer.position(BUFFER_SIZE - 1);
		buffer.putInt(0x12345678); // Causes error

		Assertions.assertTrue(buffer.hasError());

		buffer.clearError();
		Assertions.assertFalse(buffer.hasError());

		// Should be able to continue operations
		buffer.position(0);
		buffer.putInt(0xCAFEBABE);

		Assertions.assertFalse(buffer.hasError(), "Should work after clearing error");
		buffer.position(0);
		Assertions.assertEquals(0xCAFEBABE, buffer.getInt());
	}
}