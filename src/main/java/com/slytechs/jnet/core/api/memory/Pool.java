package com.slytechs.jnet.core.api.memory;
/**
 * Common interface for all pool types.
 */
public interface Pool<T> {
    /**
     * Allocates an item from the pool.
     * 
     * @return allocated item or null if exhausted
     */
    T allocate();
    
    /**
     * Releases an item back to the pool.
     * 
     * @param item the item to release
     */
    void release(T item);
    
    /**
     * Returns the pool capacity.
     * 
     * @return maximum capacity
     */
    int capacity();
    
    /**
     * Returns the number of available items.
     * 
     * @return available count
     */
    int available();
    
    /**
     * Returns the pool metrics.
     * 
     * @return metrics object
     */
    PoolMetrics getMetrics();
}