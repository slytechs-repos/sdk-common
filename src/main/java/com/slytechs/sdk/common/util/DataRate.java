/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable representation of a data rate.
 * 
 * <p>
 * This class models a data rate as bits per second and provides methods for
 * conversion to and from different {@link DataRateUnit}s. It follows the design
 * patterns of the java.time API classes like Duration.
 * </p>
 * 
 * <p>
 * The class stores the rate internally in bits per second and supports rates up
 * to {@link Long#MAX_VALUE} bits per second. All operations that would result
 * in overflow throw {@link ArithmeticException}.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * {@snippet :
 * // Create data rates
 * DataRate nicSpeed = DataRate.ofGigabitsPerSecond(100);
 * DataRate throughput = DataRate.of(12, DataRateUnit.GIGABYTES_PER_SECOND);
 * 
 * // Arithmetic operations
 * DataRate total = nicSpeed.plus(throughput);
 * DataRate half = total.dividedBy(2);
 * 
 * // Conversions
 * long bps = total.toBitsPerSecond();
 * long gbps = total.toGigabitsPerSecond();
 * long gbytesPerSec = total.toGigabytesPerSecond();
 * 
 * // Formatting
 * System.out.println(nicSpeed);                          // "100 Gbps"
 * System.out.println(throughput.format("%v %s"));        // "12 GBps"
 * System.out.println(total.toString(DataRateUnit.MEGABITS_PER_SECOND));
 * 
 * // Parsing
 * DataRate parsed = DataRate.parse("800 Gbps");
 * 
 * // Validation - license limits
 * DataRate license = DataRate.ofGigabitsPerSecond(100);
 * DataRate current = DataRate.ofGigabitsPerSecond(47);
 * current.validate(DataRate.atMost(license), "Exceeds licensed rate");
 * 
 * // Time to transfer calculations
 * long bytesToTransfer = 1_000_000_000L; // 1 GB
 * DataRate rate = DataRate.ofGigabitsPerSecond(10);
 * double seconds = rate.timeToTransfer(bytesToTransfer);
 * }
 *
 * @author Mark Bednarczyk
 */
public final class DataRate implements Comparable<DataRate> {

	/** A constant for zero rate. */
	public static final DataRate ZERO = new DataRate(0);

	/** Pattern for parsing data rate strings. */
	private static final Pattern PARSE_PATTERN = Pattern.compile(
			"^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/]+)?\\s*$");

	/** The rate in bits per second. */
	private final long bitsPerSecond;

	/**
	 * Private constructor to enforce factory method usage.
	 *
	 * @param bitsPerSecond the rate in bits per second
	 */
	private DataRate(long bitsPerSecond) {
		this.bitsPerSecond = bitsPerSecond;
	}

	/**
	 * Obtains a DataRate representing the specified value in the specified unit.
	 *
	 * @param value the rate value
	 * @param unit  the unit of the rate
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate of(long value, DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return new DataRate(unit.toBitsPerSecond(value));
	}

	/**
	 * Obtains a DataRate representing the specified bits per second.
	 *
	 * @param bitsPerSecond the bits per second
	 * @return a DataRate
	 */
	public static DataRate ofBitsPerSecond(long bitsPerSecond) {
		return bitsPerSecond == 0 ? ZERO : new DataRate(bitsPerSecond);
	}

	/**
	 * Obtains a DataRate representing the specified kilobits per second.
	 *
	 * @param kilobitsPerSecond the kilobits per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofKilobitsPerSecond(long kilobitsPerSecond) {
		return new DataRate(DataRateUnit.KILOBITS_PER_SECOND.toBitsPerSecond(kilobitsPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified megabits per second.
	 *
	 * @param megabitsPerSecond the megabits per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofMegabitsPerSecond(long megabitsPerSecond) {
		return new DataRate(DataRateUnit.MEGABITS_PER_SECOND.toBitsPerSecond(megabitsPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified gigabits per second.
	 *
	 * @param gigabitsPerSecond the gigabits per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofGigabitsPerSecond(long gigabitsPerSecond) {
		return new DataRate(DataRateUnit.GIGABITS_PER_SECOND.toBitsPerSecond(gigabitsPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified terabits per second.
	 *
	 * @param terabitsPerSecond the terabits per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofTerabitsPerSecond(long terabitsPerSecond) {
		return new DataRate(DataRateUnit.TERABITS_PER_SECOND.toBitsPerSecond(terabitsPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified petabits per second.
	 *
	 * @param petabitsPerSecond the petabits per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofPetabitsPerSecond(long petabitsPerSecond) {
		return new DataRate(DataRateUnit.PETABITS_PER_SECOND.toBitsPerSecond(petabitsPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified bytes per second.
	 *
	 * @param bytesPerSecond the bytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofBytesPerSecond(long bytesPerSecond) {
		return new DataRate(DataRateUnit.BYTES_PER_SECOND.toBitsPerSecond(bytesPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified kilobytes per second.
	 *
	 * @param kilobytesPerSecond the kilobytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofKilobytesPerSecond(long kilobytesPerSecond) {
		return new DataRate(DataRateUnit.KILOBYTES_PER_SECOND.toBitsPerSecond(kilobytesPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified megabytes per second.
	 *
	 * @param megabytesPerSecond the megabytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofMegabytesPerSecond(long megabytesPerSecond) {
		return new DataRate(DataRateUnit.MEGABYTES_PER_SECOND.toBitsPerSecond(megabytesPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified gigabytes per second.
	 *
	 * @param gigabytesPerSecond the gigabytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofGigabytesPerSecond(long gigabytesPerSecond) {
		return new DataRate(DataRateUnit.GIGABYTES_PER_SECOND.toBitsPerSecond(gigabytesPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified terabytes per second.
	 *
	 * @param terabytesPerSecond the terabytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofTerabytesPerSecond(long terabytesPerSecond) {
		return new DataRate(DataRateUnit.TERABYTES_PER_SECOND.toBitsPerSecond(terabytesPerSecond));
	}

	/**
	 * Obtains a DataRate representing the specified petabytes per second.
	 *
	 * @param petabytesPerSecond the petabytes per second
	 * @return a DataRate
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static DataRate ofPetabytesPerSecond(long petabytesPerSecond) {
		return new DataRate(DataRateUnit.PETABYTES_PER_SECOND.toBitsPerSecond(petabytesPerSecond));
	}

	/**
	 * Obtains a DataRate from a text string such as "100 Gbps", "5.5 MB/s", or
	 * "1000000".
	 * 
	 * <p>
	 * The string must consist of an optional sign, a number (integer or decimal),
	 * and an optional unit suffix. Valid unit suffixes are case-insensitive and
	 * include any of the symbols recognized by {@link DataRateUnit}. If no unit is
	 * specified, bits per second is assumed.
	 * </p>
	 * 
	 * <p>
	 * Examples: "100", "10 Gbps", "5.5 GB/s", "-256 mbps", "+1000 kbps"
	 * </p>
	 *
	 * @param text the text to parse
	 * @return a DataRate
	 * @throws IllegalArgumentException if the text cannot be parsed
	 */
	public static DataRate parse(CharSequence text) {
		Objects.requireNonNull(text, "text");
		Matcher matcher = PARSE_PATTERN.matcher(text);

		if (!matcher.matches())
			throw new IllegalArgumentException("Cannot parse DataRate: " + text);

		String valueStr = matcher.group(1);
		String unitStr = matcher.group(2);

		double value = Double.parseDouble(valueStr);

		DataRateUnit unit = DataRateUnit.BITS_PER_SECOND;
		if (unitStr != null && !unitStr.isEmpty()) {
			unit = parseUnit(unitStr);
		}

		long bitsPerSecond = (long) (value * unit.toBitsPerSecond(1));
		return new DataRate(bitsPerSecond);
	}

	/**
	 * Parses a unit string to a DataRateUnit.
	 *
	 * @param unitStr the unit string
	 * @return the DataRateUnit
	 * @throws IllegalArgumentException if the unit cannot be parsed
	 */
	private static DataRateUnit parseUnit(String unitStr) {
		String normalized = unitStr.toLowerCase().trim();

		for (DataRateUnit unit : DataRateUnit.values()) {
			if (unit.name().equalsIgnoreCase(normalized))
				return unit;

			for (String symbol : unit.getSymbols()) {
				if (symbol.equalsIgnoreCase(normalized))
					return unit;
			}
		}

		throw new IllegalArgumentException("Unknown data rate unit: " + unitStr);
	}

	/**
	 * Returns the minimum of two data rates.
	 *
	 * @param rate1 the first rate
	 * @param rate2 the second rate
	 * @return the minimum rate
	 */
	public static DataRate min(DataRate rate1, DataRate rate2) {
		Objects.requireNonNull(rate1, "rate1");
		Objects.requireNonNull(rate2, "rate2");
		return rate1.bitsPerSecond <= rate2.bitsPerSecond ? rate1 : rate2;
	}

	/**
	 * Returns the maximum of two data rates.
	 *
	 * @param rate1 the first rate
	 * @param rate2 the second rate
	 * @return the maximum rate
	 */
	public static DataRate max(DataRate rate1, DataRate rate2) {
		Objects.requireNonNull(rate1, "rate1");
		Objects.requireNonNull(rate2, "rate2");
		return rate1.bitsPerSecond >= rate2.bitsPerSecond ? rate1 : rate2;
	}

	/**
	 * Returns a predicate that tests if a rate is a multiple of the specified
	 * number of bits per second.
	 *
	 * @param bitsPerSecond the bits per second
	 * @return a predicate for testing multiples
	 */
	public static java.util.function.LongPredicate multipleOf(long bitsPerSecond) {
		if (bitsPerSecond <= 0)
			throw new IllegalArgumentException("bitsPerSecond must be positive");
		return rate -> rate % bitsPerSecond == 0;
	}

	/**
	 * Returns a predicate that tests if a rate is a multiple of the specified value
	 * in the given unit.
	 *
	 * @param value the value
	 * @param unit  the unit of the value
	 * @return a predicate for testing multiples
	 */
	public static java.util.function.LongPredicate multipleOf(long value, DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long bitsPerSecondMultiple = unit.toBitsPerSecond(value);
		return rate -> rate % bitsPerSecondMultiple == 0;
	}

	/**
	 * Returns a predicate that tests if a rate is within the specified range
	 * (inclusive).
	 *
	 * @param min the minimum rate
	 * @param max the maximum rate
	 * @return a predicate for testing range
	 */
	public static java.util.function.LongPredicate between(DataRate min, DataRate max) {
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		return rate -> rate >= min.bitsPerSecond && rate <= max.bitsPerSecond;
	}

	/**
	 * Returns a predicate that tests if a rate is at least the specified minimum.
	 *
	 * @param min the minimum rate
	 * @return a predicate for testing minimum
	 */
	public static java.util.function.LongPredicate atLeast(DataRate min) {
		Objects.requireNonNull(min, "min");
		return rate -> rate >= min.bitsPerSecond;
	}

	/**
	 * Returns a predicate that tests if a rate is at most the specified maximum.
	 *
	 * @param max the maximum rate
	 * @return a predicate for testing maximum
	 */
	public static java.util.function.LongPredicate atMost(DataRate max) {
		Objects.requireNonNull(max, "max");
		return rate -> rate <= max.bitsPerSecond;
	}

	/**
	 * Gets the rate in bits per second.
	 *
	 * @return the rate in bits per second
	 */
	public long toBitsPerSecond() {
		return bitsPerSecond;
	}

	/**
	 * Gets the rate in kilobits per second.
	 *
	 * @return the rate in kilobits per second
	 */
	public long toKilobitsPerSecond() {
		return DataRateUnit.KILOBITS_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in megabits per second.
	 *
	 * @return the rate in megabits per second
	 */
	public long toMegabitsPerSecond() {
		return DataRateUnit.MEGABITS_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in gigabits per second.
	 *
	 * @return the rate in gigabits per second
	 */
	public long toGigabitsPerSecond() {
		return DataRateUnit.GIGABITS_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in terabits per second.
	 *
	 * @return the rate in terabits per second
	 */
	public long toTerabitsPerSecond() {
		return DataRateUnit.TERABITS_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in petabits per second.
	 *
	 * @return the rate in petabits per second
	 */
	public long toPetabitsPerSecond() {
		return DataRateUnit.PETABITS_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in bytes per second.
	 *
	 * @return the rate in bytes per second
	 */
	public long toBytesPerSecond() {
		return DataRateUnit.BYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in kilobytes per second.
	 *
	 * @return the rate in kilobytes per second
	 */
	public long toKilobytesPerSecond() {
		return DataRateUnit.KILOBYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in megabytes per second.
	 *
	 * @return the rate in megabytes per second
	 */
	public long toMegabytesPerSecond() {
		return DataRateUnit.MEGABYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in gigabytes per second.
	 *
	 * @return the rate in gigabytes per second
	 */
	public long toGigabytesPerSecond() {
		return DataRateUnit.GIGABYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in terabytes per second.
	 *
	 * @return the rate in terabytes per second
	 */
	public long toTerabytesPerSecond() {
		return DataRateUnit.TERABYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Gets the rate in petabytes per second.
	 *
	 * @return the rate in petabytes per second
	 */
	public long toPetabytesPerSecond() {
		return DataRateUnit.PETABYTES_PER_SECOND.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Converts this rate to the specified unit.
	 *
	 * @param unit the target unit
	 * @return the rate in the specified unit
	 */
	public long to(DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convert(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Converts this rate to the specified unit as a double for precision.
	 *
	 * @param unit the target unit
	 * @return the rate in the specified unit as a double
	 */
	public double toDouble(DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convertf(bitsPerSecond, DataRateUnit.BITS_PER_SECOND);
	}

	/**
	 * Calculates the time in seconds required to transfer the specified number of
	 * bytes at this data rate.
	 *
	 * @param bytes the number of bytes to transfer
	 * @return the time in seconds
	 */
	public double timeToTransfer(long bytes) {
		if (bitsPerSecond == 0)
			throw new ArithmeticException("Cannot calculate transfer time with zero rate");
		long bits = bytes * 8;
		return (double) bits / bitsPerSecond;
	}

	/**
	 * Calculates the number of bytes that can be transferred in the specified
	 * number of seconds at this data rate.
	 *
	 * @param seconds the time in seconds
	 * @return the number of bytes
	 */
	public long bytesTransferred(double seconds) {
		long bits = (long) (bitsPerSecond * seconds);
		return bits / 8;
	}

	/**
	 * Returns a copy of this rate with the specified rate added.
	 *
	 * @param other the rate to add
	 * @return a DataRate based on this rate with the specified rate added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plus(DataRate other) {
		Objects.requireNonNull(other, "other");
		return ofBitsPerSecond(Math.addExact(bitsPerSecond, other.bitsPerSecond));
	}

	/**
	 * Returns a copy of this rate with the specified value added.
	 *
	 * @param value the value to add
	 * @param unit  the unit of the value
	 * @return a DataRate based on this rate with the specified value added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plus(long value, DataRateUnit unit) {
		return plus(DataRate.of(value, unit));
	}

	/**
	 * Returns a copy of this rate with the specified number of bits per second
	 * added.
	 *
	 * @param bitsPerSecond the bits per second to add
	 * @return a DataRate based on this rate with the specified bits per second
	 *         added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plusBitsPerSecond(long bitsPerSecond) {
		return ofBitsPerSecond(Math.addExact(this.bitsPerSecond, bitsPerSecond));
	}

	/**
	 * Returns a copy of this rate with the specified number of kilobits per second
	 * added.
	 *
	 * @param kilobitsPerSecond the kilobits per second to add
	 * @return a DataRate based on this rate with the specified kilobits per second
	 *         added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plusKilobitsPerSecond(long kilobitsPerSecond) {
		return plus(kilobitsPerSecond, DataRateUnit.KILOBITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate with the specified number of megabits per second
	 * added.
	 *
	 * @param megabitsPerSecond the megabits per second to add
	 * @return a DataRate based on this rate with the specified megabits per second
	 *         added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plusMegabitsPerSecond(long megabitsPerSecond) {
		return plus(megabitsPerSecond, DataRateUnit.MEGABITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate with the specified number of gigabits per second
	 * added.
	 *
	 * @param gigabitsPerSecond the gigabits per second to add
	 * @return a DataRate based on this rate with the specified gigabits per second
	 *         added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate plusGigabitsPerSecond(long gigabitsPerSecond) {
		return plus(gigabitsPerSecond, DataRateUnit.GIGABITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate with the specified rate subtracted.
	 *
	 * @param other the rate to subtract
	 * @return a DataRate based on this rate with the specified rate subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minus(DataRate other) {
		Objects.requireNonNull(other, "other");
		return ofBitsPerSecond(Math.subtractExact(bitsPerSecond, other.bitsPerSecond));
	}

	/**
	 * Returns a copy of this rate with the specified value subtracted.
	 *
	 * @param value the value to subtract
	 * @param unit  the unit of the value
	 * @return a DataRate based on this rate with the specified value subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minus(long value, DataRateUnit unit) {
		return minus(DataRate.of(value, unit));
	}

	/**
	 * Returns a copy of this rate with the specified number of bits per second
	 * subtracted.
	 *
	 * @param bitsPerSecond the bits per second to subtract
	 * @return a DataRate based on this rate with the specified bits per second
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minusBitsPerSecond(long bitsPerSecond) {
		return ofBitsPerSecond(Math.subtractExact(this.bitsPerSecond, bitsPerSecond));
	}

	/**
	 * Returns a copy of this rate with the specified number of kilobits per second
	 * subtracted.
	 *
	 * @param kilobitsPerSecond the kilobits per second to subtract
	 * @return a DataRate based on this rate with the specified kilobits per second
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minusKilobitsPerSecond(long kilobitsPerSecond) {
		return minus(kilobitsPerSecond, DataRateUnit.KILOBITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate with the specified number of megabits per second
	 * subtracted.
	 *
	 * @param megabitsPerSecond the megabits per second to subtract
	 * @return a DataRate based on this rate with the specified megabits per second
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minusMegabitsPerSecond(long megabitsPerSecond) {
		return minus(megabitsPerSecond, DataRateUnit.MEGABITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate with the specified number of gigabits per second
	 * subtracted.
	 *
	 * @param gigabitsPerSecond the gigabits per second to subtract
	 * @return a DataRate based on this rate with the specified gigabits per second
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate minusGigabitsPerSecond(long gigabitsPerSecond) {
		return minus(gigabitsPerSecond, DataRateUnit.GIGABITS_PER_SECOND);
	}

	/**
	 * Returns a copy of this rate multiplied by the specified scalar.
	 *
	 * @param multiplicand the value to multiply by
	 * @return a DataRate based on this rate multiplied by the specified scalar
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate multipliedBy(long multiplicand) {
		return ofBitsPerSecond(Math.multiplyExact(bitsPerSecond, multiplicand));
	}

	/**
	 * Returns a copy of this rate divided by the specified divisor.
	 *
	 * @param divisor the value to divide by
	 * @return a DataRate based on this rate divided by the specified divisor
	 * @throws ArithmeticException if the divisor is zero
	 */
	public DataRate dividedBy(long divisor) {
		if (divisor == 0)
			throw new ArithmeticException("Cannot divide by zero");
		return ofBitsPerSecond(bitsPerSecond / divisor);
	}

	/**
	 * Returns a copy of this rate with the rate negated.
	 *
	 * @return a DataRate based on this rate with the rate negated
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public DataRate negated() {
		return ofBitsPerSecond(Math.negateExact(bitsPerSecond));
	}

	/**
	 * Returns a copy of this rate with a positive rate.
	 *
	 * @return a DataRate based on this rate with an absolute rate
	 */
	public DataRate abs() {
		return bitsPerSecond < 0 ? negated() : this;
	}

	/**
	 * Checks if this rate is zero.
	 *
	 * @return true if this rate equals zero
	 */
	public boolean isZero() {
		return bitsPerSecond == 0;
	}

	/**
	 * Checks if this rate is negative.
	 *
	 * @return true if this rate is negative
	 */
	public boolean isNegative() {
		return bitsPerSecond < 0;
	}

	/**
	 * Checks if this rate is a multiple of the specified number of bits per second.
	 *
	 * @param bitsPerSecond the bits per second to check divisibility by
	 * @return true if this rate is a multiple of the specified bits per second
	 * @throws IllegalArgumentException if bitsPerSecond is zero or negative
	 */
	public boolean isMultipleOf(long bitsPerSecond) {
		if (bitsPerSecond <= 0)
			throw new IllegalArgumentException("bitsPerSecond must be positive");
		return this.bitsPerSecond % bitsPerSecond == 0;
	}

	/**
	 * Checks if this rate is a multiple of the specified value in the given unit.
	 *
	 * @param value the value to check divisibility by
	 * @param unit  the unit of the value
	 * @return true if this rate is a multiple of the specified value
	 * @throws IllegalArgumentException if value is zero or negative
	 */
	public boolean isMultipleOf(long value, DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long bitsPerSecondMultiple = unit.toBitsPerSecond(value);
		return this.bitsPerSecond % bitsPerSecondMultiple == 0;
	}

	/**
	 * Tests this rate against the provided predicate.
	 * 
	 * <p>
	 * The predicate is applied to the rate in bits per second. This allows for
	 * flexible validation logic to be applied.
	 * </p>
	 *
	 * @param predicate the predicate to test
	 * @return true if the predicate is satisfied
	 */
	public boolean test(java.util.function.LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		return predicate.test(bitsPerSecond);
	}

	/**
	 * Validates that this rate satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a default message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @return this DataRate for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public DataRate validate(java.util.function.LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(bitsPerSecond))
			throw new IllegalArgumentException("DataRate validation failed: " + this);
		return this;
	}

	/**
	 * Validates that this rate satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * the specified message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @param message   the exception message to use if validation fails
	 * @return this DataRate for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public DataRate validate(java.util.function.LongPredicate predicate, String message) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(bitsPerSecond))
			throw new IllegalArgumentException(message);
		return this;
	}

	/**
	 * Validates that this rate satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a message supplied by the messageSupplier. The supplier is only invoked if
	 * validation fails, allowing for efficient lazy message construction.
	 * </p>
	 *
	 * @param predicate       the predicate to validate against
	 * @param messageSupplier the supplier of the exception message
	 * @return this DataRate for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public DataRate validate(java.util.function.LongPredicate predicate,
			java.util.function.Supplier<String> messageSupplier) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(messageSupplier, "messageSupplier");
		if (!predicate.test(bitsPerSecond))
			throw new IllegalArgumentException(messageSupplier.get());
		return this;
	}

	/**
	 * Compares this rate to the specified rate.
	 *
	 * @param other the other rate to compare to
	 * @return the comparator value, negative if less, positive if greater
	 */
	@Override
	public int compareTo(DataRate other) {
		return Long.compare(this.bitsPerSecond, other.bitsPerSecond);
	}

	/**
	 * Checks if this rate is equal to the specified rate.
	 *
	 * @param obj the object to check
	 * @return true if this is equal to the other rate
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof DataRate))
			return false;
		DataRate other = (DataRate) obj;
		return this.bitsPerSecond == other.bitsPerSecond;
	}

	/**
	 * Returns a hash code for this rate.
	 *
	 * @return a suitable hash code
	 */
	@Override
	public int hashCode() {
		return Long.hashCode(bitsPerSecond);
	}

	/**
	 * Returns a string representation of this rate using the nearest appropriate
	 * unit.
	 * 
	 * <p>
	 * The format is controlled by the DataRateUnit formatting rules and will
	 * automatically select the most appropriate unit for readability.
	 * </p>
	 *
	 * @return a string representation of this rate
	 */
	@Override
	public String toString() {
		return DataRateUnit.formatScaled("%v %s", bitsPerSecond);
	}

	/**
	 * Returns a string representation of this rate in the specified unit.
	 *
	 * @param unit the unit to use for the string representation
	 * @return a string representation of this rate in the specified unit
	 */
	public String toString(DataRateUnit unit) {
		Objects.requireNonNull(unit, "unit");
		long value = to(unit);
		return value + " " + unit.getSymbol();
	}

	/**
	 * Formats this rate according to the specified format string.
	 * 
	 * <p>
	 * The format string is passed to
	 * {@link DataRateUnit#formatScaled(String, long)} for processing.
	 * </p>
	 *
	 * @param format the format string
	 * @return the formatted string
	 */
	public String format(String format) {
		Objects.requireNonNull(format, "format");
		return DataRateUnit.formatScaled(format, bitsPerSecond);
	}
}