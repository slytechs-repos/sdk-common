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
 * An immutable representation of a count value.
 * 
 * <p>
 * This class models a count as a quantity and provides methods for conversion
 * to and from different {@link CountUnit}s. It follows the design patterns of
 * the java.time API classes like Duration.
 * </p>
 * 
 * <p>
 * The class stores the count internally as a base count and supports counts up
 * to {@link Long#MAX_VALUE}. All operations that would result in overflow throw
 * {@link ArithmeticException}.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * {@snippet :
 * // Create counts
 * Count packets = Count.ofMega(100);
 * Count bytes = Count.of(5, CountUnit.GIGA);
 * 
 * // Arithmetic operations
 * Count total = packets.plus(bytes);
 * Count half = total.dividedBy(2);
 * 
 * // Conversions
 * long count = total.toCount();
 * long mega = total.toMega();
 * 
 * // Formatting
 * System.out.println(total);                    // "5.1 G"
 * System.out.println(total.format("%v %s"));    // "5.1 G"
 * System.out.println(total.toString(CountUnit.MEGA)); // "5100 M"
 * 
 * // Parsing
 * Count parsed = Count.parse("10 G");
 * 
 * // Validation - checking constraints
 * Count limit = Count.ofKilo(1000);
 * boolean isMultiple = limit.isMultipleOf(100, CountUnit.KILO);
 * 
 * // Using predicates for validation
 * Count.ofMega(16)
 *     .validate(Count.multipleOf(1, CountUnit.MEGA))
 *     .validate(Count.between(
 *         Count.ofMega(1),
 *         Count.ofGiga(1)));
 * 
 * // Custom validation with message
 * Count.of(1000, CountUnit.COUNT)
 *     .validate(c -> c >= 512, "Count must be at least 512");
 * }
 *
 * @author Mark Bednarczyk
 */
public final class Count implements Comparable<Count> {

	/** A constant for zero count. */
	public static final Count ZERO = new Count(0);

	/** Pattern for parsing count strings. */
	private static final Pattern PARSE_PATTERN = Pattern.compile(
			"^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)?\\s*$");

	/** The base count value. */
	private final long count;

	/**
	 * Private constructor to enforce factory method usage.
	 *
	 * @param count the base count value
	 */
	private Count(long count) {
		this.count = count;
	}

	/**
	 * Obtains a Count representing the specified value in the specified unit.
	 *
	 * @param value the count value
	 * @param unit  the unit of the count
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count of(long value, CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return new Count(unit.toCount(value));
	}

	/**
	 * Obtains a Count representing the specified base count.
	 *
	 * @param count the base count value
	 * @return a Count
	 */
	public static Count ofCount(long count) {
		return count == 0 ? ZERO : new Count(count);
	}

	/**
	 * Obtains a Count representing the specified number of kilo units.
	 *
	 * @param kilo the number of kilo units
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count ofKilo(long kilo) {
		return new Count(CountUnit.KILO.toCount(kilo));
	}

	/**
	 * Obtains a Count representing the specified number of mega units.
	 *
	 * @param mega the number of mega units
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count ofMega(long mega) {
		return new Count(CountUnit.MEGA.toCount(mega));
	}

	/**
	 * Obtains a Count representing the specified number of giga units.
	 *
	 * @param giga the number of giga units
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count ofGiga(long giga) {
		return new Count(CountUnit.GIGA.toCount(giga));
	}

	/**
	 * Obtains a Count representing the specified number of tera units.
	 *
	 * @param tera the number of tera units
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count ofTera(long tera) {
		return new Count(CountUnit.TERA.toCount(tera));
	}

	/**
	 * Obtains a Count representing the specified number of peta units.
	 *
	 * @param peta the number of peta units
	 * @return a Count
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Count ofPeta(long peta) {
		return new Count(CountUnit.PETA.toCount(peta));
	}

	/**
	 * Obtains a Count from a text string such as "10 G", "5M", or "1000".
	 * 
	 * <p>
	 * The string must consist of an optional sign, a number (integer or decimal),
	 * and an optional unit suffix. Valid unit suffixes are case-insensitive and
	 * include any of the symbols recognized by {@link CountUnit}. If no unit is
	 * specified, base count is assumed.
	 * </p>
	 * 
	 * <p>
	 * Examples: "100", "10 K", "5.5G", "-256m", "+1024 kilo"
	 * </p>
	 *
	 * @param text the text to parse
	 * @return a Count
	 * @throws IllegalArgumentException if the text cannot be parsed
	 */
	public static Count parse(CharSequence text) {
		Objects.requireNonNull(text, "text");
		Matcher matcher = PARSE_PATTERN.matcher(text);

		if (!matcher.matches())
			throw new IllegalArgumentException("Cannot parse Count: " + text);

		String valueStr = matcher.group(1);
		String unitStr = matcher.group(2);

		double value = Double.parseDouble(valueStr);

		CountUnit unit = CountUnit.COUNT;
		if (unitStr != null && !unitStr.isEmpty()) {
			unit = parseUnit(unitStr);
		}

		long count = (long) (value * unit.toCount(1));
		return new Count(count);
	}

	/**
	 * Parses a unit string to a CountUnit.
	 *
	 * @param unitStr the unit string
	 * @return the CountUnit
	 * @throws IllegalArgumentException if the unit cannot be parsed
	 */
	private static CountUnit parseUnit(String unitStr) {
		String normalized = unitStr.toLowerCase().trim();

		for (CountUnit unit : CountUnit.values()) {
			if (unit.name().equalsIgnoreCase(normalized))
				return unit;

			for (String symbol : unit.getSymbols()) {
				if (symbol.equalsIgnoreCase(normalized))
					return unit;
			}
		}

		throw new IllegalArgumentException("Unknown count unit: " + unitStr);
	}

	/**
	 * Returns the minimum of two counts.
	 *
	 * @param count1 the first count
	 * @param count2 the second count
	 * @return the minimum count
	 */
	public static Count min(Count count1, Count count2) {
		Objects.requireNonNull(count1, "count1");
		Objects.requireNonNull(count2, "count2");
		return count1.count <= count2.count ? count1 : count2;
	}

	/**
	 * Returns the maximum of two counts.
	 *
	 * @param count1 the first count
	 * @param count2 the second count
	 * @return the maximum count
	 */
	public static Count max(Count count1, Count count2) {
		Objects.requireNonNull(count1, "count1");
		Objects.requireNonNull(count2, "count2");
		return count1.count >= count2.count ? count1 : count2;
	}

	/**
	 * Returns a predicate that tests if a count is a multiple of the specified
	 * number.
	 *
	 * @param count the base count value
	 * @return a predicate for testing multiples
	 */
	public static java.util.function.LongPredicate multipleOf(long count) {
		if (count <= 0)
			throw new IllegalArgumentException("count must be positive");
		return c -> c % count == 0;
	}

	/**
	 * Returns a predicate that tests if a count is a multiple of the specified
	 * value in the given unit.
	 *
	 * @param value the value
	 * @param unit  the unit of the value
	 * @return a predicate for testing multiples
	 */
	public static java.util.function.LongPredicate multipleOf(long value, CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long countMultiple = unit.toCount(value);
		return c -> c % countMultiple == 0;
	}

	/**
	 * Returns a predicate that tests if a count is a power of ten.
	 *
	 * @return a predicate for testing power of ten
	 */
	public static java.util.function.LongPredicate powerOfTen() {
		return c -> {
			if (c <= 0)
				return false;
			long temp = c;
			while (temp % 10 == 0)
				temp /= 10;
			return temp == 1;
		};
	}

	/**
	 * Returns a predicate that tests if a count is within the specified range
	 * (inclusive).
	 *
	 * @param min the minimum count
	 * @param max the maximum count
	 * @return a predicate for testing range
	 */
	public static java.util.function.LongPredicate between(Count min, Count max) {
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		return c -> c >= min.count && c <= max.count;
	}

	/**
	 * Returns a predicate that tests if a count is at least the specified minimum.
	 *
	 * @param min the minimum count
	 * @return a predicate for testing minimum
	 */
	public static java.util.function.LongPredicate atLeast(Count min) {
		Objects.requireNonNull(min, "min");
		return c -> c >= min.count;
	}

	/**
	 * Returns a predicate that tests if a count is at most the specified maximum.
	 *
	 * @param max the maximum count
	 * @return a predicate for testing maximum
	 */
	public static java.util.function.LongPredicate atMost(Count max) {
		Objects.requireNonNull(max, "max");
		return c -> c <= max.count;
	}

	/**
	 * Gets the base count value.
	 *
	 * @return the base count value
	 */
	public long toCount() {
		return count;
	}

	/**
	 * Gets the count in kilo units.
	 *
	 * @return the count in kilo units
	 */
	public long toKilo() {
		return CountUnit.KILO.convert(count, CountUnit.COUNT);
	}

	/**
	 * Gets the count in mega units.
	 *
	 * @return the count in mega units
	 */
	public long toMega() {
		return CountUnit.MEGA.convert(count, CountUnit.COUNT);
	}

	/**
	 * Gets the count in giga units.
	 *
	 * @return the count in giga units
	 */
	public long toGiga() {
		return CountUnit.GIGA.convert(count, CountUnit.COUNT);
	}

	/**
	 * Gets the count in tera units.
	 *
	 * @return the count in tera units
	 */
	public long toTera() {
		return CountUnit.TERA.convert(count, CountUnit.COUNT);
	}

	/**
	 * Gets the count in peta units.
	 *
	 * @return the count in peta units
	 */
	public long toPeta() {
		return CountUnit.PETA.convert(count, CountUnit.COUNT);
	}

	/**
	 * Converts this count to the specified unit.
	 *
	 * @param unit the target unit
	 * @return the count in the specified unit
	 */
	public long to(CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convert(count, CountUnit.COUNT);
	}

	/**
	 * Converts this count to the specified unit as a double for precision.
	 *
	 * @param unit the target unit
	 * @return the count in the specified unit as a double
	 */
	public double toDouble(CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convertf(count, CountUnit.COUNT);
	}

	/**
	 * Returns a copy of this count with the specified count added.
	 *
	 * @param other the count to add
	 * @return a Count based on this count with the specified count added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plus(Count other) {
		Objects.requireNonNull(other, "other");
		return ofCount(Math.addExact(count, other.count));
	}

	/**
	 * Returns a copy of this count with the specified value added.
	 *
	 * @param value the value to add
	 * @param unit  the unit of the value
	 * @return a Count based on this count with the specified value added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plus(long value, CountUnit unit) {
		return plus(Count.of(value, unit));
	}

	/**
	 * Returns a copy of this count with the specified base count added.
	 *
	 * @param count the count to add
	 * @return a Count based on this count with the specified count added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plusCount(long count) {
		return ofCount(Math.addExact(this.count, count));
	}

	/**
	 * Returns a copy of this count with the specified number of kilo units added.
	 *
	 * @param kilo the kilo units to add
	 * @return a Count based on this count with the specified kilo units added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plusKilo(long kilo) {
		return plus(kilo, CountUnit.KILO);
	}

	/**
	 * Returns a copy of this count with the specified number of mega units added.
	 *
	 * @param mega the mega units to add
	 * @return a Count based on this count with the specified mega units added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plusMega(long mega) {
		return plus(mega, CountUnit.MEGA);
	}

	/**
	 * Returns a copy of this count with the specified number of giga units added.
	 *
	 * @param giga the giga units to add
	 * @return a Count based on this count with the specified giga units added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count plusGiga(long giga) {
		return plus(giga, CountUnit.GIGA);
	}

	/**
	 * Returns a copy of this count with the specified count subtracted.
	 *
	 * @param other the count to subtract
	 * @return a Count based on this count with the specified count subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minus(Count other) {
		Objects.requireNonNull(other, "other");
		return ofCount(Math.subtractExact(count, other.count));
	}

	/**
	 * Returns a copy of this count with the specified value subtracted.
	 *
	 * @param value the value to subtract
	 * @param unit  the unit of the value
	 * @return a Count based on this count with the specified value subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minus(long value, CountUnit unit) {
		return minus(Count.of(value, unit));
	}

	/**
	 * Returns a copy of this count with the specified base count subtracted.
	 *
	 * @param count the count to subtract
	 * @return a Count based on this count with the specified count subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minusCount(long count) {
		return ofCount(Math.subtractExact(this.count, count));
	}

	/**
	 * Returns a copy of this count with the specified number of kilo units
	 * subtracted.
	 *
	 * @param kilo the kilo units to subtract
	 * @return a Count based on this count with the specified kilo units subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minusKilo(long kilo) {
		return minus(kilo, CountUnit.KILO);
	}

	/**
	 * Returns a copy of this count with the specified number of mega units
	 * subtracted.
	 *
	 * @param mega the mega units to subtract
	 * @return a Count based on this count with the specified mega units subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minusMega(long mega) {
		return minus(mega, CountUnit.MEGA);
	}

	/**
	 * Returns a copy of this count with the specified number of giga units
	 * subtracted.
	 *
	 * @param giga the giga units to subtract
	 * @return a Count based on this count with the specified giga units subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count minusGiga(long giga) {
		return minus(giga, CountUnit.GIGA);
	}

	/**
	 * Returns a copy of this count multiplied by the specified scalar.
	 *
	 * @param multiplicand the value to multiply by
	 * @return a Count based on this count multiplied by the specified scalar
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count multipliedBy(long multiplicand) {
		return ofCount(Math.multiplyExact(count, multiplicand));
	}

	/**
	 * Returns a copy of this count divided by the specified divisor.
	 *
	 * @param divisor the value to divide by
	 * @return a Count based on this count divided by the specified divisor
	 * @throws ArithmeticException if the divisor is zero
	 */
	public Count dividedBy(long divisor) {
		if (divisor == 0)
			throw new ArithmeticException("Cannot divide by zero");
		return ofCount(count / divisor);
	}

	/**
	 * Returns a copy of this count with the count negated.
	 *
	 * @return a Count based on this count with the count negated
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Count negated() {
		return ofCount(Math.negateExact(count));
	}

	/**
	 * Returns a copy of this count with a positive count.
	 *
	 * @return a Count based on this count with an absolute count
	 */
	public Count abs() {
		return count < 0 ? negated() : this;
	}

	/**
	 * Checks if this count is zero.
	 *
	 * @return true if this count equals zero
	 */
	public boolean isZero() {
		return count == 0;
	}

	/**
	 * Checks if this count is negative.
	 *
	 * @return true if this count is negative
	 */
	public boolean isNegative() {
		return count < 0;
	}

	/**
	 * Checks if this count is a multiple of the specified number.
	 *
	 * @param count the number to check divisibility by
	 * @return true if this count is a multiple of the specified count
	 * @throws IllegalArgumentException if count is zero or negative
	 */
	public boolean isMultipleOf(long count) {
		if (count <= 0)
			throw new IllegalArgumentException("count must be positive");
		return this.count % count == 0;
	}

	/**
	 * Checks if this count is a multiple of the specified value in the given unit.
	 *
	 * @param value the value to check divisibility by
	 * @param unit  the unit of the value
	 * @return true if this count is a multiple of the specified value
	 * @throws IllegalArgumentException if value is zero or negative
	 */
	public boolean isMultipleOf(long value, CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long countMultiple = unit.toCount(value);
		return this.count % countMultiple == 0;
	}

	/**
	 * Checks if this count is a power of ten.
	 * 
	 * <p>
	 * A count is considered a power of ten if it is positive and can be expressed
	 * as 10^n for some non-negative integer n.
	 * </p>
	 *
	 * @return true if this count is a power of ten
	 */
	public boolean isPowerOfTen() {
		if (count <= 0)
			return false;
		long temp = count;
		while (temp % 10 == 0)
			temp /= 10;
		return temp == 1;
	}

	/**
	 * Tests this count against the provided predicate.
	 * 
	 * <p>
	 * The predicate is applied to the base count value. This allows for flexible
	 * validation logic to be applied.
	 * </p>
	 *
	 * @param predicate the predicate to test
	 * @return true if the predicate is satisfied
	 */
	public boolean test(java.util.function.LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		return predicate.test(count);
	}

	/**
	 * Validates that this count satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a default message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @return this Count for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Count validate(java.util.function.LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(count))
			throw new IllegalArgumentException("Count validation failed: " + this);
		return this;
	}

	/**
	 * Validates that this count satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * the specified message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @param message   the exception message to use if validation fails
	 * @return this Count for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Count validate(java.util.function.LongPredicate predicate, String message) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(count))
			throw new IllegalArgumentException(message);
		return this;
	}

	/**
	 * Validates that this count satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a message supplied by the messageSupplier. The supplier is only invoked if
	 * validation fails, allowing for efficient lazy message construction.
	 * </p>
	 *
	 * @param predicate       the predicate to validate against
	 * @param messageSupplier the supplier of the exception message
	 * @return this Count for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Count validate(java.util.function.LongPredicate predicate,
			java.util.function.Supplier<String> messageSupplier) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(messageSupplier, "messageSupplier");
		if (!predicate.test(count))
			throw new IllegalArgumentException(messageSupplier.get());
		return this;
	}

	/**
	 * Compares this count to the specified count.
	 *
	 * @param other the other count to compare to
	 * @return the comparator value, negative if less, positive if greater
	 */
	@Override
	public int compareTo(Count other) {
		return Long.compare(this.count, other.count);
	}

	/**
	 * Checks if this count is equal to the specified count.
	 *
	 * @param obj the object to check
	 * @return true if this is equal to the other count
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Count))
			return false;
		Count other = (Count) obj;
		return this.count == other.count;
	}

	/**
	 * Returns a hash code for this count.
	 *
	 * @return a suitable hash code
	 */
	@Override
	public int hashCode() {
		return Long.hashCode(count);
	}

	/**
	 * Returns a string representation of this count using the nearest appropriate
	 * unit.
	 * 
	 * <p>
	 * The format is controlled by the CountUnit formatting rules and will
	 * automatically select the most appropriate unit for readability.
	 * </p>
	 *
	 * @return a string representation of this count
	 */
	@Override
	public String toString() {
		return CountUnit.formatScaled("%v %s", count);
	}

	/**
	 * Returns a string representation of this count in the specified unit.
	 *
	 * @param unit the unit to use for the string representation
	 * @return a string representation of this count in the specified unit
	 */
	public String toString(CountUnit unit) {
		Objects.requireNonNull(unit, "unit");
		long value = to(unit);
		return value + " " + unit.getSymbol().toUpperCase();
	}

	/**
	 * Formats this count according to the specified format string.
	 * 
	 * <p>
	 * The format string is passed to {@link CountUnit#formatScaled(String, long)}
	 * for processing.
	 * </p>
	 *
	 * @param format the format string
	 * @return the formatted string
	 */
	public String format(String format) {
		Objects.requireNonNull(format, "format");
		return CountUnit.formatScaled(format, count);
	}
}