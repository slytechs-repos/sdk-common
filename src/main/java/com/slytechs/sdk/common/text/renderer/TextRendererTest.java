package com.slytechs.sdk.common.text.renderer;

import com.slytechs.sdk.common.text.DataEmitter;
import com.slytechs.sdk.common.text.DataEmitter.DataContexts;
import com.slytechs.sdk.common.text.Text;

public class TextRendererTest {

	static int passed = 0;
	static int failed = 0;

	static void test(String name, String expected, String actual) {
		if (expected.equals(actual)) {
			System.out.printf("  PASS: %s%n", name);
			passed++;
		} else {
			System.out.printf("  FAIL: %s%n    expected: [%s]%n    actual:   [%s]%n", name, expected, actual);
			failed++;
		}
	}

	record MockPacket(int frameNumber, int captureLength, int wireLength) {}

	interface FrameTiming {
		String deltaTime(MockPacket pkt);
	}

	static class MockFrameTiming implements FrameTiming {
		@Override
		public String deltaTime(MockPacket pkt) {
			return "0.003000";
		}
	}

	public static void main(String[] args) {

		System.out.println("=== Basic TextRenderer ===");
		testBasicRender();

		System.out.println("\n=== Reusable Renderer ===");
		testReusable();

		System.out.println("\n=== Section Indentation ===");
		testSectionIndentation();

		System.out.println("\n=== Context Skipping ===");
		testContextSkipping();

		System.out.println("\n=== Full Packet ===");
		testFullPacket();

		System.out.println("\n=== Append ===");
		testAppend();

		System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
	}

	static void testBasicRender() {
		DataEmitter<MockPacket> dsl = new DataEmitter<>();
		dsl.field("Frame Number", MockPacket::frameNumber);
		dsl.field("Capture Length", p -> p.captureLength());

		TextRenderer renderer = new TextRenderer();
		Text text = renderer.render(dsl, new MockPacket(1, 200, 200));

		test("basic output",
				"Frame Number: 1\n" +
				"Capture Length: 200",
				text.toString());
	}

	static void testReusable() {
		TextRenderer renderer = new TextRenderer();

		DataEmitter<MockPacket> pktDsl = new DataEmitter<>();
		pktDsl.field("Frame Number", MockPacket::frameNumber);

		record Server(String name, int port) {}
		DataEmitter<Server> srvDsl = new DataEmitter<>();
		srvDsl.field("Name", Server::name);
		srvDsl.field("Port", Server::port);

		Text pktText = renderer.render(pktDsl, new MockPacket(1, 100, 100));
		Text srvText = renderer.render(srvDsl, new Server("web", 8080));

		test("packet output", "Frame Number: 1", pktText.toString());
		test("server output", "Name: web\nPort: 8080", srvText.toString());
	}

	static void testSectionIndentation() {
		DataEmitter<MockPacket> dsl = new DataEmitter<>();
		dsl.section("Frame Summary", sec -> sec
				.field("Frame Number", MockPacket::frameNumber, "frame.number")
				.field("Length", p -> p.captureLength(), "frame.cap_len")
				.meta("Marked", p -> "False"));

		TextRenderer renderer = new TextRenderer();
		Text text = renderer.render(dsl, new MockPacket(3, 500, 500));

		test("section output",
				"Frame Summary\n" +
				"    Frame Number: 3\n" +
				"    Length: 500\n" +
				"    [Marked: False]",
				text.toString());
	}

	static void testContextSkipping() {
		DataEmitter<MockPacket> dsl = new DataEmitter<>();
		dsl.field("Frame Number", MockPacket::frameNumber);
		dsl.meta("Time Delta", FrameTiming.class,
				(FrameTiming ft, MockPacket pkt) -> ft.deltaTime(pkt), "frame.time_delta");
		dsl.meta("Always Shown", p -> "yes");

		TextRenderer renderer = new TextRenderer();

		Text noCtx = renderer.render(dsl, new MockPacket(1, 100, 100));
		test("no context",
				"Frame Number: 1\n" +
				"[Always Shown: yes]",
				noCtx.toString());

		DataContexts ctx = DataContexts.of(new MockFrameTiming());
		Text withCtx = renderer.render(dsl, new MockPacket(1, 100, 100), ctx);
		test("with context",
				"Frame Number: 1\n" +
				"[Time Delta: 0.003000]\n" +
				"[Always Shown: yes]",
				withCtx.toString());
	}

	static void testFullPacket() {
		String SUMMARY = "Frame {frame.number}: "
				+ "{frame.cap_len} bytes on wire ({frame.cap_len * 8} bits), "
				+ "{frame.wire_len} bytes captured ({frame.wire_len * 8} bits)";

		DataEmitter<MockPacket> dsl = new DataEmitter<>();
		dsl.section(SUMMARY, sec -> sec
				.field("Frame Number", MockPacket::frameNumber, "frame.number")
				.field("Frame Length", p -> p.captureLength(), "frame.cap_len")
				.field("Capture Length", p -> p.wireLength(), "frame.wire_len")
				.meta("Frame is marked", p -> "False")
				.meta("Frame is ignored", p -> "False"));

		TextRenderer renderer = new TextRenderer();
		MockPacket pkt = new MockPacket(7, 1514, 1514);
		Text text = renderer.render(dsl, pkt);

		String expected = "Frame 7: 1514 bytes on wire (12112 bits), 1514 bytes captured (12112 bits)\n"
				+ "    Frame Number: 7\n"
				+ "    Frame Length: 1514\n"
				+ "    Capture Length: 1514\n"
				+ "    [Frame is marked: False]\n"
				+ "    [Frame is ignored: False]";

		test("full packet output", expected, text.toString());
	}

	static void testAppend() {
		DataEmitter<MockPacket> dsl = new DataEmitter<>();
		dsl.field("Frame", p -> p.frameNumber());

		TextRenderer renderer = new TextRenderer();
		Text text = renderer.render(dsl, new MockPacket(5, 100, 100));

		StringBuilder sb = new StringBuilder("prefix:");
		text.append(sb);

		test("append to builder",
				"prefix:Frame: 5",
				sb.toString());
	}
}