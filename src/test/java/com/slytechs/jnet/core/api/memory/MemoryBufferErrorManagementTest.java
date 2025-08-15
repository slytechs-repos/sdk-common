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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test suite for MemoryBuffer error management in normal operation scenarios.
 * 
 * <p>
 * This test class validates the error handling mechanisms including error
 * accumulation, propagation, clearing, and the various error handler methods
 * (hasError, getError, clearError, orElseThrow, onError, ifError).
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryBuffer Error Management - Normal Operations")
class MemoryBufferErrorManagementTest {

	private static final long BUFFER_SIZE = 1024;
	private MemoryByteBuffer buffer;
	private Arena arena;

	@BeforeEach
	void setUp() {
	    arena = Arena.ofConfined();
	    MemorySegment segment = arena.allocate(BUFFER_SIZE);
	    
	    // Create buffer without pool - data bounds match memory bounds
	    buffer = new MemoryByteBuffer(segment);  // Uses entire segment
	}

	// ==================== Basic Error State Tests ====================

	@Nested
	@DisplayName("Basic Error State Operations")
	class BasicErrorStateTests {

		@Test
		@DisplayName("Initial state has no error")
		void testInitialNoError() {
			assertFalse(buffer.hasError());
			assertNull(buffer.getError());
		}

		@Test
		@DisplayName("Error set after invalid operation")
		void testErrorAfterInvalidOperation() {
			buffer.position(-1);

			assertTrue(buffer.hasError());
			assertNotNull(buffer.getError());
			assertTrue(buffer.getError() instanceof BufferOperationException);
		}

		@Test
		@DisplayName("Error message contains useful information")
		void testErrorMessageContent() {
			buffer.position(-1);

			BufferOperationException error = buffer.getError();
			assertNotNull(error);
			assertNotNull(error.getMessage());
			assertTrue(error.getMessage().contains("Position out of bounds"));
			assertTrue(error.getMessage().contains("-1"));
		}

		@Test
		@DisplayName("Clear error restores normal operation")
		void testClearError() {
			// Cause an error
			buffer.position(-1);
			assertTrue(buffer.hasError());

			// Clear the error
			buffer.clearError();
			assertFalse(buffer.hasError());
			assertNull(buffer.getError());

			// Normal operations should work now
			buffer.position(100);
			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Clear error returns buffer for chaining")
		void testClearErrorChaining() {
			buffer.position(-1);

			MemoryBuffer result = buffer.clearError()
					.position(50)
					.mark();

			assertSame(buffer, result);
			assertEquals(50, buffer.position());
			assertFalse(buffer.hasError());
		}
	}

	// ==================== Error Accumulation Tests ====================

	@Nested
	@DisplayName("Error Accumulation and First-Error-Wins")
	class ErrorAccumulationTests {

		@Test
		@DisplayName("First error is preserved")
		void testFirstErrorPreserved() {
			buffer.position(-1); // First error
			BufferOperationException firstError = buffer.getError();

			buffer.limit(-1); // Second error attempt
			BufferOperationException secondError = buffer.getError();

			assertSame(firstError, secondError);
			assertTrue(firstError.getMessage().contains("Position")); // First error about position
			assertFalse(firstError.getMessage().contains("Limit")); // Not about limit
		}

		@Test
		@DisplayName("Multiple operations become no-ops after error")
		void testOperationsNoOpAfterError() {
		    // Verify initial state
		    assertEquals(0, buffer.position());
		    assertEquals(BUFFER_SIZE, buffer.limit(), "Initial limit should equal capacity");
		    assertFalse(buffer.hasError());
		    
		    // Set valid state
		    buffer.position(100);
		    buffer.limit(500);
		    assertEquals(100, buffer.position());
		    assertEquals(500, buffer.limit());
		    
		    // Cause error
		    buffer.position(-1);
		    assertTrue(buffer.hasError());
		    
		    // Position should remain at last valid value
		    assertEquals(100, buffer.position());
		    
		    // These should all be no-ops
		    buffer.position(200);
		    buffer.limit(600);
		    buffer.mark();
		    buffer.skip(50);
		    buffer.flip();
		    
		    // State should be unchanged from before the error
		    assertEquals(100, buffer.position());
		    assertEquals(500, buffer.limit());
		    assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Error accumulation through method chain")
		void testErrorInChain() {
		    // Verify initial state
		    assertEquals(0, buffer.position());
		    assertEquals(BUFFER_SIZE, buffer.limit());
		    assertFalse(buffer.hasError());
		    
		    // Set valid state
		    buffer.position(100).mark();
		    assertEquals(100, buffer.position());
		    
		    // Cause error in chain
		    MemoryBuffer result = buffer
		        .position(-1)      // Error here - position stays at 100
		        .skip(50)          // No-op
		        .limit(200)        // No-op
		        .flip();           // No-op
		    
		    assertSame(buffer, result);
		    assertTrue(buffer.hasError());
		    assertEquals(100, buffer.position());  // Position unchanged
		    assertEquals(BUFFER_SIZE, buffer.limit());  // Limit unchanged
		}

		@ParameterizedTest
		@ValueSource(ints = {
				1,
				5,
				10,
				20
		})
		@DisplayName("Multiple errors only preserve first")
		void testMultipleErrorsPreserveFirst(int errorCount) {
			List<BufferOperationException> errors = new ArrayList<>();

			for (int i = 0; i < errorCount; i++) {
				buffer.position(-i - 1); // Different negative values
				errors.add(buffer.getError());
			}

			// All should be the same first error
			BufferOperationException firstError = errors.get(0);
			for (BufferOperationException error : errors) {
				assertSame(firstError, error);
			}

			assertTrue(firstError.getMessage().contains("-1")); // First error value
		}
	}

	// ==================== orElseThrow Tests ====================

	@Nested
	@DisplayName("orElseThrow Operation")
	class OrElseThrowTests {

		@Test
		@DisplayName("orElseThrow does nothing when no error")
		void testOrElseThrowNoError() {
			buffer.position(100);

			assertDoesNotThrow(() -> buffer.orElseThrow());
			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("orElseThrow throws accumulated error")
		void testOrElseThrowWithError() {
			buffer.position(-1);

			BufferOperationException thrown = assertThrows(
					BufferOperationException.class,
					() -> buffer.orElseThrow());

			assertNotNull(thrown);
			assertTrue(thrown.getMessage().contains("Position out of bounds"));
		}

		@Test
		@DisplayName("orElseThrow clears error after throwing")
		void testOrElseThrowClearsError() {
			buffer.position(-1);
			assertTrue(buffer.hasError());

			assertThrows(BufferOperationException.class, () -> buffer.orElseThrow());

			// Error should be cleared after throwing
			assertFalse(buffer.hasError());
			assertNull(buffer.getError());

			// Normal operations should work
			buffer.position(100);
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("orElseThrow returns buffer for chaining when no error")
		void testOrElseThrowChaining() {
			MemoryBuffer result = buffer
					.position(100)
					.orElseThrow()
					.mark()
					.position(200)
					.orElseThrow();

			assertSame(buffer, result);
			assertEquals(200, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("orElseThrow in middle of chain with error")
		void testOrElseThrowMidChainWithError() {
			assertThrows(BufferOperationException.class, () -> buffer.position(100)
					.position(-1) // Error here
					.skip(50) // No-op
					.orElseThrow() // Throws here
					.position(200) // Never reached
			);

			// After exception, error should be cleared
			assertFalse(buffer.hasError());
			assertEquals(100, buffer.position()); // Last valid position
		}
	}

	// ==================== onError Handler Tests ====================

	@Nested
	@DisplayName("onError Handler Operation")
	class OnErrorHandlerTests {

		@Test
		@DisplayName("onError not called when no error")
		void testOnErrorNotCalledNoError() {
			AtomicBoolean handlerCalled = new AtomicBoolean(false);

			buffer.position(100)
					.onError((buf, error) -> {
						handlerCalled.set(true);
						throw new BufferOperationException("Should not be called");
					});

			assertFalse(handlerCalled.get());
			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("onError called with error and clears on success")
		void testOnErrorClearsOnSuccess() {
			AtomicBoolean handlerCalled = new AtomicBoolean(false);

			buffer.position(-1) // Cause error
					.onError((buf, error) -> {
						handlerCalled.set(true);
						assertNotNull(error);
						assertTrue(error.getMessage().contains("Position"));
						// Handler succeeds (no exception thrown)
					});

			assertTrue(handlerCalled.get());
			assertFalse(buffer.hasError()); // Error cleared
			assertNull(buffer.getError());
		}

		@Test
		@DisplayName("onError replaces error when handler throws")
		void testOnErrorReplacesError() {
			buffer.position(-1) // Original error
					.onError((buf, error) -> {
						throw new BufferOperationException("Transformed error: " + error.getMessage());
					});

			assertTrue(buffer.hasError());
			BufferOperationException newError = buffer.getError();
			assertNotNull(newError);
			assertTrue(newError.getMessage().contains("Transformed error"));
			assertTrue(newError.getMessage().contains("Position"));
		}

		@Test
		@DisplayName("onError allows recovery and continuation")
		void testOnErrorRecovery() {
			buffer.position(-1) // Cause error
					.onError((buf, error) -> {
						// Recovery: clear error and set valid position
						buf.clearError();
						buf.position(50);
					})
					.mark() // Should work after recovery
					.position(100);

			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("onError chaining with multiple handlers")
		void testMultipleOnErrorHandlers() {
			AtomicInteger handlerCount = new AtomicInteger(0);

			buffer.position(-1) // Cause error
					.onError((buf, error) -> {
						handlerCount.incrementAndGet();
						throw new BufferOperationException("Handler 1 failed");
					})
					.onError((buf, error) -> {
						handlerCount.incrementAndGet();
						// This handler succeeds
						assertTrue(error.getMessage().contains("Handler 1 failed"));
					});

			assertEquals(2, handlerCount.get());
			assertFalse(buffer.hasError()); // Second handler cleared error
		}

		@Test
		@DisplayName("onError with complex recovery logic")
		void testOnErrorComplexRecovery() {
			buffer.limit(100) // Small limit
					.position(95)
					.skip(10) // This will fail (would exceed limit)
					.onError((buf, error) -> {
						// Complex recovery: expand limit if needed
						if (error.getMessage().contains("Position adjustment out of bounds")) {
							buf.clearError();
							buf.limit(200); // Expand limit
							buf.position(105); // Set desired position
						}
					});

			assertEquals(105, buffer.position());
			assertEquals(200, buffer.limit());
			assertFalse(buffer.hasError());
		}
	}

	// ==================== ifError Monitor Tests ====================

	@Nested
	@DisplayName("ifError Monitor Operation")
	class IfErrorMonitorTests {

		@Test
		@DisplayName("ifError not called when no error")
		void testIfErrorNotCalledNoError() {
			AtomicBoolean monitorCalled = new AtomicBoolean(false);

			buffer.position(100)
					.ifError(error -> monitorCalled.set(true));

			assertFalse(monitorCalled.get());
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("ifError called but doesn't clear error")
		void testIfErrorDoesNotClearError() {
			AtomicBoolean monitorCalled = new AtomicBoolean(false);
			BufferOperationException observedError = null;

			buffer.position(-1) // Cause error
					.ifError(error -> {
						monitorCalled.set(true);
						assertNotNull(error);
					});

			assertTrue(monitorCalled.get());
			assertTrue(buffer.hasError()); // Error NOT cleared
			assertNotNull(buffer.getError());
		}

		@Test
		@DisplayName("Multiple ifError calls for monitoring")
		void testMultipleIfErrorMonitors() {
			List<String> messages = new ArrayList<>();

			buffer.position(-1) // Cause error
					.ifError(e -> messages.add("Monitor 1: " + e.getMessage()))
					.ifError(e -> messages.add("Monitor 2: " + e.getMessage()))
					.ifError(e -> messages.add("Monitor 3: " + e.getMessage()));

			assertEquals(3, messages.size());
			assertTrue(buffer.hasError()); // Error still present

			// All monitors saw the same error
			assertTrue(messages.stream().allMatch(m -> m.contains("Position out of bounds")));
		}

		@Test
		@DisplayName("ifError with logging simulation")
		void testIfErrorLogging() {
			List<String> logs = new ArrayList<>();
			Consumer<BufferOperationException> logger = e -> logs.add("[ERROR] " + e.getMessage());

			buffer.position(-1)
					.ifError(logger)
					.skip(10) // No-op due to error
					.ifError(logger) // Log again
					.clearError() // Now clear
					.position(100)
					.ifError(logger); // Should not be called

			assertEquals(2, logs.size()); // Only 2 logs before clear
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("ifError exception replaces current error")
		void testIfErrorExceptionReplacesError() {
			buffer.position(-1) // Original error
					.ifError(error -> {
						throw new RuntimeException("Monitor failed");
					});

			assertTrue(buffer.hasError());
			BufferOperationException newError = buffer.getError();
			assertTrue(newError.getMessage().contains("Monitor failed"));
		}

		@Test
		@DisplayName("ifError followed by onError pattern")
		void testIfErrorThenOnError() {
			List<String> events = new ArrayList<>();

			buffer.position(-1)
					.ifError(e -> events.add("Monitored: " + e.getMessage()))
					.ifError(e -> events.add("Logged: " + e.getMessage()))
					.onError((buf, e) -> {
						events.add("Handled: " + e.getMessage());
						// Recovery succeeds
					});

			assertEquals(3, events.size());
			assertFalse(buffer.hasError()); // onError cleared it

			// Verify order
			assertTrue(events.get(0).startsWith("Monitored"));
			assertTrue(events.get(1).startsWith("Logged"));
			assertTrue(events.get(2).startsWith("Handled"));
		}
	}

	// ==================== Error Propagation Tests ====================

	@Nested
	@DisplayName("Error Propagation Through Operations")
	class ErrorPropagationTests {

		@Test
		@DisplayName("Error propagates through positioning operations")
		void testErrorPropagationPositioning() {
			buffer.position(-1); // Initial error

			// Track which operations were attempted
			long initialPos = 0; // Buffer starts at 0
			long initialLimit = BUFFER_SIZE;

			buffer.position(100)
					.limit(500)
					.mark()
					.skip(50)
					.backup(25)
					.adjustPosition(10)
					.flip()
					.clear()
					.rewind();

			// All operations should have been no-ops
			assertEquals(initialPos, buffer.position());
			assertEquals(initialLimit, buffer.limit());
			assertTrue(buffer.hasError());

			// Original error still present
			assertTrue(buffer.getError().getMessage().contains("-1"));
		}

		@Test
		@DisplayName("Error propagates through space management")
		void testErrorPropagationSpaceManagement() {
			buffer.position(-1); // Initial error

			buffer.ensureRemaining(100)
					.compact();

			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Unchanged
		}

		@Test
		@DisplayName("Clear error breaks propagation chain")
		void testClearErrorBreaksPropagation() {
			buffer.position(-1) // Error
					.skip(50) // No-op
					.clearError() // Clear
					.skip(50) // Works now
					.mark(); // Works

			assertEquals(50, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Error state check with checkError method")
		void testCheckErrorMethod() {
			assertFalse(buffer.hasError());

			buffer.position(-1);
			assertTrue(buffer.hasError());

			// Even checking error doesn't clear it
			assertTrue(buffer.hasError());
			assertTrue(buffer.hasError()); // Can check multiple times
		}
	}

	// ==================== Integration Tests ====================

	@Nested
	@DisplayName("Error Management Integration Scenarios")
	class ErrorIntegrationTests {

		@Test
		@DisplayName("Complex error handling workflow")
		void testComplexErrorWorkflow() {
			List<String> events = new ArrayList<>();

			buffer.position(100)
					.mark()
					.position(-1) // Error occurs
					.ifError(e -> events.add("Error detected: " + e.getMessage()))
					.skip(50) // No-op
					.ifError(e -> events.add("Still in error state"))
					.onError((buf, e) -> {
						events.add("Attempting recovery");
						buf.clearError();
						buf.position(150);
					})
					.ifError(e -> events.add("Should not be called"))
					.skip(50); // Should work now

			assertEquals(200, buffer.position());
			assertFalse(buffer.hasError());
			assertEquals(3, events.size());
			assertFalse(events.contains("Should not be called"));
		}

		@Test
		@DisplayName("Error handling with mark/reset interaction")
		void testErrorWithMarkReset() {
			buffer.position(100)
					.mark()
					.position(200)
					.position(-1) // Error, but mark should still be valid at 100
					.onError((buf, e) -> {
						buf.clearError();
						buf.reset(); // Go back to mark
					});

			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Chained error handlers with different strategies")
		void testChainedErrorStrategies() {
			AtomicInteger recoveryAttempts = new AtomicInteger(0);

			buffer.limit(100)
					.position(90)
					.skip(20) // Will fail - would exceed limit
					.ifError(e -> System.out.println("Debug: " + e)) // Monitor
					.onError((buf, e) -> {
						recoveryAttempts.incrementAndGet();
						// First recovery attempt fails
						throw new BufferOperationException("Recovery failed");
					})
					.onError((buf, e) -> {
						recoveryAttempts.incrementAndGet();
						// Second recovery succeeds
						buf.clearError();
						buf.limit(200);
						buf.position(110);
					})
					.mark(); // Should work after successful recovery

			assertEquals(2, recoveryAttempts.get());
			assertEquals(110, buffer.position());
			assertEquals(200, buffer.limit());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Error accumulation with orElseThrow pattern")
		void testErrorAccumulationPattern() {
			// Typical usage pattern
			assertThrows(BufferOperationException.class, () -> buffer.position(100)
					.mark()
					.skip(50) // OK
					.position(-1) // Error
					.skip(50) // No-op
					.flip() // No-op
					.orElseThrow() // Throws here
			);

			// After throw, buffer should be usable
			buffer.position(200);
			assertEquals(200, buffer.position());
			assertFalse(buffer.hasError());
		}
	}
}