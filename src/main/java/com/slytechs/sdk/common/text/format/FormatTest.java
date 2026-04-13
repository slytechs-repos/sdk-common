package com.slytechs.sdk.common.text.format;

public class FormatTest {

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

	public static void main(String[] args) {

		System.out.println("=== BitFormat Tests ===");
		testBitFormat();

		System.out.println("\n=== TextFormat Tests ===");
		testTextFormat();

		System.out.println("\n=== Built-in Macros Tests ===");
		testBuiltinMacros();

		System.out.println("\n=== Shared Macros Tests ===");
		testSharedMacros();

		System.out.println("\n=== Named Macro Tests ===");
		testNamedMacros();

		System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
	}

	static void testBitFormat() {
		BitFormat fmt = new BitFormat();

		test("simple value ref",
				"Value: 42",
				fmt.format("Value: {}", 42));

		test("formatted value ref",
				"Hex: 0x00FF",
				fmt.format("Hex: {:0x%04X}", 255));

		test("single bit field",
				"1010 ....",
				fmt.format("{/1111 ..../}", 0xA0));

		test("bit field with @set",
				"1010 .... = Set",
				fmt.format("{/1111 ..../} = {@set}", 0xA0));

		test("bit field off @set",
				"0000 .... = Not Set",
				fmt.format("{/1111 ..../} = {@set}", 0x05));

		test("custom on-chars",
				"....AP...",
				fmt.format("{/RECUAPRSF/0=.}", 0x018));

		test("hidden bit field",
				"0xB",
				fmt.format("{/.... 1111/hide}{:0x%X}", 0xAB));

		test("skip chars",
				"0101",
				fmt.format("{/++++1111/}", 0xA5));

		test("align chars",
				"    0101",
				fmt.format("{/====1111/}", 0xA5));

		test("escaped braces",
				"value {not} = 42",
				fmt.format("value \\{not\\} = {}", 42));

		test("multiple bitfields per line",
				"1010 .... = 0xA | .... 0101 = 0x5",
				fmt.format("{/1111 ..../} = {:0x%X} | {/.... 1111/} = {:0x%X}", 0xA5));

		// TCP flags multi-line
		FormatPattern tcpPattern = fmt.compile("""
				Flags: {:0x%03X}
				    {/.... ...1 ..../} = Acknowledgment: {@set}
				    {/.... .... 1.../} = Push: {@set}""");

		String[] lines = tcpPattern.formatLines(0x018);
		test("tcp line 0", "Flags: 0x018", lines[0]);
		test("tcp line 1", "    .... ...1 .... = Acknowledgment: Set", lines[1]);
		test("tcp line 2", "    .... .... 1... = Push: Set", lines[2]);

		// Compiled reuse
		FormatPattern.Line line = fmt.compileLine("{/1111 ..../} = {:0x%X}");
		test("compiled 0x30", "0011 .... = 0x3", line.format(0x30));
		test("compiled 0xF0", "1111 .... = 0xF", line.format(0xF0));
	}

	static void testTextFormat() {
		TextFormat fmt = new TextFormat();

		test("sequential args",
				"Frame 1 of 100",
				fmt.format("Frame {} of {}", 1, 100));

		test("sequential with format",
				"Hex: 0x0800",
				fmt.format("Hex: {:0x%04X}", 0x0800));

		test("multiple sequential",
				"a=1 b=2 c=3",
				fmt.format("a={} b={} c={}", 1, 2, 3));

		test("escaped braces",
				"use {} for placeholders",
				fmt.format("use \\{\\} for placeholders"));

		byte[] mac = {0x00, 0x1d, 0x60, (byte) 0xb3, 0x01, (byte) 0x84};
		test("mac macro",
				"MAC: 00:1d:60:b3:01:84",
				fmt.format("MAC: {@mac}", mac));

		test("mac.oui macro",
				"OUI: 00:1d:60",
				fmt.format("OUI: {@mac.oui}", mac));
	}

	static void testBuiltinMacros() {
		BitFormat fmt = new BitFormat();

		test("@hex", "0x1A", fmt.format("{@hex}", 0x1A));
		test("@hex02", "0x1A", fmt.format("{@hex02}", 0x1A));
		test("@hex04", "0x001A", fmt.format("{@hex04}", 0x1A));
		test("@hex08", "0x0000001A", fmt.format("{@hex08}", 0x1A));

		test("@bool true", "True", fmt.format("{@bool}", 1));
		test("@bool false", "False", fmt.format("{@bool}", 0));

		test("@enabled", "Enabled", fmt.format("{@enabled}", 1));
		test("@yes", "Yes", fmt.format("{@yes}", 1));

		test("@dec,", "1,000,000", fmt.format("{@dec,}", 1_000_000));

		test("@ipv4 long",
				"192.168.1.1",
				fmt.format("{@ipv4}", (192L << 24) | (168L << 16) | (1L << 8) | 1L));

		byte[] ipv4 = {(byte) 10, 0, 0, 1};
		test("@ipv4 bytes", "10.0.0.1", fmt.compile("{@ipv4}").format(ipv4));

		byte[] ipv6 = {
				0x20, 0x01, 0x0d, (byte) 0xb8,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x01
		};
		test("@ipv6", "2001:db8::1", fmt.compile("{@ipv6}").format(ipv6));

		byte[] macBytes = {0x00, 0x26, 0x62, 0x2f, 0x47, (byte) 0x87};
		test("@bytes", "00:26:62:2f:47:87", fmt.compile("{@bytes}").format(macBytes));
		test("@bytes-", "00-26-62-2f-47-87", fmt.compile("{@bytes-}").format(macBytes));
	}

	static void testSharedMacros() {
		BitFormat bitFmt = new BitFormat();
		TextFormat textFmt = new TextFormat(bitFmt);

		bitFmt.setMacro("proto", Macro.ofLong(v -> switch ((int) v) {
			case 6 -> "TCP";
			case 17 -> "UDP";
			default -> "Unknown(%d)".formatted(v);
		}));

		test("shared macro via bit",
				"Protocol: TCP",
				bitFmt.format("Protocol: {@proto}", 6));

		FormatPattern pattern = textFmt.compile("Proto: {@proto}");
		test("shared macro via text (TCP)", "Proto: TCP", pattern.format(6));
		test("shared macro via text (UDP)", "Proto: UDP", pattern.format(17));

		bitFmt.setMacro("type", Macro.of(v -> "Type=" + v));
		test("generic macro", "Type=hello", bitFmt.compile("{@type}").format("hello"));
	}

	static void testNamedMacros() {
		BitFormat fmt = new BitFormat();

		Macro dscp = Macro.enumLookup(java.util.Map.of(
				0, "CS0", 8, "CS1", 46, "EF"));

		test("enum lookup hit", "CS0", dscp.accept(0));
		test("enum lookup hit EF", "EF", dscp.accept(46));
		test("enum lookup miss", "Unknown (99)", dscp.accept(99));

		Macro proto = Macro.enumLookup(
				java.util.Map.of(6, "TCP", 17, "UDP"),
				"Proto(%d)");
		test("enum custom fallback", "Proto(99)", proto.accept(99));

		// @formatter:off
		Macro tcpFlags = Macro.flagList(
				new long[]  { 1 << 5,  1 << 4,  1 << 3,  1 << 2,  1 << 1,  1 << 0 },
				new String[]{ "URG",   "ACK",   "PSH",   "RST",   "SYN",   "FIN"  });
		// @formatter:on

		test("flagList ACK+PSH", "ACK, PSH", tcpFlags.accept(0x18));
		test("flagList SYN only", "SYN", tcpFlags.accept(0x02));
		test("flagList none", "None", tcpFlags.accept(0x00));
		test("flagList all", "URG, ACK, PSH, RST, SYN, FIN", tcpFlags.accept(0x3F));

		Macro.Named dscpNamed = Macro.named("name", java.util.Map.of(
				0, "CS0", 8, "CS1", 46, "EF"));

		String fieldName = "ip.dsfield.dscp";
		fmt.setMacro(fieldName + "." + dscpNamed.suffix(), dscpNamed.macro());

		test("named macro registered", "CS0", fmt.format("{@ip.dsfield.dscp.name}", 0));
		test("named macro registered EF", "EF", fmt.format("{@ip.dsfield.dscp.name}", 46));
		test("named macro unknown", "Unknown (12)", fmt.format("{@ip.dsfield.dscp.name}", 12));
	}
}