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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive test suite for MemoryBufferView positioning functions.
 * 
 * <p>
 * This test class validates all positioning operations including: position,
 * limit, mark/reset, clear, flip, rewind, and movement operations like skip,
 * backup, and adjustPosition.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryBufferView Positioning Tests")
class MemoryBufferPositioningTest {

	private static final long BUFFER_SIZE = 1024;
	private MemoryBuffer buffer;
	private Arena arena;

	@BeforeEach
	void setUp() {
		arena = Arena.ofConfined();
		MemorySegment segment = arena.allocate(BUFFER_SIZE);

		// Create buffer without pool - data bounds match memory bounds
		buffer = new MemoryBuffer(segment); // Uses entire segment
	}

	// ==================== Position Tests ====================

	@Nested
	@DisplayName("Position Operations")
	class PositionTests {

		@Test
		@DisplayName("Initial position should be 0")
		void testInitialPosition() {
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Set valid position")
		void testSetValidPosition() {
			buffer.position(100);
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Set position to limit")
		void testSetPositionToLimit() {
			buffer.limit(500);
			buffer.position(500);
			assertEquals(500, buffer.position());
		}

		@Test
		@DisplayName("Set position to 0")
		void testSetPositionToZero() {
			buffer.position(100);
			buffer.position(0);
			assertEquals(0, buffer.position());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				-1,
				-100,
				-Long.MAX_VALUE
		})
		@DisplayName("Setting negative position should set error")
		void testNegativePosition(long position) {
			buffer.position(position);
			assertTrue(buffer.hasError());
			assertNotNull(buffer.getError());
			assertEquals(0, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Setting position beyond limit should set error")
		void testPositionBeyondLimit() {
			buffer.limit(500);
			buffer.position(501);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Position changes should discard mark if mark > position")
		void testPositionChangeDiscardsInvalidMark() {
			buffer.position(100);
			buffer.mark();
			buffer.position(50); // Position < mark

			// Mark should be discarded, reset should fail
			buffer.reset();
			assertTrue(buffer.hasError());
			assertEquals(50, buffer.position());
		}

		@Test
		@DisplayName("Position preserved when mark is valid")
		void testPositionPreservesMark() {
			buffer.position(50);
			buffer.mark();
			buffer.position(100); // Position > mark
			buffer.reset();

			assertFalse(buffer.hasError());
			assertEquals(50, buffer.position()); // Reset to mark
		}

		@Test
		@DisplayName("Chained position operations")
		void testChainedPositions() {
			buffer.position(10)
					.position(20)
					.position(30);
			assertEquals(30, buffer.position());
		}
	}

	// ==================== Limit Tests ====================

	@Nested
	@DisplayName("Limit Operations")
	class LimitTests {

		@Test
		@DisplayName("Initial limit should equal capacity")
		void testInitialLimit() {
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Set valid limit")
		void testSetValidLimit() {
			buffer.limit(500);
			assertEquals(500, buffer.limit());
		}

		@Test
		@DisplayName("Set limit to 0")
		void testSetLimitToZero() {
			buffer.limit(0);
			assertEquals(0, buffer.limit());
		}

		@Test
		@DisplayName("Set limit to capacity")
		void testSetLimitToCapacity() {
			buffer.limit(100);
			buffer.limit(BUFFER_SIZE);
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				-1,
				-100,
				-Long.MAX_VALUE
		})
		@DisplayName("Setting negative limit should set error")
		void testNegativeLimit(long limit) {
			buffer.limit(limit);
			assertTrue(buffer.hasError());
			assertEquals(BUFFER_SIZE, buffer.limit()); // Limit unchanged
		}

		@Test
		@DisplayName("Setting limit beyond capacity should set error")
		void testLimitBeyondCapacity() {
			buffer.limit(BUFFER_SIZE + 1);
			assertTrue(buffer.hasError());
			assertEquals(BUFFER_SIZE, buffer.limit()); // Limit unchanged
		}

		@Test
		@DisplayName("Limit should adjust position if position > new limit")
		void testLimitAdjustsPosition() {
			buffer.position(500);
			buffer.limit(100);
			assertEquals(100, buffer.position()); // Position adjusted
			assertEquals(100, buffer.limit());
		}

		@Test
		@DisplayName("Limit should discard mark if mark > new limit")
		void testLimitDiscardsMark() {
			buffer.position(500);
			buffer.mark();
			buffer.position(600);
			buffer.limit(100);

			// Mark should be discarded
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Chained limit operations")
		void testChainedLimits() {
			buffer.limit(800)
					.limit(600)
					.limit(400);
			assertEquals(400, buffer.limit());
		}
	}

	// ==================== Remaining Tests ====================

	@Nested
	@DisplayName("Remaining Operations")
	class RemainingTests {

		@Test
		@DisplayName("Initial remaining equals capacity")
		void testInitialRemaining() {
			assertEquals(BUFFER_SIZE, buffer.remaining());
			assertTrue(buffer.hasRemaining());
		}

		@Test
		@DisplayName("Remaining after position change")
		void testRemainingAfterPosition() {
			buffer.position(100);
			assertEquals(BUFFER_SIZE - 100, buffer.remaining());
		}

		@Test
		@DisplayName("Remaining after limit change")
		void testRemainingAfterLimit() {
			buffer.limit(500);
			assertEquals(500, buffer.remaining());
		}

		@Test
		@DisplayName("Remaining with position and limit")
		void testRemainingWithPositionAndLimit() {
			buffer.position(100);
			buffer.limit(500);
			assertEquals(400, buffer.remaining());
		}

		@Test
		@DisplayName("No remaining when position equals limit")
		void testNoRemaining() {
			buffer.position(500);
			buffer.limit(500);
			assertEquals(0, buffer.remaining());
			assertFalse(buffer.hasRemaining());
		}

		@Test
		@DisplayName("hasRemaining boundary conditions")
		void testHasRemainingBoundaries() {
			buffer.limit(1);

			buffer.position(0);
			assertTrue(buffer.hasRemaining());

			buffer.position(1);
			assertFalse(buffer.hasRemaining());
		}
	}

	// ==================== Mark/Reset Tests ====================

	@Nested
	@DisplayName("Mark and Reset Operations")
	class MarkResetTests {

		@Test
		@DisplayName("Simple mark and reset")
		void testSimpleMarkReset() {
			buffer.position(100);
			buffer.mark();
			buffer.position(200);
			buffer.reset();

			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Reset without mark should set error")
		void testResetWithoutMark() {
			buffer.reset();
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Multiple marks - last one wins")
		void testMultipleMarks() {
			buffer.position(100);
			buffer.mark();
			buffer.position(200);
			buffer.mark();
			buffer.position(300);
			buffer.reset();

			assertEquals(200, buffer.position()); // Reset to last mark
		}

		@Test
		@DisplayName("Mark at position 0")
		void testMarkAtZero() {
			buffer.mark(); // Mark at 0
			buffer.position(100);
			buffer.reset();

			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Mark at limit")
		void testMarkAtLimit() {
			buffer.limit(500);
			buffer.position(500);
			buffer.mark(); // Mark at 500

			// Move to a position less than limit but still valid for reset
			// Note: moving to position < mark will discard the mark per java.nio.Buffer
			// spec
			buffer.position(400); // Position less than mark but won't discard it

			// Actually, ANY position less than mark will discard it per spec
			// So let's test differently - move forward from mark
			buffer.position(500); // Back to mark position
			buffer.mark(); // Re-mark at 500

			// Now we can only move to positions >= 500 to keep the mark
			// But 500 is the limit, so we can't go beyond
			// So reset should work when we're still at 500
			buffer.reset();

			assertEquals(500, buffer.position());
		}

		@Test
		@DisplayName("Chained mark and reset")
		void testChainedMarkReset() {
			buffer.position(100)
					.mark()
					.position(200)
					.reset();

			assertEquals(100, buffer.position());
		}
	}

	// ==================== Clear Tests ====================

	@Nested
	@DisplayName("Clear Operation")
	class ClearTests {

		@Test
		@DisplayName("Clear resets position to 0")
		void testClearResetsPosition() {
			buffer.position(100);
			buffer.clear();
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Clear sets limit to capacity")
		void testClearSetsLimit() {
			buffer.limit(500);
			buffer.clear();
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Clear discards mark")
		void testClearDiscardsMark() {
			buffer.position(100);
			buffer.mark();
			buffer.clear();

			// Mark should be discarded
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Clear does not clear error state")
		void testClearDoesNotClearError() {
			buffer.position(-1); // Cause error
			assertTrue(buffer.hasError());

			buffer.clear();
			assertTrue(buffer.hasError()); // Error still present
		}

		@Test
		@DisplayName("Chained clear operations")
		void testChainedClear() {
			buffer.position(100)
					.limit(500)
					.clear()
					.position(50);

			assertEquals(50, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Clear after various operations")
		void testClearAfterOperations() {
			// Setup complex state
			buffer.position(200);
			buffer.mark();
			buffer.position(400);
			buffer.limit(600);

			buffer.clear();

			assertEquals(0, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
			assertEquals(BUFFER_SIZE, buffer.remaining());
		}
	}

	// ==================== Flip Tests ====================

	@Nested
	@DisplayName("Flip Operation")
	class FlipTests {

		@Test
		@DisplayName("Flip sets limit to position and position to 0")
		void testBasicFlip() {
			buffer.position(100);
			buffer.flip();

			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());
		}

		@Test
		@DisplayName("Flip with position at 0")
		void testFlipAtZero() {
			buffer.flip();

			assertEquals(0, buffer.position());
			assertEquals(0, buffer.limit());
		}

		@Test
		@DisplayName("Flip with position at capacity")
		void testFlipAtCapacity() {
			buffer.position(BUFFER_SIZE);
			buffer.flip();

			assertEquals(0, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Flip discards mark")
		void testFlipDiscardsMark() {
			buffer.position(50);
			buffer.mark();
			buffer.position(100);
			buffer.flip();

			// Mark should be discarded
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Double flip")
		void testDoubleFlip() {
			buffer.position(100);
			buffer.flip(); // limit=100, pos=0
			buffer.position(50);
			buffer.flip(); // limit=50, pos=0

			assertEquals(0, buffer.position());
			assertEquals(50, buffer.limit());
		}

		@Test
		@DisplayName("Flip for write-then-read pattern")
		void testFlipWriteReadPattern() {
			// Simulate writing data
			buffer.position(100); // Wrote 100 bytes

			// Flip to prepare for reading
			buffer.flip();

			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());
			assertEquals(100, buffer.remaining()); // Ready to read 100 bytes
		}
	}

	// ==================== Rewind Tests ====================

	@Nested
	@DisplayName("Rewind Operation")
	class RewindTests {

		@Test
		@DisplayName("Rewind sets position to 0")
		void testBasicRewind() {
			buffer.position(100);
			buffer.rewind();
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Rewind preserves limit")
		void testRewindPreservesLimit() {
			buffer.limit(500);
			buffer.position(100);
			buffer.rewind();

			assertEquals(0, buffer.position());
			assertEquals(500, buffer.limit());
		}

		@Test
		@DisplayName("Rewind discards mark")
		void testRewindDiscardsMark() {
			buffer.position(50);
			buffer.mark();
			buffer.position(100);
			buffer.rewind();

			// Mark should be discarded
			buffer.reset();
			assertTrue(buffer.hasError());
		}

		@Test
		@DisplayName("Rewind when already at 0")
		void testRewindAtZero() {
			buffer.rewind();
			assertEquals(0, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Multiple rewinds")
		void testMultipleRewinds() {
			buffer.position(100);
			buffer.rewind();
			buffer.position(200);
			buffer.rewind();

			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Rewind for re-reading pattern")
		void testRewindForReReading() {
			buffer.limit(100);
			buffer.position(100); // Read all data

			// Rewind to re-read
			buffer.rewind();

			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());
			assertEquals(100, buffer.remaining()); // Ready to re-read
		}
	}

	// ==================== Skip Tests ====================

	@Nested
	@DisplayName("Skip Operation")
	class SkipTests {

		@Test
		@DisplayName("Skip forward within limit")
		void testSkipForward() {
			buffer.skip(100);
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Skip by 0")
		void testSkipZero() {
			buffer.position(50);
			buffer.skip(0);
			assertEquals(50, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Skip to exactly limit")
		void testSkipToLimit() {
			buffer.limit(500);
			buffer.skip(500);
			assertEquals(500, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Skip beyond limit should set error")
		void testSkipBeyondLimit() {
			buffer.limit(500);
			buffer.skip(501);
			assertTrue(buffer.hasError());
			assertEquals(0, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Skip negative amount should set error")
		void testSkipNegative() {
			buffer.position(100);
			buffer.skip(-10);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Multiple skips")
		void testMultipleSkips() {
			buffer.skip(10)
					.skip(20)
					.skip(30);
			assertEquals(60, buffer.position());
		}

		@Test
		@DisplayName("Skip with mark preservation")
		void testSkipPreservesMark() {
			buffer.mark();
			buffer.skip(100);
			buffer.reset();
			assertEquals(0, buffer.position()); // Mark at 0 preserved
		}
	}

	// ==================== Backup Tests ====================

	@Nested
	@DisplayName("Backup Operation")
	class BackupTests {

		@Test
		@DisplayName("Backup within bounds")
		void testBackupWithinBounds() {
			buffer.position(100);
			buffer.backup(50);
			assertEquals(50, buffer.position());
		}

		@Test
		@DisplayName("Backup by 0")
		void testBackupZero() {
			buffer.position(50);
			buffer.backup(0);
			assertEquals(50, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Backup to exactly 0")
		void testBackupToZero() {
			buffer.position(100);
			buffer.backup(100);
			assertEquals(0, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Backup beyond 0 should set error")
		void testBackupBeyondZero() {
			buffer.position(50);
			buffer.backup(51);
			assertTrue(buffer.hasError());
			assertEquals(50, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Backup negative amount should set error")
		void testBackupNegative() {
			buffer.position(100);
			buffer.backup(-10);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Multiple backups")
		void testMultipleBackups() {
			buffer.position(100);
			buffer.backup(10)
					.backup(20)
					.backup(30);
			assertEquals(40, buffer.position());
		}

		@Test
		@DisplayName("Backup invalidates mark if needed")
		void testBackupInvalidatesMark() {
			buffer.position(100);
			buffer.mark();
			buffer.position(150);
			buffer.backup(60); // Position now 90, less than mark at 100

			// Mark should have been discarded when position went below it
			// Attempting to reset should fail since mark is invalid
			buffer.reset();
			assertTrue(buffer.hasError()); // Mark was invalidated
			assertEquals(90, buffer.position()); // Position should remain at 90 after failed reset
		}
	}

	// ==================== AdjustPosition Tests ====================

	@Nested
	@DisplayName("AdjustPosition Operation")
	class AdjustPositionTests {

		@Test
		@DisplayName("Adjust position forward")
		void testAdjustForward() {
			buffer.position(100);
			buffer.adjustPosition(50);
			assertEquals(150, buffer.position());
		}

		@Test
		@DisplayName("Adjust position backward")
		void testAdjustBackward() {
			buffer.position(100);
			buffer.adjustPosition(-50);
			assertEquals(50, buffer.position());
		}

		@Test
		@DisplayName("Adjust by 0")
		void testAdjustZero() {
			buffer.position(100);
			buffer.adjustPosition(0);
			assertEquals(100, buffer.position());
			assertFalse(buffer.hasError());
		}

		@Test
		@DisplayName("Adjust to exactly 0")
		void testAdjustToZero() {
			buffer.position(100);
			buffer.adjustPosition(-100);
			assertEquals(0, buffer.position());
		}

		@Test
		@DisplayName("Adjust to exactly limit")
		void testAdjustToLimit() {
			buffer.limit(500);
			buffer.position(100);
			buffer.adjustPosition(400);
			assertEquals(500, buffer.position());
		}

		@Test
		@DisplayName("Adjust beyond limit should set error")
		void testAdjustBeyondLimit() {
			buffer.limit(500);
			buffer.position(100);
			buffer.adjustPosition(401);
			assertTrue(buffer.hasError());
			assertEquals(100, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Adjust below 0 should set error")
		void testAdjustBelowZero() {
			buffer.position(50);
			buffer.adjustPosition(-51);
			assertTrue(buffer.hasError());
			assertEquals(50, buffer.position()); // Position unchanged
		}

		@Test
		@DisplayName("Multiple adjustments")
		void testMultipleAdjustments() {
			buffer.position(100);
			buffer.adjustPosition(50) // 150
					.adjustPosition(-30) // 120
					.adjustPosition(80); // 200
			assertEquals(200, buffer.position());
		}
	}

	// ==================== Complex Scenarios ====================

	@Nested
	@DisplayName("Complex Positioning Scenarios")
	class ComplexScenarios {

		@Test
		@DisplayName("Write-flip-read pattern")
		void testWriteFlipReadPattern() {
			// Simulate writing
			buffer.position(100);

			// Flip for reading
			buffer.flip();
			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());

			// Simulate reading
			buffer.position(100);
			assertEquals(0, buffer.remaining());

			// Clear for next cycle
			buffer.clear();
			assertEquals(0, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Compact pattern simulation")
		void testCompactPattern() {
			// Setup: some data consumed, some remaining
			buffer.limit(500);
			buffer.position(200); // 200 bytes consumed, 300 remaining

			long remaining = buffer.remaining();

			// Simulate compact (would move data in real implementation)
			buffer.position(remaining);
			buffer.limit(BUFFER_SIZE);

			assertEquals(300, buffer.position());
			assertEquals(BUFFER_SIZE, buffer.limit());
		}

		@Test
		@DisplayName("Mark across multiple operations")
		void testMarkAcrossOperations() {
			buffer.position(100);
			buffer.mark();

			buffer.skip(50); // pos = 150
			assertEquals(150, buffer.position());

			buffer.backup(25); // pos = 125
			assertEquals(125, buffer.position());

			buffer.adjustPosition(75); // pos = 200
			assertEquals(200, buffer.position());

			buffer.reset(); // back to mark
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Error accumulation with positioning")
		void testErrorAccumulation() {
			buffer.position(-1); // First error
			assertTrue(buffer.hasError());

			// Subsequent operations become no-ops
			buffer.skip(100);
			buffer.flip();
			buffer.clear();

			assertTrue(buffer.hasError()); // Error persists
			assertEquals(0, buffer.position()); // Position unchanged from initial

			// Clear error and continue
			buffer.clearError();
			assertFalse(buffer.hasError());

			buffer.position(100);
			assertEquals(100, buffer.position());
		}

		@Test
		@DisplayName("Boundary conditions stress test")
		void testBoundaryConditions() {
			// Test all boundary positions
			buffer.position(0);
			assertEquals(0, buffer.position());

			buffer.position(BUFFER_SIZE);
			assertEquals(BUFFER_SIZE, buffer.position());

			buffer.limit(0);
			assertEquals(0, buffer.limit());
			assertEquals(0, buffer.position()); // Adjusted

			buffer.limit(BUFFER_SIZE);
			buffer.position(BUFFER_SIZE);
			assertEquals(0, buffer.remaining());

			buffer.rewind();
			assertEquals(BUFFER_SIZE, buffer.remaining());
		}

		@Test
		@DisplayName("Chain all positioning operations")
		void testChainAllOperations() {
			MemoryBuffer result = buffer
					.clear() // pos=0, limit=capacity
					.position(100) // pos=100
					.mark() // mark=100
					.skip(50) // pos=150
					.backup(25) // pos=125
					.adjustPosition(75) // pos=200
					.limit(500) // limit=500
					.reset() // pos=100 (back to mark)
					.flip() // limit=100, pos=0
					.rewind() // pos=0, limit=100
					.clearError(); // ensure no errors

			assertSame(buffer, result); // Verify chaining
			assertEquals(0, buffer.position());
			assertEquals(100, buffer.limit());
			assertFalse(buffer.hasError());
		}
	}
}