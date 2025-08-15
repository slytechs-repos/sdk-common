package com.slytechs.jnet.core.api.memory;

/**
 * Interface for memory objects that can be managed by a MemoryPool.
 * 
 * <p>
 * This marker interface identifies memory objects that support pool management,
 * enabling automatic return to their originating pool when reference counts
 * reach zero. Implementations must maintain a reference to their owning pool to
 * enable proper pool integration.
 * </p>
 * 
 * <h2>Implementation Requirements</h2>
 * <ul>
 * <li>Store reference to owning pool during construction</li>
 * <li>Return correct pool reference from {@link #getOwningPool()}</li>
 * <li>Support pool-managed lifecycle through reference counting</li>
 * </ul>
 * 
 * @see MemoryPool for pool management
 * @see MemoryBuffer for example implementation
 */
public interface MemoryPoolable {
    /**
     * Returns the MemoryPool that owns this memory object.
     * 
     * <p>
     * This method enables the memory object to identify its originating pool, which
     * is essential for automatic return when reference counting reaches zero. The
     * returned pool must be the same instance that created this memory object.
     * </p>
     * 
     * @return the owning MemoryPool, or {@code null} if this memory is not pooled
     */
    MemoryPool<?> getOwningPool();
}