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
package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.MemorySegment;

/**
 * Packet implementation with configurable pinning behavior.
 * 
 * <p>
 * Supports two pinning modes controlled by a flag:
 * <ul>
 * <li><strong>Immediate mode:</strong> Pin/unpin operations directly manipulate
 * refcount (DPDK/Napatech)</li>
 * <li><strong>Deferred mode:</strong> Pin sets a flag, actual copy happens on
 * release (PCAP)</li>
 * </ul>
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class Packet extends BoundView implements MemoryPoolable {

	/** Packet timestamp */
	protected long timestamp;

	/** Wire length (original packet size) */
	protected int wireLength;

	/** Capture length (actual captured bytes) */
	protected int captureLength;

	/** Whether this packet is pinned */
	protected boolean isPinned;

	/** Whether to use deferred pinning (true) or immediate (false) */
	protected boolean isDeferredMode;

	// Do not reset during recycle!
	private MemoryPool<?> pool;

	/**
	 * Default constructor.
	 */
	public Packet() {
		super();
	}

	/**
	 * Returns the capture length.
	 * 
	 * @return captured bytes count
	 */
	public int captureLength() {
		return captureLength;
	}

	/**
	 * Sets the capture length.
	 * 
	 * @param captureLength captured bytes count
	 */
	public void captureLength(int captureLength) {
		this.captureLength = captureLength;
	}

	/**
	 * Creates a copy of the current packet data.
	 * 
	 * @return a new FixedMemory containing the packet data
	 */
	protected FixedMemory createCopy() {
		if (view == null || !isBound()) {
			throw new IllegalStateException("Cannot copy unbound packet");
		}

		long length = view.length;
		MemorySegment copySegment = MemorySegment.ofArray(new byte[(int) length]);

		// Copy the data
		MemorySegment.copy(
				view.segment, view.start,
				copySegment, 0,
				length);

		return new FixedMemory(copySegment, 0, length);
	}

	/**
	 * Creates a duplicate of this packet.
	 * 
	 * @return a new Packet with copied data
	 */
	public Packet duplicate() {
		Packet dup = new Packet();
		dup.timestamp = this.timestamp;
		dup.wireLength = this.wireLength;
		dup.captureLength = this.captureLength;
		dup.isDeferredMode = this.isDeferredMode;

		if (isBound()) {
			FixedMemory copy = createCopy();
			dup.bind(copy);
		}

		return dup;
	}

	@Override
	public MemoryPool<?> getPool() {
		return pool;
	}

	/**
	 * Checks if this packet is pinned.
	 * 
	 * @return true if pinned
	 */
	public boolean isPinned() {
		return isPinned;
	}

	/**
	 * Pins this packet to prevent release.
	 * 
	 * <p>
	 * In immediate mode, increments refcount. In deferred mode, just sets the flag.
	 * </p>
	 */
	public void pin() {
		if (isPinned) {
			return; // Already pinned
		}

		isPinned = true;

		if (!isDeferredMode && boundSource != null) {
			// Immediate mode: increment refcount now
			boundSource.incrementRef();
		}
		// Deferred mode: just flag is set, copy happens in unbind()
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void recycle() {
		if (isPinned) {
			unpin();
		}
		super.unbind();
		timestamp = 0;
		wireLength = 0;
		captureLength = 0;
		isPinned = false;
		isDeferredMode = false;
	}

	/**
	 * Sets the pinning mode for this packet.
	 * 
	 * @param deferred true for deferred mode (PCAP), false for immediate
	 *                 (DPDK/Napatech)
	 */
	public void setPinningMode(boolean deferred) {
		this.isDeferredMode = deferred;
	}

	@Override
	public void setPool(MemoryPool<?> pool) {
		this.pool = pool;
	}

	/**
	 * Returns the packet timestamp.
	 * 
	 * @return timestamp in nanoseconds
	 */
	public long timestamp() {
		return timestamp;
	}

	/**
	 * Sets the packet timestamp.
	 * 
	 * @param timestamp in nanoseconds
	 */
	public void timestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Handles deferred copy if packet was pinned in deferred mode.
	 * </p>
	 */
	@Override
	public void unbind() {
		if (isDeferredMode && isPinned && boundSource != null) {
			// Deferred mode: make a copy before unbinding
			FixedMemory copy = createCopy();

			// Unbind from original
			super.unbind();

			// Bind to copy
			super.bind(copy);

			isPinned = false; // Clear pin flag after copy
		} else {
			// Normal unbind
			if (isPinned) {
				unpin(); // Clean up pin state
			}
			super.unbind();
		}
	}

	/**
	 * Unpins this packet.
	 * 
	 * <p>
	 * In immediate mode, decrements refcount. In deferred mode, clears the flag.
	 * </p>
	 */
	public void unpin() {
		if (!isPinned) {
			return; // Not pinned
		}

		isPinned = false;

		if (!isDeferredMode && boundSource != null) {
			// Immediate mode: decrement refcount now
			boundSource.decrementRef();
		}
		// Deferred mode: just clear flag
	}

	/**
	 * Returns the wire length.
	 * 
	 * @return original packet size in bytes
	 */
	public int wireLength() {
		return wireLength;
	}

	/**
	 * Sets the wire length.
	 * 
	 * @param wireLength original packet size
	 */
	public void wireLength(int wireLength) {
		this.wireLength = wireLength;
	}
}