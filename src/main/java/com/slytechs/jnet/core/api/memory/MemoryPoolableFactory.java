package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.MemorySegment;

/**
 * Factory interface for creating memory objects during pool initialization.
 * 
 * <p>
 * The Factory pattern enables the pool to create different types of memory
 * objects while maintaining type safety and ensuring proper pool integration.
 * Factories are responsible for creating memory objects with correct pool
 * ownership and initial state.
 * </p>
 * 
 * <h2>Implementation Guidelines</h2>
 * <ul>
 * <li>Always set the owning pool reference in created objects</li>
 * <li>Ensure created objects start with reference count = 1</li>
 * <li>Handle any custom initialization required by the memory type</li>
 * <li>Validate parameters and throw appropriate exceptions for invalid
 * input</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * 
 * <pre>{@code
 * public class MemoryByteBufferFactory implements MemoryPoolableFactory<MemoryByteBuffer> {
 * 	@Override
 * 	public MemoryByteBuffer newInstance(MemoryPool<MemoryByteBuffer> owningPool,
 * 			MemorySegment memorySegment,
 * 			long memoryOffset, long memoryEnd,
 * 			long memoryDataOffset, long memoryDataEnd) {
 * 		return new MemoryByteBuffer(owningPool, memorySegment,
 * 				memoryOffset, memoryEnd,
 * 				memoryDataOffset, memoryDataEnd);
 * 	}
 * }
 * }</pre>
 * 
 * @param <T> the type of memory objects created by this factory
 */
public interface MemoryPoolableFactory<T extends AbstractMemory & MemoryPoolable> {
	/**
	 * Creates a new memory object instance with the specified parameters.
	 * 
	 * @param owningPool       the MemoryPool that will own the created object
	 * @param memorySegment    the backing MemorySegment for the memory object
	 * @param memoryOffset     the starting offset within the segment
	 * @param memoryEnd        the ending offset within the segment (exclusive)
	 * @param memoryDataOffset the starting offset of data within the segment
	 * @param memoryDataEnd    the ending offset of data within the segment
	 *                         (exclusive)
	 * @return a new memory object ready for pool management
	 */
	T newInstance(MemoryPool<T> owningPool, MemorySegment memorySegment,
			long memoryOffset, long memoryEnd,
			long memoryDataOffset, long memoryDataEnd);
}