/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.jnet.core.api.format;

import java.nio.ByteBuffer;

/**
 * Utility class for generating hex dumps of byte arrays and ByteBuffers in various formats.
 * Provides flexible formatting options with selectable columns.
 */
public final class HexDump {
    
    /**
	 * Instantiates a new hex dump.
	 */
    private HexDump() {
        // Utility class - no instantiation
    }
    
    /**
     * Columns that can be displayed in the hex dump.
     */
    public enum Column {
        /** Offset column showing position in hex (e.g., "0000:") */
        OFFSET,
        
        /** Hex bytes column showing bytes in hexadecimal format. */
        HEX,
        
        /** ASCII column showing printable characters or dots. */
        ASCII
    }
    
    /**
     * Configuration for hex dump formatting.
     */
    public static class Config {
        
        /** The columns. */
        private final Column[] columns;
        
        /** The bytes per line. */
        private final int bytesPerLine;
        
        /** The group size. */
        private final int groupSize;
        
        /** The offset separator. */
        private final String offsetSeparator;
        
        /** The byte separator. */
        private final String byteSeparator;
        
        /** The group separator. */
        private final String groupSeparator;
        
        /** The column separator. */
        private final String columnSeparator;
        
        /** The unprintable char. */
        private final char unprintableChar;
        
        /** The upper case hex. */
        private final boolean upperCaseHex;
        
        /**
		 * Instantiates a new config.
		 *
		 * @param builder the builder
		 */
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
        
        /**
		 * Columns.
		 *
		 * @return the column[]
		 */
        public Column[] columns() { return columns.clone(); }
        
        /**
		 * Bytes per line.
		 *
		 * @return the int
		 */
        public int bytesPerLine() { return bytesPerLine; }
        
        /**
		 * Group size.
		 *
		 * @return the int
		 */
        public int groupSize() { return groupSize; }
        
        /**
		 * Offset separator.
		 *
		 * @return the string
		 */
        public String offsetSeparator() { return offsetSeparator; }
        
        /**
		 * Byte separator.
		 *
		 * @return the string
		 */
        public String byteSeparator() { return byteSeparator; }
        
        /**
		 * Group separator.
		 *
		 * @return the string
		 */
        public String groupSeparator() { return groupSeparator; }
        
        /**
		 * Column separator.
		 *
		 * @return the string
		 */
        public String columnSeparator() { return columnSeparator; }
        
        /**
		 * Unprintable char.
		 *
		 * @return the char
		 */
        public char unprintableChar() { return unprintableChar; }
        
        /**
		 * Upper case hex.
		 *
		 * @return true, if successful
		 */
        public boolean upperCaseHex() { return upperCaseHex; }
        
        /**
		 * Builder.
		 *
		 * @return the builder
		 */
        public static Builder builder() {
            return new Builder();
        }
        
        /**
		 * The Class Builder.
		 */
        public static class Builder {
            
            /** The columns. */
            private Column[] columns = {Column.OFFSET, Column.HEX, Column.ASCII};
            
            /** The bytes per line. */
            private int bytesPerLine = 16;
            
            /** The group size. */
            private int groupSize = 8;
            
            /** The offset separator. */
            private String offsetSeparator = ": ";
            
            /** The byte separator. */
            private String byteSeparator = " ";
            
            /** The group separator. */
            private String groupSeparator = "  ";
            
            /** The column separator. */
            private String columnSeparator = "   ";
            
            /** The unprintable char. */
            private char unprintableChar = '.';
            
            /** The upper case hex. */
            private boolean upperCaseHex = true;
            
            /**
			 * Columns.
			 *
			 * @param columns the columns
			 * @return the builder
			 */
            public Builder columns(Column... columns) {
                this.columns = columns.clone();
                return this;
            }
            
            /**
			 * All columns.
			 *
			 * @return the builder
			 */
            public Builder allColumns() {
                this.columns = new Column[]{Column.OFFSET, Column.HEX, Column.ASCII};
                return this;
            }
            
            /**
			 * No columns.
			 *
			 * @return the builder
			 */
            public Builder noColumns() {
                this.columns = new Column[0];
                return this;
            }
            
            /**
			 * Bytes per line.
			 *
			 * @param bytesPerLine the bytes per line
			 * @return the builder
			 */
            public Builder bytesPerLine(int bytesPerLine) {
                if (bytesPerLine <= 0) {
                    throw new IllegalArgumentException("Bytes per line must be positive");
                }
                this.bytesPerLine = bytesPerLine;
                return this;
            }
            
            /**
			 * Group size.
			 *
			 * @param groupSize the group size
			 * @return the builder
			 */
            public Builder groupSize(int groupSize) {
                if (groupSize <= 0) {
                    throw new IllegalArgumentException("Group size must be positive");
                }
                this.groupSize = groupSize;
                return this;
            }
            
            /**
			 * Offset separator.
			 *
			 * @param separator the separator
			 * @return the builder
			 */
            public Builder offsetSeparator(String separator) {
                this.offsetSeparator = separator != null ? separator : "";
                return this;
            }
            
            /**
			 * Byte separator.
			 *
			 * @param separator the separator
			 * @return the builder
			 */
            public Builder byteSeparator(String separator) {
                this.byteSeparator = separator != null ? separator : "";
                return this;
            }
            
            /**
			 * Group separator.
			 *
			 * @param separator the separator
			 * @return the builder
			 */
            public Builder groupSeparator(String separator) {
                this.groupSeparator = separator != null ? separator : "";
                return this;
            }
            
            /**
			 * Column separator.
			 *
			 * @param separator the separator
			 * @return the builder
			 */
            public Builder columnSeparator(String separator) {
                this.columnSeparator = separator != null ? separator : "";
                return this;
            }
            
            /**
			 * Unprintable char.
			 *
			 * @param ch the ch
			 * @return the builder
			 */
            public Builder unprintableChar(char ch) {
                this.unprintableChar = ch;
                return this;
            }
            
            /**
			 * Upper case hex.
			 *
			 * @param upperCase the upper case
			 * @return the builder
			 */
            public Builder upperCaseHex(boolean upperCase) {
                this.upperCaseHex = upperCase;
                return this;
            }
            
            /**
			 * Lower case hex.
			 *
			 * @return the builder
			 */
            public Builder lowerCaseHex() {
                return upperCaseHex(false);
            }
            
            /**
			 * Builds the.
			 *
			 * @return the config
			 */
            public Config build() {
                return new Config(this);
            }
        }
    }
    
    /** The Constant DEFAULT. */
    // Default configurations
    public static final Config DEFAULT = Config.builder().build();
    
    /** The Constant OFFSET_ONLY. */
    public static final Config OFFSET_ONLY = Config.builder().columns(Column.OFFSET).build();
    
    /** The Constant HEX_ONLY. */
    public static final Config HEX_ONLY = Config.builder().columns(Column.HEX).build();
    
    /** The Constant ASCII_ONLY. */
    public static final Config ASCII_ONLY = Config.builder().columns(Column.ASCII).build();
    
    /** The Constant NO_OFFSET. */
    public static final Config NO_OFFSET = Config.builder().columns(Column.HEX, Column.ASCII).build();
    
    /** The Constant NO_ASCII. */
    public static final Config NO_ASCII = Config.builder().columns(Column.OFFSET, Column.HEX).build();
    
    /** The Constant COMPACT. */
    public static final Config COMPACT = Config.builder().byteSeparator("").groupSeparator(" ").build();
    
    /**
	 * Dumps a byte array using the default configuration.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dump(byte[] data) {
        return dump(data, DEFAULT);
    }
    
    /**
	 * Dumps a byte array with the specified columns enabled.
	 *
	 * @param data    the data
	 * @param columns the columns
	 * @return the string
	 */
    public static String dump(byte[] data, Column... columns) {
        Config config = Config.builder().columns(columns).build();
        return dump(data, config);
    }
    
    /**
	 * Dumps a byte array using the specified configuration.
	 *
	 * @param data   the data
	 * @param config the config
	 * @return the string
	 */
    public static String dump(byte[] data, Config config) {
        if (data == null) {
            return "";
        }
        return dump(data, 0, data.length, config);
    }
    
    /**
	 * Dumps a portion of a byte array using the default configuration.
	 *
	 * @param data   the data
	 * @param offset the offset
	 * @param length the length
	 * @return the string
	 */
    public static String dump(byte[] data, int offset, int length) {
        return dump(data, offset, length, DEFAULT);
    }
    
    // ByteBuffer overloads
    
    /**
	 * Dumps a ByteBuffer between position and limit using the default
	 * configuration. Does not modify the buffer's position.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dump(ByteBuffer buffer) {
        return dump(buffer, DEFAULT);
    }
    
    /**
	 * Dumps a ByteBuffer between position and limit with the specified columns
	 * enabled. Does not modify the buffer's position.
	 *
	 * @param buffer  the buffer
	 * @param columns the columns
	 * @return the string
	 */
    public static String dump(ByteBuffer buffer, Column... columns) {
        Config config = Config.builder().columns(columns).build();
        return dump(buffer, config);
    }
    
    /**
	 * Dumps a ByteBuffer between position and limit using the specified
	 * configuration. Does not modify the buffer's position.
	 *
	 * @param buffer the buffer
	 * @param config the config
	 * @return the string
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
	 * Dumps a portion of a ByteBuffer using the default configuration. The offset
	 * is relative to the buffer's current position. Does not modify the buffer's
	 * position.
	 *
	 * @param buffer the buffer
	 * @param offset the offset
	 * @param length the length
	 * @return the string
	 */
    public static String dump(ByteBuffer buffer, int offset, int length) {
        return dump(buffer, offset, length, DEFAULT);
    }
    
    /**
	 * Dumps a portion of a ByteBuffer using the specified configuration. The offset
	 * is relative to the buffer's current position. Does not modify the buffer's
	 * position.
	 *
	 * @param buffer the buffer
	 * @param offset the offset
	 * @param length the length
	 * @param config the config
	 * @return the string
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
	 * Dumps the entire ByteBuffer (from 0 to capacity) using the default
	 * configuration. Does not modify the buffer's position.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpAll(ByteBuffer buffer) {
        return dumpAll(buffer, DEFAULT);
    }
    
    /**
	 * Dumps the entire ByteBuffer (from 0 to capacity) using the specified
	 * configuration. Does not modify the buffer's position.
	 *
	 * @param buffer the buffer
	 * @param config the config
	 * @return the string
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
	 *
	 * @param data   the data
	 * @param offset the offset
	 * @param length the length
	 * @param config the config
	 * @return the string
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
	 *
	 * @param maxOffset the max offset
	 * @return the offset format
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
	 *
	 * @param sb        the sb
	 * @param data      the data
	 * @param start     the start
	 * @param length    the length
	 * @param config    the config
	 * @param hexFormat the hex format
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
	 *
	 * @param sb     the sb
	 * @param data   the data
	 * @param start  the start
	 * @param length the length
	 * @param config the config
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
	 *
	 * @param ch the ch
	 * @return true, if is printable
	 */
    private static boolean isPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }
    
    /**
	 * Convenience methods for common use cases with byte arrays.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dumpWithOffset(byte[] data) {
        return dump(data, Column.OFFSET, Column.HEX);
    }
    
    /**
	 * Dump hex only.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dumpHexOnly(byte[] data) {
        return dump(data, Column.HEX);
    }
    
    /**
	 * Dump ascii only.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dumpAsciiOnly(byte[] data) {
        return dump(data, Column.ASCII);
    }
    
    /**
	 * Dump no offset.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dumpNoOffset(byte[] data) {
        return dump(data, Column.HEX, Column.ASCII);
    }
    
    /**
	 * Dump compact.
	 *
	 * @param data the data
	 * @return the string
	 */
    public static String dumpCompact(byte[] data) {
        return dump(data, COMPACT);
    }
    
    /**
	 * Convenience methods for common use cases with ByteBuffers.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpWithOffset(ByteBuffer buffer) {
        return dump(buffer, Column.OFFSET, Column.HEX);
    }
    
    /**
	 * Dump hex only.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpHexOnly(ByteBuffer buffer) {
        return dump(buffer, Column.HEX);
    }
    
    /**
	 * Dump ascii only.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpAsciiOnly(ByteBuffer buffer) {
        return dump(buffer, Column.ASCII);
    }
    
    /**
	 * Dump no offset.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpNoOffset(ByteBuffer buffer) {
        return dump(buffer, Column.HEX, Column.ASCII);
    }
    
    /**
	 * Dump compact.
	 *
	 * @param buffer the buffer
	 * @return the string
	 */
    public static String dumpCompact(ByteBuffer buffer) {
        return dump(buffer, COMPACT);
    }
    
    /**
	 * Creates a single line hex dump (no line breaks, no offset) for byte arrays.
	 *
	 * @param data the data
	 * @return the string
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
	 *
	 * @param buffer the buffer
	 * @return the string
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
	 *
	 * @param data the data
	 * @return the string
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
	 *
	 * @param buffer the buffer
	 * @return the string
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

	/**
	 * @param sb
	 * @param data
	 * @param offset
	 * @param length
	 * @param dataIndent
	 */
	public static void render(StringBuilder sb, byte[] data, int offset, int length, String dataIndent) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}