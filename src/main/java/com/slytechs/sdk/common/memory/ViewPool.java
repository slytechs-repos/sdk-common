package com.slytechs.sdk.common.memory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ViewPool<T extends BoundView> extends AbstractPool<T> implements Pool<T> {
	private final long minCapacity;
	private final long maxCapacity;
	private final AtomicInteger currentSize = new AtomicInteger(0);
	private final Supplier<T> factory;

	private static <T> T newView(Class<T> viewClass) {
		try {
			var constructor = viewClass.getConstructor();
			
			return constructor.newInstance();
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return null;
		}
	}

	public ViewPool(String name, Class<T> viewClass, int minCapacity, int maxCapacity) {
		this(name, () -> newView(viewClass), minCapacity, maxCapacity);
	}

	public ViewPool(String name, Supplier<T> factory, int minCapacity, int maxCapacity) {
		super(name, maxCapacity);
		this.factory = factory;
		this.minCapacity = minCapacity;
		this.maxCapacity = maxCapacity;

		// Pre-allocate minimum
		for (long i = 0; i < minCapacity; i++) {
			T view = factory.get();
			if (view != null) {
				addToFreeList(view);
				currentSize.incrementAndGet();
			}
		}
	}

	@Override
	protected Object getPoolNext(T item) {
		return item.poolNext;
	}

	@Override
	protected void setPoolNext(T item, Object next) {
		item.poolNext = (BoundView) next;
	}

	@Override
	protected void prepareForAllocation(T item) {
		item.recycle();
	}

	@Override
	protected void prepareForRelease(T item) {
		if (item.isBound()) {
			item.unbind();
		}
	}

	@Override
	public T allocate() {
		T view = allocateFromFreeList();

		if (view == null && currentSize.get() < maxCapacity) {
			if (currentSize.incrementAndGet() <= maxCapacity) {
				view = factory.get();
				if (view != null) {
					metrics.recordAllocation();
				}
			} else {
				currentSize.decrementAndGet();
				metrics.recordExhaustion();
			}
		} else if (view != null) {
			metrics.recordAllocation();
		} else {
			metrics.recordExhaustion();
		}

		return view;
	}

	@Override
	public void release(T view) {
		if (view == null || view.owningPool != this) {
			return;
		}
		addToFreeList(view);
		metrics.recordRelease();
	}

	public void compact() {
		long toRemove = Math.max(0, available() - minCapacity);
		for (int i = 0; i < toRemove; i++) {
			if (allocateFromFreeList() != null) {
				currentSize.decrementAndGet();
			}
		}
	}
}