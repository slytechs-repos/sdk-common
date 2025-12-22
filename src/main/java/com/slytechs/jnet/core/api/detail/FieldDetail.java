package com.slytechs.jnet.core.api.detail;

import java.util.List;

/**
 * Field node - single protocol field
 */
public record FieldDetail(
    String name,                    // "Source Port"
    Object value,                   // Raw value (Integer, Long, byte[], InetAddress, etc.)
    String display,                 // Formatted display string
    long bitOffset,                  // Bit offset within header (-1 if N/A)
    long bitLength,                  // Bit length (-1 if N/A)
    List<DetailNode> children       // Sub-fields (for expandable fields like flags)
) implements DetailNode {
    
    // Convenience constructors
    public FieldDetail(String name, Object value, String display) {
        this(name, value, display, -1, -1, List.of());
    }
    
    public FieldDetail(String name, Object value, String display, long bitOffset, long bitLength) {
        this(name, value, display, bitOffset, bitLength, List.of());
    }
    
    /** Byte offset (derived from bit offset) */
    public long byteOffset() {
        return bitOffset >= 0 ? bitOffset / 8 : -1;
    }
    
    /** Byte length (derived from bit length, rounded up) */
    public long byteLength() {
        return bitLength >= 0 ? (bitLength + 7) / 8 : -1;
    }
}

