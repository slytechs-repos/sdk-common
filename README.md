# Core API

[![Java](https://img.shields.io/badge/Java-22%2B-orange.svg)](https://openjdk.java.net/projects/jdk/22/)
[![Panama FFM](https://img.shields.io/badge/Panama-Foreign%20Memory-blue.svg)](https://openjdk.java.net/projects/panama/)
[![License](https://img.shields.io/badge/License-Sly%20Technologies-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)

The foundational module providing core services and APIs for high-performance network packet capture and analysis systems. Built on Java's Foreign Function & Memory (Panama FFM) API for zero-allocation, native-speed packet processing.

> **Note**: Requires JDK 22+ for full Panama FFM support. While FFM was available as preview in JDK 21, production-ready features require JDK 22 final release.

## Overview

Core API serves as the foundational layer for a comprehensive network analysis ecosystem, providing essential services to 20+ specialized modules for packet capture, protocol analysis, and network monitoring. Designed for extreme performance scenarios requiring 100M+ packets per second throughput.

## Key Features

### 🚀 **High-Performance Memory Management**
- **Zero-allocation packet processing** at 100M+ pps scale
- **Pool-based memory management** with lock-free operations  
- **Native memory integration** via Panama Foreign Memory API
- **Reference-counted lifecycle management** with automatic cleanup

### 🔧 **Core Service Areas**
- **Foreign Memory Integration** - Java Panama FFM wrapper and utilities
- **Native Memory Abstractions** - High-level memory management APIs with backend allocator support
- **System Structure Bindings** - mbuf and network structure wrappers
- **Data Pipeline Framework** - Stream processing infrastructure
- **Configuration Management** - Centralized settings and runtime config
- **Utility Functions** - Common operations and helper classes

### 📊 **Performance Characteristics**
- **Tier 1**: 100M+ pps (zero-allocation inline operations)
- **Tier 2**: 10M+ pps (pool-backed structural operations)
- **Tier 3**: 1M+ pps (full-featured editing and transformations)

### 🏗️ **Backend Integration**
- **Multi-Backend Support** - Pluggable memory allocators (Arena, DPDK, NTAPI, Libpcap)
- **Production Monitoring** - Comprehensive metrics and error tracking
- **Resource Management** - Named resources for operational visibility

## Architecture

### Module Structure

```
core-api/
├── com.slytechs.jnet.core.api.foreign/     # Panama FFM integration
├── com.slytechs.jnet.core.api.format/      # Data formatting utilities
├── com.slytechs.jnet.core.api.memory/      # Memory management APIs
├── com.slytechs.jnet.core.api.memory.impl/ # Memory implementation classes
├── com.slytechs.jnet.core.api.time/        # Timestamp and timing utilities
├── com.slytechs.jnet.core.api.util/        # Common utility functions
└── com.slytechs.jnet.core.api.util.function/ # Functional programming utilities
```

### Core Interfaces

#### Memory Management Hierarchy
```java
// Primary memory interfaces
public interface MemoryView     // Read-only access and navigation
public interface MemoryWindow   // Bounds and positioning management
public interface MemoryRef      // Reference counting and lifecycle
public interface Memory         // Complete memory abstraction

// Backend abstraction
public interface MemoryAllocator // Backend-agnostic memory allocation

// Performance-tiered editors
public interface MemoryInlineOperations    // 100M+ pps, zero allocation
public interface MemoryStructuralEditor   // 10M+ pps, pool-backed
public interface MemoryEditor             // 1M+ pps, full-featured
```

#### Key Implementation Classes
- **`AbstractMemory`** - Base implementation with reference counting
- **`MemoryWrapper`** - Zero-overhead immutable memory wrapper
- **`MemorySlice`** - Mutable data bounds management
- **`MemoryBuffer`** - Poolable buffer with full operations
- **`MemoryProxy`** - Zero-allocation rebindable memory access
- **`MemoryPool`** - Lock-free pool management
- **`MemoryEditor`** - Complex chain editing operations

#### Backend Allocators
- **`ArenaMemoryAllocator`** - Standard Java Arena allocation
- **`DpdkMemoryAllocator`** - DPDK rte_mempool integration
- **`NtapiMemoryAllocator`** - Napatech stream buffer allocation
- **`LibpcapMemoryAllocator`** - Libpcap packet buffer allocation

## Quick Start

### Basic Memory Operations

```java
// Create memory from native segment
MemorySegment segment = arena.allocate(2048);
Memory memory = Memory.of(segment);

// High-performance inline operations (100M+ pps)
MemoryInlineOperations inline = memory.inline();
inline.position(12)
      .insertSpace(4)        // VLAN tag space
      .put((short) 0x8100)   // VLAN TPID
      .put((short) vlanId);  // VLAN TCI
```

### Pool-Based Operations

```java
// Lock-free memory pool with named resource
MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
    "packet-processing-pool",    // Resource name for monitoring
    2048,                        // segment size
    1000,                        // pool capacity  
    Arena.global(),              // memory arena
    MemoryBuffer::new            // factory
);

// Dual allocation strategy
try {
    MemoryBuffer buffer = pool.allocate();        // Returns null on exhaustion
    if (buffer == null) {
        // Handle pool exhaustion gracefully
        handlePoolExhaustion();
        return;
    }
    processPacket(buffer);
} finally {
    buffer.decrementRef(); // Auto-return to pool
}

// Or fail-fast approach for editors
MemoryBuffer buffer = pool.allocateOrThrow();    // Throws on exhaustion
```

### Buffer-Style Operations

```java
// Position buffer at protocol headers with automatic bounds
MemoryBuffer packetBuf = packet.asMemoryBuffer();

// Edit Ethernet header (position=0, limit=14)
packetBuf.positionAt(ethernetProxy)
         .skip(6)                    // Move to source MAC
         .put(newSrcMacBytes)        // Write source MAC (6 bytes)
         .putShort(0x0800);          // Write EtherType

// Edit IP header (position=14, limit=34) 
packetBuf.positionAt(ipProxy)
         .skip(8)                    // Move to TTL field
         .put((byte)(ttl - 1))       // Decrement TTL
         .adjustPosition(-8)         // Back to start of IP header
         .updateChecksum();          // Recalculate checksum
```

### Complex Editing with MemoryEditor

```java
// Named editor for monitoring and debugging
try (MemoryEditor<MemoryBuffer> editor = MemoryEditor.create("vlan-tag-insertion")) {
    MemoryBuffer modified = editor
        .edit(packet)
        .insertAt(12, vlanHeader)      // Insert VLAN tag after MAC addresses
        .removeRange(200, 250)         // Remove optional headers
        .appendToChain(trailer)        // Add trailer
        .commit();                     // Apply changes and return result
    
    // Monitor editor performance
    EditorMetrics metrics = editor.getMetrics();
    log.info("Editor '{}': {} operations, {} failures", 
             editor.name(), metrics.getEditOperations(), metrics.getAllocationFailures());
}

### Protocol Processing

```java
// Zero-allocation protocol layer access
MemoryProxy ethernetLayer = new MemoryProxy();
MemoryProxy ipLayer = new MemoryProxy();

// Bind to packet regions
ethernetLayer.bindMemory(packet, 0, 14);   // Ethernet header
ipLayer.bindMemory(packet, 14, 20);        // IP header

// Process without allocation
processEthernet(ethernetLayer);
processIp(ipLayer);
```

## Performance Guidelines

### Memory Access Patterns

| Operation Type | Target Performance | Allocation Strategy | Use Case |
|---------------|-------------------|-------------------|----------|
| **Inline Operations** | 100M+ pps | Zero allocation | High-speed packet processing |
| **Structural Editing** | 10M+ pps | Pool-backed only | Dynamic packet construction |
| **Full Editing** | 1M+ pps | As needed | Complex transformations |

### Best Practices

1. **Use appropriate performance tier** based on throughput requirements
2. **Leverage memory pools** for sustained high-performance scenarios  
3. **Minimize object allocation** in critical processing paths
4. **Reuse MemoryProxy instances** for protocol layer processing
5. **Use named resources** for operational monitoring and debugging
6. **Monitor error counters** to detect allocation failures and performance issues
7. **Choose backend allocators** based on deployment environment (DPDK for DPDK apps, etc.)

### Error Handling Philosophy

The Memory API uses **non-throwing error handling** for production network processing:
- Expected failures increment error counters and continue operation
- Resource exhaustion uses dual allocation strategy (`allocate()` vs `allocateOrThrow()`)
- Pool and editor metrics provide operational visibility
- Exceptions reserved for programming errors and resource corruption

```java
// Monitor pool health
PoolMetrics poolMetrics = pool.getMetrics();
if (poolMetrics.getUtilizationRatio() > 0.9) {
    log.warn("Pool '{}' nearly exhausted: {:.1f}% utilized", 
             pool.name(), poolMetrics.getUtilizationRatio() * 100);
}

// Check for allocation failures
if (poolMetrics.getAllocationFailures() > previousFailures) {
    log.error("Pool '{}' allocation failures: {}", 
              pool.name(), poolMetrics.getAllocationFailures());
}
```

## Integration

Core API is designed to support specialized network analysis modules with multiple backend options:

### **Network Analysis Modules**
- **Packet Capture Modules** - Raw packet acquisition and buffering
- **Protocol Analyzers** - Layer 2-7 protocol parsing and analysis  
- **Flow Analysis** - Connection tracking and session analysis
- **Security Modules** - Intrusion detection and threat analysis
- **Performance Monitoring** - Network performance metrics and alerting
- **Data Export** - PCAP, JSON, and custom format output

### **Backend Integration**
- **Standard Java** - Arena-based allocation for testing and development
- **DPDK** - rte_mempool and rte_mbuf integration for highest performance
- **Napatech NTAPI** - Stream buffer integration for hardware acceleration  
- **Libpcap** - Packet buffer integration for broad compatibility
- **Custom Backends** - Pluggable MemoryAllocator interface for specialized needs

### **Production Deployment**
```java
// High-performance DPDK deployment
MemoryAllocator allocator = new DpdkMemoryAllocator("dpdk-port-0", hugePagePool);
MemoryPool<MemoryBuffer> pool = new MemoryPool<>("rx-pool", 2048, 10000, allocator, MemoryBuffer::new);

// Development/testing deployment  
MemoryPool<MemoryBuffer> pool = new MemoryPool<>("test-pool", 2048, 100, Arena.global(), MemoryBuffer::new);
```

## Requirements

- **Java 22+** with full Panama FFM support
- **Native memory access** capabilities
- **Linux/Windows/macOS** platform support

> **Important**: JDK 22 is required for production use. While Panama FFM was available as preview in JDK 21, the mature API features needed for high-performance packet processing require JDK 22's final implementation.

### **Backend-Specific Requirements**
- **DPDK**: DPDK 23.11+ with hugepage support
- **Napatech**: NTAPI 3.x+ with appropriate hardware
- **Libpcap**: libpcap 1.10+ for packet capture integration
- **Standard**: No additional requirements beyond JDK 22

## Building

```bash
# Build with Maven
mvn clean compile

# Run tests  
mvn test

# Package module
mvn package
```

## License

Licensed under the Sly Technologies License. See [LICENSE](LICENSE) for details.

## Contributing

Core API is the foundation for a large-scale network analysis project. Contributions should focus on:

- **Performance optimization** in critical paths
- **Memory safety** and proper resource management  
- **API consistency** across the module ecosystem
- **Backend integration** for new memory allocation strategies
- **Comprehensive testing** especially for edge cases and multi-threading
- **Production monitoring** and operational visibility improvements

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

**Note**: This module requires careful attention to memory management and performance characteristics. Always profile critical paths and validate memory cleanup in production scenarios. Use named resources and monitor error counters for operational visibility.