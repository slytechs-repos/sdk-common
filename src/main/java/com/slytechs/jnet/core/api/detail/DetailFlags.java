package com.slytechs.jnet.core.api.detail;
/**
 * Processing flags for headers
 */
public final class DetailFlags {
    public static final int REASSEMBLED    = 1 << 0;
    public static final int DECRYPTED      = 1 << 1;
    public static final int DECOMPRESSED   = 1 << 2;
    public static final int DECAPSULATED   = 1 << 3;
    public static final int TRUNCATED      = 1 << 4;
    public static final int MALFORMED      = 1 << 5;
    public static final int OUT_OF_ORDER   = 1 << 6;
    public static final int RETRANSMISSION = 1 << 7;
    
    private DetailFlags() {}
}