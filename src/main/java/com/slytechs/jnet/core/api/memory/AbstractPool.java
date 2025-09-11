package com.slytechs.jnet.core.api.memory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// AbstractPool.java - fixed version
abstract class AbstractPool<T> {
    private final AtomicReference<Object> head = new AtomicReference<>(); // Use Object to avoid cast issues
    private final AtomicInteger available = new AtomicInteger(0);
    protected final PoolMetrics metrics;
    protected final String name;
    protected final int capacity;
    
    protected AbstractPool(String name, int capacity) {
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
    
    public int available() {
        return available.get();
    }
    
    public int capacity() {
        return capacity;
    }
    
    public PoolMetrics getMetrics() {
        return metrics;
    }
}