/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
package com.slytechs.jnet.core.api.util.function;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

/**
 * Provides thread-safe execution of code blocks using read-write locks. This
 * class simplifies the implementation of thread-safe operations by managing
 * lock acquisition and release, and providing exception handling capabilities.
 * 
 * <p>
 * The class supports both read and write operations with different locking
 * strategies, and allows for custom exception handling.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class GuardedCode {

	/** The rw lock. */
	@SuppressWarnings("unused")
	private final ReadWriteLock rwLock;
	
	/** The read lock. */
	private final Lock readLock;
	
	/** The write lock. */
	private final Lock writeLock;
	
	/** The exception handler. */
	private final Consumer<Throwable> exceptionHandler;

	/**
	 * Constructs a new GuardedCode instance with the specified read-write lock,
	 * using default exception handling (printStackTrace).
	 *
	 * @param rwLock the read-write lock to use for synchronization
	 */
	public GuardedCode(ReadWriteLock rwLock) {
		this(rwLock, Throwable::printStackTrace);
	}

	/**
	 * Constructs a new GuardedCode instance with the specified read-write lock and
	 * custom exception handler.
	 *
	 * @param rwLock           the read-write lock to use for synchronization
	 * @param exceptionHandler the handler for uncaught exceptions
	 */
	public GuardedCode(ReadWriteLock rwLock, Consumer<Throwable> exceptionHandler) {
		this.rwLock = rwLock;
		this.exceptionHandler = exceptionHandler;
		this.readLock = rwLock.readLock();
		this.writeLock = rwLock.writeLock();
	}

	/**
	 * Executes the provided code block with a read lock. The lock is automatically
	 * released after execution, even if an exception occurs.
	 *
	 * @param code the code block to execute under the read lock
	 */
	public void readLockedVoid(Runnable code) {
		readLock.lock();
		try {
			code.run();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Executes the provided code block with a write lock. The lock is automatically
	 * released after execution, even if an exception occurs.
	 *
	 * @param code the code block to execute under the write lock
	 */
	public void writeLockedVoid(Runnable code) {
		writeLock.lock();
		try {
			code.run();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Executes the provided code block with a read lock, handling specific
	 * exceptions.
	 *
	 * @param <T>            the return type of the code block
	 * @param <E>            the type of exception to handle specifically
	 * @param code           the code block to execute under the read lock
	 * @param exceptionClass the class of exception to handle specifically
	 * @return the result of the code block execution, or null if an exception
	 *         occurs
	 * @throws E if an exception of type E occurs during execution
	 */
	@SuppressWarnings("unchecked")
	public <T, E extends Throwable> T readLocked(Callable<T> code, Class<E> exceptionClass) throws E {
		readLock.lock();
		try {
			return code.call();
		} catch (Throwable e) {
			if (exceptionClass.isAssignableFrom(e.getClass()))
				throw (E) e;
			exceptionHandler.accept(e);
			return null;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Executes the provided code block with a write lock, handling specific
	 * exceptions.
	 *
	 * @param <T>            the return type of the code block
	 * @param <E>            the type of exception to handle specifically
	 * @param code           the code block to execute under the write lock
	 * @param exceptionClass the class of exception to handle specifically
	 * @return the result of the code block execution, or null if an exception
	 *         occurs
	 * @throws E if an exception of type E occurs during execution
	 */
	@SuppressWarnings("unchecked")
	public <T, E extends Throwable> T writeLocked(Callable<T> code, Class<E> exceptionClass) throws E {
		writeLock.lock();
		try {
			return code.call();
		} catch (Throwable e) {
			if (exceptionClass.isAssignableFrom(e.getClass()))
				throw (E) e;
			exceptionHandler.accept(e);
			return null;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Executes the provided code block with a read lock, handling all exceptions
	 * through the configured exception handler.
	 *
	 * @param <T>  the return type of the code block
	 * @param code the code block to execute under the read lock
	 * @return the result of the code block execution, or null if an exception
	 *         occurs
	 */
	public <T> T readLocked(Callable<T> code) {
		readLock.lock();
		try {
			return code.call();
		} catch (Exception e) {
			exceptionHandler.accept(e);
			return null;
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Executes the provided code block with a write lock, handling all exceptions
	 * through the configured exception handler.
	 *
	 * @param <T>  the return type of the code block
	 * @param code the code block to execute under the write lock
	 * @return the result of the code block execution, or null if an exception
	 *         occurs
	 */
	public <T> T writeLocked(Callable<T> code) {
		writeLock.lock();
		try {
			return code.call();
		} catch (Exception e) {
			exceptionHandler.accept(e);
			return null;
		} finally {
			writeLock.unlock();
		}
	}
}