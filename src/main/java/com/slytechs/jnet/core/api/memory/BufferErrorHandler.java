package com.slytechs.jnet.core.api.memory;

/**
 * Functional interface for handling buffer errors with type-safe buffer access.
 * 
 * @param <T> the specific buffer type being handled
 */
@FunctionalInterface
public interface BufferErrorHandler<T extends MemoryBuffer> {
	/**
	 * Handles a buffer operation error.
	 * 
	 * @param buf the buffer where the error occurred
	 * @param e   the exception describing the error
	 * @throws BufferOperationException if the error cannot be recovered
	 */
	void handle(T buf, BufferOperationException e) throws BufferOperationException;
}