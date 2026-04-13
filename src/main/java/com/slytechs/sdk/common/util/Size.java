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
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable representation of a data size.
 * 
 * <p>
 * This class models a data size as a quantity in bytes and provides methods for
 * conversion to and from different {@link SizeUnit}s. It follows the design
 * patterns of the java.time API classes like Duration.
 * </p>
 * 
 * <p>
 * The class stores the size internally in bytes and supports sizes up to
 * {@link Long#MAX_VALUE} bytes. All operations that would result in overflow
 * throw {@link ArithmeticException}.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * {@snippet :
 * // Create data sizes
 * MemorySize size1 = MemorySize.ofMegabytes(100);
 * MemorySize size2 = MemorySize.of(5, MemoryUnit.GIGABYTES);
 * 
 * // Arithmetic operations
 * MemorySize total = size1.plus(size2);
 * MemorySize half = total.dividedBy(2);
 * 
 * // Conversions
 * long bytes = total.toBytes();
 * long mb = total.toMegabytes();
 * 
 * // Formatting
 * System.out.println(total);                    // "5.1 GB"
 * System.out.println(total.format("%v %s"));    // "5.1 GB"
 * System.out.println(total.toString(MemoryUnit.MEGABYTES)); // "5100 MB"
 * 
 * // Parsing
 * MemorySize parsed = MemorySize.parse("10 GB");
 * 
 * // Validation - checking constraints
 * MemorySize bufferSize = MemorySize.ofKilobytes(4);
 * boolean is4KB = bufferSize.isMultipleOf(4, MemoryUnit.KILOBYTES);
 * boolean isPower2 = bufferSize.isPowerOfTwo();
 * 
 * // Using predicates for validation
 * MemorySize.ofMegabytes(16)
 * 		.validate(MemorySize.multipleOf(1, MemoryUnit.MEGABYTES))
 * 		.validate(MemorySize.powerOfTwo())
 * 		.validate(MemorySize.between(
 * 				MemorySize.ofMegabytes(1),
 * 				MemorySize.ofGigabytes(1)));
 * 
 * // Custom validation with message
 * MemorySize.ofBytes(1024)
 * 		.validate(size -> size >= 512, "Size must be at least 512 bytes");
 * }
 *
 * @author Mark Bednarczyk
 */
public final class Size implements Comparable<Size> {

	/** A constant for zero bytes. */
	public static final Size ZERO = new Size(0);

	/** Pattern for parsing data size strings. */
	private static final Pattern PARSE_PATTERN = Pattern.compile(
			"^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)?\\s*$");

	/** The size in bytes. */
	private final long bytes;

	/**
	 * Private constructor to enforce factory method usage.
	 *
	 * @param bytes the size in bytes
	 */
	private Size(long bytes) {
		this.bytes = bytes;
	}

	/**
	 * Obtains a MemorySize representing the specified value in the specified unit.
	 *
	 * @param value the size value
	 * @param unit  the unit of the size
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size of(long value, SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return new Size(unit.toBytes(value));
	}

	/**
	 * Obtains a MemorySize representing the specified number of bits.
	 *
	 * @param bits the number of bits
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofBits(long bits) {
		return new Size(SizeUnit.BITS.toBytes(bits));
	}

	/**
	 * Obtains a MemorySize representing the specified number of bytes.
	 *
	 * @param bytes the number of bytes
	 * @return a MemorySize
	 */
	public static Size ofBytes(long bytes) {
		return bytes == 0 ? ZERO : new Size(bytes);
	}

	/**
	 * Obtains a MemorySize representing the specified number of kilobytes.
	 *
	 * @param kilobytes the number of kilobytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofKilobytes(long kilobytes) {
		return new Size(SizeUnit.KILOBYTES.toBytes(kilobytes));
	}

	/**
	 * Obtains a MemorySize representing the specified number of megabytes.
	 *
	 * @param megabytes the number of megabytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofMegabytes(long megabytes) {
		return new Size(SizeUnit.MEGABYTES.toBytes(megabytes));
	}

	/**
	 * Obtains a MemorySize representing the specified number of gigabytes.
	 *
	 * @param gigabytes the number of gigabytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofGigabytes(long gigabytes) {
		return new Size(SizeUnit.GIGABYTES.toBytes(gigabytes));
	}

	/**
	 * Obtains a MemorySize representing the specified number of terabytes.
	 *
	 * @param terabytes the number of terabytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofTerabytes(long terabytes) {
		return new Size(SizeUnit.TERABYTES.toBytes(terabytes));
	}

	/**
	 * Obtains a MemorySize representing the specified number of petabytes.
	 *
	 * @param petabytes the number of petabytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofPetabytes(long petabytes) {
		return new Size(SizeUnit.PETABYTES.toBytes(petabytes));
	}

	/**
	 * Obtains a MemorySize representing the specified number of exabytes.
	 *
	 * @param exabytes the number of exabytes
	 * @return a MemorySize
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public static Size ofExabytes(long exabytes) {
		return new Size(SizeUnit.EXABYTES.toBytes(exabytes));
	}

	/**
	 * Obtains a MemorySize from a text string such as "10 GB", "5MB", or "1024".
	 * 
	 * <p>
	 * The string must consist of an optional sign, a number (integer or decimal),
	 * and an optional unit suffix. Valid unit suffixes are case-insensitive and
	 * include any of the symbols recognized by {@link SizeUnit}. If no unit is
	 * specified, bytes are assumed.
	 * </p>
	 * 
	 * <p>
	 * Examples: "100", "10 KB", "5.5GB", "-256mb", "+1024 bytes"
	 * </p>
	 *
	 * @param text the text to parse
	 * @return a MemorySize
	 * @throws IllegalArgumentException if the text cannot be parsed
	 */
	public static Size parse(CharSequence text) {
		Objects.requireNonNull(text, "text");
		Matcher matcher = PARSE_PATTERN.matcher(text);

		if (!matcher.matches())
			throw new IllegalArgumentException("Cannot parse MemorySize: " + text);

		String valueStr = matcher.group(1);
		String unitStr = matcher.group(2);

		double value = Double.parseDouble(valueStr);

		SizeUnit unit = SizeUnit.BYTES;
		if (unitStr != null && !unitStr.isEmpty()) {
			unit = parseUnit(unitStr);
		}

		long bytes = (long) (value * unit.toBytes(1));
		return new Size(bytes);
	}

	/**
	 * Parses a unit string to a MemoryUnit.
	 *
	 * @param unitStr the unit string
	 * @return the MemoryUnit
	 * @throws IllegalArgumentException if the unit cannot be parsed
	 */
	private static SizeUnit parseUnit(String unitStr) {
		String normalized = unitStr.toLowerCase().trim();

		for (SizeUnit unit : SizeUnit.values()) {
			if (unit.name().equalsIgnoreCase(normalized))
				return unit;

			for (String symbol : unit.getSymbols()) {
				if (symbol.equalsIgnoreCase(normalized))
					return unit;
			}
		}

		throw new IllegalArgumentException("Unknown memory unit: " + unitStr);
	}

	/**
	 * Returns the minimum of two data sizes.
	 *
	 * @param size1 the first size
	 * @param size2 the second size
	 * @return the minimum size
	 */
	public static Size min(Size size1, Size size2) {
		Objects.requireNonNull(size1, "size1");
		Objects.requireNonNull(size2, "size2");
		return size1.bytes <= size2.bytes ? size1 : size2;
	}

	/**
	 * Returns the maximum of two data sizes.
	 *
	 * @param size1 the first size
	 * @param size2 the second size
	 * @return the maximum size
	 */
	public static Size max(Size size1, Size size2) {
		Objects.requireNonNull(size1, "size1");
		Objects.requireNonNull(size2, "size2");
		return size1.bytes >= size2.bytes ? size1 : size2;
	}

	/**
	 * Returns a predicate that tests if a size is a multiple of the specified
	 * number of bytes.
	 *
	 * @param bytes the number of bytes
	 * @return a predicate for testing multiples
	 */
	public static LongPredicate multipleOf(long bytes) {
		if (bytes <= 0)
			throw new IllegalArgumentException("bytes must be positive");
		return size -> size % bytes == 0;
	}

	/**
	 * Returns a predicate that tests if a size is a multiple of the specified value
	 * in the given unit.
	 *
	 * @param value the value
	 * @param unit  the unit of the value
	 * @return a predicate for testing multiples
	 */
	public static LongPredicate multipleOf(long value, SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long bytesMultiple = unit.toBytes(value);
		return size -> size % bytesMultiple == 0;
	}

	/**
	 * Returns a predicate that tests if a size is a power of two.
	 *
	 * @return a predicate for testing power of two
	 */
	public static LongPredicate powerOfTwo() {
		return size -> size > 0 && (size & (size - 1)) == 0;
	}

	/**
	 * Returns a predicate that tests if a size is within the specified range
	 * (inclusive).
	 *
	 * @param min the minimum size
	 * @param max the maximum size
	 * @return a predicate for testing range
	 */
	public static LongPredicate between(Size min, Size max) {
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		return size -> size >= min.bytes && size <= max.bytes;
	}

	/**
	 * Returns a predicate that tests if a size is at least the specified minimum.
	 *
	 * @param min the minimum size
	 * @return a predicate for testing minimum
	 */
	public static LongPredicate atLeast(Size min) {
		Objects.requireNonNull(min, "min");
		return size -> size >= min.bytes;
	}

	/**
	 * Returns a predicate that tests if a size is at most the specified maximum.
	 *
	 * @param max the maximum size
	 * @return a predicate for testing maximum
	 */
	public static LongPredicate atMost(Size max) {
		Objects.requireNonNull(max, "max");
		return size -> size <= max.bytes;
	}

	/**
	 * Gets the size in bits.
	 *
	 * @return the size in bits
	 */
	public long toBits() {
		return SizeUnit.BYTES.toBits(bytes);
	}

	/**
	 * Gets the size in bytes.
	 *
	 * @return the size in bytes
	 */
	public long toBytes() {
		return bytes;
	}

	/**
	 * Gets the size in kilobytes.
	 *
	 * @return the size in kilobytes
	 */
	public long toKilobytes() {
		return SizeUnit.KILOBYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Gets the size in megabytes.
	 *
	 * @return the size in megabytes
	 */
	public long toMegabytes() {
		return SizeUnit.MEGABYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Gets the size in gigabytes.
	 *
	 * @return the size in gigabytes
	 */
	public long toGigabytes() {
		return SizeUnit.GIGABYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Gets the size in terabytes.
	 *
	 * @return the size in terabytes
	 */
	public long toTerabytes() {
		return SizeUnit.TERABYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Gets the size in petabytes.
	 *
	 * @return the size in petabytes
	 */
	public long toPetabytes() {
		return SizeUnit.PETABYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Gets the size in exabytes.
	 *
	 * @return the size in exabytes
	 */
	public long toExabytes() {
		return SizeUnit.EXABYTES.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Converts this size to the specified unit.
	 *
	 * @param unit the target unit
	 * @return the size in the specified unit
	 */
	public long to(SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convert(bytes, SizeUnit.BYTES);
	}

	/**
	 * Converts this size to the specified unit as a double for precision.
	 *
	 * @param unit the target unit
	 * @return the size in the specified unit as a double
	 */
	public double toDouble(SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		return unit.convertf(bytes, SizeUnit.BYTES);
	}

	/**
	 * Returns a copy of this size with the specified size added.
	 *
	 * @param other the size to add
	 * @return a MemorySize based on this size with the specified size added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plus(Size other) {
		Objects.requireNonNull(other, "other");
		return ofBytes(Math.addExact(bytes, other.bytes));
	}

	/**
	 * Returns a copy of this size with the specified size added.
	 *
	 * @param value the value to add
	 * @param unit  the unit of the value
	 * @return a MemorySize based on this size with the specified size added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plus(long value, SizeUnit unit) {
		return plus(Size.of(value, unit));
	}

	/**
	 * Returns a copy of this size with the specified number of bytes added.
	 *
	 * @param bytes the bytes to add
	 * @return a MemorySize based on this size with the specified bytes added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plusBytes(long bytes) {
		return ofBytes(Math.addExact(this.bytes, bytes));
	}

	/**
	 * Returns a copy of this size with the specified number of kilobytes added.
	 *
	 * @param kilobytes the kilobytes to add
	 * @return a MemorySize based on this size with the specified kilobytes added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plusKilobytes(long kilobytes) {
		return plus(kilobytes, SizeUnit.KILOBYTES);
	}

	/**
	 * Returns a copy of this size with the specified number of megabytes added.
	 *
	 * @param megabytes the megabytes to add
	 * @return a MemorySize based on this size with the specified megabytes added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plusMegabytes(long megabytes) {
		return plus(megabytes, SizeUnit.MEGABYTES);
	}

	/**
	 * Returns a copy of this size with the specified number of gigabytes added.
	 *
	 * @param gigabytes the gigabytes to add
	 * @return a MemorySize based on this size with the specified gigabytes added
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size plusGigabytes(long gigabytes) {
		return plus(gigabytes, SizeUnit.GIGABYTES);
	}

	/**
	 * Returns a copy of this size with the specified size subtracted.
	 *
	 * @param other the size to subtract
	 * @return a MemorySize based on this size with the specified size subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minus(Size other) {
		Objects.requireNonNull(other, "other");
		return ofBytes(Math.subtractExact(bytes, other.bytes));
	}

	/**
	 * Returns a copy of this size with the specified size subtracted.
	 *
	 * @param value the value to subtract
	 * @param unit  the unit of the value
	 * @return a MemorySize based on this size with the specified size subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minus(long value, SizeUnit unit) {
		return minus(Size.of(value, unit));
	}

	/**
	 * Returns a copy of this size with the specified number of bytes subtracted.
	 *
	 * @param bytes the bytes to subtract
	 * @return a MemorySize based on this size with the specified bytes subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minusBytes(long bytes) {
		return ofBytes(Math.subtractExact(this.bytes, bytes));
	}

	/**
	 * Returns a copy of this size with the specified number of kilobytes
	 * subtracted.
	 *
	 * @param kilobytes the kilobytes to subtract
	 * @return a MemorySize based on this size with the specified kilobytes
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minusKilobytes(long kilobytes) {
		return minus(kilobytes, SizeUnit.KILOBYTES);
	}

	/**
	 * Returns a copy of this size with the specified number of megabytes
	 * subtracted.
	 *
	 * @param megabytes the megabytes to subtract
	 * @return a MemorySize based on this size with the specified megabytes
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minusMegabytes(long megabytes) {
		return minus(megabytes, SizeUnit.MEGABYTES);
	}

	/**
	 * Returns a copy of this size with the specified number of gigabytes
	 * subtracted.
	 *
	 * @param gigabytes the gigabytes to subtract
	 * @return a MemorySize based on this size with the specified gigabytes
	 *         subtracted
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size minusGigabytes(long gigabytes) {
		return minus(gigabytes, SizeUnit.GIGABYTES);
	}

	/**
	 * Returns a copy of this size multiplied by the specified scalar.
	 *
	 * @param multiplicand the value to multiply by
	 * @return a MemorySize based on this size multiplied by the specified scalar
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size multipliedBy(long multiplicand) {
		return ofBytes(Math.multiplyExact(bytes, multiplicand));
	}

	/**
	 * Returns a copy of this size divided by the specified divisor.
	 *
	 * @param divisor the value to divide by
	 * @return a MemorySize based on this size divided by the specified divisor
	 * @throws ArithmeticException if the divisor is zero
	 */
	public Size dividedBy(long divisor) {
		if (divisor == 0)
			throw new ArithmeticException("Cannot divide by zero");
		return ofBytes(bytes / divisor);
	}

	/**
	 * Returns a copy of this size with the size negated.
	 *
	 * @return a MemorySize based on this size with the size negated
	 * @throws ArithmeticException if numeric overflow occurs
	 */
	public Size negated() {
		return ofBytes(Math.negateExact(bytes));
	}

	/**
	 * Returns a copy of this size with a positive size.
	 *
	 * @return a MemorySize based on this size with an absolute size
	 */
	public Size abs() {
		return bytes < 0 ? negated() : this;
	}

	/**
	 * Checks if this size is zero.
	 *
	 * @return true if this size has a total size equal to zero
	 */
	public boolean isZero() {
		return bytes == 0;
	}

	/**
	 * Checks if this size is negative.
	 *
	 * @return true if this size is negative
	 */
	public boolean isNegative() {
		return bytes < 0;
	}

	/**
	 * Checks if this size is a multiple of the specified number of bytes.
	 *
	 * @param bytes the number of bytes to check divisibility by
	 * @return true if this size is a multiple of the specified bytes
	 * @throws IllegalArgumentException if bytes is zero or negative
	 */
	public boolean isMultipleOf(long bytes) {
		if (bytes <= 0)
			throw new IllegalArgumentException("bytes must be positive");
		return this.bytes % bytes == 0;
	}

	/**
	 * Checks if this size is a multiple of the specified value in the given unit.
	 *
	 * @param value the value to check divisibility by
	 * @param unit  the unit of the value
	 * @return true if this size is a multiple of the specified value
	 * @throws IllegalArgumentException if value is zero or negative
	 */
	public boolean isMultipleOf(long value, SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		if (value <= 0)
			throw new IllegalArgumentException("value must be positive");
		long bytesMultiple = unit.toBytes(value);
		return this.bytes % bytesMultiple == 0;
	}

	/**
	 * Checks if this size is a power of two.
	 * 
	 * <p>
	 * A size is considered a power of two if it is positive and has exactly one bit
	 * set in its binary representation.
	 * </p>
	 *
	 * @return true if this size is a power of two
	 */
	public boolean isPowerOfTwo() {
		return bytes > 0 && (bytes & (bytes - 1)) == 0;
	}

	/**
	 * Tests this size against the provided predicate.
	 * 
	 * <p>
	 * The predicate is applied to the size in bytes. This allows for flexible
	 * validation logic to be applied.
	 * </p>
	 *
	 * @param predicate the predicate to test
	 * @return true if the predicate is satisfied
	 */
	public boolean test(LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		return predicate.test(bytes);
	}

	/**
	 * Validates that this size satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a default message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @return this MemorySize for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Size validate(LongPredicate predicate) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(bytes))
			throw new IllegalArgumentException("Size validation failed: " + this);
		return this;
	}

	/**
	 * Validates that this size satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * the specified message.
	 * </p>
	 *
	 * @param predicate the predicate to validate against
	 * @param message   the exception message to use if validation fails
	 * @return this MemorySize for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Size validate(LongPredicate predicate, String message) {
		Objects.requireNonNull(predicate, "predicate");
		if (!predicate.test(bytes))
			throw new IllegalArgumentException(message);
		return this;
	}

	/**
	 * Validates that this size satisfies the provided predicate.
	 * 
	 * <p>
	 * If the predicate is not satisfied, an IllegalArgumentException is thrown with
	 * a message supplied by the messageSupplier. The supplier is only invoked if
	 * validation fails, allowing for efficient lazy message construction.
	 * </p>
	 *
	 * @param predicate       the predicate to validate against
	 * @param messageSupplier the supplier of the exception message
	 * @return this MemorySize for method chaining
	 * @throws IllegalArgumentException if the predicate is not satisfied
	 */
	public Size validate(LongPredicate predicate,
			Supplier<String> messageSupplier) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(messageSupplier, "messageSupplier");
		if (!predicate.test(bytes))
			throw new IllegalArgumentException(messageSupplier.get());
		return this;
	}

	/**
	 * Compares this size to the specified size.
	 *
	 * @param other the other size to compare to
	 * @return the comparator value, negative if less, positive if greater
	 */
	@Override
	public int compareTo(Size other) {
		return Long.compare(this.bytes, other.bytes);
	}

	/**
	 * Checks if this size is equal to the specified size.
	 *
	 * @param obj the object to check
	 * @return true if this is equal to the other size
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Size))
			return false;
		Size other = (Size) obj;
		return this.bytes == other.bytes;
	}

	/**
	 * Returns a hash code for this size.
	 *
	 * @return a suitable hash code
	 */
	@Override
	public int hashCode() {
		return Long.hashCode(bytes);
	}

	/**
	 * Returns a string representation of this size using the nearest appropriate
	 * unit.
	 * 
	 * <p>
	 * The format is controlled by the MemoryUnit formatting rules and will
	 * automatically select the most appropriate unit for readability.
	 * </p>
	 *
	 * @return a string representation of this size
	 */
	@Override
	public String toString() {
		return SizeUnit.format("%s %s", bytes);
	}

	/**
	 * Returns a string representation of this size in the specified unit.
	 *
	 * @param unit the unit to use for the string representation
	 * @return a string representation of this size in the specified unit
	 */
	public String toString(SizeUnit unit) {
		Objects.requireNonNull(unit, "unit");
		long value = to(unit);
		return value + " " + unit.getSymbol().toUpperCase();
	}

	/**
	 * Formats this size according to the specified format string.
	 * 
	 * <p>
	 * The format string is passed to {@link SizeUnit#format(String, long)} for
	 * processing.
	 * </p>
	 *
	 * @param format the format string
	 * @return the formatted string
	 */
	public String format(String format) {
		Objects.requireNonNull(format, "format");
		return SizeUnit.format(format, bytes);
	}
}