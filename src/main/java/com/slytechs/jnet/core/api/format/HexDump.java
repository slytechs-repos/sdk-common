package com.slytechs.jnet.core.api.format;

import java.nio.ByteBuffer;

/**
 * Utility class for generating hex dumps of byte arrays and ByteBuffers in various formats.
 * Provides flexible formatting options with selectable columns.
 */
public final class HexDump {
    
    private HexDump() {
        // Utility class - no instantiation
    }
    
    /**
     * Columns that can be displayed in the hex dump.
     */
    public enum Column {
        /** Offset column showing position in hex (e.g., "0000:") */
        OFFSET,
        /** Hex bytes column showing bytes in hexadecimal format */
        HEX,
        /** ASCII column showing printable characters or dots */
        ASCII
    }
    
    /**
     * Configuration for hex dump formatting.
     */
    public static class Config {
        private final Column[] columns;
        private final int bytesPerLine;
        private final int groupSize;
        private final String offsetSeparator;
        private final String byteSeparator;
        private final String groupSeparator;
        private final String columnSeparator;
        private final char unprintableChar;
        private final boolean upperCaseHex;
        
        private Config(Builder builder) {
            this.columns = builder.columns.clone();
            this.bytesPerLine = builder.bytesPerLine;
            this.groupSize = builder.groupSize;
            this.offsetSeparator = builder.offsetSeparator;
            this.byteSeparator = builder.byteSeparator;
            this.groupSeparator = builder.groupSeparator;
            this.columnSeparator = builder.columnSeparator;
            this.unprintableChar = builder.unprintableChar;
            this.upperCaseHex = builder.upperCaseHex;
        }
        
        public Column[] columns() { return columns.clone(); }
        public int bytesPerLine() { return bytesPerLine; }
        public int groupSize() { return groupSize; }
        public String offsetSeparator() { return offsetSeparator; }
        public String byteSeparator() { return byteSeparator; }
        public String groupSeparator() { return groupSeparator; }
        public String columnSeparator() { return columnSeparator; }
        public char unprintableChar() { return unprintableChar; }
        public boolean upperCaseHex() { return upperCaseHex; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Column[] columns = {Column.OFFSET, Column.HEX, Column.ASCII};
            private int bytesPerLine = 16;
            private int groupSize = 8;
            private String offsetSeparator = ": ";
            private String byteSeparator = " ";
            private String groupSeparator = "  ";
            private String columnSeparator = "   ";
            private char unprintableChar = '.';
            private boolean upperCaseHex = true;
            
            public Builder columns(Column... columns) {
                this.columns = columns.clone();
                return this;
            }
            
            public Builder allColumns() {
                this.columns = new Column[]{Column.OFFSET, Column.HEX, Column.ASCII};
                return this;
            }
            
            public Builder noColumns() {
                this.columns = new Column[0];
                return this;
            }
            
            public Builder bytesPerLine(int bytesPerLine) {
                if (bytesPerLine <= 0) {
                    throw new IllegalArgumentException("Bytes per line must be positive");
                }
                this.bytesPerLine = bytesPerLine;
                return this;
            }
            
            public Builder groupSize(int groupSize) {
                if (groupSize <= 0) {
                    throw new IllegalArgumentException("Group size must be positive");
                }
                this.groupSize = groupSize;
                return this;
            }
            
            public Builder offsetSeparator(String separator) {
                this.offsetSeparator = separator != null ? separator : "";
                return this;
            }
            
            public Builder byteSeparator(String separator) {
                this.byteSeparator = separator != null ? separator : "";
                return this;
            }
            
            public Builder groupSeparator(String separator) {
                this.groupSeparator = separator != null ? separator : "";
                return this;
            }
            
            public Builder columnSeparator(String separator) {
                this.columnSeparator = separator != null ? separator : "";
                return this;
            }
            
            public Builder unprintableChar(char ch) {
                this.unprintableChar = ch;
                return this;
            }
            
            public Builder upperCaseHex(boolean upperCase) {
                this.upperCaseHex = upperCase;
                return this;
            }
            
            public Builder lowerCaseHex() {
                return upperCaseHex(false);
            }
            
            public Config build() {
                return new Config(this);
            }
        }
    }
    
    // Default configurations
    public static final Config DEFAULT = Config.builder().build();
    public static final Config OFFSET_ONLY = Config.builder().columns(Column.OFFSET).build();
    public static final Config HEX_ONLY = Config.builder().columns(Column.HEX).build();
    public static final Config ASCII_ONLY = Config.builder().columns(Column.ASCII).build();
    public static final Config NO_OFFSET = Config.builder().columns(Column.HEX, Column.ASCII).build();
    public static final Config NO_ASCII = Config.builder().columns(Column.OFFSET, Column.HEX).build();
    public static final Config COMPACT = Config.builder().byteSeparator("").groupSeparator(" ").build();
    
    /**
     * Dumps a byte array using the default configuration.
     */
    public static String dump(byte[] data) {
        return dump(data, DEFAULT);
    }
    
    /**
     * Dumps a byte array with the specified columns enabled.
     */
    public static String dump(byte[] data, Column... columns) {
        Config config = Config.builder().columns(columns).build();
        return dump(data, config);
    }
    
    /**
     * Dumps a byte array using the specified configuration.
     */
    public static String dump(byte[] data, Config config) {
        if (data == null) {
            return "";
        }
        return dump(data, 0, data.length, config);
    }
    
    /**
     * Dumps a portion of a byte array using the default configuration.
     */
    public static String dump(byte[] data, int offset, int length) {
        return dump(data, offset, length, DEFAULT);
    }
    
    // ByteBuffer overloads
    
    /**
     * Dumps a ByteBuffer between position and limit using the default configuration.
     * Does not modify the buffer's position.
     */
    public static String dump(ByteBuffer buffer) {
        return dump(buffer, DEFAULT);
    }
    
    /**
     * Dumps a ByteBuffer between position and limit with the specified columns enabled.
     * Does not modify the buffer's position.
     */
    public static String dump(ByteBuffer buffer, Column... columns) {
        Config config = Config.builder().columns(columns).build();
        return dump(buffer, config);
    }
    
    /**
     * Dumps a ByteBuffer between position and limit using the specified configuration.
     * Does not modify the buffer's position.
     */
    public static String dump(ByteBuffer buffer, Config config) {
        if (buffer == null) {
            return "";
        }
        
        int remaining = buffer.remaining();
        if (remaining == 0) {
            return "";
        }
        
        // Extract bytes without modifying buffer position
        byte[] data = new byte[remaining];
        int originalPosition = buffer.position();
        try {
            buffer.get(data);
            return dump(data, 0, remaining, config);
        } finally {
            buffer.position(originalPosition); // Restore original position
        }
    }
    
    /**
     * Dumps a portion of a ByteBuffer using the default configuration.
     * The offset is relative to the buffer's current position.
     * Does not modify the buffer's position.
     */
    public static String dump(ByteBuffer buffer, int offset, int length) {
        return dump(buffer, offset, length, DEFAULT);
    }
    
    /**
     * Dumps a portion of a ByteBuffer using the specified configuration.
     * The offset is relative to the buffer's current position.
     * Does not modify the buffer's position.
     */
    public static String dump(ByteBuffer buffer, int offset, int length, Config config) {
        if (buffer == null) {
            return "";
        }
        
        int bufferRemaining = buffer.remaining();
        if (offset < 0 || length < 0 || offset + length > bufferRemaining) {
            throw new IndexOutOfBoundsException("Invalid offset/length for buffer");
        }
        
        if (length == 0) {
            return "";
        }
        
        // Extract bytes without modifying buffer position
        byte[] data = new byte[length];
        int originalPosition = buffer.position();
        try {
            buffer.position(originalPosition + offset);
            buffer.get(data);
            return dump(data, 0, length, config);
        } finally {
            buffer.position(originalPosition); // Restore original position
        }
    }
    
    /**
     * Dumps the entire ByteBuffer (from 0 to capacity) using the default configuration.
     * Does not modify the buffer's position.
     */
    public static String dumpAll(ByteBuffer buffer) {
        return dumpAll(buffer, DEFAULT);
    }
    
    /**
     * Dumps the entire ByteBuffer (from 0 to capacity) using the specified configuration.
     * Does not modify the buffer's position.
     */
    public static String dumpAll(ByteBuffer buffer, Config config) {
        if (buffer == null) {
            return "";
        }
        
        int capacity = buffer.capacity();
        if (capacity == 0) {
            return "";
        }
        
        // Extract all bytes without modifying buffer position
        byte[] data = new byte[capacity];
        int originalPosition = buffer.position();
        int originalLimit = buffer.limit();
        try {
            buffer.clear(); // Set position=0, limit=capacity
            buffer.get(data);
            return dump(data, 0, capacity, config);
        } finally {
            buffer.limit(originalLimit);   // Restore original limit
            buffer.position(originalPosition); // Restore original position
        }
    }
    
    /**
     * Dumps a portion of a byte array using the specified configuration.
     */
    public static String dump(byte[] data, int offset, int length, Config config) {
        if (data == null) {
            return "";
        }
        
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }
        
        if (length == 0 || config.columns().length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String hexFormat = config.upperCaseHex() ? "%02X" : "%02x";
        String offsetFormat = getOffsetFormat(offset + length - 1);
        
        for (int i = 0; i < length; i += config.bytesPerLine()) {
            if (i > 0) {
                sb.append('\n');
            }
            
            int lineStart = offset + i;
            int lineEnd = Math.min(lineStart + config.bytesPerLine(), offset + length);
            int lineLength = lineEnd - lineStart;
            
            // Process columns in the order specified, including duplicates
            Column[] columns = config.columns();
            for (int colIndex = 0; colIndex < columns.length; colIndex++) {
                if (colIndex > 0) {
                    sb.append(config.columnSeparator());
                }
                
                Column column = columns[colIndex];
                switch (column) {
                    case OFFSET:
                        sb.append(String.format(offsetFormat, lineStart));
                        sb.append(config.offsetSeparator());
                        break;
                        
                    case HEX:
                        appendHexLine(sb, data, lineStart, lineLength, config, hexFormat);
                        break;
                        
                    case ASCII:
                        appendAsciiLine(sb, data, lineStart, lineLength, config);
                        break;
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Determines the format string for offset display based on the maximum offset.
     */
    private static String getOffsetFormat(int maxOffset) {
        if (maxOffset <= 0xFFFF) {
            return "%04X";
        } else if (maxOffset <= 0xFFFFFF) {
            return "%06X";
        } else {
            return "%08X";
        }
    }
    
    /**
     * Appends the hex representation of a line to the string builder.
     */
    private static void appendHexLine(StringBuilder sb, byte[] data, int start, int length, 
                                     Config config, String hexFormat) {
        for (int i = 0; i < config.bytesPerLine(); i++) {
            if (i > 0) {
                // Add group separator between groups
                if (i % config.groupSize() == 0) {
                    sb.append(config.groupSeparator());
                } else {
                    sb.append(config.byteSeparator());
                }
            }
            
            if (i < length) {
                sb.append(String.format(hexFormat, data[start + i] & 0xFF));
            } else {
                // Pad with spaces to maintain alignment
                sb.append("  ");
            }
        }
    }
    
    /**
     * Appends the ASCII representation of a line to the string builder.
     */
    private static void appendAsciiLine(StringBuilder sb, byte[] data, int start, int length, Config config) {
        for (int i = 0; i < length; i++) {
            char ch = (char) (data[start + i] & 0xFF);
            if (isPrintable(ch)) {
                sb.append(ch);
            } else {
                sb.append(config.unprintableChar());
            }
        }
        
        // Pad with spaces to maintain consistent width for partial lines
        for (int i = length; i < config.bytesPerLine(); i++) {
            sb.append(' ');
        }
    }
    
    /**
     * Determines if a character is printable.
     */
    private static boolean isPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }
    
    /**
     * Convenience methods for common use cases with byte arrays.
     */
    public static String dumpWithOffset(byte[] data) {
        return dump(data, Column.OFFSET, Column.HEX);
    }
    
    public static String dumpHexOnly(byte[] data) {
        return dump(data, Column.HEX);
    }
    
    public static String dumpAsciiOnly(byte[] data) {
        return dump(data, Column.ASCII);
    }
    
    public static String dumpNoOffset(byte[] data) {
        return dump(data, Column.HEX, Column.ASCII);
    }
    
    public static String dumpCompact(byte[] data) {
        return dump(data, COMPACT);
    }
    
    /**
     * Convenience methods for common use cases with ByteBuffers.
     */
    public static String dumpWithOffset(ByteBuffer buffer) {
        return dump(buffer, Column.OFFSET, Column.HEX);
    }
    
    public static String dumpHexOnly(ByteBuffer buffer) {
        return dump(buffer, Column.HEX);
    }
    
    public static String dumpAsciiOnly(ByteBuffer buffer) {
        return dump(buffer, Column.ASCII);
    }
    
    public static String dumpNoOffset(ByteBuffer buffer) {
        return dump(buffer, Column.HEX, Column.ASCII);
    }
    
    public static String dumpCompact(ByteBuffer buffer) {
        return dump(buffer, COMPACT);
    }
    
    /**
     * Creates a single line hex dump (no line breaks, no offset) for byte arrays.
     */
    public static String dumpSingleLine(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        
        Config config = Config.builder()
            .columns(Column.HEX)
            .bytesPerLine(data.length)
            .groupSize(data.length) // No grouping
            .build();
        
        return dump(data, config);
    }
    
    /**
     * Creates a single line hex dump (no line breaks, no offset) for ByteBuffers.
     */
    public static String dumpSingleLine(ByteBuffer buffer) {
        if (buffer == null) {
            return "";
        }
        
        int remaining = buffer.remaining();
        if (remaining == 0) {
            return "";
        }
        
        Config config = Config.builder()
            .columns(Column.HEX)
            .bytesPerLine(remaining)
            .groupSize(remaining) // No grouping
            .build();
        
        return dump(buffer, config);
    }
    
    /**
     * Creates a C-style array representation for byte arrays.
     */
    public static String dumpCStyle(byte[] data) {
        if (data == null || data.length == 0) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("0x%02X", data[i] & 0xFF));
        }
        sb.append(" }");
        
        return sb.toString();
    }
    
    /**
     * Creates a C-style array representation for ByteBuffers.
     */
    public static String dumpCStyle(ByteBuffer buffer) {
        if (buffer == null) {
            return "{}";
        }
        
        int remaining = buffer.remaining();
        if (remaining == 0) {
            return "{}";
        }
        
        // Extract bytes without modifying buffer position
        byte[] data = new byte[remaining];
        int originalPosition = buffer.position();
        try {
            buffer.get(data);
            return dumpCStyle(data);
        } finally {
            buffer.position(originalPosition); // Restore original position
        }
    }
}