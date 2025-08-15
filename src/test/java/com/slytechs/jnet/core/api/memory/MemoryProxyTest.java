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
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive test suite for MemoryProxy operations.
 * 
 * Tests the minimalist proxy design with both unbounded and bounded binding
 * modes, delegation behavior, reference counting, and error handling.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 */
@DisplayName("MemoryProxy Tests")
class MemoryProxyTest {

	private static final long SEGMENT_SIZE = 1024;
	private Arena arena;
	private MemorySegment segment;
	private Memory baseMemory;
	private MemoryProxy proxy;

	@BeforeEach
	void setUp() {
		arena = Arena.ofConfined();
		segment = arena.allocate(SEGMENT_SIZE);
		baseMemory = Memory.of(segment, 0, SEGMENT_SIZE);
		proxy = new MemoryProxy();
	}

	// ==================== Basic Proxy State Tests ====================

	@Nested
	@DisplayName("Proxy State Management")
	class ProxyStateTests {

		@Test
		@DisplayName("Initial state is unbound")
		void testInitialState() {
			assertFalse(proxy.isBound());
			assertNull(proxy.getBoundMemory());
		}

		@Test
		@DisplayName("State after binding")
		void testStateAfterBinding() {
			proxy.bindMemory(baseMemory, 0);

			assertTrue(proxy.isBound());
			assertSame(baseMemory, proxy.getBoundMemory());
		}

		@Test
		@DisplayName("State after unbinding")
		void testStateAfterUnbinding() {
			proxy.bindMemory(baseMemory, 0);
			proxy.unbindMemory();

			assertFalse(proxy.isBound());
			assertNull(proxy.getBoundMemory());
		}

		@Test
		@DisplayName("Cannot double bind")
		void testCannotDoubleBind() {
			proxy.bindMemory(baseMemory, 0);

			assertThrows(IllegalStateException.class,
					() -> proxy.bindMemory(baseMemory, 100));
		}

		@Test
		@DisplayName("Cannot unbind when not bound")
		void testCannotUnbindWhenNotBound() {
			assertThrows(IllegalStateException.class,
					() -> proxy.unbindMemory());
		}

		@Test
		@DisplayName("Can rebind after unbinding")
		void testRebindAfterUnbind() {
			proxy.bindMemory(baseMemory, 0);
			proxy.unbindMemory();

			// Should be able to bind again
			assertDoesNotThrow(() -> proxy.bindMemory(baseMemory, 100));
			assertTrue(proxy.isBound());
		}

		@Test
		@DisplayName("Operations fail when unbound")
		void testOperationsFailWhenUnbound() {
			assertThrows(IllegalStateException.class, () -> proxy.segmentOffset());
			assertThrows(IllegalStateException.class, () -> proxy.segmentSize());
			assertThrows(IllegalStateException.class, () -> proxy.asByteBuffer());
			assertThrows(IllegalStateException.class, () -> proxy.asMemorySegment());
		}

	}

	// ==================== Unbounded Binding Tests ====================

	@Nested
	@DisplayName("Unbounded Binding (Full View)")
	class UnboundedBindingTests {

		@Test
		@DisplayName("Unbounded from offset 0")
		void testUnboundedFromZero() {
			proxy.bindMemory(baseMemory, 0);

			assertEquals(baseMemory.segmentOffset(), proxy.segmentOffset());
			assertEquals(baseMemory.segmentEnd(), proxy.segmentEnd());
			assertEquals(baseMemory.segmentSize(), proxy.segmentSize());
		}

		@Test
		@DisplayName("Unbounded from offset 100")
		void testUnboundedFromOffset() {
			proxy.bindMemory(baseMemory, 100);

			assertEquals(baseMemory.segmentOffset() + 100, proxy.segmentOffset());
			assertEquals(baseMemory.segmentEnd(), proxy.segmentEnd());
			assertEquals(SEGMENT_SIZE - 100, proxy.segmentSize());
		}

		@Test
		@DisplayName("Chain navigation works for unbounded")
		void testUnboundedChainNavigation() {
			// Create chain
			Memory second = Memory.of(arena.allocate(512), 0, 512);
			baseMemory.setNextMemory(second);

			proxy.bindMemory(baseMemory, 0);

			assertTrue(proxy.hasNextSegment());
			assertSame(second, proxy.nextSegment());
			assertEquals(2, proxy.segmentCount());
		}

		@Test
		@DisplayName("Chain data length for unbounded")
		void testUnboundedChainDataLength() {
			proxy.bindMemory(baseMemory, 100);

			// Should be from offset to end
			assertEquals(SEGMENT_SIZE - 100, proxy.totalActiveBytes());
		}

		@Test
		@DisplayName("Unbounded with chain calculates total")
		void testUnboundedWithChainTotal() {
			Memory second = Memory.of(arena.allocate(512), 0, 512);
			baseMemory.setNextMemory(second);

			proxy.bindMemory(baseMemory, 100);

			// Should be (SEGMENT_SIZE - 100) + 512
			assertEquals(SEGMENT_SIZE - 100 + 512, proxy.totalActiveBytes());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				0,
				50,
				100,
				500,
				SEGMENT_SIZE - 1
		})
		@DisplayName("Unbounded from various offsets")
		void testUnboundedVariousOffsets(long offset) {
			proxy.bindMemory(baseMemory, offset);

			assertEquals(baseMemory.segmentOffset() + offset, proxy.segmentOffset());
			assertEquals(SEGMENT_SIZE - offset, proxy.segmentSize());
		}
	}

	// ==================== Bounded Binding Tests ====================

	@Nested
	@DisplayName("Bounded Binding (Slice View)")
	class BoundedBindingTests {

		@Test
		@DisplayName("Bounded slice at start")
		void testBoundedAtStart() {
			proxy.bindMemory(baseMemory, 0, 100);

			assertEquals(baseMemory.segmentOffset(), proxy.segmentOffset());
			assertEquals(baseMemory.segmentOffset() + 100, proxy.segmentEnd());
			assertEquals(100, proxy.segmentSize());
		}

		@Test
		@DisplayName("Bounded slice in middle")
		void testBoundedInMiddle() {
			proxy.bindMemory(baseMemory, 200, 300);

			assertEquals(baseMemory.segmentOffset() + 200, proxy.segmentOffset());
			assertEquals(baseMemory.segmentOffset() + 500, proxy.segmentEnd());
			assertEquals(300, proxy.segmentSize());
		}

		@Test
		@DisplayName("No chain navigation for bounded")
		void testBoundedNoChainNavigation() {
			// Create chain
			Memory second = Memory.of(arena.allocate(512), 0, 512);
			baseMemory.setNextMemory(second);

			proxy.bindMemory(baseMemory, 0, 100);

			assertFalse(proxy.hasNextSegment());
			assertNull(proxy.nextSegment());
			assertEquals(1, proxy.segmentCount());
		}

		@Test
		@DisplayName("Chain data length for bounded")
		void testBoundedChainDataLength() {
			proxy.bindMemory(baseMemory, 100, 200);

			// Should be just the bounded length
			assertEquals(200, proxy.totalActiveBytes());
		}

		@Test
		@DisplayName("Cannot set next memory for bounded view")
		void testBoundedCannotSetNext() {
			proxy.bindMemory(baseMemory, 0, 100);
			Memory another = Memory.of(arena.allocate(256), 0, 256);

			assertThrows(UnsupportedOperationException.class,
					() -> proxy.setNextMemory(another));
		}

		@Test
		@DisplayName("Bounded slice of size 0")
		void testBoundedZeroSize() {
			proxy.bindMemory(baseMemory, 100, 0);

			assertEquals(100, proxy.segmentOffset());
			assertEquals(100, proxy.segmentEnd());
			assertEquals(0, proxy.segmentSize());
			assertEquals(0, proxy.totalActiveBytes());
		}

		@ParameterizedTest
		@ValueSource(longs = {
				1,
				10,
				100,
				256,
				512
		})
		@DisplayName("Bounded slices of various sizes")
		void testBoundedVariousSizes(long size) {
			proxy.bindMemory(baseMemory, 0, size);

			assertEquals(size, proxy.segmentSize());
			assertEquals(size, proxy.totalActiveBytes());
		}
	}

	// ==================== Delegation Tests ====================

	@Nested
	@DisplayName("Delegation Behavior")
	class DelegationTests {

		@Test
		@DisplayName("AsMemorySegment delegates with offset")
		void testAsMemorySegmentDelegation() {
			proxy.bindMemory(baseMemory, 100);

			MemorySegment proxySegment = proxy.asMemorySegment();
			MemorySegment expectedSegment = baseMemory.asMemorySegment().asSlice(100);

			assertEquals(expectedSegment.address(), proxySegment.address());
			assertEquals(expectedSegment.byteSize(), proxySegment.byteSize());
		}

		@Test
		@DisplayName("AsMemorySegmentAt delegates with adjustment")
		void testAsMemorySegmentAtDelegation() {
			proxy.bindMemory(baseMemory, 100);

			MemorySegment segAt50 = proxy.asMemorySegmentAt(50);
			MemorySegment expected = baseMemory.asMemorySegmentAt(150);

			assertEquals(expected.address(), segAt50.address());
		}

		@Test
		@DisplayName("SeekMemory delegates with offset")
		void testSeekMemoryDelegation() {
			proxy.bindMemory(baseMemory, 100);

			Memory sought = proxy.seekSegment(50);
			Memory expected = baseMemory.seekSegment(150);

			assertSame(expected, sought);
		}

		@Test
		@DisplayName("ByteBuffer creation for unbounded")
		void testByteBufferUnbounded() {
			proxy.bindMemory(baseMemory, 100);

			ByteBuffer bb = proxy.asByteBuffer();

			assertEquals(0, bb.position());
			assertEquals(SEGMENT_SIZE - 100, bb.capacity());
		}

		@Test
		@DisplayName("ByteBuffer creation for bounded")
		void testByteBufferBounded() {
			proxy.bindMemory(baseMemory, 100, 200);

			ByteBuffer bb = proxy.asByteBuffer();

			assertEquals(0, bb.position());
			assertEquals(200, bb.capacity());
		}

		@Test
		@DisplayName("Null and pointer checks delegate")
		void testNullPointerDelegation() {
			proxy.bindMemory(baseMemory, 0);

			assertEquals(baseMemory.isNull(), proxy.isNull());
			assertEquals(baseMemory.isPointer(), proxy.isPointer());
		}
	}

	// ==================== Reference Counting Tests ====================

	@Nested
	@DisplayName("Reference Counting")
	class ReferenceCountingTests {

		@Test
		@DisplayName("Binding increments ref count")
		void testBindingIncrementsRef() {
			int initialRef = baseMemory.refCount();

			proxy.bindMemory(baseMemory, 0);

			assertEquals(initialRef + 1, baseMemory.refCount());
		}

		@Test
		@DisplayName("Unbinding decrements ref count")
		void testUnbindingDecrementsRef() {
			proxy.bindMemory(baseMemory, 0);
			int boundRef = baseMemory.refCount();

			proxy.unbindMemory();

			assertEquals(boundRef - 1, baseMemory.refCount());
		}

		@Test
		@DisplayName("RefCount delegates to bound memory")
		void testRefCountDelegation() {
			proxy.bindMemory(baseMemory, 0);

			assertEquals(baseMemory.refCount(), proxy.refCount());

			baseMemory.incrementRef();
			assertEquals(baseMemory.refCount(), proxy.refCount());
		}

		@Test
		@DisplayName("IncrementRef delegates")
		void testIncrementRefDelegation() {
			proxy.bindMemory(baseMemory, 0);
			int before = proxy.refCount();

			int newRef = proxy.incrementRef();

			assertEquals(before + 1, newRef);
			assertEquals(baseMemory.refCount(), newRef);
		}

		@Test
		@DisplayName("DecrementRef auto-unbinds at zero")
		void testDecrementRefAutoUnbind() {
			// Create memory with refcount 1
			Memory temp = Memory.of(arena.allocate(256), 0, 256);

			proxy.bindMemory(temp, 0); // refcount now 2
			assertEquals(2, temp.refCount());

			temp.decrementRef(); // Back to 1
			assertEquals(1, temp.refCount());
			assertTrue(proxy.isBound());

			proxy.decrementRef(); // Goes to 0, should auto-unbind
			assertFalse(proxy.isBound());
		}
	}

	// ==================== Data Bounds Tests ====================

	@Nested
	@DisplayName("Data Bounds Management")
	class DataBoundsTests {

		@Test
		@DisplayName("Data bounds respect memory bounds")
		void testDataBoundsRespectMemory() {
			// Set data bounds within memory
			baseMemory.activeBytesStart(baseMemory.segmentOffset() + 50);
			baseMemory.activeBytesEnd(baseMemory.segmentEnd() - 50);

			proxy.bindMemory(baseMemory, 0);

			assertEquals(baseMemory.activeBytesStart(), proxy.activeBytesStart());
			assertEquals(baseMemory.activeBytesEnd(), proxy.activeBytesEnd());
		}

		@Test
		@DisplayName("Data bounds for bounded view")
		void testDataBoundsForBounded() {
			proxy.bindMemory(baseMemory, 100, 200);

			// Data bounds should be within slice
			long dataOffset = proxy.activeBytesStart();
			long dataEnd = proxy.activeBytesEnd();

			assertTrue(dataOffset >= proxy.segmentOffset());
			assertTrue(dataEnd <= proxy.segmentEnd());
			assertEquals(200, dataEnd - dataOffset);
		}

		@Test
		@DisplayName("Set data offset validates bounds")
		void testSetDataOffsetValidation() {
			proxy.bindMemory(baseMemory, 100, 200);

			// Valid setting
			long newOffset = proxy.segmentOffset() + 50;
			assertEquals(newOffset, proxy.activeBytesStart(newOffset));

			// Invalid - before proxy start
			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesStart(proxy.segmentOffset() - 1));

			// Invalid - after proxy end
			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesStart(proxy.segmentEnd() + 1));
		}

		@Test
		@DisplayName("Set data end validates bounds")
		void testSetDataEndValidation() {
			proxy.bindMemory(baseMemory, 100, 200);

			// Valid setting
			long newEnd = proxy.segmentEnd() - 50;
			assertEquals(newEnd, proxy.activeBytesEnd(newEnd));

			// Invalid - before data offset
			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesEnd(proxy.activeBytesStart() - 1));

			// Invalid - after proxy end
			assertThrows(IllegalArgumentException.class,
					() -> proxy.activeBytesEnd(proxy.segmentEnd() + 1));
		}
	}

	// ==================== Error Condition Tests ====================

	@Nested
	@DisplayName("Error Conditions")
	class ErrorConditionTests {

		@Test
		@DisplayName("Null memory binding throws NPE")
		void testNullMemoryBinding() {
			assertThrows(NullPointerException.class,
					() -> proxy.bindMemory(null, 0));

			assertThrows(NullPointerException.class,
					() -> proxy.bindMemory(null, 0, 100));
		}

		@Test
		@DisplayName("Negative offset throws")
		void testNegativeOffset() {
			assertThrows(IllegalArgumentException.class,
					() -> proxy.bindMemory(baseMemory, -1));

			assertThrows(IllegalArgumentException.class,
					() -> proxy.bindMemory(baseMemory, -100, 100));
		}

		@Test
		@DisplayName("Negative length throws")
		void testNegativeLength() {
			assertThrows(IllegalArgumentException.class,
					() -> proxy.bindMemory(baseMemory, 0, -1));
		}

		@Test
		@DisplayName("Seek beyond bounded view throws")
		void testSeekBeyondBounded() {
			proxy.bindMemory(baseMemory, 0, 100);

			assertThrows(IllegalArgumentException.class,
					() -> proxy.seekSegment(101));
		}
	}

	// ==================== Hook Method Tests ====================

	@Nested
	@DisplayName("Hook Methods")
	class HookMethodTests {

		static class TestProxy extends MemoryProxy {
			boolean bindCalled = false;
			boolean unbindCalled = false;

			@Override
			protected void onBindMemory() {
				bindCalled = true;
			}

			@Override
			protected void onUnbindMemory() {
				unbindCalled = true;
			}
		}

		@Test
		@DisplayName("onBindMemory called on binding")
		void testOnBindMemoryCalled() {
			TestProxy testProxy = new TestProxy();

			assertFalse(testProxy.bindCalled);
			testProxy.bindMemory(baseMemory, 0);
			assertTrue(testProxy.bindCalled);
		}

		@Test
		@DisplayName("onUnbindMemory called on unbinding")
		void testOnUnbindMemoryCalled() {
			TestProxy testProxy = new TestProxy();
			testProxy.bindMemory(baseMemory, 0);

			assertFalse(testProxy.unbindCalled);
			testProxy.unbindMemory();
			assertTrue(testProxy.unbindCalled);
		}

		@Test
		@DisplayName("Hooks called for bounded binding")
		void testHooksForBounded() {
			TestProxy testProxy = new TestProxy();

			testProxy.bindMemory(baseMemory, 0, 100);
			assertTrue(testProxy.bindCalled);

			testProxy.unbindMemory();
			assertTrue(testProxy.unbindCalled);
		}

	}

	// ==================== Integration Tests ====================

	@Nested
	@DisplayName("Integration Scenarios")
	class IntegrationTests {

		@Test
		@DisplayName("Multiple rebinds")
		void testMultipleRebinds() {
			// First binding
			proxy.bindMemory(baseMemory, 0, 100);
			assertEquals(100, proxy.segmentSize());

			proxy.unbindMemory();

			// Second binding - different offset
			proxy.bindMemory(baseMemory, 200, 150);
			assertEquals(150, proxy.segmentSize());

			proxy.unbindMemory();

			// Third binding - unbounded
			proxy.bindMemory(baseMemory, 50);
			assertEquals(SEGMENT_SIZE - 50, proxy.segmentSize());
		}

		@Test
		@DisplayName("Nested proxy binding")
		void testNestedProxyBinding() {
			// Packet proxy
			MemoryProxy packet = new MemoryProxy();
			packet.bindMemory(baseMemory, 0);

			// Header proxy binds to packet
			MemoryProxy header = new MemoryProxy();
			header.bindMemory(packet, 0, 14);

			// Sub-element binds to header
			MemoryProxy subElement = new MemoryProxy();
			subElement.bindMemory(header, 6, 6);

			// Verify nested bounds
			assertEquals(14, header.segmentSize());
			assertEquals(6, subElement.segmentSize());

			// Cleanup in reverse order
			subElement.unbindMemory();
			header.unbindMemory();
			packet.unbindMemory();
		}

		@Test
		@DisplayName("Data access through proxy")
		void testDataAccessThroughProxy() {
			// Write data to base memory
			baseMemory.asMemorySegment().set(ValueLayout.JAVA_INT, 100, 0x12345678);

			// Bind proxy and read through it
			proxy.bindMemory(baseMemory, 100, 4);
			int value = proxy.asMemorySegment().get(ValueLayout.JAVA_INT, 0);

			assertEquals(0x12345678, value);
		}

		@Test
		@DisplayName("ToString provides useful info")
		void testToString() {
			String unbound = proxy.toString();
			assertTrue(unbound.contains("UNBOUND"));

			proxy.bindMemory(baseMemory, 100);
			String unbounded = proxy.toString();
			assertTrue(unbounded.contains("unbounded"));
			assertTrue(unbounded.contains("100"));

			proxy.unbindMemory();
			proxy.bindMemory(baseMemory, 50, 200);
			String bounded = proxy.toString();
			assertTrue(bounded.contains("bounded"));
			assertTrue(bounded.contains("200"));
		}
	}
}