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
 * Generic pool for view objects (Packet, Header, etc).
 * 
 * <p>
 * ViewPool manages pools of objects that implement BindableView or extend
 * BoundView. These are lightweight view objects that don't own memory but can
 * be bound to Memory objects for data access.
 * </p>
 * 
 * @param <T> the type of view object
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class ViewPool<T extends BindableView> extends AbstractMemoryPool<T> {

	/** Factory for creating view instances */
	public interface ViewFactory<T extends BindableView> {
		T newInstance();
	}

	private final ViewFactory<T> factory;

	/**
	 * Creates a pool with a factory.
	 * 
	 * @param name     pool name
	 * @param capacity pool capacity
	 * @param factory  factory for creating instances
	 */
	public ViewPool(String name, int capacity, ViewFactory<T> factory) {
		super(name, capacity, 0); // Views don't need headroom
		this.factory = factory;

		initializePool();
	}

	/**
	 * Initializes the pool by pre-allocating view objects.
	 */
	private void initializePool() {
		for (int i = 0; i < capacity; i++) {
			T instance = factory.newInstance();
			addToFreeList(instance);
		}
	}

	@Override
	public T allocate() {
		T view = allocateFromFreeList();
		if (view != null) {
			// Ensure clean state
			if (view.isBound()) {
				view.unbind();
			}
		}
		return view;
	}

	/**
	 * Allocates and binds a view to memory.
	 * 
	 * @param memory the memory to bind to
	 * @return bound view or null if exhausted
	 */
	public T allocate(Memory memory) {
		T view = allocate();
		if (view != null) {
			view.bind(memory);
		}
		return view;
	}

	/**
	 * Allocates and binds a view to memory with offset.
	 * 
	 * @param memory the memory to bind to
	 * @param offset offset within memory
	 * @param length length of view
	 * @return bound view or null if exhausted
	 */
	public T allocate(Memory memory, long offset, long length) {
		T view = allocate();
		if (view != null) {
			view.bind(memory, offset, length);
		}
		return view;
	}
}