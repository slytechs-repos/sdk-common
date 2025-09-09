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
 * Interface for objects that can be bound to memory regions through delegation.
 * 
 * <p>
 * BindableView provides a delegation-based binding mechanism where all
 * operations are forwarded to a BoundView instance. This allows classes to gain
 * binding capability through composition while maintaining freedom in their
 * inheritance hierarchy.
 * </p>
 * 
 * <h2>Implementation Requirements</h2>
 * <p>
 * Classes implementing this interface need only:
 * </p>
 * <ol>
 * <li>Provide a single field: {@code private final BoundView boundView = new
 * BoundView() {};}</li>
 * <li>Implement one method: {@code public BoundView boundView() { return
 * boundView; }}</li>
 * </ol>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 * <li><strong>Pure delegation:</strong> All operations delegate to
 * BoundView</li>
 * <li><strong>Maximum flexibility:</strong> No inheritance constraints</li>
 * <li><strong>Optimized performance:</strong> Leverages BoundView's
 * optimizations</li>
 * <li><strong>Minimal burden:</strong> One field, one method</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Implementation via Composition</h3>
 * 
 * <pre>{@code
 * public class Packet implements BindableView {
 * 	private final BoundView boundView = new BoundView() {};
 * 
 * 	@Override
 * 	public BoundView boundView() {
 * 		return boundView;
 * 	}
 * 
 * 	// Can extend any base class while having binding capability
 * }
 * }</pre>
 * 
 * <h3>Alternative: Direct Extension</h3>
 * 
 * <pre>{@code
 * public class Packet extends BoundView {
 * 	// Gets everything directly, no delegation needed
 * }
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see BoundView for the delegated implementation
 * @since 1.0
 */
public interface BindableView {

	/**
	 * Binds to a memory region.
	 * 
	 * <p>
	 * Delegates to the BoundView's optimized implementation which shares the
	 * source's view directly when possible.
	 * </p>
	 * 
	 * @param memory the memory to bind to
	 */
	default void bind(Memory memory) {
		boolean wasUnbound = !isBound();
		boundView().bind(memory);
		if (wasUnbound) {
			onBind(); // Notify only if we were previously unbound
		}
	}

	/**
	 * Binds to a memory region with an offset.
	 * 
	 * @param memory the memory to bind to
	 * @param offset the offset within the memory's active data
	 */
	default void bind(Memory memory, long offset) {
		boolean wasUnbound = !isBound();
		boundView().bind(memory, offset);
		if (wasUnbound) {
			onBind();
		}
	}

	/**
	 * Binds to a memory region with offset and length.
	 * 
	 * @param memory the memory to bind to
	 * @param offset the offset within the memory's active data
	 * @param length the length of the view
	 */
	default void bind(Memory memory, long offset, long length) {
		boolean wasUnbound = !isBound();
		boundView().bind(memory, offset, length);
		if (wasUnbound) {
			onBind();
		}
	}

	/**
	 * Returns the BoundView instance for delegation.
	 * 
	 * <p>
	 * All binding operations are delegated to this instance, which provides the
	 * optimized implementation including direct view sharing for zero-offset
	 * bindings.
	 * </p>
	 * 
	 * @return the BoundView instance (never null)
	 */
	BoundView boundView();

	/**
	 * Checks if currently bound.
	 * 
	 * @return true if bound to memory
	 */
	default boolean isBound() {
		return boundView().isBound();
	}

	/**
	 * Called after successful binding.
	 * 
	 * <p>
	 * Default implementation delegates to the BoundView's onBind method. Classes
	 * implementing this interface can override to add their own binding logic.
	 * </p>
	 */
	default void onBind() {}

	/**
	 * Called before unbinding.
	 * 
	 * <p>
	 * Default implementation delegates to the BoundView's onUnbind method. Classes
	 * implementing this interface can override to add their own unbinding logic.
	 * </p>
	 */
	default void onUnbind() {}

	/**
	 * Unbinds from the current memory.
	 */
	default void unbind() {
		if (isBound()) {
			onUnbind(); // Notify only if we were bound
			boundView().unbind();
		}
	}

	/**
	 * Returns the memory view from the bound view.
	 * 
	 * @return the memory view
	 * @throws IllegalStateException if not bound
	 */
	default MemoryView view() {
		return boundView().view();
	}

}