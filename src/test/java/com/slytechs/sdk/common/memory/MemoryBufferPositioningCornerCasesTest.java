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
package com.slytechs.sdk.common.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Corner case and extreme edge condition tests for MemoryBufferView
 * positioning.
 * 
 * <p>
 * This test class explores rare, extreme, and potentially problematic scenarios
 * that might not occur in normal usage but could reveal subtle bugs or
 * undefined behaviors in the positioning system.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryBufferView Positioning Corner Cases")
class MemoryBufferPositioningCornerCasesTest {

	private static final long BUFFER_SIZE = 1024;
	private ByteBuf buffer;
	private Arena arena;

	@BeforeEach
	void setUp() {
		arena = Arena.ofConfined();
		MemorySegment segment = arena.allocate(BUFFER_SIZE);

		// Create buffer without pool - data bounds match memory bounds
		buffer = new ByteBuf(segment); // Uses entire segment
	}

	// ==================== Extreme Value Tests ====================

	@Nested
	@DisplayName("Extreme Value Corner Cases")
	class ExtremeValueTests {

		@Test
		@DisplayName("Position at Long.MAX_VALUE should fail gracefully")
		void testPositionAtLongMaxValue() {
			buffer.position(Long.MAX_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Should remain unchanged
		}

		@Test
		@DisplayName("Limit at Long.MAX_VALUE should fail gracefully")
		void testLimitAtLongMaxValue() {
			buffer.limit(Long.MAX_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(BUFFER_SIZE, buffer.limit()); // Should remain unchanged
		}

		@Test
		@DisplayName("Skip by Long.MAX_VALUE should fail gracefully")
		void testSkipByLongMaxValue() {
			buffer.skip(Long.MAX_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Backup by Long.MAX_VALUE should fail gracefully")
		void testBackupByLongMaxValue() {
			buffer.position(100);
			buffer.backup(Long.MAX_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("AdjustPosition by Long.MAX_VALUE")
		void testAdjustPositionByLongMaxValue() {
			buffer.adjustPosition(Long.MAX_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("AdjustPosition by Long.MIN_VALUE")
		void testAdjustPositionByLongMinValue() {
			buffer.position(100);
			buffer.adjustPosition(Long.MIN_VALUE);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				Long.MIN_VALUE,
				Long.MIN_VALUE + 1,
				-9223372036854775807L
		})
		@DisplayName("Extreme negative position values")
		void testExtremeNegativePositions(long position) {
			buffer.position(position);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position());
		}
	}

	// ==================== Zero-Size Buffer Tests ====================

	@Nested
	@DisplayName("Zero-Size Buffer Corner Cases")
	class ZeroSizeBufferTests {

		private ByteBuf zeroBuffer;

		@BeforeEach
		void setUpZeroBuffer() {
			MemorySegment segment = arena.allocate(1); // Minimum allocation
			zeroBuffer = new ByteBuf(); 
			zeroBuffer.bind(Memory.of(segment, 0, 0)); // Zero-size buffer
		}

		@Test
		@DisplayName("Zero-size buffer initial state")
		void testZeroSizeInitialState() {
			assertEquals(0, zeroBuffer.capacity());
			assertEquals(0, zeroBuffer.limit());
			assertEquals(0, zeroBuffer.position());
			assertEquals(0, zeroBuffer.remaining());
			assertFalse(zeroBuffer.hasRemaining());
		}

		@Test
		@DisplayName("Operations on zero-size buffer")
		void testZeroSizeOperations() {
			zeroBuffer.flip();
			assertEquals(0, zeroBuffer.limit());
			assertEquals(0, zeroBuffer.position());

			zeroBuffer.clear();
			assertEquals(0, zeroBuffer.limit());
			assertEquals(0, zeroBuffer.position());

			zeroBuffer.rewind();
			assertEquals(0, zeroBuffer.position());
		}

		@Test
		@DisplayName("Mark/reset on zero-size buffer")
		void testZeroSizeMarkReset() {
			zeroBuffer.mark();
			zeroBuffer.reset();
			assertEquals(0, zeroBuffer.position());
			assertFalse(zeroBuffer.hasError());
		}

		@Test
		@DisplayName("Skip on zero-size buffer")
		void testZeroSizeSkip() {
			zeroBuffer.skip(1);
			assertTrue(zeroBuffer.hasError()); // Can't skip in zero-size buffer
			assertEquals(0, zeroBuffer.position());
		}
	}

	// ==================== Rapid State Changes ====================

	@Nested
	@DisplayName("Rapid State Change Corner Cases")
	class RapidStateChangeTests {

		@Test
		@DisplayName("Alternating position forward and backward rapidly")
		void testRapidPositionAlternation() {
			for (int i = 0; i < 1000; i++) {
				buffer.position(i % 100);
			}
			assertEquals(99, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Rapid mark/reset cycles")
		void testRapidMarkResetCycles() {
			@SuppressWarnings("unused")
			int lastValidMarkPos = -1;

			for (int i = 0; i < 1000; i++) {
				int markPos = i % 100;
				buffer.position(markPos);
				buffer.mark();

				int newPos = (markPos + 50) % 100;
				buffer.position(newPos);

				// Mark is only valid if newPos >= markPos
				// Otherwise mark gets invalidated when position goes below it
				if (newPos >= markPos) {
					buffer.reset(); // Goes back to markPos
					lastValidMarkPos = markPos;
					assertEquals(markPos, buffer.position());
				} else {
					// Mark was invalidated, reset would fail
					// Don't reset, just continue
					lastValidMarkPos = -1; // No valid mark
				}
			}

			// The final position depends on whether the last mark was valid
			// Let's just verify there's no error and we have a valid position
			assertFalse(buffer.hasError());
			assertTrue(buffer.position() >= 0 && buffer.position() <= BUFFER_SIZE);
		}

		@Test
		@DisplayName("Rapid flip/clear cycles")
		void testRapidFlipClearCycles() {
			for (int i = 0; i < 1000; i++) {
				buffer.position(100);
				buffer.flip();
				buffer.clear();
			}
			assertEquals(0, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Rapid limit changes with position adjustments")
		void testRapidLimitChanges() {
			for (int i = 1; i <= 100; i++) {
				buffer.limit(BUFFER_SIZE - i);
				// Position should be adjusted if it exceeds new limit
			}
			assertEquals(BUFFER_SIZE - 100, buffer.limit());
			assertTrue(buffer.position() <= buffer.limit());
		}
	}

	// ==================== Boundary Arithmetic Overflow ====================

	@Nested
	@DisplayName("Arithmetic Overflow Corner Cases")
	class ArithmeticOverflowTests {

		@Test
		@DisplayName("Position + skip causing overflow")
		void testPositionSkipOverflow() {
			buffer.position(BUFFER_SIZE - 10);
			buffer.skip(Long.MAX_VALUE - BUFFER_SIZE + 20);
			assertTrue(buffer.hasError());
			assertEquals(BUFFER_SIZE - 10, buffer.position());
		}

		@Test
		@DisplayName("AdjustPosition causing overflow with large positive delta")
		void testAdjustPositionOverflowPositive() {
			buffer.position(100);
			buffer.adjustPosition(Long.MAX_VALUE - 50);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("AdjustPosition causing underflow with large negative delta")
		void testAdjustPositionOverflowNegative() {
			buffer.position(100);
			buffer.adjustPosition(Long.MIN_VALUE + 50);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Skip with value near Long.MAX_VALUE")
		void testSkipNearMaxValue() {
			buffer.skip(Long.MAX_VALUE - 100);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Backup with value near Long.MAX_VALUE")
		void testBackupNearMaxValue() {
			buffer.position(500);
			buffer.backup(Long.MAX_VALUE - 100);
			assertTrue(buffer.hasError());
			assertEquals(500, buffer.position());
		}
	}

	// ==================== Mark Invalidation Edge Cases ====================

	@Nested
	@DisplayName("Mark Invalidation Edge Cases")
	class MarkInvalidationEdgeCases {

		@Test
		@DisplayName("Mark at exact position boundary")
		void testMarkAtExactBoundary() {
			buffer.position(100);
			buffer.mark();
			buffer.position(100); // Set to same position as mark
			buffer.reset();
			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Multiple operations that should preserve mark")
		void testMarkPreservationChain() {
			buffer.position(100);
			buffer.mark();

			// These operations should preserve mark
			buffer.position(101); // Forward movement
			buffer.limit(500); // Limit change above mark
			buffer.position(200); // Further forward

			buffer.reset();
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Mark invalidation by exactly 1 position backward")
		void testMarkInvalidationByOne() {
			buffer.position(100);
			buffer.mark();
			buffer.position(99); // One less than mark

			// Mark should be invalidated
			buffer.reset();
			assertTrue(buffer.hasError());
			assertEquals(99, buffer.position());
		}

		@Test
		@DisplayName("Mark at 0, position to 0")
		void testMarkAtZeroPositionToZero() {
			buffer.position(0);
			buffer.mark();
			buffer.position(100);
			buffer.position(0); // Back to mark position
			buffer.reset();

			assertEquals(0, buffer.position());
			assertFalse(buffer.hasError());
		}
	}

	// ==================== Error State Propagation ====================

	@Nested
	@DisplayName("Error State Propagation Corner Cases")
	class ErrorStatePropagationTests {

		@Test
		@DisplayName("Error state through complex operation chain")
		void testErrorPropagationChain() {
			buffer.position(-1); // Cause initial error
			assertTrue(buffer.hasError());

			// All subsequent operations should be no-ops
			long initialPos = buffer.position();
			long initialLimit = buffer.limit();

			buffer.position(100)
					.limit(500)
					.mark()
					.skip(50)
					.backup(25)
					.adjustPosition(10)
					.flip()
					.clear()
					.rewind();

			// Nothing should have changed due to error state
			assertEquals(initialPos, buffer.position());
			assertEquals(initialLimit, buffer.limit());
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Clear error and continue with complex operations")
		void testClearErrorMidChain() {
			buffer.position(-1); // Cause error
			assertTrue(buffer.hasError());

			buffer.clearError(); // Clear the error
			assertFalse(buffer.hasError());

			// Operations should work now
			buffer.position(100)
					.mark()
					.position(200)
					.reset();

			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Multiple error accumulation attempts")
		void testMultipleErrors() {
			buffer.position(-1); // First error
			BufferOperationException firstError = buffer.getError();

			buffer.limit(-1); // Attempt second error (should be ignored)
			BufferOperationException secondError = buffer.getError();

			assertSame(firstError, secondError); // Should be same error (first one preserved)
		}
	}

	// ==================== Position/Limit Interaction Edge Cases
	// ====================

	@Nested
	@DisplayName("Position/Limit Interaction Edge Cases")
	class PositionLimitInteractionTests {

		@Test
		@DisplayName("Set position and limit to same value repeatedly")
		void testPositionLimitSameValue() {
			for (int i = 0; i < 100; i += 10) {
				// Must set limit first if it's increasing, to allow position to be set
				if (i > buffer.limit()) {
					buffer.limit(i);
					buffer.position(i);
				} else {
					buffer.position(i);
					buffer.limit(i);
				}

				assertEquals(i, buffer.position());
				assertEquals(i, buffer.limit());
				assertEquals(0, buffer.remaining());
			}
		}

		@Test
		@DisplayName("Limit reduction cascading to position")
		void testLimitReductionCascade() {
			buffer.position(900);
			buffer.mark(); // Mark at 900

			// Reduce limit below position
			buffer.limit(500);

			assertEquals(500, buffer.position()); // Position adjusted
			assertEquals(500, buffer.limit());

			// Mark should be invalidated
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@ParameterizedTest
		@CsvSource({
				"0,0",
				"0,1",
				"1,1",
				"100,100",
				"100,101",
				"1023,1024",
				"1024,1024"
		})
		@DisplayName("Position/limit boundary combinations")
		void testPositionLimitBoundaries(long pos, long lim) {
			buffer.limit(lim);
			buffer.position(pos);

			assertTrue(buffer.position() <= buffer.limit());
			assertTrue(buffer.position() >= 0);
			assertTrue(buffer.limit() <= buffer.capacity());
			assertFalse(buffer.hasError());
		}
	}

	// ==================== Method Chaining Edge Cases ====================

	@Nested
	@DisplayName("Method Chaining Edge Cases")
	class MethodChainingEdgeCases {

		@Test
		@DisplayName("Extremely long method chain")
		void testExtremelyLongChain() {
			ByteBuf result = buffer;
			for (int i = 0; i < 1000; i++) {
				result = result.position(i % 100);
			}
			assertSame(buffer, result); // Should be same instance
			assertEquals(99, buffer.position());
		}

		@Test
		@DisplayName("Chain with alternating valid and invalid operations")
		void testAlternatingValidInvalidChain() {
			buffer.position(100) // Valid
					.position(-1) // Invalid - sets error
					.position(200) // No-op due to error
					.clearError() // Clear error
					.position(300); // Valid again

			assertEquals(300, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Null-safe chaining after error")
		void testNullSafeChaining() {
			ByteBuf result = buffer
					.position(-1) // Cause error
					.mark() // No-op
					.skip(100) // No-op
					.flip() // No-op
					.clear() // No-op
					.rewind(); // No-op

			assertNotNull(result);
			assertSame(buffer, result);
			assertTrue(buffer.hasError());
		}
	}

	// ==================== Segment Boundary Simulation ====================

	@Nested
	@DisplayName("Segment Boundary Simulation (Single-Segment Edge Cases)")
	class SegmentBoundarySimulation {

		@Test
		@DisplayName("Position at exact capacity boundary")
		void testPositionAtCapacityBoundary() {
			buffer.position(BUFFER_SIZE);
			assertEquals(BUFFER_SIZE, buffer.position());
			assertEquals(0, buffer.remaining());
			assertFalse(buffer.hasRemaining());
		}

		@Test
		@DisplayName("Operations at capacity boundary")
		void testOperationsAtCapacityBoundary() {
			buffer.position(BUFFER_SIZE);

			// These should fail or no-op
			buffer.skip(1);
			assertTrue(buffer.hasError());
			buffer.clearError();

			// These should work
			buffer.backup(1);
			assertEquals(BUFFER_SIZE - 1, buffer.position());

			buffer.position(BUFFER_SIZE);
			buffer.mark();
			buffer.reset();
			assertEquals(BUFFER_SIZE, buffer.position());
		}

		@Test
		@DisplayName("Rapid position changes near boundaries")
		void testRapidBoundaryPositioning() {
			// Rapidly alternate between near-start and near-end positions
			for (int i = 0; i < 100; i++) {
				if (i % 2 == 0) {
					buffer.position(i % 10); // Near start
				} else {
					buffer.position(BUFFER_SIZE - (i % 10)); // Near end
				}
			}
			assertFalse(buffer.hasError());
		}
	}

	// ==================== Unusual But Valid Patterns ====================

	@Nested
	@DisplayName("Unusual But Valid Pattern Tests")
	class UnusualPatternTests {

		@Test
		@DisplayName("Mark after flip pattern")
		void testMarkAfterFlip() {
			buffer.position(100);
			buffer.flip(); // limit=100, pos=0
			buffer.mark(); // Mark at 0
			buffer.position(50);
			buffer.reset();

			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());
		}

		@Test
		@DisplayName("Double flip with intermediate operations")
		void testDoubleFlipPattern() {
			buffer.position(100);
			buffer.flip(); // limit=100, pos=0
			buffer.position(50);
			buffer.flip(); // limit=50, pos=0

			assertEquals(0, buffer.position());
			assertEquals(50, buffer.limit());

			// Third flip
			buffer.position(25);
			buffer.flip(); // limit=25, pos=0

			assertEquals(0, buffer.position());
			assertEquals(25, buffer.limit());
		}

		@Test
		@DisplayName("Clear-flip-clear pattern")
		void testClearFlipClearPattern() {
			buffer.position(500);
			buffer.limit(600);
			buffer.mark();

			buffer.clear(); // pos=0, limit=capacity, mark=-1
			buffer.flip(); // limit=0, pos=0
			buffer.clear(); // pos=0, limit=capacity

			assertEquals(0, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());

			// Mark should be gone
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Skip-backup oscillation")
		void testSkipBackupOscillation() {
			long pos = 500;
			buffer.position(pos);

			// Oscillate position
			for (int i = 0; i < 100; i++) {
				buffer.skip(10).backup(10);
			}

			assertEquals(pos, buffer.position());
			assertFalse(buffer.hasError());
		}
	}
}