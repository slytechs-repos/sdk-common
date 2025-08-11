package com.slytechs.jnet.core.api.memory;

/**
 * Functional interface for handling buffer errors, returns boolean for recovery
 * success.
 */
@FunctionalInterface
public interface BufferErrorHandler {
	void handle(MemoryBuffer buf, BufferOperationException e) throws BufferOperationException;
}