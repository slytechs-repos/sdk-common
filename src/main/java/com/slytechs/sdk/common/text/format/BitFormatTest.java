package com.slytechs.sdk.common.text.format;

public class BitFormatTest {

	interface TcpFlagsMacro {
		// @formatter:off
		long RSV = 7 << 8;
		long ECN = 1 << 7;
		long CWR = 1 << 6;
		long URG = 1 << 5;
		long ACK = 1 << 4;
		long PSH = 1 << 3;
		long RST = 1 << 2;
		long SYN = 1 << 1;
		long FIN = 1 << 0;

		long[]   MAP =   { RSV,   ECN,   CWR,   URG,   ACK,   PSH,   RST,   SYN,   FIN };
		String[] NAMES = {"RSV", "ECN", "CWR", "URG", "ACK", "PSH", "RST", "SYN", "FIN"};
		// @formatter:on

		static Macro flagNames() {
			return Macro.ofLong((v, m) -> {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < MAP.length; i++) {
					if ((v & MAP[i]) != 0) {
						if (sb.length() != 0)
							sb.append(", ");
						sb.append(NAMES[i]);
					}
				}
				return sb.isEmpty() ? "None" : sb.toString();
			});
		}
	}

	public static void main(String[] args) {
		BitFormat fmt = new BitFormat();
		fmt.setMacro("flagNames", TcpFlagsMacro.flagNames());

		System.out.println("=== Test 1: Simple value ref ===");
		String out1 = fmt.format("Value: {}", 42);
		System.out.println(out1);

		System.out.println("\n=== Test 2: Formatted value ref ===");
		String out2 = fmt.format("Hex: {:0x%04X}", 255);
		System.out.println(out2);

		System.out.println("\n=== Test 3: Single bit field ===");
		String out3 = fmt.format("{/1111 ..../}", 0xA0);
		System.out.println(out3);

		System.out.println("\n=== Test 4: Bit field with macro ===");
		String out4 = fmt.format("{/1111 ..../} = High nibble: {@set}", 0xA0);
		System.out.println(out4);

		System.out.println("\n=== Test 5: Bit field off ===");
		String out5 = fmt.format("{/1111 ..../} = High nibble: {@set}", 0x05);
		System.out.println(out5);

		System.out.println("\n=== Test 6: TCP Flags multi-line ===");
		int flags = 0x018; // ACK + PSH

		FormatPattern pattern = fmt.compile("""
				Flags: {:0x%03X} ({@flagNames})
				    {/111. .... ..../} = Reserved: {@set}
				    {/...1 .... ..../} = Accurate ECN: {@set}
				    {/.... 1... ..../} = Congestion Window Reduced: {@set}
				    {/.... .1.. ..../} = ECN-Echo: {@set}
				    {/.... ..1. ..../} = Urgent: {@set}
				    {/.... ...1 ..../} = Acknowledgment: {@set}
				    {/.... .... 1.../} = Push: {@set}
				    {/.... .... .1../} = Reset: {@set}
				    {/.... .... ..1./} = Syn: {@set}
				    {/.... .... ...1/} = Fin: {@set}""");

		System.out.println(pattern.format(flags));

		System.out.println("\n=== Test 7: formatLines ===");
		String[] lines = pattern.formatLines(flags);
		for (int i = 0; i < lines.length; i++)
			System.out.printf("  [%2d] %s%n", i, lines[i]);

		System.out.println("\n=== Test 8: Custom on-chars (TCP flag letters) ===");
		String out8 = fmt.format("{/RECUAPRSF/0=.}", flags);
		System.out.println(out8);

		System.out.println("\n=== Test 9: Hidden bit field sets mask ===");
		String out9 = fmt.format("{/.... 1111/hide}{:0x%X}", 0xAB);
		System.out.println(out9);

		System.out.println("\n=== Test 10: Skip/align chars ===");
		String out10 = fmt.format("{/++++1111/}", 0xA5);
		System.out.println(out10);

		System.out.println("\n=== Test 11: Align with space output ===");
		String out11 = fmt.format("{/====1111/}", 0xA5);
		System.out.println(out11);

		System.out.println("\n=== Test 12: Inverted mask ===");
		String out12 = fmt.format("{/1111 ..../~} = Low nibble inverted: {:0x%X}", 0xA5);
		System.out.println(out12);

		System.out.println("\n=== Test 13: Multiple bitfields on one line ===");
		String out13 = fmt.format("{/1111 ..../} = {:0x%X} | {/.... 1111/} = {:0x%X}", 0xA5);
		System.out.println(out13);

		System.out.println("\n=== Test 14: Escaped braces ===");
		String out14 = fmt.format("value \\{not a directive\\} = {}", 42);
		System.out.println(out14);

		System.out.println("\n=== Test 15: Compiled pattern reuse ===");
		FormatPattern.Line line = fmt.compileLine("{/1111 ..../} = {:0x%X}");
		for (int v = 0; v <= 0xFF; v += 0x30) {
			System.out.printf("  0x%02X -> %s%n", v, line.format(v));
		}

		System.out.println("\n=== Test 16: Shifted value {>>} ===");
		String out16 = fmt.format("{/1111 ..../} = {>>:0x%X}", 0xA5);
		System.out.println(out16);

		System.out.println("\n=== Test 17: Shifted no format {>>} ===");
		String out17 = fmt.format("{/1111 ..../} = {>>}", 0xA5);
		System.out.println(out17);

		System.out.println("\n=== Test 18: Shifted via {>>:fmt} ===");
		String out18 = fmt.format("{/1111 ..../} = {>>:0x%X}", 0xA5);
		System.out.println(out18);

		System.out.println("\n=== Test 19: Multiple shifted on one line ===");
		String out19 = fmt.format("{/1111 ..../} = {>>:0x%X} | {/.... 1111/} = {>>:0x%X}", 0xA5);
		System.out.println(out19);

		System.out.println("\n=== Test 20: Compiled reuse with shift ===");
		FormatPattern.Line line2 = fmt.compileLine("{/1111 ..../} = {>>:0x%X}");
		for (int v = 0; v <= 0xFF; v += 0x30) {
			System.out.printf("  0x%02X -> %s%n", v, line2.format(v));
		}

		System.out.println("\n=== Test 21: Expression {* 4} ===");
		String out21 = fmt.format("{/.... 1111/} = Header Length: {>>* 4} bytes ({>>})", 0x45);
		System.out.println(out21);

		System.out.println("\n=== Test 22: Expression {<< 2} ===");
		String out22 = fmt.format("{/.... 1111/hide}{<< 2}", 0x45);
		System.out.println(out22);

		System.out.println("\n=== Test 23: Expression {& 0xFF} ===");
		String out23 = fmt.format("{& 0xFF:0x%02X}", 0x1245);
		System.out.println(out23);

		System.out.println("\n=== Test 24: Expression {~} ===");
		String out24 = fmt.format("{~}", 42);
		System.out.println(out24);
	}
}