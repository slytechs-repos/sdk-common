package com.slytechs.sdk.common.util;

public final class HexBytes {

	public static byte[] parse(String hex) {
		String clean = hex.replaceAll("[\\s:_\\-]", "");
		if (clean.length() % 2 != 0)
			throw new IllegalArgumentException("odd number of hex characters");

		byte[] bytes = new byte[clean.length() / 2];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);

		return bytes;
	}

	private HexBytes() {}
}