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
package com.slytechs.sdk.common.util;

import com.slytechs.sdk.common.util.UnitUtils.ConvertableUnit;
import com.slytechs.sdk.common.util.function.Pair;

/**
 * Enumeration of data rate units for network speeds and throughput. All units
 * are based on bits per second with decimal (1000) multipliers following
 * network industry standards.
 * 
 * <p>
 * This enum provides both bits-per-second (standard for network line rates) and
 * bytes-per-second (common for data transfer rates) variants. Note that 1 byte
 * = 8 bits.
 * </p>
 * 
 * <p>
 * Examples:
 * </p>
 * 
 * {@snippet :
 * // Network line rate
 * long lineRate = 100; // 100 Gbps NIC
 * long bps = DataRateUnit.GIGABITS_PER_SECOND.toBitsPerSecond(lineRate);
 * 
 * // Data throughput
 * long throughput = 12; // 12 GB/s to disk
 * long bytesPerSec = DataRateUnit.GIGABYTES_PER_SECOND.toBytesPerSecond(throughput);
 * 
 * // Conversion between units
 * long gbps = DataRateUnit.GIGABYTES_PER_SECOND.toGigabitsPerSecond(10); // 80 Gbps
 * 
 * // Formatting
 * String formatted = DataRateUnit.formatScaled("%v %s", 47_300_000_000L); // "47.3 Gbps"
 * }
 *
 * @author Mark Bednarczyk
 */
public enum DataRateUnit implements ConvertableUnit<DataRateUnit>, Unit {

	/** Represents bits per second (base unit). */
	BITS_PER_SECOND("bps", "bits/s", "bit/s", "b/s"),

	/** Represents kilobits per second (1,000 bps). */
	KILOBITS_PER_SECOND("kbps", "kb/s", "kbit/s"),

	/** Represents megabits per second (1,000,000 bps). */
	MEGABITS_PER_SECOND("mbps", "mb/s", "mbit/s"),

	/** Represents gigabits per second (1,000,000,000 bps). */
	GIGABITS_PER_SECOND("gbps", "gb/s", "gbit/s"),

	/** Represents terabits per second (1,000,000,000,000 bps). */
	TERABITS_PER_SECOND("tbps", "tb/s", "tbit/s"),

	/** Represents petabits per second (1,000,000,000,000,000 bps). */
	PETABITS_PER_SECOND("pbps", "pb/s", "pbit/s"),

	/** Represents bytes per second (8 bps). */
	BYTES_PER_SECOND("Bps", "bytes/s", "byte/s", "B/s"),

	/** Represents kilobytes per second (8,000 bps). */
	KILOBYTES_PER_SECOND("KBps", "KB/s", "kbyte/s"),

	/** Represents megabytes per second (8,000,000 bps). */
	MEGABYTES_PER_SECOND("MBps", "MB/s", "mbyte/s"),

	/** Represents gigabytes per second (8,000,000,000 bps). */
	GIGABYTES_PER_SECOND("GBps", "GB/s", "gbyte/s"),

	/** Represents terabytes per second (8,000,000,000,000 bps). */
	TERABYTES_PER_SECOND("TBps", "TB/s", "tbyte/s"),

	/** Represents petabytes per second (8,000,000,000,000,000 bps). */
	PETABYTES_PER_SECOND("PBps", "PB/s", "pbyte/s");

	/** Conversion factor from bits to bytes. */
	private static final int BITS_PER_BYTE = 8;

	/** Decimal multiplier for kilo (1000). */
	private static final long KILO = 1_000L;

	/** The base value in bits per second. */
	private final long base;

	/** The base value as a double for floating point conversions. */
	private final double basef;

	/** The symbols representing this unit. */
	private final String[] symbols;

	/**
	 * Constructs a DataRateUnit with the specified symbols.
	 *
	 * @param symbols The symbols representing this unit
	 */
	DataRateUnit(String... symbols) {
		this.base = calculateBase(ordinal());
		this.basef = this.base;
		this.symbols = symbols != null ? symbols : new String[] { "" + name().charAt(0) };
	}

	/**
	 * Calculates the base value in bits per second for the given ordinal.
	 *
	 * @param ordinal The ordinal of the enum constant
	 * @return The base value in bits per second
	 */
	private static long calculateBase(int ordinal) {
		// First 6 are bits (0-5): bps, kbps, mbps, gbps, tbps, pbps
		// Next 6 are bytes (6-11): Bps, KBps, MBps, GBps, TBps, PBps
		boolean isBits = ordinal < 6;
		int powerIndex = ordinal % 6;

		long multiplier = 1;
		for (int i = 0; i < powerIndex; i++) {
			multiplier *= KILO;
		}

		return isBits ? multiplier : multiplier * BITS_PER_BYTE;
	}

	/**
	 * Formats a data rate value into a string representation using the specified
	 * format string.
	 * 
	 * <p>
	 * The format string accepts two arguments:
	 * </p>
	 * <ul>
	 * <li>{@code arg1}: The scaled value in the nearest appropriate unit</li>
	 * <li>{@code arg2}: The unit abbreviation (e.g., "Gbps", "MB/s")</li>
	 * </ul>
	 *
	 * @param fmt           The format string, such as {@code "%v %s"}
	 * @param bitsPerSecond The data rate in bits per second
	 * @return The formatted string
	 */
	public static String formatScaled(String fmt, long bitsPerSecond) {
		return UnitUtils.format(fmt, bitsPerSecond, DataRateUnit.class, BITS_PER_SECOND);
	}

	/**
	 * Scales a data rate value to the nearest appropriate unit.
	 *
	 * @param bitsPerSecond The data rate in bits per second
	 * @return A pair containing the scaled value and the corresponding unit
	 */
	public static Pair<Long, DataRateUnit> scaleUnit(long bitsPerSecond) {
		var scalingUnit = nearest(bitsPerSecond);
		var scaledValue = scalingUnit.convert(bitsPerSecond, BITS_PER_SECOND);

		return Pair.of(scaledValue, scalingUnit);
	}

	/**
	 * Scales a data rate value to the nearest appropriate unit, starting from a
	 * specified base unit.
	 *
	 * @param value The value to scale
	 * @param unit  The base unit
	 * @return A pair containing the scaled value and the corresponding unit
	 */
	public static Pair<Long, DataRateUnit> scaleUnit(long value, DataRateUnit unit) {
		var scalingUnit = nearest(unit.toBitsPerSecond(value));
		var scaledValue = scalingUnit.convert(value, unit);

		return Pair.of(scaledValue, scalingUnit);
	}

	/**
	 * Determines the nearest DataRateUnit for a given data rate in bits per second.
	 *
	 * @param bitsPerSecond The data rate in bits per second
	 * @return The nearest DataRateUnit
	 */
	public static DataRateUnit nearest(long bitsPerSecond) {
		return UnitUtils.nearest(bitsPerSecond, DataRateUnit.class, BITS_PER_SECOND);
	}

	/**
	 * Converts a value from the source unit to this unit.
	 *
	 * @param value      The value to convert
	 * @param sourceUnit The source unit
	 * @return The converted value in this unit
	 */
	@Override
	public long convert(long value, DataRateUnit sourceUnit) {
		return sourceUnit.toBitsPerSecond(value) / this.base;
	}

	/**
	 * Converts a data rate value to this unit.
	 *
	 * @param bitsPerSecond The data rate in bits per second
	 * @return The converted value in this unit
	 */
	@Override
	public double convertf(double bitsPerSecond) {
		return convertf(bitsPerSecond, BITS_PER_SECOND);
	}

	/**
	 * Converts a value from the source unit to this unit.
	 *
	 * @param value      The value to convert
	 * @param sourceUnit The source unit
	 * @return The converted value in this unit as a double
	 */
	@Override
	public double convertf(double value, DataRateUnit sourceUnit) {
		return sourceUnit.toBitsPerSecondAsDouble(value) / this.basef;
	}

	/**
	 * Gets the symbols associated with this unit.
	 *
	 * @return An array of symbols for this unit
	 */
	@Override
	public String[] getSymbols() {
		return symbols;
	}

	/**
	 * Converts the given value to the base unit (bits per second).
	 *
	 * @param value The value to convert
	 * @return The value in bits per second
	 */
	@Override
	public long toBase(long value) {
		return toBitsPerSecond(value);
	}

	/**
	 * Converts the given value to bits per second.
	 *
	 * @param value The value to convert
	 * @return The value in bits per second
	 */
	public long toBitsPerSecond(long value) {
		return value * base;
	}

	/**
	 * Converts the given value to bits per second as a double.
	 *
	 * @param value The value to convert
	 * @return The value in bits per second as a double
	 */
	private double toBitsPerSecondAsDouble(double value) {
		return value * basef;
	}

	/**
	 * Converts the given value to bytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in bytes per second
	 */
	public long toBytesPerSecond(long value) {
		return toBitsPerSecond(value) / BITS_PER_BYTE;
	}

	/**
	 * Converts the given value to kilobits per second.
	 *
	 * @param value The value to convert
	 * @return The value in kilobits per second
	 */
	public long toKilobitsPerSecond(long value) {
		return KILOBITS_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to megabits per second.
	 *
	 * @param value The value to convert
	 * @return The value in megabits per second
	 */
	public long toMegabitsPerSecond(long value) {
		return MEGABITS_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to gigabits per second.
	 *
	 * @param value The value to convert
	 * @return The value in gigabits per second
	 */
	public long toGigabitsPerSecond(long value) {
		return GIGABITS_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to terabits per second.
	 *
	 * @param value The value to convert
	 * @return The value in terabits per second
	 */
	public long toTerabitsPerSecond(long value) {
		return TERABITS_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to petabits per second.
	 *
	 * @param value The value to convert
	 * @return The value in petabits per second
	 */
	public long toPetabitsPerSecond(long value) {
		return PETABITS_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to kilobytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in kilobytes per second
	 */
	public long toKilobytesPerSecond(long value) {
		return KILOBYTES_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to megabytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in megabytes per second
	 */
	public long toMegabytesPerSecond(long value) {
		return MEGABYTES_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to gigabytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in gigabytes per second
	 */
	public long toGigabytesPerSecond(long value) {
		return GIGABYTES_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to terabytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in terabytes per second
	 */
	public long toTerabytesPerSecond(long value) {
		return TERABYTES_PER_SECOND.convert(value, this);
	}

	/**
	 * Converts the given value to petabytes per second.
	 *
	 * @param value The value to convert
	 * @return The value in petabytes per second
	 */
	public long toPetabytesPerSecond(long value) {
		return PETABYTES_PER_SECOND.convert(value, this);
	}
}