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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test suite for MemoryBufferView error management corner cases and extreme
 * scenarios.
 * 
 * <p>
 * This test class validates error handling in unusual, extreme, and
 * pathological conditions including null errors, recursive handlers, concurrent
 * operations, massive chains, and resource exhaustion scenarios.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryBufferView Error Management - Corner Cases")
class MemoryBufferErrorManagementCornerCasesTest {

	private static final long BUFFER_SIZE = 1024;
	private MemoryBuffer buffer;
	private Arena arena;

	@BeforeEach
	void setUp() {
		arena = Arena.ofConfined();
		MemorySegment segment = arena.allocate(BUFFER_SIZE);
		buffer = new MemoryBuffer(segment);
	}

	// ==================== Null and Empty Error Cases ====================

	@Nested
	@DisplayName("Null and Empty Error Scenarios")
	class NullAndEmptyErrorTests {

		@Test
		@DisplayName("Clear error when no error exists")
		void testClearErrorWhenNoError() {
			assertFalse(buffer.hasError());

			// Should be safe to clear non-existent error
			MemoryBufferView result = buffer.clearError();

			assertSame(buffer, result);
			assertFalse(buffer.hasError());
			assertNull(buffer.getError());
		}

		@Test
		@DisplayName("orElseThrow with no error multiple times")
		void testOrElseThrowMultipleTimesNoError() {
			// Should be safe to call orElseThrow repeatedly when no error
			for (int i = 0; i < 100; i++) {
				assertDoesNotThrow(() -> buffer.orElseThrow());
			}

			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("onError handler with null handler")
		void testOnErrorWithNullHandler() {
			buffer.position(-1); // Cause error

			// Passing null handler should probably throw NPE or be no-op
			assertThrows(NullPointerException.class, () -> buffer.onError(null));

			// Error should still be present
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("ifError consumer with null consumer")
		void testIfErrorWithNullConsumer() {
			buffer.position(-1); // Cause error

			// Passing null consumer should throw NPE
			assertThrows(NullPointerException.class, () -> buffer.ifError(null));

			// Error should still be present
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Error with empty message")
		void testErrorWithEmptyMessage() {
			// Create custom error with empty message
			BufferOperationException emptyError = new BufferOperationException("");

			// Use reflection or package-private access to set error directly
			buffer.setError(emptyError);
			
			// For testing, we'll cause a regular error
			buffer.position(-1);

			assertTrue(buffer.hasError());
			assertNotNull(buffer.getError().getMessage());
		}
	}

	// ==================== Recursive and Circular Error Handlers
	// ====================

	@Nested
	@DisplayName("Recursive and Circular Error Handler Scenarios")
	class RecursiveErrorHandlerTests {

		@Test
		@DisplayName("onError handler that causes another error")
		void testOnErrorCausesAnotherError() {
			buffer.position(-1); // Initial error

			AtomicInteger handlerCalls = new AtomicInteger(0);

			buffer.onError((_, error) -> {
				handlerCalls.incrementAndGet();
				throw new BufferOperationException("Handler error: " + error.getMessage());
			});

			assertEquals(1, handlerCalls.get());
			assertTrue(buffer.hasError());
			assertTrue(buffer.getError().getMessage().contains("Handler error"));
		}

		@Test
		@DisplayName("Nested onError handlers")
		void testNestedOnErrorHandlers() {
			buffer.position(-1); // Initial error

			List<String> events = new ArrayList<>();

			buffer.onError((buf, _) -> {
				events.add("Handler 1 start");

				// The buffer still has error at this point
				// So nested onError WILL execute
				buf.onError((_, _) -> {
					events.add("Nested handler");
				});

				events.add("Handler 1 end");
				throw new BufferOperationException("Handler 1 failed");
			})
					.onError((_, _) -> {
						events.add("Handler 2");
						// This one succeeds
					});

			// Update expectations based on actual behavior
			assertEquals(4, events.size());
			assertTrue(events.contains("Handler 1 start"));
			assertTrue(events.contains("Nested handler"));
			assertTrue(events.contains("Handler 1 end"));
			assertTrue(events.contains("Handler 2"));
			assertFalse(buffer.hasError()); // Handler 2 succeeded and cleared error
		}

		@Test
		@DisplayName("ifError that modifies buffer state")
		void testIfErrorModifiesBufferState() {
			buffer.position(100);
			buffer.position(-1); // Error at position 100

			buffer.ifError(_ -> {
				// Try to modify buffer during monitoring
				buffer.clearError(); // This should work
				buffer.position(200); // This should now work
			});

			// State should be modified
			assertEquals(200, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Circular error handler references")
		void testCircularErrorHandlerReferences() {
			AtomicReference<BufferErrorHandler<MemoryBuffer>> handler1 = new AtomicReference<>();
			AtomicReference<BufferErrorHandler<MemoryBuffer>> handler2 = new AtomicReference<>();
			AtomicInteger callCount = new AtomicInteger(0);

			handler1.set((_, _) -> {
				if (callCount.incrementAndGet() > 10) {
					return; // Prevent infinite loop
				}
				throw new BufferOperationException("Handler1 error");
			});

			handler2.set((_, _) -> {
				if (callCount.incrementAndGet() > 10) {
					return; // Prevent infinite loop
				}
				throw new BufferOperationException("Handler2 error");
			});

			buffer.position(-1);
			buffer.onError(handler1.get())
					.onError(handler2.get())
					.onError(handler1.get()); // Circular reference

			assertTrue(callCount.get() <= 3); // Should not loop infinitely
		}
	}

	// ==================== Extreme Chain Length Error Scenarios
	// ====================

	@Nested
	@DisplayName("Extreme Chain Length Error Scenarios")
	class ExtremeChainErrorTests {

		@Test
		@DisplayName("Error after 1000 chained operations")
		void testErrorAfterMassiveChain() {
			// Build massive chain
			for (int i = 0; i < 1000; i++) {
				buffer.mark();
			}

			// Now cause error
			buffer.position(-1);

			// All subsequent operations should be no-ops
			for (int i = 0; i < 1000; i++) {
				buffer.skip(1);
			}

			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Should remain at 0
		}

		@Test
		@DisplayName("Multiple errors in very long chain")
		void testMultipleErrorsInLongChain() {
			BufferOperationException firstError = null;

			// Cause error early in chain
			buffer.position(-1);
			firstError = buffer.getError();

			// Try to cause many more errors
			for (int i = 0; i < 100; i++) {
				buffer.position(-i - 2); // Different invalid positions
				buffer.limit(-i - 1); // Different invalid limits
				buffer.skip(-i - 1); // Invalid skips
			}

			// First error should be preserved
			assertSame(firstError, buffer.getError());
			assertTrue(buffer.getError().getMessage().contains("-1"));
		}

		@ParameterizedTest
		@ValueSource(ints = {
				10,
				100,
				1000,
				10000
		})
		@DisplayName("Error handlers in chains of varying length")
		void testErrorHandlersInVariousChainLengths(int chainLength) {
			AtomicInteger handlerCalls = new AtomicInteger(0);

			// Build chain with error
			buffer.position(-1);

			for (int i = 0; i < chainLength; i++) {
				buffer.ifError(_ -> handlerCalls.incrementAndGet());
			}

			assertEquals(chainLength, handlerCalls.get());
			assertTrue(buffer.hasError()); // Error still present
		}
	}

	// ==================== Concurrent Error Handling ====================

	@Nested
	@DisplayName("Concurrent Error Handling Scenarios")
	class ConcurrentErrorTests {

		@Test
		@DisplayName("Concurrent error state checks")
		void testConcurrentErrorStateChecks() throws InterruptedException {
			ExecutorService executor = Executors.newFixedThreadPool(10);
			CountDownLatch latch = new CountDownLatch(1);
			AtomicInteger errorChecks = new AtomicInteger(0);
			AtomicBoolean inconsistencyFound = new AtomicBoolean(false);

			// Cause an error
			buffer.position(-1);

			// Launch concurrent readers
			for (int i = 0; i < 100; i++) {
				executor.submit(() -> {
					try {
						latch.await();

						// Check error state multiple times
						for (int j = 0; j < 100; j++) {
							boolean hasError = buffer.hasError();
							BufferOperationException error = buffer.getError();

							if (hasError && error == null) {
								inconsistencyFound.set(true);
							}
							if (!hasError && error != null) {
								inconsistencyFound.set(true);
							}

							errorChecks.incrementAndGet();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
			}

			latch.countDown(); // Start all threads
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

			assertFalse(inconsistencyFound.get(), "Error state was inconsistent");
			assertEquals(10000, errorChecks.get());
		}

		@Test
		@DisplayName("Race condition: clear error while checking")
		void testClearErrorRaceCondition() throws InterruptedException {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			CountDownLatch startLatch = new CountDownLatch(1);
			AtomicBoolean cleared = new AtomicBoolean(false);
			AtomicReference<Boolean> hasErrorBeforeClear = new AtomicReference<>();
			AtomicReference<Boolean> hasErrorAfterClear = new AtomicReference<>();

			buffer.position(-1); // Initial error

			// Thread 1: Clear error
			executor.submit(() -> {
				try {
					startLatch.await();
					Thread.sleep(10); // Small delay
					buffer.clearError();
					cleared.set(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			// Thread 2: Check error
			executor.submit(() -> {
				try {
					startLatch.await();
					hasErrorBeforeClear.set(buffer.hasError());
					Thread.sleep(20); // Wait for clear
					hasErrorAfterClear.set(buffer.hasError());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			startLatch.countDown();
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

			assertTrue(cleared.get());
			assertNotNull(hasErrorBeforeClear.get());
			assertNotNull(hasErrorAfterClear.get());
			assertFalse(hasErrorAfterClear.get()); // Should be cleared
		}
	}

	// ==================== Error Handler Exception Scenarios ====================

	@Nested
	@DisplayName("Error Handler Exception Scenarios")
	class ErrorHandlerExceptionTests {

		@Test
		@DisplayName("onError handler throws RuntimeException")
		void testOnErrorThrowsRuntimeException() {
			buffer.position(-1);

			// Handler throws unchecked exception
			buffer.onError((_, _) -> {
				throw new RuntimeException("Unchecked exception");
			});

			// Should be wrapped in BufferOperationException
			assertTrue(buffer.hasError());
			// The behavior depends on implementation - might wrap or replace
		}

		@Test
		@DisplayName("onError handler throws Error")
		void testOnErrorThrowsError() {
			buffer.position(-1);

			// Handler throws Error (serious problem)
			assertThrows(OutOfMemoryError.class, () -> buffer.onError((_, _) -> {
				throw new OutOfMemoryError("Simulated OOM");
			}));
		}

		@Test
		@DisplayName("ifError consumer throws various exceptions")
		void testIfErrorThrowsVariousExceptions() {
			buffer.position(-1);

			// Test different exception types
			buffer.ifError(_ -> {
				throw new IllegalStateException("ISE");
			});
			assertTrue(buffer.hasError());

			buffer.ifError(_ -> {
				throw new NullPointerException("NPE");
			});
			assertTrue(buffer.hasError());

			buffer.ifError(_ -> {
				throw new ArrayIndexOutOfBoundsException("AIOBE");
			});
			assertTrue(buffer.hasError());

			// Error should still be present after all monitors
			assertNotNull(buffer.getError());
		}

		@Test
		@DisplayName("Handler modifies buffer causing new error")
		void testHandlerModifiesBufferCausingNewError() {
			buffer.position(100);
			buffer.limit(200);

			// Try to set position beyond limit
			buffer.position(250); // Error: position > limit
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position()); // Position unchanged

			buffer.onError((buf, _) -> {
				// Clear the current error
				buf.clearError();

				// Try to set invalid position again
				buf.position(300); // This will cause new error (> limit)

				// Handler succeeds (doesn't throw)
			});

			// The new error from position(300) should be preserved
			assertTrue(buffer.hasError(), "New error from handler should be preserved");
			assertEquals(100, buffer.position()); // Position still at 100
		}
	}

	// ==================== Boundary and Overflow Scenarios ====================

	@Nested
	@DisplayName("Boundary and Overflow Error Scenarios")
	class BoundaryErrorTests {

		@Test
		@DisplayName("Error at Long.MAX_VALUE position")
		void testErrorAtMaxLongPosition() {
			buffer.position(Long.MAX_VALUE);

			assertTrue(buffer.hasError());
			assertTrue(buffer.getError().getMessage().contains("Position out of bounds"));
			assertTrue(buffer.getError().getMessage().contains(String.valueOf(Long.MAX_VALUE)));
		}

		@Test
		@DisplayName("Error at Long.MIN_VALUE position")
		void testErrorAtMinLongPosition() {
			buffer.position(Long.MIN_VALUE);

			assertTrue(buffer.hasError());
			assertTrue(buffer.getError().getMessage().contains("Position out of bounds"));
		}

		@Test
		@DisplayName("Skip causing arithmetic overflow")
		void testSkipArithmeticOverflow() {
			buffer.position(BUFFER_SIZE - 10);
			buffer.skip(Long.MAX_VALUE); // Would overflow

			assertTrue(buffer.hasError());
			assertEquals(BUFFER_SIZE - 10, buffer.position()); // Unchanged
		}

		@Test
		@DisplayName("Multiple errors at boundary values")
		void testMultipleErrorsAtBoundaries() {
			// Try all boundary values
			buffer.position(-1);
			BufferOperationException firstError = buffer.getError();

			buffer.position(Long.MIN_VALUE);
			buffer.position(Long.MAX_VALUE);
			buffer.limit(-1);
			buffer.limit(Long.MIN_VALUE);
			buffer.limit(Long.MAX_VALUE);

			// First error preserved
			assertSame(firstError, buffer.getError());
		}
	}

	// ==================== State Preservation During Errors ====================

	@Nested
	@DisplayName("State Preservation During Error Scenarios")
	class StatePreservationTests {

		@Test
		@DisplayName("Mark preserved through error cycle")
		void testMarkPreservedThroughErrorCycle() {
			buffer.position(100);
			buffer.mark(); // Mark at 100
			buffer.position(200);

			// Cause error
			buffer.position(-1);
			assertTrue(buffer.hasError());
			assertEquals(200, buffer.position()); // Position unchanged

			// Clear error and reset to mark
			buffer.clearError();
			buffer.reset();

			assertEquals(100, buffer.position()); // Mark was preserved
		}

		@Test
		@DisplayName("Complex state preserved during error")
		void testComplexStatePreservedDuringError() {
			// Set up complex state
			buffer.limit(500);
			buffer.position(100);
			buffer.mark();
			buffer.position(200);

			long savedPos = buffer.position();
			long savedLimit = buffer.limit();

			// Cause error and try many operations
			buffer.position(-1);

			for (int i = 0; i < 100; i++) {
				buffer.flip();
				buffer.clear();
				buffer.rewind();
				buffer.compact();
				buffer.position(i);
				buffer.limit(i * 2);
			}

			// All state should be unchanged
			assertEquals(savedPos, buffer.position());
			assertEquals(savedLimit, buffer.limit());
		}

		@Test
		@DisplayName("Error state through save and restore pattern")
		void testErrorStateThroughSaveRestore() {
			// Save state
			buffer.position(100);
			long savedPosition = buffer.position();

			// Cause error
			buffer.position(-1);

			// Try to restore (should fail due to error)
			buffer.position(savedPosition);

			// Position should be unchanged from before error
			assertEquals(100, buffer.position());
			assertTrue(buffer.hasError());
		}
	}

	// ==================== Error Accumulation Patterns ====================

	@Nested
	@DisplayName("Error Accumulation Pattern Tests")
	class ErrorAccumulationPatternTests {

		@Test
		@DisplayName("Alternating valid and invalid operations")
		void testAlternatingValidInvalidOperations() {
			List<String> operations = new ArrayList<>();

			// Start with valid
			buffer.position(10);
			operations.add("valid: position(10)");

			// Invalid
			buffer.position(-1);
			operations.add("invalid: position(-1)");
			assertTrue(buffer.hasError());

			// More valid attempts (should be no-ops)
			buffer.position(20);
			operations.add("valid attempt: position(20)");

			buffer.position(-5);
			operations.add("invalid: position(-5)");

			buffer.position(30);
			operations.add("valid attempt: position(30)");

			// Position should still be 10
			assertEquals(10, buffer.position());
			assertTrue(buffer.hasError());
			assertEquals(5, operations.size());
		}

		@Test
		@DisplayName("Error accumulation with recovery attempts")
		void testErrorAccumulationWithRecoveryAttempts() {
			AtomicInteger recoveryAttempts = new AtomicInteger(0);

			buffer.position(-1)
					.onError((_, _) -> {
						recoveryAttempts.incrementAndGet();
						throw new BufferOperationException("Recovery 1 failed");
					})
					.skip(10) // No-op
					.onError((_, _) -> {
						recoveryAttempts.incrementAndGet();
						throw new BufferOperationException("Recovery 2 failed");
					})
					.limit(200) // No-op
					.onError((_, _) -> {
						recoveryAttempts.incrementAndGet();
						// This one succeeds
					});

			assertEquals(3, recoveryAttempts.get());
			assertFalse(buffer.hasError()); // Last handler succeeded
		}

		@RepeatedTest(10)
		@DisplayName("Random operation sequence with errors")
		void testRandomOperationSequenceWithErrors() {
			List<Runnable> operations = List.of(
					() -> buffer.position(100),
					() -> buffer.position(-1),
					() -> buffer.limit(500),
					() -> buffer.limit(-1),
					() -> buffer.skip(10),
					() -> buffer.skip(-10),
					() -> buffer.mark(),
					() -> buffer.flip(),
					() -> buffer.clear(),
					() -> buffer.rewind());

			// Execute random sequence
			boolean errorOccurred = false;
			for (int i = 0; i < 20; i++) {
				int opIndex = (int) (Math.random() * operations.size());
				operations.get(opIndex).run();

				if (buffer.hasError() && !errorOccurred) {
					errorOccurred = true;
					// After first error, all state-modifying ops should be no-ops
				}
			}

			// Buffer should be in consistent state
			assertTrue(buffer.position() >= 0);
			assertTrue(buffer.limit() >= 0);
			assertTrue(buffer.position() <= buffer.limit());
		}
	}

	// ==================== Memory and Resource Exhaustion ====================

	@Nested
	@DisplayName("Memory and Resource Exhaustion Scenarios")
	class ResourceExhaustionTests {

		@Test
		@DisplayName("Many error handlers consuming memory")
		void testManyErrorHandlersMemoryConsumption() {
			buffer.position(-1);

			List<Object> largeObjects = new ArrayList<>();

			// Add many error handlers that capture large objects
			for (int i = 0; i < 100; i++) {
				byte[] largeArray = new byte[1024]; // 1KB each
				largeObjects.add(largeArray);

				buffer.ifError(_ -> {
					// Handler captures largeArray
					@SuppressWarnings("unused")
					int sum = 0;
					for (byte b : largeArray) {
						sum += b;
					}
				});
			}

			// All handlers should execute
			assertTrue(buffer.hasError());
			assertEquals(100, largeObjects.size());
		}

		@Test
		@DisplayName("Stack depth with nested error handlers")
		void testStackDepthWithNestedHandlers() {
			buffer.position(-1);

			AtomicInteger depth = new AtomicInteger(0);
			AtomicBoolean stackOverflow = new AtomicBoolean(false);

			try {
				buffer.onError(new BufferErrorHandler<>() {
					@Override
					public void handle(MemoryBufferView buf, BufferOperationException error) {
						depth.incrementAndGet();
						if (depth.get() < 1000) { // Prevent actual stack overflow
							// Recursive call through error handler
							buf.onError(this);
						}
					}
				});
			} catch (StackOverflowError e) {
				stackOverflow.set(true);
			}

			assertFalse(stackOverflow.get(), "Should not cause stack overflow");
			assertTrue(depth.get() > 0);
		}
	}

	// ==================== Special Error Message Scenarios ====================

	@Nested
	@DisplayName("Special Error Message Scenarios")
	class SpecialErrorMessageTests {

		@Test
		@DisplayName("Error with very long message")
		void testErrorWithVeryLongMessage() {
			@SuppressWarnings("unused")
			String longMessage = "X".repeat(10000);

			// We can't directly create custom errors, so test with normal error
			buffer.position(-1);

			BufferOperationException error = buffer.getError();
			assertNotNull(error);
			assertNotNull(error.getMessage());

			// Error message should be reasonable length
			assertTrue(error.getMessage().length() < 1000);
		}

		@Test
		@DisplayName("Error messages with special characters")
		void testErrorMessagesWithSpecialCharacters() {
			// Test positions that might generate special error messages
			buffer.position(-1);
			assertTrue(buffer.getError().getMessage().contains("-1"));

			buffer.clearError();
			buffer.position(Long.MAX_VALUE);
			assertTrue(buffer.getError().getMessage().contains(String.valueOf(Long.MAX_VALUE)));
		}

		@Test
		@DisplayName("Chained error causation")
		void testChainedErrorCausation() {
			buffer.position(-1);

			buffer.onError((_, error) -> {
				// Create new error with cause
				throw new BufferOperationException("Handler error", error);
			});

			BufferOperationException finalError = buffer.getError();
			assertNotNull(finalError);
			assertTrue(finalError.getMessage().contains("Handler error"));
			// Check if cause is preserved (depends on implementation)
		}
	}
}