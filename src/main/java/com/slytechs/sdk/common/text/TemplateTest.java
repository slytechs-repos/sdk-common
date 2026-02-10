package com.slytechs.sdk.common.text;

import com.slytechs.sdk.common.text.format.TextFormat;

public class TemplateTest {

	static int passed = 0;
	static int failed = 0;

	static void test(String name, String expected, String actual) {
		if (expected.equals(actual)) {
			System.out.printf("  PASS: %s%n", name);
			passed++;
		} else {
			System.out.printf("  FAIL: %s%n    expected: [%s]%n    actual:   [%s]%n",
					name, expected, actual);
			failed++;
		}
	}

	public static void main(String[] args) {

		TextFormat fmt = new TextFormat();

		System.out.println("=== Basic Name Resolution ===");
		testBasicResolution(fmt);

		System.out.println("\n=== Expressions on Resolved Values ===");
		testExpressions(fmt);

		System.out.println("\n=== Format Specs ===");
		testFormatSpecs(fmt);

		System.out.println("\n=== Deduplication ===");
		testDedup(fmt);

		System.out.println("\n=== Packet Summary Simulation ===");
		testPacketSummary(fmt);

		System.out.println("\n=== Edge Cases ===");
		testEdgeCases(fmt);

		System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
	}

	static void testBasicResolution(TextFormat fmt) {
		Template tmpl = Template.compile(fmt, "Frame {frame.number}");

		test("single name",
				"Frame 4",
				tmpl.format(name -> switch (name) {
					case "frame.number" -> 4;
					default -> null;
				}));

		Template tmpl2 = Template.compile(fmt,
				"Src: {ip.src}, Dst: {ip.dst}");

		test("two names",
				"Src: 192.168.1.1, Dst: 10.0.0.1",
				tmpl2.format(name -> switch (name) {
					case "ip.src" -> "192.168.1.1";
					case "ip.dst" -> "10.0.0.1";
					default -> null;
				}));

		test("resolve count", 2, tmpl2.resolveCount());
	}

	static void testExpressions(TextFormat fmt) {
		Template tmpl = Template.compile(fmt,
				"{frame.cap_len} bytes ({frame.cap_len * 8} bits)");

		test("name with multiply",
				"200 bytes (1600 bits)",
				tmpl.format(name -> switch (name) {
					case "frame.cap_len" -> 200;
					default -> null;
				}));

		Template tmpl2 = Template.compile(fmt,
				"IHL: {ip.ihl * 4} bytes");

		test("multiply only",
				"IHL: 20 bytes",
				tmpl2.format(name -> switch (name) {
					case "ip.ihl" -> 5;
					default -> null;
				}));

		Template tmpl3 = Template.compile(fmt,
				"Shifted: {value >> 4}");

		test("shift expression",
				"Shifted: 10",
				tmpl3.format(name -> switch (name) {
					case "value" -> 0xA5;
					default -> null;
				}));
	}

	static void testFormatSpecs(TextFormat fmt) {
		Template tmpl = Template.compile(fmt,
				"Type: {eth.type:0x%04X}");

		test("name with format",
				"Type: 0x0800",
				tmpl.format(name -> switch (name) {
					case "eth.type" -> 0x0800;
					default -> null;
				}));

		Template tmpl2 = Template.compile(fmt,
				"Offset: {ip.frag_offset * 8:%,d} bytes");

		test("name with expr and format",
				"Offset: 1,480 bytes",
				tmpl2.format(name -> switch (name) {
					case "ip.frag_offset" -> 185;
					default -> null;
				}));
	}

	static void testDedup(TextFormat fmt) {
		Template tmpl = Template.compile(fmt,
				"{frame.cap_len} bytes ({frame.cap_len * 8} bits)");

		test("dedup resolve count", 1, tmpl.resolveCount());

		String[] names = tmpl.referenceNames();
		test("dedup ref names", "frame.cap_len", names[0]);
		test("dedup ref count", 1, names.length);

		// Verify both positions get the same resolved value
		test("dedup same value",
				"200 bytes (1600 bits)",
				tmpl.format(name -> 200));
	}

	static void testPacketSummary(TextFormat fmt) {
		String SUMMARY = "Frame {frame.number}: "
				+ "{frame.cap_len} bytes on wire ({frame.cap_len * 8} bits), "
				+ "{frame.wire_len} bytes captured ({frame.wire_len * 8} bits)";

		Template tmpl = Template.compile(fmt, SUMMARY);

		Template.Resolver resolver = name -> switch (name) {
			case "frame.number" -> 4;
			case "frame.cap_len" -> 200;
			case "frame.wire_len" -> 200;
			default -> null;
		};

		test("packet summary",
				"Frame 4: 200 bytes on wire (1600 bits), 200 bytes captured (1600 bits)",
				tmpl.format(resolver));

		test("summary resolve count", 3, tmpl.resolveCount());

		// Different wire vs capture length
		Template.Resolver truncated = name -> switch (name) {
			case "frame.number" -> 7;
			case "frame.cap_len" -> 96;
			case "frame.wire_len" -> 1514;
			default -> null;
		};

		test("truncated packet",
				"Frame 7: 96 bytes on wire (768 bits), 1514 bytes captured (12112 bits)",
				tmpl.format(truncated));
	}

	static void testEdgeCases(TextFormat fmt) {
		// No named references — pure literal
		Template literal = Template.compile(fmt, "just literal text");
		test("pure literal",
				"just literal text",
				literal.format(name -> null));

		test("literal resolve count", 0, literal.resolveCount());

		// Escaped braces
		Template escaped = Template.compile(fmt,
				"use \\{braces\\} with {name}");
		test("escaped braces",
				"use {braces} with hello",
				escaped.format(name -> "hello"));

		// Null resolver result
		Template nullable = Template.compile(fmt,
				"value: {missing}");
		test("null resolves to null string",
				"value: null",
				nullable.format(name -> null));
	}

	static void test(String name, int expected, int actual) {
		test(name, String.valueOf(expected), String.valueOf(actual));
	}
}