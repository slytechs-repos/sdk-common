/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
package com.slytechs.sdk.common.format;

/**
 * Utility class for parsing hexadecimal strings into byte arrays.
 * The input strings can contain zero-padded 2-digit hex numbers,
 * with or without spaces between them.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class HexParser {

    /**
	 * Instantiates a new hex parser.
	 */
    private HexParser() {
        // Utility class, prevent instantiation
    }

    /**
     * Parses one or more hexadecimal strings into a byte array.
     * The strings are concatenated, spaces are removed, and each pair
     * of hex digits is converted to a byte.
     *
     * @param hexStrings varargs of hexadecimal strings to parse
     * @return the resulting byte array
     * @throws IllegalArgumentException if the hex string length (after removing spaces) is odd
     *                                  or contains invalid hex characters
     */
    public static byte[] parseHex(String... hexStrings) {
        StringBuilder sb = new StringBuilder();
        for (String s : hexStrings) {
            sb.append(s);
        }
        String hex = sb.toString().replace(" ", "");
        
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length: must be even after removing spaces");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        try {
            for (int i = 0; i < bytes.length; i++) {
                String pair = hex.substring(i * 2, i * 2 + 2);
                bytes[i] = (byte) Integer.parseInt(pair, 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex characters in string", e);
        }
        
        return bytes;
    }
}