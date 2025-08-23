package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests basic data access operations for MemoryBuffer. Focuses on typical
 * usage patterns with valid operations.
 */
class MemoryByteBufferBasicAccessTest {

	private MemoryBuffer buffer;
	private static final int BUFFER_SIZE = 1024;

	@BeforeEach
	void setUp() {
		Arena arena = Arena.ofAuto();
		buffer = new MemoryBuffer(arena.allocate(BUFFER_SIZE));
		buffer.clear(); // Start fresh: position=0, limit=capacity
	}

	// ==================== Byte Operations ====================

	@Test
	void testBytePutGet() {
		byte value = 42;

		buffer.put(value);
		Assertions.assertEquals(1, buffer.position(), "Position should advance by 1");

		buffer.flip(); // Prepare for reading
		byte result = buffer.get();

		Assertions.assertEquals(value, result, "Should read back same value");
		Assertions.assertEquals(1, buffer.position(), "Position should advance after get");
	}

	@Test
	void testByteAbsolutePutGet() {
		byte value1 = 10;
		byte value2 = 20;
		byte value3 = 30;

		// Absolute puts don't change position
		buffer.put(100, value1);
		buffer.put(200, value2);
		buffer.put(300, value3);
		Assertions.assertEquals(0, buffer.position(), "Position should not change");

		// Absolute gets don't change position
		Assertions.assertEquals(value1, buffer.get(100));
		Assertions.assertEquals(value2, buffer.get(200));
		Assertions.assertEquals(value3, buffer.get(300));
		Assertions.assertEquals(0, buffer.position(), "Position should not change");
	}

	@Test
	void testByteArrayPutGet() {
		byte[] data = {
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9,
				10
		};

		buffer.put(data);
		Assertions.assertEquals(data.length, buffer.position());

		buffer.flip();
		byte[] result = new byte[data.length];
		buffer.get(result);

		Assertions.assertArrayEquals(data, result, "Array should match");
		Assertions.assertEquals(data.length, buffer.position());
	}

	@Test
	void testByteArrayPartialPutGet() {
		byte[] data = {
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				8,
				9,
				10
		};
		int offset = 2;
		int length = 5;

		buffer.put(data, offset, length);
		Assertions.assertEquals(length, buffer.position());

		buffer.flip();
		byte[] result = new byte[length];
		buffer.get(result, 0, length);

		// Should have elements 3,4,5,6,7 (indices 2-6 from original)
		for (int i = 0; i < length; i++) {
			Assertions.assertEquals(data[offset + i], result[i]);
		}
	}

	// ==================== Short Operations ====================

	@Test
	void testShortPutGet() {
		short value = 12345;

		buffer.putShort(value);
		Assertions.assertEquals(2, buffer.position(), "Position should advance by 2");

		buffer.flip();
		short result = buffer.getShort();

		Assertions.assertEquals(value, result, "Should read back same value");
		Assertions.assertEquals(2, buffer.position());
	}

	@Test
	void testShortAbsolutePutGet() {
		short value1 = 100;
		short value2 = 200;

		buffer.putShort(10, value1);
		buffer.putShort(50, value2);
		Assertions.assertEquals(0, buffer.position(), "Position should not change");

		Assertions.assertEquals(value1, buffer.getShort(10));
		Assertions.assertEquals(value2, buffer.getShort(50));
		Assertions.assertEquals(0, buffer.position(), "Position should not change");
	}

	@Test
	void testMultipleShorts() {
		short[] values = {
				100,
				200,
				300,
				400,
				500
		};

		for (short v : values) {
			buffer.putShort(v);
		}
		Assertions.assertEquals(values.length * 2, buffer.position());

		buffer.flip();
		for (short expected : values) {
			Assertions.assertEquals(expected, buffer.getShort());
		}
	}

	// ==================== Int Operations ====================

	@Test
	void testIntPutGet() {
		int value = 0x12345678;

		buffer.putInt(value);
		Assertions.assertEquals(4, buffer.position(), "Position should advance by 4");

		buffer.flip();
		int result = buffer.getInt();

		Assertions.assertEquals(value, result, "Should read back same value");
		Assertions.assertEquals(4, buffer.position());
	}

	@Test
	void testIntAbsolutePutGet() {
		int value1 = 0xDEADBEEF;
		int value2 = 0xCAFEBABE;

		buffer.putInt(20, value1);
		buffer.putInt(100, value2);
		Assertions.assertEquals(0, buffer.position());

		Assertions.assertEquals(value1, buffer.getInt(20));
		Assertions.assertEquals(value2, buffer.getInt(100));
		Assertions.assertEquals(0, buffer.position());
	}

	@Test
	void testMultipleInts() {
		int[] values = {
				0x11111111,
				0x22222222,
				0x33333333,
				0x44444444
		};

		for (int v : values) {
			buffer.putInt(v);
		}
		Assertions.assertEquals(values.length * 4, buffer.position());

		buffer.flip();
		for (int expected : values) {
			Assertions.assertEquals(expected, buffer.getInt());
		}
	}

	// ==================== Long Operations ====================

	@Test
	void testLongPutGet() {
		long value = 0x123456789ABCDEF0L;

		buffer.putLong(value);
		Assertions.assertEquals(8, buffer.position(), "Position should advance by 8");

		buffer.flip();
		long result = buffer.getLong();

		Assertions.assertEquals(value, result, "Should read back same value");
		Assertions.assertEquals(8, buffer.position());
	}

	@Test
	void testLongAbsolutePutGet() {
		long value1 = Long.MAX_VALUE;
		long value2 = Long.MIN_VALUE;

		buffer.putLong(32, value1);
		buffer.putLong(64, value2);
		Assertions.assertEquals(0, buffer.position());

		Assertions.assertEquals(value1, buffer.getLong(32));
		Assertions.assertEquals(value2, buffer.getLong(64));
		Assertions.assertEquals(0, buffer.position());
	}

	// ==================== Float Operations ====================

	@Test
	void testFloatPutGet() {
		float value = 3.14159f;

		buffer.putFloat(value);
		Assertions.assertEquals(4, buffer.position());

		buffer.flip();
		float result = buffer.getFloat();

		Assertions.assertEquals(value, result, 0.0001f, "Should read back same value");
		Assertions.assertEquals(4, buffer.position());
	}

	@Test
	void testFloatSpecialValues() {
		buffer.putFloat(Float.POSITIVE_INFINITY);
		buffer.putFloat(Float.NEGATIVE_INFINITY);
		buffer.putFloat(Float.NaN);
		buffer.putFloat(0.0f);
		buffer.putFloat(-0.0f);

		buffer.flip();

		Assertions.assertEquals(Float.POSITIVE_INFINITY, buffer.getFloat());
		Assertions.assertEquals(Float.NEGATIVE_INFINITY, buffer.getFloat());
		Assertions.assertTrue(Float.isNaN(buffer.getFloat()));
		Assertions.assertEquals(0.0f, buffer.getFloat());
		Assertions.assertEquals(-0.0f, buffer.getFloat());
	}

	// ==================== Double Operations ====================

	@Test
	void testDoublePutGet() {
		double value = Math.PI;

		buffer.putDouble(value);
		Assertions.assertEquals(8, buffer.position());

		buffer.flip();
		double result = buffer.getDouble();

		Assertions.assertEquals(value, result, 0.0000001, "Should read back same value");
		Assertions.assertEquals(8, buffer.position());
	}

	@Test
	void testDoubleSpecialValues() {
		buffer.putDouble(Double.POSITIVE_INFINITY);
		buffer.putDouble(Double.NEGATIVE_INFINITY);
		buffer.putDouble(Double.NaN);
		buffer.putDouble(0.0);
		buffer.putDouble(-0.0);

		buffer.flip();

		Assertions.assertEquals(Double.POSITIVE_INFINITY, buffer.getDouble());
		Assertions.assertEquals(Double.NEGATIVE_INFINITY, buffer.getDouble());
		Assertions.assertTrue(Double.isNaN(buffer.getDouble()));
		Assertions.assertEquals(0.0, buffer.getDouble());
		Assertions.assertEquals(-0.0, buffer.getDouble());
	}

	// ==================== Char Operations ====================

	@Test
	void testCharPutGet() {
		char value = 'A';

		buffer.putChar(value);
		Assertions.assertEquals(2, buffer.position());

		buffer.flip();
		char result = buffer.getChar();

		Assertions.assertEquals(value, result);
		Assertions.assertEquals(2, buffer.position());
	}

	@Test
	void testUnicodeChars() {
		char[] chars = {
				'H',
				'e',
				'l',
				'l',
				'o',
				' ',
				'世',
				'界',
				'!'
		}; // Hello World! with Chinese

		for (char c : chars) {
			buffer.putChar(c);
		}

		buffer.flip();

		for (char expected : chars) {
			Assertions.assertEquals(expected, buffer.getChar());
		}
	}

	// ==================== Mixed Type Operations ====================

	@Test
	void testMixedTypePutGet() {
		// Simulate a typical packet header structure
		byte version = 4;
		byte headerLength = 20;
		short totalLength = 1500;
		int identification = 0x1234;
		long timestamp = System.currentTimeMillis();

		// Write in sequence
		buffer.put(version);
		buffer.put(headerLength);
		buffer.putShort(totalLength);
		buffer.putInt(identification);
		buffer.putLong(timestamp);

		Assertions.assertEquals(1 + 1 + 2 + 4 + 8, buffer.position());

		// Read back
		buffer.flip();

		Assertions.assertEquals(version, buffer.get());
		Assertions.assertEquals(headerLength, buffer.get());
		Assertions.assertEquals(totalLength, buffer.getShort());
		Assertions.assertEquals(identification, buffer.getInt());
		Assertions.assertEquals(timestamp, buffer.getLong());
	}

	@Test
	void testSequentialWrites() {
		// Write different types sequentially
		for (int i = 0; i < 10; i++) {
			buffer.put((byte) i);
			buffer.putShort((short) (i * 10));
			buffer.putInt(i * 100);
			buffer.putLong(i * 1000L);
		}

		buffer.flip();

		// Read back in same order
		for (int i = 0; i < 10; i++) {
			Assertions.assertEquals((byte) i, buffer.get());
			Assertions.assertEquals((short) (i * 10), buffer.getShort());
			Assertions.assertEquals(i * 100, buffer.getInt());
			Assertions.assertEquals(i * 1000L, buffer.getLong());
		}
	}

	// ==================== Position Management During Access ====================

	@Test
	void testRelativeVsAbsolutePositioning() {
		// Relative operations advance position
		buffer.putInt(0x11111111);
		Assertions.assertEquals(4, buffer.position());

		buffer.putInt(0x22222222);
		Assertions.assertEquals(8, buffer.position());

		// Absolute operations don't change position
		buffer.putInt(100, 0x33333333);
		Assertions.assertEquals(8, buffer.position(), "Absolute put shouldn't change position");

		// Can mix relative and absolute
		buffer.putInt(0x44444444);
		Assertions.assertEquals(12, buffer.position());

		// Set limit to include position 100
		buffer.limit(104); // Need at least 104 to read int at position 100
		buffer.position(0); // Reset position for reading

		// Verify all values
		Assertions.assertEquals(0x11111111, buffer.getInt());
		Assertions.assertEquals(0x22222222, buffer.getInt());
		Assertions.assertEquals(0x44444444, buffer.getInt());

		// Absolute get at position 100
		Assertions.assertEquals(0x33333333, buffer.getInt(100));
	}

	@Test
	void testBulkOperationPositioning() {
		byte[] data1 = new byte[100];
		byte[] data2 = new byte[200];

		for (int i = 0; i < data1.length; i++)
			data1[i] = (byte) i;
		for (int i = 0; i < data2.length; i++)
			data2[i] = (byte) (i + 100);

		buffer.put(data1);
		Assertions.assertEquals(100, buffer.position());

		buffer.put(data2);
		Assertions.assertEquals(300, buffer.position());

		buffer.flip();

		byte[] result1 = new byte[100];
		byte[] result2 = new byte[200];

		buffer.get(result1);
		Assertions.assertEquals(100, buffer.position());

		buffer.get(result2);
		Assertions.assertEquals(300, buffer.position());

		Assertions.assertArrayEquals(data1, result1);
		Assertions.assertArrayEquals(data2, result2);
	}
}