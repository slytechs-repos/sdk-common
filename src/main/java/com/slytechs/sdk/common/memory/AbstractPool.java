package com.slytechs.sdk.common.memory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// AbstractPool.java - fixed version
abstract class AbstractPool<T> {
    private final AtomicReference<Object> head = new AtomicReference<>(); // Use Object to avoid cast issues
    private final AtomicLong available = new AtomicLong(0);
    protected final PoolMetrics metrics;
    protected final String name;
    protected final long capacity;
    
    protected AbstractPool(String name, long capacity) {
        this.name = name;
        this.capacity = capacity;
        this.metrics = new PoolMetrics(name, capacity);
    }
    
    // Subclasses implement these to access poolNext field
    protected abstract Object getPoolNext(T item);
    protected abstract void setPoolNext(T item, Object next);
    protected abstract void prepareForAllocation(T item);
    protected abstract void prepareForRelease(T item);
    
    @SuppressWarnings("unchecked")
    protected T allocateFromFreeList() {
        Object current;
        Object next;
        
        do {
            current = head.get();
            if (current == null) {
                return null;
            }
            next = getPoolNext((T) current);
        } while (!head.compareAndSet(current, next));
        
        T item = (T) current;
        setPoolNext(item, null);
        prepareForAllocation(item);
        available.decrementAndGet();
        
        return item;
    }
    
    protected void addToFreeList(T item) {
        prepareForRelease(item);
        
        Object current;
        do {
            current = head.get();
            setPoolNext(item, current);
        } while (!head.compareAndSet(current, item));
        
        available.incrementAndGet();
    }
    
    public long available() {
        return available.get();
    }
    
    public long capacity() {
        return capacity;
    }
    
    public PoolMetrics getMetrics() {
        return metrics;
    }
}