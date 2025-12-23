package com.slytechs.sdk.common.detail;

import java.util.List;

/**
 * Raw data node - hex dump
 */
public record DataDetail(
    String name,                    // "Payload", "Options Data", etc.
    byte[] data,                    // The actual bytes
    long offset,                     // Offset in packet
    long length                      // Length to display
) implements DetailNode {
    
    @Override
    public List<DetailNode> children() { return List.of(); }
}

