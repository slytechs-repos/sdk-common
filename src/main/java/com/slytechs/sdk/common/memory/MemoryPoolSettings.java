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
package com.slytechs.sdk.common.memory;

import com.slytechs.sdk.common.settings.BooleanProperty;
import com.slytechs.sdk.common.settings.IntProperty;
import com.slytechs.sdk.common.settings.LongProperty;
import com.slytechs.sdk.common.settings.Settings;

/**
 * Configuration settings for memory pools.
 * 
 * <p>
 * MemoryPoolSettings provides configuration for pre-allocated memory pools.
 * Memory pools avoid runtime allocation overhead by pre-allocating buffers
 * at startup.
 * </p>
 * 
 * <h2>Configuration Options</h2>
 * <ul>
 * <li><b>size</b> - Total pool size in bytes (default: 256MB)</li>
 * <li><b>buffer.count</b> - Number of buffers to pre-allocate (default: 1024)</li>
 * <li><b>direct</b> - Use direct (off-heap) memory (default: true)</li>
 * </ul>
 * 
 * <h2>External Configuration</h2>
 * <pre>
 * # System property
 * -Dmemory.pool.size=536870912
 * 
 * # Environment variable
 * MEMORY_POOL_SIZE=536870912
 * </pre>
 * 
 * <h2>Inheritance</h2>
 * <p>
 * This class is designed for inheritance. Subclasses can provide a different
 * domain and base name to create distinct configuration namespaces while
 * inheriting common memory pool properties.
 * </p>
 * 
 * <pre>{@code
 * public class PacketMemoryPoolSettings extends MemoryPoolSettings {
 *     
 *     public PacketMemoryPoolSettings() {
 *         super("capture", "packet.memory.pool");  // Different domain and baseName
 *     }
 *     
 *     // Additional properties specific to packet pools...
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings
 */
public class MemoryPoolSettings extends Settings {

    /** Default pool size: 256 MB. */
    private static final long DEFAULT_SIZE = 256L * 1024 * 1024;

    /** Default buffer count. */
    private static final int DEFAULT_BUFFER_COUNT = 1024;

    /** Total pool size in bytes. */
    private final LongProperty size;

    /** Number of buffers to pre-allocate. */
    private final IntProperty bufferCount;

    /** Whether to use direct (off-heap) memory. */
    private final BooleanProperty direct;

    /**
     * Constructs memory pool settings with the default base name "memory.pool"
     * and no domain (standalone).
     */
    public MemoryPoolSettings() {
        this(null, "memory.pool");
    }

    /**
     * Constructs memory pool settings with the specified domain and base name.
     * 
     * <p>
     * This constructor is intended for subclasses that need a different
     * configuration namespace.
     * </p>
     *
     * @param domain   the domain for grouped save/load, or null for standalone
     * @param baseName the base name for property resolution
     */
    protected MemoryPoolSettings(String domain, String baseName) {
        super(domain, baseName);
        setComment("Memory pool configuration");
        
        this.size = longProperty("size", DEFAULT_SIZE)
            .comment("Total pool size in bytes");
        this.bufferCount = intProperty("buffer.count", DEFAULT_BUFFER_COUNT)
            .comment("Number of buffers to pre-allocate");
        this.direct = booleanProperty("direct", true)
            .comment("Use direct (off-heap) memory");
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Returns the total pool size in bytes.
     *
     * @return pool size in bytes
     */
    public long size() {
        return size.getLong();
    }

    /**
     * Returns the number of buffers to pre-allocate.
     *
     * @return buffer count
     */
    public int bufferCount() {
        return bufferCount.getInt();
    }

    /**
     * Returns whether to use direct (off-heap) memory.
     *
     * @return true for direct memory, false for heap memory
     */
    public boolean isDirect() {
        return direct.getBoolean();
    }

    /**
     * Calculates the size of each buffer based on total size and count.
     *
     * @return buffer size in bytes
     */
    public long bufferSize() {
        return size() / bufferCount();
    }

    // =========================================================================
    // Fluent Setters
    // =========================================================================

    /**
     * Sets the total pool size in bytes.
     *
     * @param bytes pool size in bytes
     * @return this settings instance for chaining
     */
    public MemoryPoolSettings withSize(long bytes) {
        size.setLong(bytes);
        return this;
    }

    /**
     * Sets the total pool size using a memory unit.
     *
     * @param value the size value
     * @param unit  the memory unit
     * @return this settings instance for chaining
     */
    public MemoryPoolSettings withSize(long value, MemoryUnit unit) {
        size.setLong(unit.toBytes(value));
        return this;
    }

    /**
     * Sets the number of buffers to pre-allocate.
     *
     * @param count buffer count
     * @return this settings instance for chaining
     */
    public MemoryPoolSettings withBufferCount(int count) {
        bufferCount.setInt(count);
        return this;
    }

    /**
     * Sets whether to use direct (off-heap) memory.
     *
     * @param direct true for direct memory, false for heap
     * @return this settings instance for chaining
     */
    public MemoryPoolSettings withDirect(boolean direct) {
        this.direct.setBoolean(direct);
        return this;
    }
}