# Core API

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Panama FFM](https://img.shields.io/badge/Panama-Foreign%20Memory-blue.svg)](https://openjdk.java.net/projects/panama/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)

The foundational module providing core services and APIs for high-performance network packet capture and analysis systems. Built on Java's Foreign Function & Memory (Panama FFM) API for zero-allocation, native-speed packet processing.

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
- **Native Memory Abstractions** - High-level memory management APIs
- **System Structure Bindings** - mbuf and network structure wrappers
- **Data Pipeline Framework** - Stream processing infrastructure
- **Configuration Management** - Centralized settings and runtime config
- **Utility Functions** - Common operations and helper classes

### 📊 **Performance Characteristics**
- **Tier 1**: 100M+ pps (zero-allocation inline operations)
- **Tier 2**: 10M+ pps (pool-backed structural operations)
- **Tier 3**: 1M+ pps (full-featured editing and transformations)

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
// Lock-free memory pool
MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
    2048,              // segment size
    1000,              // pool capacity  
    arena,             // memory arena
    MemoryBuffer::new  // factory
);

// Automatic pool management
try (MemoryBuffer buffer = pool.allocate()) {
    buffer.clear()
          .put(ethernetHeader)
          .insert(4).put(vlanTag)      // Pool-backed insertion
          .expand(payloadSize)         // Dynamic expansion
          .put(payload);
} // Auto-return to pool on close
```

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
5. **Prefer inline operations** for simple packet modifications

## Integration

Core API is designed to support specialized network analysis modules:

- **Packet Capture Modules** - Raw packet acquisition and buffering
- **Protocol Analyzers** - Layer 2-7 protocol parsing and analysis  
- **Flow Analysis** - Connection tracking and session analysis
- **Security Modules** - Intrusion detection and threat analysis
- **Performance Monitoring** - Network performance metrics and alerting
- **Data Export** - PCAP, JSON, and custom format output

## Requirements

- **Java 21+** with Panama FFM support enabled
- **Native memory access** capabilities
- **Linux/Windows/macOS** platform support

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

Licensed under the Sly Technologies Free License. See [LICENSE](LICENSE) for details.

## Contributing

Core API is the foundation for a large-scale network analysis project. Contributions should focus on:

- **Performance optimization** in critical paths
- **Memory safety** and proper resource management  
- **API consistency** across the module ecosystem
- **Comprehensive testing** especially for edge cases

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

**Note**: This module requires careful attention to memory management and performance characteristics. Always profile critical paths and validate memory cleanup in production scenarios.