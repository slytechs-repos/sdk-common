# Memory Package API Specification

**Package:** `com.slytechs.jnet.core.api.memory`  
**Version:** 1.0  
**Target:** JDK 22+ (Panama FFM)  
**License:** Sly Technologies Free License  

## Overview

The Memory package provides a comprehensive, high-performance memory management API designed for zero-allocation network packet processing at 100M+ packets per second. Built on Java's Foreign Function & Memory (Panama FFM) API, it supports both single segments and chained memory structures with reference counting, memory pooling, backend allocator abstraction, and efficient editing capabilities.

### Design Principles

- **Zero-Allocation Operations:** Enable 100M+ pps processing without garbage collection overhead
- **Reference Counting:** Automatic cleanup and pool integration through thread-safe atomic operations
- **Chain Support:** Native support for fragmented memory structures without copying
- **Pool Integration:** Lock-free memory pools for sustained high-performance scenarios
- **Backend Abstraction:** Pluggable memory allocators for different deployment environments
- **Safety First:** Bounds validation and error counters instead of exceptions for production stability
- **Named Resources:** Operational visibility through resource naming and comprehensive metrics

### Performance Tiers

| Tier | Performance Target | Allocation Strategy | Use Case |
|------|-------------------|-------------------|----------|
| **Tier 1** | 100M+ pps | Zero allocation | High-speed packet processing |
| **Tier 2** | 10M+ pps | Pool-backed only | Dynamic packet construction |
| **Tier 3** | 1M+ pps | As needed | Complex transformations |

---

## Core Interfaces

### MemoryView

**Purpose:** Core interface providing read-only view capabilities for memory access.

```java
public interface MemoryView {
    ByteBuffer asByteBuffer();
    MemorySegment asMemorySegment();
    MemorySegment asMemorySegmentAt(long chainOffset);
    Memory asMemory();
    Memory nextMemory();
    boolean hasNextMemory();
    Memory seekMemory(long chainOffset);
    boolean isNull();
    boolean isPointer();
}
```

#### Methods

##### `ByteBuffer asByteBuffer()`
**Description:** Returns a ByteBuffer view of the memory's current data region.  
**Returns:** ByteBuffer positioned at the start of usable data  
**Throws:** 
- `UnsupportedOperationException` if ByteBuffer access is not supported
- `IllegalStateException` if memory is closed or invalid

**Usage:**
```java
Memory memory = Memory.of(segment, 0, 1024);
ByteBuffer buffer = memory.asByteBuffer();
buffer.put(data); // Direct buffer operations
```

##### `MemorySegment asMemorySegment()`
**Description:** Returns a MemorySegment view of the memory's current data region.  
**Returns:** MemorySegment representing the usable data region  
**Throws:** `IllegalStateException` if memory is closed or invalid

##### `MemorySegment asMemorySegmentAt(long chainOffset)`
**Description:** Returns the MemorySegment containing the specified offset within the memory chain.  
**Parameters:** `chainOffset` - byte offset from the start of the entire chain (0-based)  
**Returns:** MemorySegment containing the specified offset  
**Throws:** 
- `IllegalArgumentException` if chainOffset is out of bounds
- `IllegalStateException` if memory is closed or invalid

##### `Memory asMemory()`
**Description:** Returns the Memory object for the current view position.  
**Returns:** Memory object at current position (for proxies, returns bound memory)  
**Throws:** `IllegalStateException` if memory is closed or invalid

##### `Memory nextMemory()`
**Description:** Returns the next Memory object in the chain.  
**Returns:** Next Memory in the chain, or `null` if none exists  
**Throws:** `IllegalStateException` if memory is closed or invalid

##### `boolean hasNextMemory()`
**Description:** Checks whether there is a next Memory object in the chain.  
**Returns:** `true` if nextMemory() would return a non-null value

##### `Memory seekMemory(long chainOffset)`
**Description:** Locates and returns the Memory object containing the specified chain offset.  
**Parameters:** `chainOffset` - byte offset from the start of the entire chain  
**Returns:** Memory object containing the specified offset  
**Throws:** 
- `IllegalArgumentException` if chainOffset is out of bounds
- `IllegalStateException` if memory is closed or invalid

##### `boolean isNull()`
**Description:** Checks if this memory represents a null or invalid memory reference.  
**Returns:** `true` if this represents a null or invalid memory reference

##### `boolean isPointer()`
**Description:** Checks if this memory represents a pointer (zero-sized memory reference).  
**Returns:** `true` if this represents a pointer (zero-sized memory)

---

### MemoryWindow

**Purpose:** Interface defining memory window and bounds management capabilities.

```java
public interface MemoryWindow {
    long memoryCapacity();
    long memoryEnd();
    long memoryOffset();
    long memoryDataEnd();
    long memoryDataLength();
    long memoryDataOffset();
    long memoryDataOffsetAt(long chainOffset);
}
```

#### Methods

##### `long memoryCapacity()`
**Description:** Returns the total capacity of the memory region in bytes.  
**Returns:** Total capacity in bytes (always ≥ 0)  
**Formula:** `memoryEnd() - memoryOffset()`

##### `long memoryOffset()`
**Description:** Returns the absolute starting offset of the memory region.  
**Returns:** Absolute starting offset (inclusive bound)

##### `long memoryEnd()`
**Description:** Returns the absolute ending offset of the memory region.  
**Returns:** Absolute ending offset (exclusive bound)

##### `long memoryDataOffset()`
**Description:** Returns the absolute starting offset of the current data region.  
**Returns:** Absolute data starting offset (inclusive bound)  
**Default:** Returns `memoryOffset()`

##### `long memoryDataEnd()`
**Description:** Returns the absolute ending offset of the current data region.  
**Returns:** Absolute data ending offset (exclusive bound)  
**Default:** Returns `memoryEnd()`

##### `long memoryDataLength()`
**Description:** Returns the length of the current data region in bytes.  
**Returns:** Current data length in bytes (always ≥ 0)  
**Formula:** `memoryDataEnd() - memoryDataOffset()`

##### `long memoryDataOffsetAt(long chainOffset)`
**Description:** Returns the data offset within the memory segment containing the specified chain offset.  
**Parameters:** `chainOffset` - offset across the entire chain  
**Returns:** Local data offset within the containing memory segment  
**Default:** Returns `memoryDataOffset()`

---

### MemoryRef

**Purpose:** Interface providing reference counting and lifecycle management for memory objects.

```java
public interface MemoryRef {
    int refCount();
    int incrementRef();
    int decrementRef();
    void close();
    void setNextMemory(Memory next);
}
```

#### Methods

##### `int refCount()`
**Description:** Returns the current reference count of this memory object.  
**Returns:** Current reference count (≥ 0)  
**Thread Safety:** Provides snapshot of current count, may change immediately after return

##### `int incrementRef()`
**Description:** Increments the reference count atomically and returns the new value.  
**Returns:** New reference count after increment (≥ 1)  
**Thread Safety:** Atomic operation  
**Throws:** `IllegalStateException` if memory is already closed

**Usage:**
```java
Memory shared = original.incrementRef(); // Now ref count = 2
try {
    processMemory(shared);
} finally {
    shared.decrementRef(); // Back to ref count = 1
}
```

##### `int decrementRef()`
**Description:** Decrements the reference count atomically and returns the new value.  
**Returns:** New reference count after decrement (≥ 0)  
**Side Effects:** When count reaches 0, automatically calls `close()`  
**Thread Safety:** Atomic operation  
**Throws:** `IllegalStateException` if refcount underflow occurs

##### `void close()`
**Description:** Explicitly closes this memory object, marking it unusable.  
**Precondition:** Reference count must be 0  
**Side Effects:** Releases resources, returns memory to pools if applicable  
**Throws:** `IllegalStateException` if refcount is not 0

##### `void setNextMemory(Memory next)`
**Description:** Sets the next Memory object in a chain structure.  
**Parameters:** `next` - next Memory object in chain, or `null` to terminate  
**Side Effects:** Manages reference counting for chain linkage  
**Thread Safety:** Synchronized operation

---

### Memory

**Purpose:** Comprehensive interface combining all memory management capabilities.

```java
public interface Memory extends MemoryView, MemoryWindow, MemoryRef {
    // Static factory methods
    static boolean isNull(MemorySegment segment);
    static Memory of(MemorySegment segment, long offset);
    static Memory of(MemorySegment segment, long offset, long capacity);
    
    // Chain operations
    long chainCapacity();
    long chainDataLength();
    int chainMemoryCount();
    
    // Conversion operations
    default MemoryBuffer asMemoryBuffer();
}
```

#### Static Methods

##### `static boolean isNull(MemorySegment segment)`
**Description:** Utility method to check if a MemorySegment represents null memory.  
**Parameters:** `segment` - MemorySegment to test, may be `null`  
**Returns:** `true` if segment is `null` or has address 0

##### `static Memory of(MemorySegment segment, long offset)`
**Description:** Creates a Memory wrapper for the specified MemorySegment starting at the given offset.  
**Parameters:**
- `segment` - backing MemorySegment
- `offset` - starting offset within the segment  
**Returns:** New Memory object wrapping the specified region  
**Implementation:** Creates `MemoryWrapper` instance

##### `static Memory of(MemorySegment segment, long offset, long capacity)`
**Description:** Creates a Memory wrapper for the specified MemorySegment region.  
**Parameters:**
- `segment` - backing MemorySegment
- `offset` - starting offset within the segment
- `capacity` - size of the accessible region  
**Returns:** New Memory object wrapping the specified region

#### Chain Operations

##### `long chainCapacity()`
**Description:** Returns the total capacity across all segments in the memory chain.  
**Returns:** Total capacity across all chain segments  
**Complexity:** O(n) where n is the number of segments

##### `long chainDataLength()`
**Description:** Returns the total usable data length across all segments in the memory chain.  
**Returns:** Total usable data length across all chain segments  
**Complexity:** O(n) where n is the number of segments

##### `int chainMemoryCount()`
**Description:** Returns the number of Memory objects in the memory chain.  
**Returns:** Number of Memory objects in the chain (≥ 1)  
**Complexity:** O(n) where n is the number of segments

#### Conversion Operations

##### `default MemoryBuffer asMemoryBuffer()`
**Description:** Converts this Memory to a MemoryBuffer for editing operations.  
**Returns:** MemoryBuffer if this is already a MemoryBuffer  
**Throws:** `UnsupportedOperationException` if conversion is not supported  
**Design Constraint:** Only MemoryBuffer instances support editing operations

---

### MemoryAllocator

**Purpose:** Backend-agnostic memory allocation interface for different deployment environments.

```java
public interface MemoryAllocator extends Named {
    Memory allocateMemory(long size);
    Memory allocateMemory(long size, long alignment);
    boolean supportsDirectAllocation();
    boolean supportsPooling();
    BackendMemoryMetrics getMetrics();
    void close();
}
```

#### Methods

##### `Memory allocateMemory(long size)`
**Description:** Allocates memory of the specified size using backend-specific allocation.  
**Parameters:** `size` - size in bytes to allocate  
**Returns:** Memory object backed by allocated segment  
**Throws:** `OutOfMemoryError` if allocation fails

##### `Memory allocateMemory(long size, long alignment)`
**Description:** Allocates aligned memory of the specified size.  
**Parameters:**
- `size` - size in bytes to allocate
- `alignment` - alignment requirement in bytes  
**Returns:** Memory object backed by aligned allocated segment

##### `boolean supportsDirectAllocation()`
**Description:** Indicates if this allocator supports direct memory allocation.  
**Returns:** `true` if direct allocation is supported

##### `boolean supportsPooling()`
**Description:** Indicates if this allocator is suitable for memory pooling.  
**Returns:** `true` if pooling is recommended for this allocator

##### `BackendMemoryMetrics getMetrics()`
**Description:** Returns metrics for this allocator's operations.  
**Returns:** Backend-specific metrics object

##### `void close()`
**Description:** Closes the allocator and releases any backend resources.

---

### BackendMemoryMetrics

**Purpose:** Interface for backend-specific memory metrics and monitoring.

```java
public interface BackendMemoryMetrics extends Named {
    // Core metrics
    long getAllocations();
    long getDeallocations();
    long getCurrentlyAllocated();
    long getTotalBytesAllocated();
    long getPeakBytesAllocated();
    
    // Error metrics
    long getAllocationFailures();
    long getFragmentationEvents();
    
    // Utilization metrics
    double getUtilizationRatio();
    
    // Backend-specific metrics
    Map<String, Object> getBackendSpecificMetrics();
    
    // Reset capabilities
    void resetCounters();
    Instant getLastResetTime();
}
```

#### Core Metrics Methods

##### `long getAllocations()`
**Description:** Returns total number of allocation operations performed.  
**Returns:** Total allocation count

##### `long getCurrentlyAllocated()`
**Description:** Returns number of currently allocated objects.  
**Returns:** Current allocation count

##### `double getUtilizationRatio()`
**Description:** Returns current utilization as a ratio (0.0 to 1.0).  
**Returns:** Utilization ratio

##### `Map<String, Object> getBackendSpecificMetrics()`
**Description:** Returns backend-specific metrics not covered by standard interface.  
**Returns:** Map of metric name to value

---

## Implementation Classes

### AbstractMemory

**Purpose:** Abstract base implementation providing common Memory interface functionality.

```java
public abstract class AbstractMemory implements Memory {
    protected final AtomicInteger refCount = new AtomicInteger(1);
    protected final MemorySegment memorySegment;
    protected final long memoryOffset;
    protected final long memoryEnd;
    protected Memory nextMemory;
    private ByteBuffer byteBuffer; // Cached view
}
```

#### Key Features
- **Thread-Safe Reference Counting:** Atomic operations for safe concurrent access
- **Automatic Cleanup:** Pool return and resource release on refcount = 0
- **Chain Support:** Complete implementation of chain traversal
- **Validation Framework:** Consistent error checking across implementations
- **ByteBuffer Caching:** Lazy creation and reuse of ByteBuffer views

#### Constructor

```java
protected AbstractMemory(MemorySegment memorySegment, long memoryOffset, long memoryEnd)
```

**Parameters:**
- `memorySegment` - backing MemorySegment, must not be null
- `memoryOffset` - starting offset within the segment (inclusive)
- `memoryEnd` - ending offset within the segment (exclusive)

**Validation:**
- memorySegment is not null
- memoryOffset ≥ 0
- memoryEnd ≥ memoryOffset
- memoryEnd - memoryOffset ≤ memorySegment.byteSize()

#### Protected Methods

##### `void checkNotClosed()`
**Description:** Validates that this memory object is not closed.  
**Throws:** `IllegalStateException` if memory is closed (refcount = 0)

##### `void resetForReuse()`
**Description:** Resets this memory object for reuse, typically called by memory pools.  
**Precondition:** Reference count must be 0  
**Side Effects:** Sets refcount to 1, clears chain linkage, invalidates cached objects

---

### MemoryWrapper

**Purpose:** Simple, immutable wrapper around a MemorySegment providing Memory interface access.

```java
final class MemoryWrapper extends AbstractMemory {
    public MemoryWrapper(MemorySegment memorySegment, long memoryOffset, long memoryEnd);
}
```

#### Characteristics
- **Minimal Implementation:** No additional fields or state beyond AbstractMemory
- **Immutable Bounds:** Memory boundaries are fixed at construction time
- **Zero Overhead:** Direct delegation to AbstractMemory with no additional cost
- **Thread Safe:** Immutable state ensures safe concurrent access
- **No Pool Integration:** Simple reference counting without pool return

#### Usage
Primary implementation for `Memory.of()` factory methods.

---

### MemorySlice

**Purpose:** Memory implementation with mutable data bounds within fixed memory boundaries.

```java
final class MemorySlice extends AbstractMemory {
    private long memoryDataOffset;
    private long memoryDataEnd;
    
    public MemorySlice(MemorySegment memorySegment, long memoryOffset, long memoryEnd,
                      long memoryDataOffset, long memoryDataEnd);
    public long memoryDataOffset(long newOffset);
    public long memoryDataEnd(long newEnd);
}
```

#### Key Features
- **Fixed Memory Bounds:** Immutable capacity and addressable region
- **Mutable Data Bounds:** Independent, adjustable data start and end positions
- **Zero Copy:** No memory allocation during bound adjustments
- **Non-Pooled:** Simple reference counting without pool integration

#### Constructor Parameters
- `memorySegment` - backing MemorySegment
- `memoryOffset` - starting offset of the memory region (inclusive)
- `memoryEnd` - ending offset of the memory region (exclusive)
- `memoryDataOffset` - initial starting offset of the data region (inclusive)
- `memoryDataEnd` - initial ending offset of the data region (exclusive)

#### Data Bounds Methods

##### `long memoryDataOffset(long newOffset)`
**Description:** Sets the absolute starting offset of the current data region.  
**Parameters:** `newOffset` - new data starting offset  
**Returns:** New data offset (same as parameter)  
**Validation:** `memoryOffset() ≤ newOffset ≤ memoryDataEnd()`

##### `long memoryDataEnd(long newEnd)`
**Description:** Sets the absolute ending offset of the current data region.  
**Parameters:** `newEnd` - new data ending offset  
**Returns:** New data end (same as parameter)  
**Validation:** `memoryDataOffset() ≤ newEnd ≤ memoryEnd()`

---

### MemoryBuffer

**Purpose:** Poolable memory buffer implementation with full buffer management capabilities.

```java
public final class MemoryBuffer extends AbstractMemory 
                             implements MemoryPool.MemoryPoolable, MemoryEditable {
    // Buffer-style positioning (chain-aware)
    private long chainPosition = 0;
    private long chainLimit = -1;  // -1 = chainDataLength()
    private long chainMark = -1;
    
    // Performance cache
    private Memory cachedPositionSegment;
    private long cachedPositionSegmentStart;
    private boolean cacheValid = false;
}
```

#### Constructors

```java
public MemoryBuffer(MemoryPool<MemoryBuffer> owningPool, MemorySegment memorySegment,
                   long offset, long length);
public MemoryBuffer(Memory memory);
public MemoryBuffer(Memory memory, long offset, long length);
```

#### Buffer-Style Positioning

##### `long position()`
**Description:** Returns the current chain position.  
**Returns:** Current position relative to chain start

##### `MemoryBuffer position(long newPosition)`
**Description:** Sets the chain position.  
**Parameters:** `newPosition` - new position within chain  
**Returns:** This buffer for chaining  
**Error Handling:** Increments error counter on bounds violation, continues operation

##### `MemoryBuffer positionAt(MemoryProxy proxy)`
**Description:** Positions buffer at proxy's location with automatic bounds.  
**Parameters:** `proxy` - proxy to position at  
**Returns:** This buffer positioned at proxy location with limit set to proxy bounds  
**Side Effects:** Sets both position and limit to proxy's bounds for safety

##### `MemoryBuffer adjustPosition(long delta)`
**Description:** Adjusts position by specified delta (positive or negative).  
**Parameters:** `delta` - amount to adjust position (can be negative)  
**Returns:** This buffer for chaining  

##### `MemoryBuffer skip(long bytes)`
**Description:** Convenience method to advance position forward.  
**Parameters:** `bytes` - number of bytes to advance  
**Returns:** This buffer for chaining  
**Implementation:** Calls `adjustPosition(bytes)`

##### `MemoryBuffer backup(long bytes)`
**Description:** Convenience method to move position backward.  
**Parameters:** `bytes` - number of bytes to move backward  
**Returns:** This buffer for chaining  
**Implementation:** Calls `adjustPosition(-bytes)`

#### Limit Management

##### `long limit()`
**Description:** Returns the current chain limit.  
**Returns:** Current limit, or chainDataLength() if not explicitly set

##### `MemoryBuffer limit(long newLimit)`
**Description:** Sets the chain limit.  
**Parameters:** `newLimit` - new limit value  
**Returns:** This buffer for chaining

##### `MemoryBuffer adjustLimit(long delta)`
**Description:** Adjusts limit by specified delta.  
**Parameters:** `delta` - amount to adjust limit  
**Returns:** This buffer for chaining

#### Buffer Operations

##### `long remaining()`
**Description:** Returns bytes between position and limit.  
**Returns:** `limit() - position()`

##### `boolean hasRemaining()`
**Description:** Checks if remaining > 0.  
**Returns:** `true` if there are remaining bytes

##### `MemoryBuffer mark()`
**Description:** Saves current position.  
**Returns:** This buffer for chaining

##### `MemoryBuffer reset()`
**Description:** Restores to marked position.  
**Returns:** This buffer for chaining  
**Error Handling:** Non-throwing if no mark set

##### `MemoryBuffer rewind()`
**Description:** Resets position to 0, clears mark.  
**Returns:** This buffer for chaining

##### `MemoryBuffer clear()`
**Description:** Resets position=0, limit=-1, mark=-1.  
**Returns:** This buffer for chaining

##### `MemoryBuffer flip()`
**Description:** Sets limit=position, position=0, mark=-1.  
**Returns:** This buffer for chaining

#### Data Access Operations

##### `MemoryBuffer put(byte b)`
**Description:** Writes byte at current position and advances position by 1.  
**Parameters:** `b` - byte to write  
**Returns:** This buffer for chaining  
**Position:** Advances by 1 byte

##### `MemoryBuffer put(byte[] src)`
**Description:** Writes byte array at current position.  
**Parameters:** `src` - source byte array  
**Returns:** This buffer for chaining  
**Position:** Advances by `src.length` bytes

##### `MemoryBuffer putShort(short value)`
**Description:** Writes 2-byte short value at current position.  
**Parameters:** `value` - short value to write  
**Returns:** This buffer for chaining  
**Position:** Advances by 2 bytes  
**Cross-Segment:** Attempts expansion on segment boundaries

##### `MemoryBuffer putInt(int value)`
**Description:** Writes 4-byte int value at current position.  
**Parameters:** `value` - int value to write  
**Returns:** This buffer for chaining  
**Position:** Advances by 4 bytes

##### `MemoryBuffer putLong(long value)`
**Description:** Writes 8-byte long value at current position.  
**Parameters:** `value` - long value to write  
**Returns:** This buffer for chaining  
**Position:** Advances by 8 bytes

##### `byte get()`
**Description:** Reads byte at current position and advances position by 1.  
**Returns:** Byte value at current position  
**Position:** Advances by 1 byte

##### `MemoryBuffer get(byte[] dst)`
**Description:** Reads bytes into destination array.  
**Parameters:** `dst` - destination byte array  
**Returns:** This buffer for chaining  
**Position:** Advances by `dst.length` bytes

#### Space Management

##### `long memoryLeadingSpace()`
**Description:** Returns unused space before the current data region.  
**Returns:** Leading space in bytes (always ≥ 0)  
**Formula:** `memoryDataOffset() - memoryOffset()`

##### `boolean hasMemoryLeadingSpace()`
**Description:** Checks if there is unused space before the current data region.  
**Returns:** `true` if leading space > 0

##### `long memoryTrailingSpace()`
**Description:** Returns unused space after the current data region.  
**Returns:** Trailing space in bytes (always ≥ 0)  
**Formula:** `memoryEnd() - memoryDataEnd()`

##### `boolean hasMemoryTrailingSpace()`
**Description:** Checks if there is unused space after the current data region.  
**Returns:** `true` if trailing space > 0

---

### MemoryProxy

**Purpose:** Lightweight, rebindable proxy for managing access to memory chains with zero-allocation efficiency.

```java
public class MemoryProxy implements MemoryView, MemoryWindow, MemoryRef {
    private Memory head;
    Memory current;  // Package-private for navigation
    private long memoryOffset;
    private long memoryEnd;
    private Memory nextMemory;
    private boolean isClosed;
}
```

#### Key Features
- **Zero-Allocation Rebinding:** Reuse proxy objects without creating new instances
- **Chain Traversal:** Efficient navigation through linked memory structures
- **Automatic Reference Management:** Proper cleanup of bound memory references
- **Flexible Bounds:** Support for both full memory and custom slice binding
- **Type Safety:** Does NOT implement Memory interface - prevents misuse in chains

#### Binding Operations

##### `void bindMemory(Memory memory, long offset)`
**Description:** Binds this proxy to the specified memory starting at the given offset.  
**Parameters:**
- `memory` - memory chain to bind to
- `offset` - starting offset within the memory  
**Side Effects:** Increments bound memory's reference count  
**Throws:** 
- `MemoryBindingException` if proxy is already bound
- `NullPointerException` if memory is null
- `IllegalArgumentException` if offset is invalid

##### `void bindMemory(Memory memory, long offset, long length)`
**Description:** Binds this proxy to the specified memory region with explicit bounds.  
**Parameters:**
- `memory` - memory chain to bind to
- `offset` - starting offset within the memory
- `length` - length of the accessible region  
**Validation:** Ensures offset + length ≤ memory capacity

##### `void unbindMemory()`
**Description:** Unbinds this proxy from its currently bound memory.  
**Side Effects:** Decrements bound memory's reference count, resets proxy state  
**Throws:** `IllegalStateException` if proxy is not bound

##### `boolean isBound()`
**Description:** Checks if this proxy is currently bound to memory.  
**Returns:** `true` if bound to memory

#### Chain Navigation

Navigation methods delegate to the bound memory while maintaining proxy-specific positioning.

#### Design Rationale

MemoryProxy implements MemoryView, MemoryWindow, and MemoryRef but **NOT** Memory. This prevents:
- Adding proxies to memory chains
- Passing proxies to editors  
- Using proxies where actual Memory is expected

Users must explicitly get the bound memory for operations requiring Memory interface.

---

### MemoryPool

**Purpose:** High-performance, thread-safe memory pool that pre-allocates memory segments for zero-allocation runtime efficiency.

```java
public final class MemoryPool<T extends AbstractMemory & MemoryPoolable> implements Named {
    public interface MemoryPoolable {
        MemoryPool<?> getOwningPool();
    }
    
    public interface Factory<T extends AbstractMemory & MemoryPoolable> {
        T newInstance(MemoryPool<T> owningPool, MemorySegment memorySegment, 
                     long offset, long length);
    }
}
```

#### Constructor

```java
public MemoryPool(String name, long segmentSize, long segmentCount, 
                 MemoryAllocator allocator, Factory<T> elementFactory)
```

**Parameters:**
- `name` - resource name for monitoring and debugging
- `segmentSize` - size of each memory segment in bytes
- `segmentCount` - number of segments to pre-allocate
- `allocator` - backend allocator for memory allocation
- `elementFactory` - factory for creating memory objects

**Convenience Constructor:**
```java
public MemoryPool(String name, long segmentSize, long segmentCount, 
                 Arena arena, Factory<T> elementFactory)
```

#### Pool Operations

##### `T allocate()`
**Description:** Allocates a memory object from the pool's free list.  
**Returns:** Memory object with reference count = 1, or `null` if pool exhausted  
**Algorithm:** Lock-free CAS operation on free list head  
**Error Handling:** Increments pool error counter on exhaustion, returns null  
**Complexity:** O(1) with potential retry under contention

##### `T allocateOrThrow()`
**Description:** Allocates a memory object from the pool's free list.  
**Returns:** Memory object with reference count = 1, ready for use  
**Throws:** `OutOfMemoryError` if pool is exhausted  
**Implementation:** Calls `allocate()` and throws if null returned

##### `void release(T mem)`
**Description:** Releases a memory object back to the pool's free list.  
**Parameters:** `mem` - memory object to release  
**Precondition:** Memory object's reference count must be 0  
**Algorithm:** Reset object state, CAS add to free list head  
**Validation:** Ensures memory belongs to this pool  
**Complexity:** O(1) with potential retry under contention

#### Monitoring

##### `String name()`
**Description:** Returns the name of this memory pool.  
**Returns:** Pool name for monitoring and debugging

##### `PoolMetrics getMetrics()`
**Description:** Returns comprehensive metrics for this pool.  
**Returns:** Pool-specific metrics object

##### `long getSegmentSize()`
**Description:** Returns the size of each memory segment in this pool.  
**Returns:** Segment size in bytes

##### `long getSegmentCount()`
**Description:** Returns the total number of segments pre-allocated in this pool.  
**Returns:** Total segment count

##### `long getFreeListSize()`
**Description:** Returns the current number of segments in the free list.  
**Returns:** Number of currently available segments  
**Complexity:** O(n) - traverses entire free list

---

### MemoryEditor

**Purpose:** Comprehensive editor for performing complex operations on memory chains.

```java
public final class MemoryEditor<T extends Memory> implements Named, AutoCloseable {
    // Static factory methods
    public static MemoryEditor<MemoryBuffer> create(String name);
    public static <T extends Memory> MemoryEditor<T> create(String name, MemoryPool<?> pool);
}
```

#### Creation

##### `static MemoryEditor<MemoryBuffer> create(String name)`
**Description:** Creates a MemoryEditor with default editing pool.  
**Parameters:** `name` - resource name for monitoring  
**Returns:** Editor configured with default pool for expansions

##### `static <T extends Memory> MemoryEditor<T> create(String name, MemoryPool<?> pool)`
**Description:** Creates a MemoryEditor with specified pool.  
**Parameters:** 
- `name` - resource name for monitoring
- `pool` - pool to use for memory allocations  
**Returns:** Editor configured with specified pool

#### Editing Operations

##### `MemoryEditor<T> edit(T memory)`
**Description:** Binds editor to memory chain for editing.  
**Parameters:** `memory` - memory chain to edit  
**Returns:** This editor for chaining  
**Side Effects:** Increments memory's reference count  
**Reusable:** Can be called multiple times with different memory

##### `MemoryEditor<T> insertAt(long chainOffset, Memory data)`
**Description:** Inserts memory at specified chain offset.  
**Parameters:**
- `chainOffset` - offset within chain for insertion
- `data` - memory to insert  
**Returns:** This editor for chaining  
**Execution:** Immediate operation, no caching

##### `MemoryEditor<T> removeRange(long start, long end)`
**Description:** Removes byte range from memory chain.  
**Parameters:**
- `

##### `MemoryEditor<T> removeRange(long start, long end)`
**Description:** Removes byte range from memory chain.  
**Parameters:**
- `start` - starting offset (inclusive)
- `end` - ending offset (exclusive)  
**Returns:** This editor for chaining

##### `MemoryEditor<T> replaceRange(long start, long end, Memory replacement)`
**Description:** Replaces byte range with new memory.  
**Parameters:**
- `start` - starting offset (inclusive)
- `end` - ending offset (exclusive)
- `replacement` - new memory to insert  
**Returns:** This editor for chaining

##### `MemoryEditor<T> appendToChain(Memory data)`
**Description:** Appends memory to end of chain.  
**Parameters:** `data` - memory to append  
**Returns:** This editor for chaining

##### `MemoryEditor<T> prependToChain(Memory data)`
**Description:** Prepends memory to start of chain.  
**Parameters:** `data` - memory to prepend  
**Returns:** This editor for chaining

##### `T commit()`
**Description:** Completes editing and returns the modified memory chain.  
**Returns:** The edited memory chain (may be modified from original)  
**Side Effects:** Applies any deferred optimizations  
**Reusable:** Editor can be used for next edit session after commit

#### Error Monitoring

##### `long getEditFailures()`
**Description:** Returns count of edit operation failures.  
**Returns:** Number of edit failures

##### `long getAllocationFailures()`
**Description:** Returns count of pool allocation failures.  
**Returns:** Number of allocation failures

---

## Utility Classes

### MemoryBindingException

**Purpose:** Exception thrown when attempting to bind an already-bound MemoryProxy.

```java
class MemoryBindingException extends RuntimeException {
    public MemoryBindingException(String message);
    public MemoryBindingException(String message, Throwable cause);
}
```

Indicates programming error where code attempts to bind a MemoryProxy without first unbinding it.

---

## Usage Examples

### Basic Memory Operations

```java
// Create memory from native segment
MemorySegment segment = arena.allocate(2048);
Memory memory = Memory.of(segment);

// Access through different views
ByteBuffer buffer = memory.asByteBuffer();
MemorySegment rawSegment = memory.asMemorySegment();
```

### Pool-Based Operations

```java
// Create memory pool
MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
    2048,                    // segment size
    1000,                    // segment count
    Arena.global(),          // memory arena
    MemoryBuffer::new        // factory
);

// Allocate and use buffer
try (MemoryBuffer buffer = pool.allocate()) {
    buffer.clear()
          .put(ethernetHeader)
          .put(ipHeader)
          .put(payload);
    
    // Process buffer
    processPacket(buffer);
    
} // Automatically returns to pool
```

### Protocol Processing with Proxies

```java
// Zero-allocation protocol processing
MemoryProxy ethernetProxy = new MemoryProxy();
MemoryProxy ipProxy = new MemoryProxy();

// Process incoming packet
MemoryBuffer packet = pool.allocate();
receivePacket(packet);

// Bind proxies to protocol layers
ethernetProxy.bindMemory(packet, 0, 14);    // Ethernet header
ipProxy.bindMemory(packet, 14, 20);         // IP header

// Process headers without allocation
if (ethernetProxy.asByteBuffer().getShort(12) == 0x0800) { // IPv4
    int ttl = ipProxy.asByteBuffer().get(8);
    if (ttl > 1) {
        processPacket(packet);
    }
}

// Cleanup
ethernetProxy.unbindMemory();
ipProxy.unbindMemory();
```

### Buffer-Style Editing

```java
// Get packet buffer for editing
MemoryBuffer packetBuf = packet.asMemoryBuffer();

// Edit Ethernet header
packetBuf.positionAt(ethernetProxy)         // Position at Ethernet, limit to header bounds
         .skip(6)                           // Move to source MAC
         .put(newSrcMacBytes)               // Write new source MAC (6 bytes)
         .putShort(0x0800);                 // Write EtherType

// Edit IP header
packetBuf.positionAt(ipProxy)              // Position at IP header
         .skip(8)                           // Move to TTL field
         .put((byte)(ttl - 1));             // Decrement TTL

// Insert VLAN tag
packetBuf.positionAt(ethernetProxy)
         .skip(12)                          // After MAC addresses
         .limit(packetBuf.chainDataLength()) // Allow expansion beyond header
         .insertSpace(4)                    // Make room for VLAN tag
         .putShort(0x8100)                  // VLAN TPID
         .putShort(vlanId);                 // VLAN TCI
```

### Complex Editing with MemoryEditor

```java
// Create reusable editor
try (MemoryEditor<MemoryBuffer> editor = MemoryEditor.create()) {
    
    // Edit first packet
    MemoryBuffer modified1 = editor
        .edit(packet1)
        .insertAt(100, vlanHeader)
        .removeRange(200, 250)
        .appendToChain(trailer)
        .commit();
    
    // Reuse editor for second packet
    MemoryBuffer modified2 = editor
        .edit(packet2)
        .prependToChain(tunnelHeader)
        .replaceRange(50, 70, newData)
        .commit();
}
```

### Chain Navigation

```java
// Process entire memory chain
for (Memory segment = chainHead; segment != null; segment = segment.nextMemory()) {
    ByteBuffer buffer = segment.asByteBuffer();
    processSegment(buffer);
}

// Random access across chain
long totalLength = chainHead.chainDataLength();
MemorySegment specificSegment = chainHead.asMemorySegmentAt(1500);
```

---

## Error Handling Strategy

### Non-Throwing Operations

The Memory API follows a non-throwing error handling philosophy for production network processing environments. Expected failures increment error counters and continue operation rather than throwing exceptions.

#### Error Categories

| Error Type | Handling Strategy | Exception Cases |
|------------|------------------|-----------------|
| **Bounds Violations** | Increment counter, clamp to valid range | Never |
| **Pool Exhaustion** | Increment counter, return null/reuse existing | `OutOfMemoryError` on first allocation |
| **Cross-Segment Writes** | Attempt expansion, fall back to fragmented write | Never |
| **Reference Underflow** | Increment counter, prevent negative counts | Never |
| **Invalid Operations** | Increment counter, no-op or safe fallback | Never |
| **Memory Corruption** | N/A - would cause JVM crash | `IllegalStateException` |
| **Programming Errors** | N/A - should be caught in testing | Standard exceptions |

#### Error Counter Monitoring

```java
// Monitor buffer operation errors
MemoryBuffer buffer = pool.allocate();
// ... perform operations ...

// Check for issues
if (buffer.getBoundaryWriteErrors() > 0) {
    log.warn("Cross-segment write failures: {}", buffer.getBoundaryWriteErrors());
}

if (buffer.getOperationErrors() > 0) {
    log.warn("Operation errors: {}", buffer.getOperationErrors());
}

// Reset counters for next use
buffer.resetErrorCounters();
```

#### Production Monitoring

```java
// Aggregate error monitoring across pools
public class MemoryMonitor {
    public void checkPoolHealth(MemoryPool<?> pool) {
        long freeCount = pool.getFreeListSize();
        long totalCount = pool.getSegmentCount();
        
        if (freeCount < totalCount * 0.1) { // Less than 10% free
            log.warn("Pool nearly exhausted: {}/{} segments available", 
                     freeCount, totalCount);
        }
    }
    
    public void checkEditorHealth(MemoryEditor<?> editor) {
        if (editor.getEditFailures() > 0) {
            log.warn("Editor failures: {}", editor.getEditFailures());
        }
        
        if (editor.getAllocationFailures() > 0) {
            log.warn("Editor allocation failures: {}", editor.getAllocationFailures());
        }
    }
}
```

---

## Performance Guidelines

### Memory Access Patterns

#### Optimal Usage Patterns

1. **Sequential Access**: Process memory chains in forward order for optimal cache performance
2. **Bulk Operations**: Prefer bulk copy operations over byte-by-byte access
3. **Pool Reuse**: Keep pools sized appropriately for sustained allocation rates
4. **Reference Management**: Minimize reference count operations in tight loops

#### Performance Characteristics

| Operation Type | Complexity | Allocation | Use Case |
|---------------|------------|------------|----------|
| **Memory.of()** | O(1) | 1 object | Simple wrapper creation |
| **Pool.allocate()** | O(1) | 0 runtime | High-throughput scenarios |
| **Chain traversal** | O(n) | 0 | Chain navigation |
| **Proxy binding** | O(1) | 0 | Protocol parsing |
| **Buffer positioning** | O(1) | 0 | Data access |
| **Cross-segment write** | O(1) | 0-1 objects | Boundary operations |

#### Memory Locality Optimization

```java
// Optimal: Process data in sequence
MemoryBuffer buffer = packet.asMemoryBuffer();
buffer.position(0);
while (buffer.hasRemaining()) {
    processChunk(buffer.get());
}

// Suboptimal: Random access patterns
for (int i = 0; i < length; i += random.nextInt(100)) {
    buffer.position(i);
    processChunk(buffer.get());
}
```

### Pool Sizing Guidelines

#### Pool Configuration

```java
// High-throughput scenario (100M+ pps)
MemoryPool<MemoryBuffer> highThroughputPool = new MemoryPool<>(
    2048,                    // Typical packet size
    10000,                   // Large pool for burst handling
    Arena.global(),
    MemoryBuffer::new
);

// Moderate-throughput scenario (1M-10M pps)
MemoryPool<MemoryBuffer> moderatePool = new MemoryPool<>(
    1024,                    // Smaller segments
    1000,                    // Moderate pool size
    Arena.global(),
    MemoryBuffer::new
);
```

#### Pool Monitoring

```java
public class PoolMonitor {
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(1);
    
    public void startMonitoring(MemoryPool<?> pool) {
        scheduler.scheduleAtFixedRate(() -> {
            long free = pool.getFreeListSize();
            long total = pool.getSegmentCount();
            double utilization = (total - free) / (double) total;
            
            if (utilization > 0.8) {
                log.warn("High pool utilization: {:.1f}%", utilization * 100);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
```

---

## Thread Safety

### Reference Counting

All reference counting operations are atomic and thread-safe:
- `incrementRef()` uses atomic compare-and-swap
- `decrementRef()` uses atomic decrement with underflow protection
- Automatic cleanup on refcount = 0 is thread-safe

### Memory Pools

Memory pools are completely lock-free and thread-safe:
- Allocation uses atomic CAS on free list head
- Release uses atomic CAS to add to free list
- No locks or synchronized blocks in critical paths

### Chain Operations

Chain navigation is thread-safe for reading:
- Chain structure is immutable once established
- Navigation methods are read-only
- Chain modification requires external synchronization

### Buffer Operations

MemoryBuffer positioning is **not** thread-safe:
- Position/limit/mark are not synchronized
- Concurrent access requires external synchronization
- Error counters use atomic operations for thread safety

```java
// Thread-safe buffer usage
public class ThreadSafeBufferProcessor {
    private final Object bufferLock = new Object();
    
    public void processBuffer(MemoryBuffer buffer) {
        synchronized (bufferLock) {
            buffer.position(0)
                  .put(data)
                  .flip();
            
            processData(buffer);
        }
    }
}
```

---

## Integration Guidelines

### Protocol Processing Integration

#### Header Binding Pattern

```java
public abstract class ProtocolHeader extends MemoryProxy {
    private Packet boundPacket;
    private long headerOffset;
    
    public final void bindToPacket(Packet packet, long offset) {
        this.boundPacket = packet;
        this.headerOffset = offset;
        
        super.bindMemory(packet, offset, getHeaderLength());
        onBind();
    }
    
    protected abstract void onBind();
    protected abstract int getHeaderLength();
    
    public final MemoryBuffer getEditableBuffer() {
        return boundPacket.asMemoryBuffer();
    }
}
```

#### Field Access Pattern

```java
public class EthernetHeader extends ProtocolHeader {
    private final MacAddressProxy dstMac = new MacAddressProxy();
    private final MacAddressProxy srcMac = new MacAddressProxy();
    
    @Override
    protected void onBind() {
        dstMac.bindMemory(asMemory(), 0, 6);
        srcMac.bindMemory(asMemory(), 6, 6);
    }
    
    @Override
    protected int getHeaderLength() {
        return 14;
    }
    
    public MacAddressProxy dst() { return dstMac; }
    public MacAddressProxy src() { return srcMac; }
    
    public void setEtherType(int etherType) {
        getEditableBuffer()
            .positionAt(this)
            .skip(12)
            .putShort((short) etherType);
    }
}
```

### Memory Pool Integration

#### Custom Pool Implementation

```java
public class PacketPool {
    private final MemoryPool<MemoryBuffer> pool;
    private final int packetSize;
    
    public PacketPool(int packetSize, int poolSize, Arena arena) {
        this.packetSize = packetSize;
        this.pool = new MemoryPool<>(
            packetSize, 
            poolSize, 
            arena,
            this::createPacketBuffer
        );
    }
    
    private MemoryBuffer createPacketBuffer(MemoryPool<MemoryBuffer> pool,
                                           MemorySegment segment,
                                           long offset, long length) {
        return new MemoryBuffer(pool, segment, offset, length);
    }
    
    public MemoryBuffer allocatePacket() {
        return pool.allocate();
    }
}
```

### Editor Integration

#### Packet Modification Pipeline

```java
public class PacketProcessor {
    private final MemoryEditor<MemoryBuffer> editor = MemoryEditor.create();
    private final EthernetHeader ethernet = new EthernetHeader();
    private final IpHeader ip = new IpHeader();
    
    public MemoryBuffer processPacket(MemoryBuffer packet) {
        // Bind headers
        ethernet.bindToPacket(packet, 0);
        ip.bindToPacket(packet, 14);
        
        // Apply modifications based on packet content
        MemoryBuffer result = editor.edit(packet);
        
        if (needsVlanTag(ethernet)) {
            result = editor.insertAt(12, createVlanTag()).commit();
            // Rebind IP header after VLAN insertion
            ip.bindToPacket(result, 18);
        }
        
        if (needsTunneling(ip)) {
            result = editor.edit(result)
                          .prependToChain(createTunnelHeader())
                          .commit();
        }
        
        return result;
    }
}
```

---

## Migration and Upgrade Guidelines

### Version Compatibility

#### API Stability Guarantees

- **Core Interfaces** (Memory, MemoryView, MemoryWindow, MemoryRef): Stable API, additions only
- **Implementation Classes**: Internal implementation may change, public API stable
- **Factory Methods**: Stable signatures and behavior
- **Error Handling**: Error counter semantics stable, new counters may be added

#### Deprecated Features

None in version 1.0. Future deprecations will be marked with `@Deprecated` and supported for at least two major versions.

### Performance Migration

#### From Traditional Buffer Management

```java
// Before: Traditional ByteBuffer approach
ByteBuffer packet = ByteBuffer.allocate(2048);
// ... populate packet ...
ByteBuffer ethernetHeader = packet.slice(0, 14);
ByteBuffer ipHeader = packet.slice(14, 20);

// After: Memory API approach
MemoryBuffer packet = pool.allocate();
// ... populate packet ...
packet.positionAt(ethernetProxy); // Automatic bounds
packet.positionAt(ipProxy);       // Automatic bounds
```

#### From Manual Memory Management

```java
// Before: Manual native memory
MemorySegment nativeMemory = arena.allocate(2048);
// ... manual offset calculations ...

// After: Memory API
Memory memory = Memory.of(nativeMemory);
MemoryBuffer buffer = memory.asMemoryBuffer();
// ... automatic bounds management ...
```

---

## Best Practices

### Design Patterns

#### Factory Pattern for Pools

```java
public class MemoryPoolFactory {
    public static MemoryPool<MemoryBuffer> createPacketPool() {
        return new MemoryPool<>(
            2048, 1000, Arena.global(), MemoryBuffer::new
        );
    }
    
    public static MemoryPool<MemoryBuffer> createJumboPool() {
        return new MemoryPool<>(
            9216, 500, Arena.global(), MemoryBuffer::new
        );
    }
}
```

#### Proxy Reuse Pattern

```java
public class ProtocolParser {
    // Reuse proxies across packets for zero allocation
    private final MemoryProxy ethernetProxy = new MemoryProxy();
    private final MemoryProxy ipProxy = new MemoryProxy();
    private final MemoryProxy tcpProxy = new MemoryProxy();
    
    public void parsePacket(MemoryBuffer packet) {
        try {
            ethernetProxy.bindMemory(packet, 0, 14);
            // ... parse ethernet ...
            
            if (isIpv4(ethernetProxy)) {
                ipProxy.bindMemory(packet, 14, 20);
                // ... parse IP ...
                
                if (isTcp(ipProxy)) {
                    tcpProxy.bindMemory(packet, 34, getTcpHeaderLength(ipProxy));
                    // ... parse TCP ...
                }
            }
        } finally {
            // Always unbind to prevent memory leaks
            ethernetProxy.unbindMemory();
            ipProxy.unbindMemory();
            tcpProxy.unbindMemory();
        }
    }
}
```

#### Resource Management Pattern

```java
public class PacketProcessor implements AutoCloseable {
    private final MemoryPool<MemoryBuffer> pool;
    private final MemoryEditor<MemoryBuffer> editor;
    private final List<MemoryProxy> proxies = new ArrayList<>();
    
    public PacketProcessor() {
        this.pool = MemoryPoolFactory.createPacketPool();
        this.editor = MemoryEditor.create(pool);
        
        // Pre-create reusable proxies
        for (int i = 0; i < 10; i++) {
            proxies.add(new MemoryProxy());
        }
    }
    
    @Override
    public void close() {
        editor.close();
        // Pool cleanup handled by Arena
    }
}
```

### Performance Optimization

#### Minimize Allocations

```java
// Good: Reuse objects
private final MacAddressProxy tempMacProxy = new MacAddressProxy();

public void processMacAddress(MemoryBuffer packet, long offset) {
    tempMacProxy.bindMemory(packet, offset, 6);
    // ... process ...
    tempMacProxy.unbindMemory();
}

// Avoid: Creating new objects
public void processMacAddress(MemoryBuffer packet, long offset) {
    MacAddressProxy mac = new MacAddressProxy(); // Allocation!
    mac.bindMemory(packet, offset, 6);
    // ... process ...
}
```

#### Batch Operations

```java
// Good: Batch processing
public void processPackets(List<MemoryBuffer> packets) {
    for (MemoryBuffer packet : packets) {
        processPacket(packet);
    }
    
    // Batch error checking
    checkErrorCounters();
}

// Avoid: Per-packet overhead
public void processPackets(List<MemoryBuffer> packets) {
    for (MemoryBuffer packet : packets) {
        processPacket(packet);
        checkErrorCounters(); // Overhead per packet
    }
}
```

### Error Handling

#### Monitoring Pattern

```java
public class MemoryHealthMonitor {
    private final Map<MemoryPool<?>, PoolMetrics> poolMetrics = new ConcurrentHashMap<>();
    
    public void recordAllocation(MemoryPool<?> pool) {
        poolMetrics.computeIfAbsent(pool, k -> new PoolMetrics())
                  .incrementAllocations();
    }
    
    public void recordError(MemoryBuffer buffer, String errorType) {
        // Log error with context
        log.warn("Memory error: type={}, buffer={}", errorType, buffer);
        
        // Update metrics
        incrementErrorMetric(errorType);
    }
    
    private static class PoolMetrics {
        private final AtomicLong allocations = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
        
        void incrementAllocations() { allocations.incrementAndGet(); }
        void incrementErrors() { errors.incrementAndGet(); }
    }
}
```

---

## Appendix

### Configuration Properties

#### System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `jnet.memory.pool.default.size` | `2048` | Default pool segment size |
| `jnet.memory.pool.default.count` | `1000` | Default pool segment count |
| `jnet.memory.error.logging` | `true` | Enable error counter logging |
| `jnet.memory.validation.strict` | `false` | Enable strict bounds validation |

#### Environment Variables

| Variable | Description |
|----------|-------------|
| `JNET_MEMORY_DEBUG` | Enable debug logging for memory operations |
| `JNET_MEMORY_STATS` | Enable runtime statistics collection |

### Troubleshooting

#### Common Issues

**OutOfMemoryError from MemoryPool**
- **Cause**: Pool exhausted due to leaked references
- **Solution**: Check for proper `decrementRef()` calls, monitor pool utilization
- **Debug**: Use `pool.getFreeListSize()` to monitor availability

**IllegalStateException: memory is closed**
- **Cause**: Accessing memory after reference count reached 0
- **Solution**: Ensure proper reference management, avoid using memory after `decrementRef()`
- **Debug**: Check `refCount()` before operations

**MemoryBindingException**
- **Cause**: Attempting to bind already-bound MemoryProxy
- **Solution**: Call `unbindMemory()` before rebinding
- **Debug**: Use `isBound()` to check binding state

**High error counters**
- **Cause**: Bounds violations, expansion failures, or invalid operations
- **Solution**: Review buffer usage patterns, increase pool segment sizes
- **Debug**: Monitor specific error types via counter methods

#### Debugging Tools

```java
public class MemoryDebugger {
    public static void dumpMemoryState(Memory memory) {
        System.out.printf("Memory: offset=%d, end=%d, capacity=%d%n",
                         memory.memoryOffset(), memory.memoryEnd(), memory.memoryCapacity());
        System.out.printf("Data: offset=%d, end=%d, length=%d%n",
                         memory.memoryDataOffset(), memory.memoryDataEnd(), memory.memoryDataLength());
        System.out.printf("Chain: length=%d, count=%d%n",
                         memory.chainDataLength(), memory.chainMemoryCount());
        System.out.printf("RefCount: %d%n", memory.refCount());
    }
    
    public static void dumpPoolState(MemoryPool<?> pool) {
        System.out.printf("Pool: segmentSize=%d, segmentCount=%d, free=%d%n",
                         pool.getSegmentSize(), pool.getSegmentCount(), pool.getFreeListSize());
    }
}
```

### Reference Implementation Notes

This specification defines the complete public API for the Memory package. Implementation details not specified here are considered internal and subject to change. The reference implementation should prioritize:

1. **Correctness**: Proper bounds checking and reference counting
2. **Performance**: Zero-allocation operation in critical paths  
3. **Safety**: Non-throwing error handling for production use
4. **Simplicity**: Clear, maintainable code following KISS principle

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-XX  
**Review Cycle:** Quarterly or before major releases