/*
 * Sly Technologies Free License
 */
package com.slytechs.sdk.common.memory;

/**
 * Interface for reference counting operations.
 * 
 * <p>
 * Provides a unified API for reference counting that can be implemented
 * by both Memory objects and views that delegate to them.
 * </p>
 */
public interface MemoryRefCounter {
    
    /**
     * Increments the reference count.
     * 
     * @return the new reference count
     */
    int incrementRef();
    
    /**
     * Decrements the reference count.
     * 
     * @return the new reference count
     */
    int decrementRef();
    
    /**
     * Returns the current reference count.
     * 
     * @return the reference count
     */
    int refCount();
}