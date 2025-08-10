/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
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
package com.slytechs.jnet.core.api;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import com.slytechs.jnet.core.api.format.StructFormat;
import com.slytechs.jnet.core.api.format.StructFormattable;
import com.slytechs.jnet.core.api.memory.MemoryStructure;

import static java.lang.foreign.MemoryLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;
import static java.lang.foreign.ValueLayout.*;

/**
 * Represents DPDK's rte_mbuf structure, used for packet buffer management.
 * <p>
 * This class provides access to the core fields of rte_mbuf, suitable for
 * low-level packet processing in DPDK and potentially other contexts. The
 * layout is padded and aligned to match DPDK's requirements (e.g.,
 * cache-aligned).
 * </p>
 * <p>
 * C structure definition (simplified from rte_mbuf_core.h):
 * 
 * {@snippet lang = c:
 * struct rte_mbuf {
 *     void *buf_addr;                  // Virtual address of segment buffer
 *     rte_iova_t buf_iova;             // Physical address of segment buffer
 *     uint16_t data_off;               // Start of data in segment buffer
 *     rte_atomic16_t refcnt;           // Reference count
 *     uint8_t nb_segs;                 // Number of segments
 *     uint8_t port;                    // Input port index
 *     uint64_t ol_flags;               // Offload flags
 *     uint32_t pkt_len;                // Total packet length
 *     uint16_t data_len;               // Amount of data in segment buffer
 *     uint16_t vlan_tci;               // VLAN Tag Control Identifier
 *     union {                          // Hash information
 *         uint32_t rss;                // RSS hash result
 *         // Other union members (e.g., fdir, sched)
 *     } hash;
 *     uint32_t vlan_tci_outer;         // Outer VLAN TCI
 *     uint16_t buf_len;                // Length of segment buffer
 *     struct rte_mempool *pool;        // Pool from which mbuf was allocated
 *     struct rte_mbuf *next;           // Next segment of scattered packet
 *     uint64_t timestamp;              // Timestamp (TX or RX)
 *     // Additional fields like userdata, etc., with padding for alignment
 * };
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class Mbuf extends MemoryStructure implements StructFormattable {

	/**
	 * Defines the memory layout for the rte_mbuf structure, including padding and
	 * alignment to ensure compatibility with DPDK's cache-aligned requirements.
	 */
	public static final MemoryLayout LAYOUT = structLayout(
			ADDRESS.withName("buf_addr"), // void *buf_addr
			JAVA_LONG.withName("buf_iova"), // rte_iova_t buf_iova
			JAVA_SHORT.withName("data_off"), // uint16_t data_off
			JAVA_SHORT.withName("refcnt"), // rte_atomic16_t refcnt (treated as short for simplicity; use atomic if
											// needed)
			JAVA_BYTE.withName("nb_segs"), // uint8_t nb_segs
			JAVA_BYTE.withName("port"), // uint8_t port
			JAVA_LONG.withName("ol_flags"), // uint64_t ol_flags
			JAVA_INT.withName("pkt_len"), // uint32_t pkt_len
			JAVA_SHORT.withName("data_len"), // uint16_t data_len
			JAVA_SHORT.withName("vlan_tci"), // uint16_t vlan_tci
			JAVA_INT.withName("hash_rss"), // union hash { uint32_t rss; ... } - accessing rss field
			JAVA_INT.withName("vlan_tci_outer"), // uint32_t vlan_tci_outer
			JAVA_SHORT.withName("buf_len"), // uint16_t buf_len
			paddingLayout(6), // Padding to align next pointer (assuming 64-bit alignment)
			ADDRESS.withName("pool"), // struct rte_mempool *pool
			ADDRESS.withName("next"), // struct rte_mbuf *next
			JAVA_LONG.withName("timestamp"), // uint64_t timestamp
			paddingLayout(8) // Additional padding for cache alignment (__rte_cache_aligned)
	).withName("rte_mbuf");

	/**
	 * Provides access to the virtual address of the segment buffer.
	 */
	private static final VarHandle BUF_ADDR = LAYOUT.varHandle(groupElement("buf_addr"));

	/**
	 * Provides access to the physical (IOVA) address of the segment buffer.
	 */
	private static final VarHandle BUF_IOVA = LAYOUT.varHandle(groupElement("buf_iova"));

	/**
	 * Provides access to the offset of data within the segment buffer.
	 */
	private static final VarHandle DATA_OFF = LAYOUT.varHandle(groupElement("data_off"));

	/**
	 * Provides access to the reference count of the mbuf.
	 */
	private static final VarHandle REFCNT = LAYOUT.varHandle(groupElement("refcnt"));

	/**
	 * Provides access to the number of segments in the packet.
	 */
	private static final VarHandle NB_SEGS = LAYOUT.varHandle(groupElement("nb_segs"));

	/**
	 * Provides access to the input port index.
	 */
	private static final VarHandle PORT = LAYOUT.varHandle(groupElement("port"));

	/**
	 * Provides access to the offload flags for the packet.
	 */
	private static final VarHandle OL_FLAGS = LAYOUT.varHandle(groupElement("ol_flags"));

	/**
	 * Provides access to the total packet length across all segments.
	 */
	private static final VarHandle PKT_LEN = LAYOUT.varHandle(groupElement("pkt_len"));

	/**
	 * Provides access to the amount of data in the current segment buffer.
	 */
	private static final VarHandle DATA_LEN = LAYOUT.varHandle(groupElement("data_len"));

	/**
	 * Provides access to the VLAN Tag Control Identifier.
	 */
	private static final VarHandle VLAN_TCI = LAYOUT.varHandle(groupElement("vlan_tci"));

	/**
	 * Provides access to the RSS hash result.
	 */
	private static final VarHandle HASH_RSS = LAYOUT.varHandle(groupElement("hash_rss"));

	/**
	 * Provides access to the outer VLAN Tag Control Identifier.
	 */
	private static final VarHandle VLAN_TCI_OUTER = LAYOUT.varHandle(groupElement("vlan_tci_outer"));

	/**
	 * Provides access to the length of the segment buffer.
	 */
	private static final VarHandle BUF_LEN = LAYOUT.varHandle(groupElement("buf_len"));

	/**
	 * Provides access to the memory pool from which the mbuf was allocated.
	 */
	private static final VarHandle POOL = LAYOUT.varHandle(groupElement("pool"));

	/**
	 * Provides access to the pointer to the next segment in a scattered packet.
	 */
	private static final VarHandle NEXT = LAYOUT.varHandle(groupElement("next"));

	/**
	 * Provides access to the timestamp associated with the packet (TX or RX).
	 */
	private static final VarHandle TIMESTAMP = LAYOUT.varHandle(groupElement("timestamp"));

	/**
	 * Constructs a new Mbuf instance with the default layout.
	 */
	public Mbuf() {
		super(LAYOUT);
	}

	/**
	 * Constructs a new Mbuf instance bound to a specific memory segment with an
	 * offset.
	 *
	 * @param segment The memory segment containing the mbuf data.
	 * @param offset  The offset within the segment where the mbuf starts.
	 */
	public Mbuf(MemorySegment segment, long offset) {
		super(LAYOUT, segment, offset);
	}

	/**
	 * Retrieves the virtual address of the segment buffer.
	 *
	 * @return The memory segment representing the buffer address.
	 */
	public MemorySegment getBufAddr() {
		return (MemorySegment) BUF_ADDR.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the physical (IOVA) address of the segment buffer.
	 *
	 * @return The IOVA as a long value.
	 */
	public long getBufIova() {
		return (long) BUF_IOVA.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the offset of data within the segment buffer.
	 *
	 * @return The data offset as an integer.
	 */
	public int getDataOff() {
		return (short) DATA_OFF.get(asMemorySegment(), 0L) & 0xFFFF;
	}

	/**
	 * Retrieves the reference count of the mbuf.
	 *
	 * @return The reference count as an integer.
	 */
	public int getRefcnt() {
		return (short) REFCNT.get(asMemorySegment(), 0L) & 0xFFFF;
	}

	/**
	 * Retrieves the number of segments in the packet.
	 *
	 * @return The number of segments as an integer.
	 */
	public int getNbSegs() {
		return (byte) NB_SEGS.get(asMemorySegment(), 0L) & 0xFF;
	}

	/**
	 * Retrieves the input port index.
	 *
	 * @return The port index as an integer.
	 */
	public int getPort() {
		return (byte) PORT.get(asMemorySegment(), 0L) & 0xFF;
	}

	/**
	 * Retrieves the offload flags for the packet.
	 *
	 * @return The offload flags as a long value.
	 */
	public long getOlFlags() {
		return (long) OL_FLAGS.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the total packet length across all segments.
	 *
	 * @return The total packet length as an integer.
	 */
	public int getPktLen() {
		return (int) PKT_LEN.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the amount of data in the current segment buffer.
	 *
	 * @return The data length as an integer.
	 */
	public int getDataLen() {
		return (short) DATA_LEN.get(asMemorySegment(), 0L) & 0xFFFF;
	}

	/**
	 * Retrieves the VLAN Tag Control Identifier.
	 *
	 * @return The VLAN TCI as an integer.
	 */
	public int getVlanTci() {
		return (short) VLAN_TCI.get(asMemorySegment(), 0L) & 0xFFFF;
	}

	/**
	 * Retrieves the RSS hash result.
	 *
	 * @return The RSS hash value as an integer.
	 */
	public int getHashRss() {
		return (int) HASH_RSS.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the outer VLAN Tag Control Identifier.
	 *
	 * @return The outer VLAN TCI as an integer.
	 */
	public int getVlanTciOuter() {
		return (int) VLAN_TCI_OUTER.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the length of the segment buffer.
	 *
	 * @return The buffer length as an integer.
	 */
	public int getBufLen() {
		return (short) BUF_LEN.get(asMemorySegment(), 0L) & 0xFFFF;
	}

	/**
	 * Retrieves the memory pool from which the mbuf was allocated.
	 *
	 * @return The memory segment representing the pool.
	 */
	public MemorySegment getPool() {
		return (MemorySegment) POOL.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the pointer to the next segment in a scattered packet.
	 *
	 * @return The memory segment representing the next mbuf.
	 */
	public MemorySegment getNext() {
		return (MemorySegment) NEXT.get(asMemorySegment(), 0L);
	}

	/**
	 * Retrieves the timestamp associated with the packet (TX or RX).
	 *
	 * @return The timestamp as a long value.
	 */
	public long getTimestamp() {
		return (long) TIMESTAMP.get(asMemorySegment(), 0L);
	}

	@Override
	public String toString() {
		return format(new StructFormat()).toString();
	}

	/**
	 * Generates a formatted string representation of the Mbuf structure.
	 *
	 * @param ssb The StructPrinter instance to append details to.
	 * @return The StructPrinter instance with the Mbuf details.
	 */
	@Override
	public StructFormat format(StructFormat ssb) {
		return ssb.openln("Mbuf(struct rte_mbuf)")
				.println("buf_addr", getBufAddr())
				.println("buf_iova", getBufIova())
				.println("data_off", getDataOff())
				.println("refcnt", getRefcnt())
				.println("nb_segs", getNbSegs())
				.println("port", getPort())
				.println("ol_flags", getOlFlags())
				.println("pkt_len", getPktLen())
				.println("data_len", getDataLen())
				.println("vlan_tci", getVlanTci())
				.println("hash_rss", getHashRss())
				.println("vlan_tci_outer", getVlanTciOuter())
				.println("buf_len", getBufLen())
				.println("pool", getPool())
				.println("next", getNext())
				.println("timestamp", getTimestamp())
				.close();
	}

	/**
	 * @return
	 */
	public Mbuf nextMemory() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @return
	 */
	public int chainMemoryCount() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @return
	 */
	public long overallLength() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @return
	 */
	public long length() {
		throw new UnsupportedOperationException("not implemented yet");
	}
}