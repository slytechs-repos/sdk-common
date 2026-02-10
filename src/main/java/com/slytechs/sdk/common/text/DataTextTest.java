package com.slytechs.sdk.common.text;

import java.util.ArrayList;
import java.util.List;

import com.slytechs.sdk.common.text.DataEmitter.DataContexts;
import com.slytechs.sdk.common.text.DataEmitter.DataRender;

public class DataTextTest {

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

	record MockPacket(int frameNumber, int captureLength, int wireLength, long timestamp) {}

	interface FrameTiming {
		String deltaTime(MockPacket pkt);
		String arrivalTime(MockPacket pkt);
	}

	static class MockFrameTiming implements FrameTiming {
		@Override
		public String deltaTime(MockPacket pkt) {
			return "0.003000 seconds";
		}

		@Override
		public String arrivalTime(MockPacket pkt) {
			return "Feb 8, 2026 14:30:00.123456789";
		}
	}

	static class TestEmitter implements DataEmitter.DataRender {
		final List<String> output = new ArrayList<>();
		int depth = 0;
		Detail detail;

		TestEmitter(Detail detail) {
			this.detail = detail;
		}

		@Override public int depth() { return depth; }
		@Override public Detail detail() { return detail; }

		@Override
		public DataRender field(String label, String value) {
			if (value.isEmpty())
				output.add(indent() + label);
			else
				output.add(indent() + label + ": " + value);
			return this;
		}

		@Override
		public DataRender bitfield(String label, String bitPattern, long maskedValue, String formattedValue) {
			output.add(indent() + formattedValue);
			return this;
		}

		@Override
		public DataRender line(String line) {
			output.add(indent() + line);
			return this;
		}

		@Override
		public DataRender meta(String label, String value) {
			output.add(indent() + "[" + label + ": " + value + "]");
			return this;
		}

		@Override public DataRender mime(String type) { return this; }

		@Override
		public DataRender pop() {
			depth--;
			return this;
		}

		@Override
		public DataRender push() {
			depth++;
			return this;
		}

		@Override
		public DataRender row(String line) {
			output.add(line);
			return this;
		}

		@Override
		public DataRender summary(String line) {
			output.add(indent() + line);
			return this;
		}

		private String indent() {
			return "    ".repeat(depth);
		}

		String getLine(int index) {
			return index < output.size() ? output.get(index) : "<missing>";
		}
	}

	public static void main(String[] args) {
		System.out.println("=== Basic Field Emission ===");
		testBasicFields();

		System.out.println("\n=== Template Resolution ===");
		testTemplateResolution();

		System.out.println("\n=== Context Fields ===");
		testContextFields();

		System.out.println("\n=== Context Skipping ===");
		testContextSkipping();

		System.out.println("\n=== Section Structure ===");
		testSectionStructure();

		System.out.println("\n=== Packet DSL Simulation ===");
		testPacketDsl();

		System.out.println("\n=== Resolver Composition ===");
		testResolverComposition();

		System.out.println("\n=== Bitfield ===");
		testBitfield();

		System.out.println("\n=== Field Section ===");
		testFieldSection();

		System.out.println("\n=== Wireshark Packet DSL ===");
		testWiresharkPacketDsl();

		System.out.println("\n=== Delegate ===");
		testDelegate();

		System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
	}

	static void testBasicFields() {
		DataEmitter<MockPacket> dt = new DataEmitter<MockPacket>()
				.field("Frame Number", MockPacket::frameNumber)
				.field("Capture Length", p -> p.captureLength())
				.meta("Wire Length", p -> p.wireLength());

		MockPacket pkt = new MockPacket(4, 200, 200, 0L);
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dt.dsl.emit(emitter, pkt, DataContexts.EMPTY);

		test("field 1", "Frame Number: 4", emitter.getLine(0));
		test("field 2", "Capture Length: 200", emitter.getLine(1));
		test("meta 1", "[Wire Length: 200]", emitter.getLine(2));
	}

	static void testTemplateResolution() {
		DataEmitter<MockPacket> dt = new DataEmitter<MockPacket>()
				.field("Frame Number", MockPacket::frameNumber, "frame.number")
				.field("Capture Length", p -> p.captureLength(), "frame.cap_len")
				.summary("Frame {frame.number}: {frame.cap_len} bytes ({frame.cap_len * 8} bits)");

		MockPacket pkt = new MockPacket(4, 200, 200, 0L);
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dt.dsl.emit(emitter, pkt, DataContexts.EMPTY);

		test("field with name", "Frame Number: 4", emitter.getLine(0));
		test("template summary",
				"Frame 4: 200 bytes (1600 bits)",
				emitter.getLine(2));
	}

	static void testContextFields() {
		DataEmitter<MockPacket> dt = new DataEmitter<MockPacket>()
				.field("Frame Number", MockPacket::frameNumber, "frame.number")
				.field("Arrival Time", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.arrivalTime(pkt), "frame.time")
				.meta("Time delta", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.deltaTime(pkt), "frame.time_delta");

		MockPacket pkt = new MockPacket(4, 200, 200, 0L);
		DataContexts ctx = DataContexts.of(new MockFrameTiming());
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dt.dsl.emit(emitter, pkt, ctx);

		test("target field", "Frame Number: 4", emitter.getLine(0));
		test("context field", "Arrival Time: Feb 8, 2026 14:30:00.123456789", emitter.getLine(1));
		test("context meta", "[Time delta: 0.003000 seconds]", emitter.getLine(2));
	}

	static void testContextSkipping() {
		DataEmitter<MockPacket> dt = new DataEmitter<MockPacket>()
				.field("Frame Number", MockPacket::frameNumber)
				.field("Arrival Time", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.arrivalTime(pkt), "frame.time")
				.meta("Time delta", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.deltaTime(pkt), "frame.time_delta")
				.meta("Static meta", "always shown");

		MockPacket pkt = new MockPacket(4, 200, 200, 0L);
		// No FrameTiming in context — those fields should be skipped
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dt.dsl.emit(emitter, pkt, DataContexts.EMPTY);

		test("target field present", "Frame Number: 4", emitter.getLine(0));
		test("static meta present", "[Static meta: always shown]", emitter.getLine(1));
		test("context fields skipped", 2, emitter.output.size());
	}

	static void testSectionStructure() {
		DataEmitter<MockPacket> dt = new DataEmitter<>();
		dt.field("Top Level", p -> "top");
		dt.section("Section Summary", sec -> sec
				.field("Inner Field 1", p -> "inner1")
				.field("Inner Field 2", p -> "inner2")
		);

		MockPacket pkt = new MockPacket(4, 200, 200, 0L);
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dt.dsl.emit(emitter, pkt, DataContexts.EMPTY);

		test("top level", "Top Level: top", emitter.getLine(0));
		test("section summary", "Section Summary", emitter.getLine(1));
		test("inner field indented", "    Inner Field 1: inner1", emitter.getLine(2));
		test("inner field 2 indented", "    Inner Field 2: inner2", emitter.getLine(3));
	}

	static void testPacketDsl() {
		String SUMMARY = "Frame {frame.number}: "
				+ "{frame.cap_len} bytes on wire ({frame.cap_len * 8} bits), "
				+ "{frame.wire_len} bytes captured ({frame.wire_len * 8} bits)";

		DataEmitter<MockPacket> packetText = new DataEmitter<>();
		packetText.section(SUMMARY, sec -> sec
				.field("Frame Number", MockPacket::frameNumber, "frame.number")
				.field("Capture Length", p -> p.captureLength(), "frame.cap_len")
				.field("Wire Length", p -> p.wireLength(), "frame.wire_len")
				.field("Arrival Time", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.arrivalTime(pkt), "frame.time")
				.meta("Time delta", FrameTiming.class,
						(FrameTiming ft, MockPacket pkt) -> ft.deltaTime(pkt), "frame.time_delta")
				.meta("Frame is marked", "False")
				.meta("Frame is ignored", "False")
		);

		MockPacket pkt = new MockPacket(7, 1514, 1514, 0L);
		DataContexts ctx = DataContexts.of(new MockFrameTiming());
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		packetText.dsl.emit(emitter, pkt, ctx);

		test("summary line",
				"Frame 7: 1514 bytes on wire (12112 bits), 1514 bytes captured (12112 bits)",
				emitter.getLine(0));
		test("frame number", "    Frame Number: 7", emitter.getLine(1));
		test("capture length", "    Capture Length: 1514", emitter.getLine(2));
		test("wire length", "    Wire Length: 1514", emitter.getLine(3));
		test("arrival time", "    Arrival Time: Feb 8, 2026 14:30:00.123456789", emitter.getLine(4));
		test("time delta", "    [Time delta: 0.003000 seconds]", emitter.getLine(5));
		test("marked meta", "    [Frame is marked: False]", emitter.getLine(6));
		test("ignored meta", "    [Frame is ignored: False]", emitter.getLine(7));
		test("total lines", 8, emitter.output.size());
	}

	static void test(String name, int expected, int actual) {
		test(name, String.valueOf(expected), String.valueOf(actual));
	}

	static void testResolverComposition() {
		// Packet-level DataText
		DataEmitter<MockPacket> packetDt = new DataEmitter<>();
		packetDt.field("Frame Number", MockPacket::frameNumber, "frame.number");
		packetDt.field("Capture Length", p -> p.captureLength(), "frame.cap_len");

		// Ip4-level DataText with its own refs
		DataEmitter<MockIp4> ip4Dt = new DataEmitter<>();
		ip4Dt.field("Source", MockIp4::src, "ip.src");
		ip4Dt.field("Destination", MockIp4::dst, "ip.dst");
		ip4Dt.field("TTL", p -> p.ttl(), "ip.ttl");

		// Build resolvers for each
		MockPacket pkt = new MockPacket(7, 1514, 1514, 0L);
		MockIp4 ip4 = new MockIp4("192.168.1.1", "10.0.0.1", 64);

		DataResolver packetResolver = packetDt.buildResolver(pkt, DataContexts.EMPTY);
		DataResolver ip4Resolver = ip4Dt.buildResolver(ip4, DataContexts.EMPTY);

		// Compose: packet first, then ip4
		DataResolver combined = packetResolver.combine(ip4Resolver);

		// Test cross-protocol resolution
		test("packet ref via combined",
				"7", String.valueOf(combined.resolve("frame.number")));

		test("ip4 ref via combined",
				"192.168.1.1", String.valueOf(combined.resolve("ip.src")));

		test("ip4 dst via combined",
				"10.0.0.1", String.valueOf(combined.resolve("ip.dst")));

		test("ip4 ttl via combined",
				"64", String.valueOf(combined.resolve("ip.ttl")));

		test("missing ref returns null",
				"null", String.valueOf(combined.resolve("nonexistent")));

		// Test Template resolution with combined resolver
		com.slytechs.sdk.common.text.format.TextFormat fmt = new com.slytechs.sdk.common.text.format.TextFormat();
		Template tmpl = Template.compile(fmt,
				"Frame {frame.number}: {ip.src} -> {ip.dst} ({frame.cap_len} bytes)");

		test("cross-protocol template",
				"Frame 7: 192.168.1.1 -> 10.0.0.1 (1514 bytes)",
				tmpl.format(combined::resolve));

		// Test composition via DataContext
		DataContexts ctx = DataContexts.of(combined);
		DataResolver fromCtx = ctx.getTyped(DataResolver.class);

		test("resolver from context",
				"192.168.1.1", String.valueOf(fromCtx.resolve("ip.src")));

		// Test combine with existing context resolver
		DataResolver extra = name -> "extra".equals(name) ? "bonus" : null;
		DataResolver layered = combined.combine(extra);

		test("layered original", "7", String.valueOf(layered.resolve("frame.number")));
		test("layered extra", "bonus", String.valueOf(layered.resolve("extra")));
	}

	static void testBitfield() {
		DataEmitter<int[]> dsl = new DataEmitter<>();
		// versionIhl = 0x45 → version=4, ihl=5
		dsl.bitfield("{/1111 ..../} = Version: {}",
				"ip.version", 0, 4, arr -> arr[0])
			.bitfield("{/.... 1111/} = Header Length: {}",
				"ip.hdr_len", 4, 4, arr -> arr[0]);

		int[] data = { 0x45 };
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dsl.dsl.emit(emitter, data, DataContexts.EMPTY);

		test("version bitfield", "0100 .... = Version: 4", emitter.getLine(0));
		test("ihl bitfield", ".... 0101 = Header Length: 5", emitter.getLine(1));
	}

	static void testFieldSection() {
		DataEmitter<int[]> dsl = new DataEmitter<>();
		dsl.field("Total Length", arr -> arr[1], "ip.len")
			.field("DS Field: {ip.dsfield:0x%02X}", arr -> arr[0], "ip.dsfield", ds -> ds
				.field("DSCP", arr -> arr[0] >> 2, "ip.dsfield.dscp")
				.field("ECN", arr -> arr[0] & 0x3, "ip.dsfield.ecn"));

		int[] data = { 0x00, 186 };
		TestEmitter emitter = new TestEmitter(Detail.HIGH);
		dsl.dsl.emit(emitter, data, DataContexts.EMPTY);

		test("total length", "Total Length: 186", emitter.getLine(0));
		test("ds field section header", "DS Field: 0x00", emitter.getLine(1));
		test("dscp child", "    DSCP: 0", emitter.getLine(2));
		test("ecn child", "    ECN: 0", emitter.getLine(3));
	}

	record MockIp4(String src, String dst, int ttl) {}

	record WsPacket(long frameNumber, int captureLength, int wireLength) {}

	interface WsFrameTiming {
		default String arrivalTime(WsPacket pkt) {
			return "Feb 9, 2026 14:30:00.123456789";
		}

		default String deltaTime(WsPacket pkt) {
			return "0.003000000 seconds";
		}

		default String timeRelative(WsPacket pkt) {
			return "0.003000 seconds";
		}
	}

	static class MockWsTiming implements WsFrameTiming {}

	static void testWiresharkPacketDsl() {
		String SUMMARY = "Frame {frame.number}: "
				+ "{frame.cap_len} bytes on wire ({frame.cap_len * 8} bits), "
				+ "{frame.wire_len} bytes captured ({frame.wire_len * 8} bits)";

		DataEmitter<WsPacket> dsl = new DataEmitter<>();
		dsl.section(SUMMARY, sec -> sec
				.field("Encapsulation type", p -> "Ethernet (1)")
				.field("Arrival Time", WsFrameTiming.class,
						(WsFrameTiming ft, WsPacket p) -> ft.arrivalTime(p), "frame.time")
				.meta("Time delta from previous captured frame", WsFrameTiming.class,
						(WsFrameTiming ft, WsPacket p) -> ft.deltaTime(p), "frame.time_delta")
				.meta("Time since reference or first frame", WsFrameTiming.class,
						(WsFrameTiming ft, WsPacket p) -> ft.timeRelative(p), "frame.time_relative")
				.field("Frame Number", p -> p.frameNumber(), "frame.number")
				.field("Frame Length",
						"{frame.cap_len} bytes ({frame.cap_len * 8} bits)",
						p -> p.captureLength(), "frame.cap_len")
				.field("Capture Length",
						"{frame.wire_len} bytes ({frame.wire_len * 8} bits)",
						p -> p.wireLength(), "frame.wire_len")
				.meta("Frame is marked", p -> "False")
				.meta("Frame is ignored", p -> "False")
				.meta("Protocols in frame", p -> "eth:ethertype:ip:tcp:http")
				.meta("Coloring Rule Name", p -> "HTTP")
				.meta("Coloring Rule String", p -> "http || tcp.port == 80 || http2")
		);

		WsPacket pkt = new WsPacket(7, 1514, 1514);
		DataContexts ctx = DataContexts.of(new MockWsTiming());

		com.slytechs.sdk.common.text.renderer.TextRenderer renderer =
				new com.slytechs.sdk.common.text.renderer.TextRenderer();
		Text text = renderer.render(dsl, pkt, ctx);
		String output = text.toString();

		System.out.println("--- Wireshark Output ---");
		System.out.println(output);
		System.out.println("--- End ---");

		String[] lines = output.split("\n");

		test("ws summary",
				"Frame 7: 1514 bytes on wire (12112 bits), 1514 bytes captured (12112 bits)",
				lines[0]);

		test("ws encapsulation",
				"    Encapsulation type: Ethernet (1)",
				lines[1]);

		test("ws arrival time",
				"    Arrival Time: Feb 9, 2026 14:30:00.123456789",
				lines[2]);

		test("ws time delta",
				"    [Time delta from previous captured frame: 0.003000000 seconds]",
				lines[3]);

		test("ws time relative",
				"    [Time since reference or first frame: 0.003000 seconds]",
				lines[4]);

		test("ws frame number",
				"    Frame Number: 7",
				lines[5]);

		test("ws frame length",
				"    Frame Length: 1514 bytes (12112 bits)",
				lines[6]);

		test("ws capture length",
				"    Capture Length: 1514 bytes (12112 bits)",
				lines[7]);

		test("ws marked",
				"    [Frame is marked: False]",
				lines[8]);

		test("ws ignored",
				"    [Frame is ignored: False]",
				lines[9]);

		test("ws protocols",
				"    [Protocols in frame: eth:ethertype:ip:tcp:http]",
				lines[10]);

		test("ws color name",
				"    [Coloring Rule Name: HTTP]",
				lines[11]);

		test("ws color string",
				"    [Coloring Rule String: http || tcp.port == 80 || http2]",
				lines[12]);

		test("ws line count", 13, lines.length);
	}

	record MockHeader(String name) {}

	static class MockEth implements Textual {
		private static final DataEmitter<MockEth> TEXT;
		static {
			DataEmitter<MockEth> parent = new DataEmitter<>();
			parent.section("Ethernet II, Src: {eth.src}, Dst: {eth.dst}", sec -> sec
					.field("Destination", p -> "00:26:62:2f:47:87", "eth.dst")
					.field("Source", p -> "00:1d:60:b3:01:84", "eth.src")
					.field("Type", p -> "IPv4 (0x0800)", "eth.type"));
			TEXT = parent;
		}

		@Override
		public void emitText(DataEmitter.DataRender render, DataContexts parentContexts) {
			DataContexts combined = TEXT.injectResolver(this, parentContexts);
			TEXT.dsl().emit(render, this, combined);
		}

		@Override
		public Text toText(Detail detail) {
			return Textual.render(TEXT, detail, this);
		}

		@Override
		public DataEmitter<?> dataEmitter() { return TEXT; }
	}

	static class MockIp4Header implements Textual {
		private static final DataEmitter<MockIp4Header> TEXT;
		static {
			DataEmitter<MockIp4Header> parent = new DataEmitter<>();
			parent.section("Internet Protocol Version 4, Src: {ip.src}, Dst: {ip.dst}", sec -> sec
					.field("Source Address", p -> "192.168.1.140", "ip.src")
					.field("Destination Address", p -> "174.143.213.184", "ip.dst")
					.field("Time to Live", p -> 64, "ip.ttl"));
			TEXT = parent;
		}

		@Override
		public void emitText(DataEmitter.DataRender render, DataContexts parentContexts) {
			DataContexts combined = TEXT.injectResolver(this, parentContexts);
			TEXT.dsl().emit(render, this, combined);
		}

		@Override
		public Text toText(Detail detail) {
			return Textual.render(TEXT, detail, this);
		}

		@Override
		public DataEmitter<?> dataEmitter() { return TEXT; }
	}

	record DelegatePacket(int frameNumber, int captureLength,
			Textual[] headers) {}

	static void testDelegate() {
		String SUMMARY = "Frame {frame.number}: {frame.cap_len} bytes";

		DataEmitter<DelegatePacket> dsl = new DataEmitter<>();
		dsl.section(SUMMARY, sec -> sec
				.field("Frame Number", p -> p.frameNumber(), "frame.number")
				.field("Frame Length", p -> p.captureLength(), "frame.cap_len"));

		// delegate chains on parent (dsl), not on the section child
		dsl.delegate((e, pkt, c) -> {
			for (Textual hdr : pkt.headers())
				hdr.emitText(e, c);
			return e;
		});

		DelegatePacket pkt = new DelegatePacket(4, 200,
				new Textual[] { new MockEth(), new MockIp4Header() });

		com.slytechs.sdk.common.text.renderer.TextRenderer renderer =
				new com.slytechs.sdk.common.text.renderer.TextRenderer();
		Text text = renderer.render(dsl, pkt);
		String output = text.toString();

		System.out.println("--- Delegate Output ---");
		System.out.println(output);
		System.out.println("--- End ---");

		String[] lines = output.split("\n");

		test("packet summary",
				"Frame 4: 200 bytes",
				lines[0]);
		test("packet frame number",
				"    Frame Number: 4",
				lines[1]);
		test("packet frame length",
				"    Frame Length: 200",
				lines[2]);
		test("eth summary",
				"Ethernet II, Src: 00:1d:60:b3:01:84, Dst: 00:26:62:2f:47:87",
				lines[3]);
		test("eth dst",
				"    Destination: 00:26:62:2f:47:87",
				lines[4]);
		test("eth src",
				"    Source: 00:1d:60:b3:01:84",
				lines[5]);
		test("eth type",
				"    Type: IPv4 (0x0800)",
				lines[6]);
		test("ip summary",
				"Internet Protocol Version 4, Src: 192.168.1.140, Dst: 174.143.213.184",
				lines[7]);
		test("ip src",
				"    Source Address: 192.168.1.140",
				lines[8]);
		test("ip dst",
				"    Destination Address: 174.143.213.184",
				lines[9]);
		test("ip ttl",
				"    Time to Live: 64",
				lines[10]);
		test("delegate line count", 11, lines.length);
	}
}