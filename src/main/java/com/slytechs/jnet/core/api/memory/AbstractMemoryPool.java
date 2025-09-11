package com.slytechs.jnet.core.api.memory;
// AbstractMemoryPool.java - extends AbstractPool
public abstract class AbstractMemoryPool<T extends Memory> extends AbstractPool<T> implements MemoryPool<T> {
    protected final long defaultHeadroom;
    
    protected AbstractMemoryPool(String name, int capacity, long defaultHeadroom) {
        super(name, capacity);
        this.defaultHeadroom = defaultHeadroom;
    }
    
    @Override
    protected Object getPoolNext(T item) {
        return ((AbstractMemory) item).poolNext;
    }
    
    @Override
    protected void setPoolNext(T item, Object next) {
        ((AbstractMemory) item).poolNext = (Memory) next;
    }
    
    @Override
    protected void prepareForAllocation(T item) {
        AbstractMemory mem = (AbstractMemory) item;
        mem.refCount.set(1);
        mem.recycle();
    }
    
    @Override
    protected void prepareForRelease(T item) {
        AbstractMemory mem = (AbstractMemory) item;
        if (mem.next != null) {
            mem.next = null;
        }
    }
    
    @Override
	public long getDefaultHeadroom() {
        return defaultHeadroom;
    }
    
    @Override
    public T allocate() {
        T memory = allocateFromFreeList();
        if (memory != null) {
            metrics.recordAllocation();
        } else {
            metrics.recordExhaustion();
        }
        return memory;
    }
    
    @Override
    public void release(T memory) {
        if (memory == null || memory.refCount() != 0) {
            return;
        }
        addToFreeList(memory);
        metrics.recordRelease();
    }
}