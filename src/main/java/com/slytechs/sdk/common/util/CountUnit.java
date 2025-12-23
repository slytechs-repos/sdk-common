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
 * Enumeration of units for counting large numbers. This enum provides a set of
 * units and methods for converting between them.
 *
 * @author Mark Bednarczyk
 */
public enum CountUnit implements ConvertableUnit<CountUnit>, Unit {

	/** Represents a single count. */
	COUNT(1L),

	/** Represents a thousand (10^3) counts. */
	KILO(1_000L, "k"),

	/** Represents a million (10^6) counts. */
	MEGA(1_000_000L, "m", "meg"),

	/** Represents a billion (10^9) counts. */
	GIGA(1_000_000_000L, "g", "gig"),

	/** Represents a trillion (10^12) counts. */
	TERA(1_000_000_000_000L, "t"),

	/** Represents a quadrillion (10^15) counts. */
	PETA(1_000_000_000_000_000L, "p");

	/** The base. */
	private final long base;

	/** The basef. */
	private final double basef;

	/** The symbols. */
	private final String[] symbols;

	/**
	 * Constructs a CountUnit with the specified base value and symbols.
	 *
	 * @param baseCount The base count for this unit
	 * @param symbols   The symbols representing this unit
	 */
	CountUnit(long baseCount, String... symbols) {
		this.base = baseCount;
		this.basef = baseCount;
		this.symbols = symbols;
	}

	/**
	 * Formats a count value into a string representation using the specified format
	 * string. The format string implicitly accepts two arguments:
	 * <ul>
	 * <li>{@code arg1}: The scaled value, representing the count adjusted to the
	 * nearest unit.</li>
	 * <li>{@code arg2}: The scaling unit abbreviation (e.g., "k" for kilo, "m" for
	 * mega).</li>
	 * </ul>
	 * Example:
	 * 
	 * <pre>
	 * String result = formatScaled("%1$d %2$s", 1500);
	 * // result = "1 k"
	 * </pre>
	 *
	 * @param fmt   The format string, such as {@code "%1$d %2$s"}, where
	 *              {@code %1$d} is replaced with the scaled value and {@code %2$s}
	 *              is replaced with the unit abbreviation.
	 * @param count The count value to be formatted.
	 * @return The formatted string representing the scaled value and its unit.
	 */
	public static String formatScaled(String fmt, long count) {
		return UnitUtils.format(fmt, count, CountUnit.class, COUNT);
	}

	/**
	 * Scales a count value to the nearest appropriate unit. This method finds the
	 * closest {@link CountUnit} to represent the given count value and converts the
	 * value to that unit.
	 * 
	 * Example:
	 * 
	 * <pre>
	 * Pair<Long, CountUnit> result = scaleUnit(1500);
	 * // result = Pair.of(1, CountUnit.KILO)
	 * </pre>
	 *
	 * @param count The count value to scale.
	 * @return A {@link Pair} containing:
	 *         <ul>
	 *         <li>The scaled value in the nearest unit.</li>
	 *         <li>The corresponding {@link CountUnit}.</li>
	 *         </ul>
	 */
	public static Pair<Long, CountUnit> scaleUnit(long count) {
		var scalingUnit = nearest(count);
		var scaledValue = scalingUnit.convert(count, CountUnit.COUNT);

		return Pair.of(scaledValue, scalingUnit);
	}

	/**
	 * Scales a count value to the nearest appropriate unit, starting from a
	 * specified base unit. This method converts the value from the given base
	 * {@link CountUnit} to the nearest suitable unit for better readability.
	 * 
	 * Example:
	 * 
	 * <pre>
	 * Pair<Long, CountUnit> result = scaleUnit(1500, CountUnit.MEGA);
	 * // result = Pair.of(0, CountUnit.KILO)
	 * </pre>
	 *
	 * @param count The count value to scale.
	 * @param unit  The base unit from which the scaling starts.
	 * @return A {@link Pair} containing:
	 *         <ul>
	 *         <li>The scaled value in the nearest unit.</li>
	 *         <li>The corresponding {@link CountUnit}.</li>
	 *         </ul>
	 */
	public static Pair<Long, CountUnit> scaleUnit(long count, CountUnit unit) {
		var scalingUnit = nearest(unit.toCount(count));
		var scaledValue = scalingUnit.convert(count, unit);

		return Pair.of(scaledValue, scalingUnit);
	}

	/**
	 * Determines the nearest {@link CountUnit} for a given count value.
	 * <p>
	 * This method identifies the most appropriate unit to represent the given count
	 * value by selecting the {@link CountUnit} that is closest to the magnitude of
	 * the input value. This is useful for scaling large numbers into human-readable
	 * formats using predefined units (e.g., KILO, MEGA, GIGA).
	 * </p>
	 * 
	 * Example:
	 * 
	 * <pre>
	 * CountUnit unit = nearest(1500);
	 * // unit = CountUnit.KILO
	 * </pre>
	 * 
	 * @param count The count value for which the nearest unit is to be determined.
	 * @return The {@link CountUnit} that best matches the magnitude of the given
	 *         count value.
	 */
	public static CountUnit nearest(long count) {
		return UnitUtils.nearest(count, CountUnit.class, COUNT);
	}

	/**
	 * Converts a value from the source unit to this unit.
	 *
	 * @param value      The value to convert
	 * @param sourceUnit The source unit
	 * @return The converted value in this unit
	 */
	@Override
	public long convert(long value, CountUnit sourceUnit) {
		return sourceUnit.toCount(value) / this.base;
	}

	/**
	 * Converts a count value to this unit.
	 *
	 * @param inCount The count value to convert
	 * @return The converted value in this unit
	 */
	@Override
	public double convertf(double inCount) {
		return convertf(inCount, COUNT);
	}

	/**
	 * Converts a value from the source unit to this unit.
	 *
	 * @param value      The value to convert
	 * @param sourceUnit The source unit
	 * @return The converted value in this unit as a double
	 */
	@Override
	public double convertf(double value, CountUnit sourceUnit) {
		return sourceUnit.toUnif(value) / this.basef;
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
	 * Converts the given value to the base unit (COUNT).
	 *
	 * @param value The value to convert
	 * @return The value in the base unit
	 */
	@Override
	public long toBase(long value) {
		return toCount(value);
	}

	/**
	 * Converts the given value to the count in this unit.
	 *
	 * @param value The value to convert
	 * @return The count in this unit
	 */
	public long toCount(long value) {
		return value * base;
	}

	/**
	 * Converts the given value to the count in this unit as an integer.
	 *
	 * @param value The value to convert
	 * @return The count in this unit as an integer
	 */
	public int toCountAsInt(long value) {
		return (int) toCount(value);
	}

	/**
	 * Converts the given value to giga units.
	 *
	 * @param value The value to convert
	 * @return The value in giga units
	 */
	public long toGiga(long value) {
		return GIGA.convert(value, this);
	}

	/**
	 * Converts the given value to kilo units.
	 *
	 * @param value The value to convert
	 * @return The value in kilo units
	 */
	public long toKilo(long value) {
		return KILO.convert(value, this);
	}

	/**
	 * Converts the given value to mega units.
	 *
	 * @param value The value to convert
	 * @return The value in mega units
	 */
	public long toMega(long value) {
		return MEGA.convert(value, this);
	}

	/**
	 * Converts the given value to peta units.
	 *
	 * @param value The value to convert
	 * @return The value in peta units
	 */
	public long toPeta(long value) {
		return PETA.convert(value, this);
	}

	/**
	 * Converts the given value to tera units.
	 *
	 * @param value The value to convert
	 * @return The value in tera units
	 */
	public long toTera(long value) {
		return TERA.convert(value, this);
	}

	/**
	 * Converts the given value to the base unit as a double.
	 *
	 * @param value The value to convert
	 * @return The value in the base unit as a double
	 */
	private double toUnif(double value) {
		return value * basef;
	}
}