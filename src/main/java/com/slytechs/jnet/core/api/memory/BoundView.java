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

/**
 * Abstract base class providing optimized memory binding implementation.
 * 
 * <p>
 * BoundView provides an optimized implementation of {@link BindableView} that
 * shares the source's view directly when binding at offset 0, avoiding field
 * copying. This optimization can improve performance in high-frequency binding
 * scenarios such as packet processing.
 * </p>
 * 
 * <h2>Optimization Strategy</h2>
 * <ul>
 * <li><strong>Direct binding (offset=0):</strong> Shares source's view directly
 * (2 reference assignments)</li>
 * <li><strong>Mapped binding (offset>0):</strong> Uses pre-allocated mappedView
 * with field copying</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <p>
 * Extend this class when you want optimized binding performance and don't need
 * a different base class:
 * </p>
 * 
 * <pre>{@code
 * public class Packet extends BoundView {
 * 	// Inherits optimized binding implementation
 * 
 * 	public void processPacket() {
 * 		// Access memory through view field
 * 		MemorySegment seg = view.segment;
 * 		long offset = view.start;
 * 		// ...
 * 	}
 * }
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li>Zero allocations - all views pre-allocated</li>
 * <li>Direct binding: 2 assignments + 1 refcount operation</li>
 * <li>Mapped binding: 4 assignments + 1 refcount operation</li>
 * <li>Unbind: 1-2 assignments + 1 refcount operation</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see BindableView for the interface definition
 * @see MemoryView for the view data structure
 * @since 1.0
 */
public abstract class BoundView implements BindableView {

	/**
	 * Pre-allocated view for mapped bindings (offset > 0).
	 * 
	 * <p>
	 * This view is reused for all bindings that require offset mapping, avoiding
	 * allocation in the binding path.
	 * </p>
	 */
	protected final MemoryView mappedView = new MemoryView();

	/**
	 * Current active view - points to either source's view or mappedView.
	 * 
	 * <p>
	 * When binding at offset 0, this references the source memory's view directly.
	 * When binding with offset, this references the mappedView.
	 * </p>
	 */
	protected MemoryView view;

	/**
	 * Reference to the bound memory for lifecycle management.
	 * 
	 * <p>
	 * Kept separate from view.source to enable direct view sharing optimization.
	 * Used for reference counting and unbinding.
	 * </p>
	 */
	protected Memory boundSource;

	/**
	 * Constructs an unbound BoundView.
	 */
	public BoundView() {
		// Start unbound
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.BindableView#boundView()
	 */
	@Override
	public BoundView boundView() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return the current view (direct or mapped)
	 * @throws IllegalStateException if not bound
	 */
	@Override
	public MemoryView view() {
		if (view == null) {
			throw new IllegalStateException("BoundView is not bound");
		}
		return view;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Optimized implementation that shares the source's view directly, requiring
	 * only 2 reference assignments instead of field copying.
	 * </p>
	 */
	@Override
	public void bind(Memory memory) {
		if (memory == null) {
			throw new NullPointerException("Cannot bind to null memory");
		}

		unbind();
		this.view = memory.view(); // Direct view sharing - no copying
		this.boundSource = memory;
		memory.incrementRef();
		
	    onBind();  // Notify after binding
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Uses the pre-allocated mappedView for offset binding.
	 * </p>
	 */
	@Override
	public void bind(Memory memory, long offset, long length) {
		if (memory == null) {
			throw new NullPointerException("Cannot bind to null memory");
		}
		if (offset < 0 || length < 0 || offset + length > memory.length()) {
			throw new IllegalArgumentException(
					String.format("Invalid bounds: offset=%d, length=%d (memory length: %d)",
							offset, length, memory.length()));
		}

		unbind();

		// Use mapped view for offset binding
		mappedView.segment = memory.segment();
		mappedView.start = memory.start() + offset;
		mappedView.length = length;
		mappedView.source = memory;

		this.view = mappedView;
		this.boundSource = memory;
		memory.incrementRef();
		
	    onBind();  // Notify after binding
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Handles both direct and mapped unbinding cases.
	 * </p>
	 */
	@Override
	public void unbind() {
		if (boundSource != null) {
	        onUnbind();  // Notify before unbinding

			boundSource.decrementRef();
			boundSource = null;
		}

		// Only clear mappedView if it was used
		if (view == mappedView) {
			mappedView.clear();
		}

		view = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isBound() {
		return boundSource != null;
	}

	/**
	 * Direct access to the memory segment for performance.
	 * 
	 * @return the memory segment
	 * @throws IllegalStateException if not bound
	 */
	public java.lang.foreign.MemorySegment segment() {
		if (view == null) {
			throw new IllegalStateException("BoundView is not bound");
		}
		return view.segment;
	}

	/**
	 * Direct access to the start offset for performance.
	 * 
	 * @return the start offset
	 * @throws IllegalStateException if not bound
	 */
	public long start() {
		if (view == null) {
			throw new IllegalStateException("BoundView is not bound");
		}
		return view.start;
	}

	/**
	 * Direct access to the length for performance.
	 * 
	 * @return the length in bytes
	 * @throws IllegalStateException if not bound
	 */
	public long length() {
		if (view == null) {
			throw new IllegalStateException("BoundView is not bound");
		}
		return view.length;
	}

	/**
	 * Returns the ending offset (exclusive).
	 * 
	 * @return start + length
	 * @throws IllegalStateException if not bound
	 */
	public long end() {
		if (view == null) {
			throw new IllegalStateException("BoundView is not bound");
		}
		return view.start + view.length;
	}

	/**
	 * Creates a string representation of this bound view.
	 * 
	 * @return a string describing the binding state
	 */
	@Override
	public String toString() {
		if (!isBound()) {
			return getClass().getSimpleName() + "[unbound]";
		}
		return String.format("%s[start=%d, length=%d]",
				getClass().getSimpleName(), view.start, view.length);
	}

	/**
	 * Called after successful binding.
	 * 
	 * <p>
	 * Subclasses can override this method to perform initialization or state
	 * updates when bound to new memory. This method is called after the view has
	 * been successfully bound and all internal state has been updated.
	 * </p>
	 */
	@Override
	public void onBind() {}

	/**
	 * Called before unbinding.
	 * 
	 * <p>
	 * Subclasses can override this method to perform cleanup or save state before
	 * the view is unbound from its current memory. This method is called before the
	 * unbinding process begins.
	 * </p>
	 */
	@Override
	public void onUnbind() {}
}