package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for MemoryBuffer accessor methods with normal, straightforward operations.
 * No cross-boundary or spanning scenarios - just single segment access patterns.
 * Uses the new nomenclature (activeBytes*, segment*, headroom/tailroom).
 */
class MemoryBufferAccessorTest {
    
    private MemoryBuffer buffer;
    private static final int SEGMENT_SIZE = 1024;
    
    @BeforeEach
    void setUp() {
        Arena arena = Arena.ofAuto();
        MemorySegment segment = arena.allocate(SEGMENT_SIZE);
        buffer = new MemoryBuffer(segment);
        buffer.clear(); // position=0, limit=segmentSize()
    }
    
    // ==================== Byte Accessors ====================
    
    @Test
    void testPutGetByte() {
        // Single byte operations
        buffer.put((byte) 0x42);
        buffer.put((byte) 0xFF);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x7F);
        buffer.put((byte) 0x80);
        
        Assertions.assertEquals(5, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals((byte) 0x42, buffer.get());
        Assertions.assertEquals((byte) 0xFF, buffer.get());
        Assertions.assertEquals((byte) 0x00, buffer.get());
        Assertions.assertEquals((byte) 0x7F, buffer.get());
        Assertions.assertEquals((byte) 0x80, buffer.get());
    }
    
    @Test
    void testPutGetByteAbsolute() {
        // Absolute indexing doesn't change position
        buffer.put(10, (byte) 0xAA);
        buffer.put(20, (byte) 0xBB);
        buffer.put(30, (byte) 0xCC);
        
        Assertions.assertEquals(0, buffer.position(), "Position shouldn't change");
        
        Assertions.assertEquals((byte) 0xAA, buffer.get(10));
        Assertions.assertEquals((byte) 0xBB, buffer.get(20));
        Assertions.assertEquals((byte) 0xCC, buffer.get(30));
        
        Assertions.assertEquals(0, buffer.position(), "Position still unchanged");
    }
    
    @Test
    void testPutGetByteArray() {
        byte[] testData = new byte[100];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i & 0xFF);
        }
        
        buffer.put(testData);
        Assertions.assertEquals(100, buffer.position());
        
        buffer.flip();
        
        byte[] result = new byte[100];
        buffer.get(result);
        
        Assertions.assertArrayEquals(testData, result);
        Assertions.assertEquals(100, buffer.position());
    }
    
    @Test
    void testPutGetByteArrayPartial() {
        byte[] source = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        
        // Put middle portion
        buffer.put(source, 3, 4); // Elements 3,4,5,6
        Assertions.assertEquals(4, buffer.position());
        
        buffer.flip();
        
        byte[] dest = new byte[10];
        buffer.get(dest, 2, 4); // Read into positions 2-5
        
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(0, dest[1]);
        Assertions.assertEquals(3, dest[2]);
        Assertions.assertEquals(4, dest[3]);
        Assertions.assertEquals(5, dest[4]);
        Assertions.assertEquals(6, dest[5]);
        Assertions.assertEquals(0, dest[6]);
    }
    
    // ==================== Short Accessors ====================
    
    @Test
    void testPutGetShort() {
        buffer.putShort((short) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) -1);
        buffer.putShort((short) 32767);
        buffer.putShort((short) -32768);
        buffer.putShort((short) 0x1234);
        
        Assertions.assertEquals(12, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals((short) 0, buffer.getShort());
        Assertions.assertEquals((short) 1, buffer.getShort());
        Assertions.assertEquals((short) -1, buffer.getShort());
        Assertions.assertEquals((short) 32767, buffer.getShort());
        Assertions.assertEquals((short) -32768, buffer.getShort());
        Assertions.assertEquals((short) 0x1234, buffer.getShort());
    }
    
    @Test
    void testPutGetShortAbsolute() {
        buffer.putShort(100, (short) 0xABCD);
        buffer.putShort(200, (short) 0x1234);
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals((short) 0xABCD, buffer.getShort(100));
        Assertions.assertEquals((short) 0x1234, buffer.getShort(200));
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    @Test
    void testPutGetShortBigEndian() {
        // Network byte order operations
        short value = (short) 0x1234;
        
        buffer.putShortBE(value);
        buffer.position(0);
        
        // Verify bytes are in big-endian order
        Assertions.assertEquals((byte) 0x12, buffer.get());
        Assertions.assertEquals((byte) 0x34, buffer.get());
        
        // Read as big-endian short
        buffer.position(0);
        short result = buffer.getShortBE();
        Assertions.assertEquals(value, result);
    }
    
    // ==================== Int Accessors ====================
    
    @Test
    void testPutGetInt() {
        buffer.putInt(0);
        buffer.putInt(1);
        buffer.putInt(-1);
        buffer.putInt(Integer.MAX_VALUE);
        buffer.putInt(Integer.MIN_VALUE);
        buffer.putInt(0xDEADBEEF);
        
        Assertions.assertEquals(24, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals(0, buffer.getInt());
        Assertions.assertEquals(1, buffer.getInt());
        Assertions.assertEquals(-1, buffer.getInt());
        Assertions.assertEquals(Integer.MAX_VALUE, buffer.getInt());
        Assertions.assertEquals(Integer.MIN_VALUE, buffer.getInt());
        Assertions.assertEquals(0xDEADBEEF, buffer.getInt());
    }
    
    @Test
    void testPutGetIntAbsolute() {
        buffer.putInt(40, 0xCAFEBABE);
        buffer.putInt(80, 0xDEADBEEF);
        buffer.putInt(120, 0x12345678);
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals(0xCAFEBABE, buffer.getInt(40));
        Assertions.assertEquals(0xDEADBEEF, buffer.getInt(80));
        Assertions.assertEquals(0x12345678, buffer.getInt(120));
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    @Test
    void testPutGetIntBigEndian() {
        int value = 0x12345678;
        
        buffer.putIntBE(value);
        buffer.position(0);
        
        // Verify bytes are in big-endian order
        Assertions.assertEquals((byte) 0x12, buffer.get());
        Assertions.assertEquals((byte) 0x34, buffer.get());
        Assertions.assertEquals((byte) 0x56, buffer.get());
        Assertions.assertEquals((byte) 0x78, buffer.get());
        
        buffer.position(0);
        int result = buffer.getIntBE();
        Assertions.assertEquals(value, result);
    }
    
    // ==================== Long Accessors ====================
    
    @Test
    void testPutGetLong() {
        buffer.putLong(0L);
        buffer.putLong(1L);
        buffer.putLong(-1L);
        buffer.putLong(Long.MAX_VALUE);
        buffer.putLong(Long.MIN_VALUE);
        buffer.putLong(0x123456789ABCDEF0L);
        
        Assertions.assertEquals(48, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals(0L, buffer.getLong());
        Assertions.assertEquals(1L, buffer.getLong());
        Assertions.assertEquals(-1L, buffer.getLong());
        Assertions.assertEquals(Long.MAX_VALUE, buffer.getLong());
        Assertions.assertEquals(Long.MIN_VALUE, buffer.getLong());
        Assertions.assertEquals(0x123456789ABCDEF0L, buffer.getLong());
    }
    
    @Test
    void testPutGetLongAbsolute() {
        buffer.putLong(16, 0xFEDCBA9876543210L);
        buffer.putLong(64, 0x0123456789ABCDEFL);
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals(0xFEDCBA9876543210L, buffer.getLong(16));
        Assertions.assertEquals(0x0123456789ABCDEFL, buffer.getLong(64));
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    @Test
    void testPutGetLongBigEndian() {
        long value = 0x123456789ABCDEF0L;
        
        buffer.putLongBE(value);
        buffer.position(0);
        
        // Verify bytes are in big-endian order
        Assertions.assertEquals((byte) 0x12, buffer.get());
        Assertions.assertEquals((byte) 0x34, buffer.get());
        Assertions.assertEquals((byte) 0x56, buffer.get());
        Assertions.assertEquals((byte) 0x78, buffer.get());
        Assertions.assertEquals((byte) 0x9A, buffer.get());
        Assertions.assertEquals((byte) 0xBC, buffer.get());
        Assertions.assertEquals((byte) 0xDE, buffer.get());
        Assertions.assertEquals((byte) 0xF0, buffer.get());
        
        buffer.position(0);
        long result = buffer.getLongBE();
        Assertions.assertEquals(value, result);
    }
    
    // ==================== Float Accessors ====================
    
    @Test
    void testPutGetFloat() {
        buffer.putFloat(0.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(-1.0f);
        buffer.putFloat(Float.MIN_VALUE);
        buffer.putFloat(Float.MAX_VALUE);
        buffer.putFloat((float) Math.PI);
        
        Assertions.assertEquals(24, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals(0.0f, buffer.getFloat());
        Assertions.assertEquals(1.0f, buffer.getFloat());
        Assertions.assertEquals(-1.0f, buffer.getFloat());
        Assertions.assertEquals(Float.MIN_VALUE, buffer.getFloat());
        Assertions.assertEquals(Float.MAX_VALUE, buffer.getFloat());
        Assertions.assertEquals((float) Math.PI, buffer.getFloat(), 0.0001f);
    }
    
    @Test
    void testPutGetFloatAbsolute() {
        buffer.putFloat(32, 3.14159f);
        buffer.putFloat(64, 2.71828f);
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals(3.14159f, buffer.getFloat(32), 0.0001f);
        Assertions.assertEquals(2.71828f, buffer.getFloat(64), 0.0001f);
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    // ==================== Double Accessors ====================
    
    @Test
    void testPutGetDouble() {
        buffer.putDouble(0.0);
        buffer.putDouble(1.0);
        buffer.putDouble(-1.0);
        buffer.putDouble(Double.MIN_VALUE);
        buffer.putDouble(Double.MAX_VALUE);
        buffer.putDouble(Math.PI);
        buffer.putDouble(Math.E);
        
        Assertions.assertEquals(56, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals(0.0, buffer.getDouble());
        Assertions.assertEquals(1.0, buffer.getDouble());
        Assertions.assertEquals(-1.0, buffer.getDouble());
        Assertions.assertEquals(Double.MIN_VALUE, buffer.getDouble());
        Assertions.assertEquals(Double.MAX_VALUE, buffer.getDouble());
        Assertions.assertEquals(Math.PI, buffer.getDouble());
        Assertions.assertEquals(Math.E, buffer.getDouble());
    }
    
    @Test
    void testPutGetDoubleAbsolute() {
        buffer.putDouble(24, Math.PI);
        buffer.putDouble(48, Math.E);
        buffer.putDouble(72, Double.NaN);
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals(Math.PI, buffer.getDouble(24));
        Assertions.assertEquals(Math.E, buffer.getDouble(48));
        Assertions.assertTrue(Double.isNaN(buffer.getDouble(72)));
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    // ==================== Char Accessors ====================
    
    @Test
    void testPutGetChar() {
        buffer.putChar('A');
        buffer.putChar('Z');
        buffer.putChar('0');
        buffer.putChar('9');
        buffer.putChar('\n');
        buffer.putChar('\u00A9'); // Copyright symbol
        buffer.putChar('\u4E2D'); // Chinese character
        
        Assertions.assertEquals(14, buffer.position());
        
        buffer.flip();
        
        Assertions.assertEquals('A', buffer.getChar());
        Assertions.assertEquals('Z', buffer.getChar());
        Assertions.assertEquals('0', buffer.getChar());
        Assertions.assertEquals('9', buffer.getChar());
        Assertions.assertEquals('\n', buffer.getChar());
        Assertions.assertEquals('\u00A9', buffer.getChar());
        Assertions.assertEquals('\u4E2D', buffer.getChar());
    }
    
    @Test
    void testPutGetCharAbsolute() {
        buffer.putChar(50, 'X');
        buffer.putChar(100, 'Y');
        buffer.putChar(150, 'Z');
        
        Assertions.assertEquals(0, buffer.position());
        
        Assertions.assertEquals('X', buffer.getChar(50));
        Assertions.assertEquals('Y', buffer.getChar(100));
        Assertions.assertEquals('Z', buffer.getChar(150));
        
        Assertions.assertEquals(0, buffer.position());
    }
    
    // ==================== Mixed Type Sequences ====================
    
    @Test
    void testPacketHeaderSimulation() {
        // Simulate writing a typical packet header structure
        
        // Ethernet-like header
        buffer.put((byte) 0xFF);    // Dst MAC first byte
        buffer.put((byte) 0xFF);    
        buffer.put((byte) 0xFF);    
        buffer.put((byte) 0xFF);    
        buffer.put((byte) 0xFF);    
        buffer.put((byte) 0xFF);    // Dst MAC last byte
        
        buffer.put((byte) 0x00);    // Src MAC first byte
        buffer.put((byte) 0x11);    
        buffer.put((byte) 0x22);    
        buffer.put((byte) 0x33);    
        buffer.put((byte) 0x44);    
        buffer.put((byte) 0x55);    // Src MAC last byte
        
        buffer.putShort((short) 0x0800); // EtherType (IPv4)
        
        // IP-like header
        buffer.put((byte) 0x45);    // Version + IHL
        buffer.put((byte) 0x00);    // TOS
        buffer.putShort((short) 60); // Total Length
        buffer.putShort((short) 0x1234); // Identification
        buffer.putShort((short) 0x4000); // Flags + Fragment
        buffer.put((byte) 64);      // TTL
        buffer.put((byte) 6);       // Protocol (TCP)
        buffer.putShort((short) 0);  // Checksum (placeholder)
        buffer.putInt(0x0A000001);   // Source IP (10.0.0.1)
        buffer.putInt(0x0A000002);   // Dest IP (10.0.0.2)
        
        Assertions.assertEquals(34, buffer.position());
        
        // Read back and verify key fields
        buffer.flip();
        
        // Skip MAC addresses
        buffer.position(12);
        Assertions.assertEquals((short) 0x0800, buffer.getShort());
        
        // Check IP version
        Assertions.assertEquals((byte) 0x45, buffer.get());
        
        // Skip to TTL
        buffer.position(22);
        Assertions.assertEquals((byte) 64, buffer.get());
        Assertions.assertEquals((byte) 6, buffer.get());
        
        // Check IPs
        buffer.position(26);
        Assertions.assertEquals(0x0A000001, buffer.getInt());
        Assertions.assertEquals(0x0A000002, buffer.getInt());
    }
    
    @Test
    void testAlternatingTypes() {
        // Test alternating between different type sizes
        for (int i = 0; i < 5; i++) {
            buffer.put((byte) i);         // 1 byte
            buffer.putShort((short) (i * 10)); // 2 bytes
            buffer.putInt(i * 100);       // 4 bytes
            buffer.putLong(i * 1000L);    // 8 bytes
            buffer.putFloat(i * 0.1f);    // 4 bytes
            buffer.putDouble(i * 0.01);   // 8 bytes
        }
        
        buffer.flip();
        
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals((byte) i, buffer.get());
            Assertions.assertEquals((short) (i * 10), buffer.getShort());
            Assertions.assertEquals(i * 100, buffer.getInt());
            Assertions.assertEquals(i * 1000L, buffer.getLong());
            Assertions.assertEquals(i * 0.1f, buffer.getFloat(), 0.001f);
            Assertions.assertEquals(i * 0.01, buffer.getDouble(), 0.001);
        }
    }
    
    // ==================== Endianness Verification ====================
    
    @Test
    void testMixedEndianness() {
        // Write same value in different endianness
        int testValue = 0x12345678;
        
        buffer.putInt(testValue);       // Native (likely little-endian)
        buffer.putIntBE(testValue);     // Big-endian
        
        buffer.position(0);
        
        // Read back
        int nativeValue = buffer.getInt();
        int bigEndianValue = buffer.getIntBE();
        
        Assertions.assertEquals(testValue, nativeValue);
        Assertions.assertEquals(testValue, bigEndianValue);
        
        // Verify byte order difference at byte level
        buffer.position(0);
        byte[] nativeBytes = new byte[4];
        buffer.get(nativeBytes);
        
        byte[] beBytes = new byte[4];
        buffer.get(beBytes);
        
        // Big-endian should always be 0x12, 0x34, 0x56, 0x78
        Assertions.assertEquals((byte) 0x12, beBytes[0]);
        Assertions.assertEquals((byte) 0x34, beBytes[1]);
        Assertions.assertEquals((byte) 0x56, beBytes[2]);
        Assertions.assertEquals((byte) 0x78, beBytes[3]);
    }
    
    // ==================== Sequential Access Patterns ====================
    
    @Test
    void testSequentialWrites() {
        // Write sequential data of each type
        for (int i = 0; i < 256; i++) {
            buffer.put((byte) i);
        }
        
        for (int i = 0; i < 50; i++) {
            buffer.putShort((short) i);
        }
        
        for (int i = 0; i < 25; i++) {
            buffer.putInt(i);
        }
        
        buffer.flip();
        
        // Verify sequential reads
        for (int i = 0; i < 256; i++) {
            Assertions.assertEquals((byte) i, buffer.get());
        }
        
        for (int i = 0; i < 50; i++) {
            Assertions.assertEquals((short) i, buffer.getShort());
        }
        
        for (int i = 0; i < 25; i++) {
            Assertions.assertEquals(i, buffer.getInt());
        }
    }
    
    @Test
    void testBulkByteOperations() {
        // Test larger bulk operations
        byte[] largeData = new byte[512];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        buffer.put(largeData);
        
        buffer.flip();
        
        byte[] result = new byte[512];
        buffer.get(result);
        
        Assertions.assertArrayEquals(largeData, result);
    }
    
    // ==================== Position Tracking ====================
    
    @Test
    void testPositionUpdatesCorrectly() {
        Assertions.assertEquals(0, buffer.position());
        
        buffer.put((byte) 1);
        Assertions.assertEquals(1, buffer.position());
        
        buffer.putShort((short) 2);
        Assertions.assertEquals(3, buffer.position());
        
        buffer.putInt(3);
        Assertions.assertEquals(7, buffer.position());
        
        buffer.putLong(4L);
        Assertions.assertEquals(15, buffer.position());
        
        buffer.putFloat(5.0f);
        Assertions.assertEquals(19, buffer.position());
        
        buffer.putDouble(6.0);
        Assertions.assertEquals(27, buffer.position());
        
        buffer.putChar('7');
        Assertions.assertEquals(29, buffer.position());
        
        byte[] array = new byte[10];
        buffer.put(array);
        Assertions.assertEquals(39, buffer.position());
    }
}