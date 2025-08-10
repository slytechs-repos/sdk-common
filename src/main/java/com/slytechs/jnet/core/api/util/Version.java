package com.slytechs.jnet.core.api.util;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A comprehensive implementation of Semantic Versioning 2.0.0 as defined by
 * <a href="https://semver.org">semver.org</a>. This class provides parsing,
 * comparison, and validation of version strings that follow the SemVer
 * specification.
 * 
 * <h2>Version Format</h2> A version number must take the form X.Y.Z where X, Y,
 * and Z are non-negative integers, and MUST NOT contain leading zeroes. Each
 * element MUST increase numerically.
 * <ul>
 * <li>X = Major version (incompatible API changes)</li>
 * <li>Y = Minor version (backwards-compatible functionality)</li>
 * <li>Z = Patch version (backwards-compatible bug fixes)</li>
 * </ul>
 * 
 * <h2>Additional Labels</h2>
 * <ul>
 * <li>Pre-release: A pre-release version MAY be denoted by appending a hyphen
 * and a series of dot-separated identifiers (e.g., 1.0.0-alpha.1)</li>
 * <li>Build metadata: Build metadata MAY be denoted by appending a plus sign
 * and a series of dot-separated identifiers (e.g., 1.0.0+20130313144700)</li>
 * </ul>
 * 
 * <h2>Examples of Valid Versions</h2>
 * 
 * <pre>
 * 1.0.0
 * 2.1.0
 * 1.0.0-alpha
 * 1.0.0-alpha.1
 * 1.0.0-alpha.beta
 * 1.0.0-beta
 * 1.0.0-beta.2
 * 1.0.0-beta.11
 * 1.0.0-rc.1
 * 1.0.0-rc.1+build.123
 * 2.1.0+build.123
 * </pre>
 * 
 * <h2>Version Precedence</h2> Precedence refers to how versions are compared to
 * each other when ordered.
 * <ol>
 * <li>Major, minor, and patch versions are compared numerically.</li>
 * <li>When major, minor, and patch are equal, a pre-release version has lower
 * precedence than a normal version.</li>
 * <li>Build metadata DOES NOT affect precedence.</li>
 * </ol>
 * 
 * @author Sly Technologies
 * @version 2.0
 * @see <a href="https://semver.org">Semantic Versioning 2.0.0 Specification</a>
 */
public class Version implements Comparable<Version> {

	private final int major;
	private final int minor;
	private final int patch;
	private final String preRelease; // Optional pre-release tag (e.g., "alpha.1")
	private final String buildMeta; // Optional build metadata (e.g., "build.123")

	/**
	 * Regular expression pattern for validating SemVer format. Matches:
	 * <ul>
	 * <li>Major version: (0|[1-9]\d*)</li>
	 * <li>Minor version: \.(0|[1-9]\d*)</li>
	 * <li>Patch version: \.(0|[1-9]\d*)</li>
	 * <li>Pre-release version (optional): -([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)</li>
	 * <li>Build metadata (optional): \+([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)</li>
	 * </ul>
	 */
	private static final String VERSION_PATTERN = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
			+ "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
			+ "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
			+ "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

	private static final String SIMPLE_VERSION_PATTERN = "(\\d+\\.\\d+\\.\\d+)";

	/**
	 * Extracts a semantic version number from the provided input string.
	 *
	 * <p>
	 * This method scans the input string for a substring that matches the Semantic
	 * Versioning format, which follows the pattern <code>MAJOR.MINOR.PATCH</code>.
	 * It also supports optional pre-release and build metadata identifiers as
	 * defined by Semantic Versioning specifications.
	 *
	 * <p>
	 * If a version number adhering to the semantic versioning pattern is found
	 * within the input string, it is returned wrapped in an {@link Optional}. If no
	 * valid version number is present, {@link Optional#empty()} is returned.
	 *
	 * <h3>Examples:</h3>
	 *
	 * <pre>{@code
	 * // Example 1: Basic semantic version
	 * String input1 = "libpcap version 1.10.4 (with TPACKET_V3)";
	 * Optional<String> version1 = Version.extractFromString(input1);
	 * // version1 contains "1.10.4"
	 *
	 * // Example 2: Semantic version with pre-release and build metadata
	 * String input2 = "Release candidate version 2.1.3-rc.1+build.789";
	 * Optional<String> version2 = Version.extractFromString(input2);
	 * // version2 contains "2.1.3-rc.1+build.789"
	 *
	 * // Example 3: No version present
	 * String input3 = "No version information available.";
	 * Optional<String> version3 = Version.extractFromString(input3);
	 * // version3 is empty
	 * }</pre>
	 *
	 * @param str the input string from which to extract the semantic version number
	 * @return an {@link Optional} containing the extracted version number if
	 *         present, or {@link Optional#empty()} if no valid semantic version is
	 *         found
	 * @throws NullPointerException if the input string {@code str} is {@code null}
	 * 
	 * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
	 */
	public static Optional<String> extractFromString(String str) {
		Pattern pattern = Pattern.compile(SIMPLE_VERSION_PATTERN);
		var matcher = pattern.matcher(str);

		return Optional.ofNullable(matcher.find() ? matcher.group(1) : null);
	}

	/**
	 * Validates semantic version compatibility between an installed library and an
	 * application. Performs a minimal check that ensures the major versions match
	 * according to Semantic Versioning rules.
	 * 
	 * This method follows these rules:
	 * <ul>
	 * <li>Major versions must match exactly (e.g., 2.x.x is compatible with
	 * 2.y.y)</li>
	 * <li>Application's minor version must not be greater than library's minor
	 * version</li>
	 * <li>Patch versions are not considered in this check</li>
	 * </ul>
	 *
	 * Example compatible versions:
	 * 
	 * <pre>
	 *   Library: 2.3.0, Application: 2.3.1  (compatible)
	 *   Library: 2.4.0, Application: 2.3.0  (compatible)
	 *   Library: 2.0.0, Application: 2.0.1  (compatible)
	 *   Library: 3.0.0, Application: 2.0.0  (incompatible - major version mismatch)
	 *   Library: 2.1.0, Application: 2.2.0  (incompatible - app minor version too high)
	 * </pre>
	 *
	 * @param name       the name of the library or component being checked
	 * @param libVersion the installed library version (format: MAJOR.MINOR.PATCH)
	 * @param appVersion the application version requiring the library (format:
	 *                   MAJOR.MINOR.PATCH)
	 * @throws InvalidVersionException if the versions are incompatible or if either
	 *                                 version string is malformed (must be in
	 *                                 format MAJOR.MINOR.PATCH)
	 * @see <a href="https://semver.org">Semantic Versioning 2.0.0</a>
	 */
	public static void minimalCheck(String name, String libVersion, String appVersion) throws InvalidVersionException {
		var lib = new Version(libVersion);
		var app = new Version(appVersion);

		if (app.major == lib.major && app.minor <= lib.minor)
			return;

		throw new InvalidVersionException("%s library version [%s] is incompatible with app version [%s]"
				.formatted(name, libVersion, appVersion));
	}

	/**
	 * Validates strict semantic version compatibility between an installed library
	 * and an application. Performs a rigorous check that enforces exact major and
	 * minor version matches, with allowance only for higher library patch versions.
	 * 
	 * This method enforces these rules:
	 * <ul>
	 * <li>Major versions must match exactly (e.g., 2.x.x requires 2.y.y)</li>
	 * <li>Minor versions must match exactly (e.g., x.3.x requires x.3.y)</li>
	 * <li>Library patch version must be greater than or equal to application's
	 * patch version</li>
	 * </ul>
	 *
	 * Version compatibility examples:
	 * 
	 * <pre>
	 *   Library: 2.3.4, Application: 2.3.4  (compatible - exact match)
	 *   Library: 2.3.5, Application: 2.3.4  (compatible - higher lib patch)
	 *   Library: 2.3.3, Application: 2.3.4  (incompatible - lib patch too low)
	 *   Library: 2.4.0, Application: 2.3.0  (incompatible - minor version mismatch)
	 *   Library: 3.3.0, Application: 2.3.0  (incompatible - major version mismatch)
	 * </pre>
	 *
	 * @param name       the name of the library or component being checked
	 * @param libVersion the installed library version (format: MAJOR.MINOR.PATCH)
	 * @param appVersion the application version requiring the library (format:
	 *                   MAJOR.MINOR.PATCH)
	 * @throws InvalidVersionException if any of these conditions are met:
	 *                                 <ul>
	 *                                 <li>Either version string is malformed (must
	 *                                 be MAJOR.MINOR.PATCH)</li>
	 *                                 <li>Major versions do not match exactly</li>
	 *                                 <li>Minor versions do not match exactly</li>
	 *                                 <li>Library's patch version is lower than
	 *                                 application's</li>
	 *                                 </ul>
	 * @see <a href="https://semver.org">Semantic Versioning 2.0.0</a>
	 */
	public static void strictCheck(String name, String libVersion, String appVersion) throws InvalidVersionException {
		var lib = new Version(libVersion);
		var app = new Version(appVersion);

		if (app.major == lib.major && app.minor == lib.minor && app.patch <= lib.patch)
			return;

		throw new InvalidVersionException("%s library version [%s] is incompatible with app version [%s]"
				.formatted(name, libVersion, appVersion));
	}

	/**
	 * Parses a version string into its constituent components according to SemVer
	 * rules.
	 *
	 * @param version the version string to parse
	 * @return array containing [major, minor, patch, preRelease, buildMeta]
	 * @throws InvalidVersionException if the version string doesn't conform to
	 *                                 SemVer format
	 * @throws NullPointerException    if version is null
	 */

	private static String[] parseVersion(String version) throws InvalidVersionException {
		Objects.requireNonNull(version, "Version string cannot be null");

		if (!version.matches(VERSION_PATTERN)) {
			throw new InvalidVersionException("Invalid version format: " + version);
		}

		String[] components = new String[5];

		// Split on +, handling build metadata
		String[] buildParts = version.split("\\+", 2);
		String versionPart = buildParts[0];
		components[4] = buildParts.length > 1 ? buildParts[1] : null;

		// Split on -, handling pre-release
		String[] preParts = versionPart.split("-", 2);
		String numberPart = preParts[0];
		components[3] = preParts.length > 1 ? preParts[1] : null;

		// Split version numbers
		String[] numbers = numberPart.split("\\.");
		components[0] = numbers[0];
		components[1] = numbers[1];
		components[2] = numbers[2];

		return components;
	}

	/**
	 * Creates a new Version instance by parsing a version string that follows the
	 * SemVer format.
	 * 
	 * <p>
	 * Version string must be in one of these formats:
	 * </p>
	 * <ul>
	 * <li>MAJOR.MINOR.PATCH (e.g., "2.0.0")</li>
	 * <li>MAJOR.MINOR.PATCH-PRERELEASE (e.g., "2.0.0-alpha.1")</li>
	 * <li>MAJOR.MINOR.PATCH+BUILD (e.g., "2.0.0+build.123")</li>
	 * <li>MAJOR.MINOR.PATCH-PRERELEASE+BUILD (e.g., "2.0.0-alpha.1+build.123")</li>
	 * </ul>
	 *
	 * @param version the version string to parse
	 * @throws InvalidVersionException if the version string is invalid or malformed
	 * @throws NullPointerException    if version is null
	 */
	public Version(String version) throws InvalidVersionException {
		String[] components = parseVersion(version);

		try {
			this.major = Integer.parseInt(components[0]);
			this.minor = Integer.parseInt(components[1]);
			this.patch = Integer.parseInt(components[2]);
			this.preRelease = components[3];
			this.buildMeta = components[4];
		} catch (NumberFormatException e) {
			throw new InvalidVersionException("Invalid version numbers in: " + version, e);
		}
	}

	/**
	 * Creates a new Version with the specified major, minor, and patch versions.
	 * Pre-release and build metadata will be null.
	 *
	 * @param major the major version number (must be non-negative)
	 * @param minor the minor version number (must be non-negative)
	 * @param patch the patch version number (must be non-negative)
	 * @throws IllegalArgumentException if any version number is negative
	 */
	public Version(int major, int minor, int patch) {
		this(major, minor, patch, null, null);
	}

	/**
	 * Creates a new Version with the specified major, minor, patch versions and
	 * pre-release identifier. Build metadata will be null.
	 *
	 * @param major      the major version number (must be non-negative)
	 * @param minor      the minor version number (must be non-negative)
	 * @param patch      the patch version number (must be non-negative)
	 * @param preRelease the pre-release identifier (may be null)
	 * @throws IllegalArgumentException if any version number is negative
	 */
	public Version(int major, int minor, int patch, String preRelease) {
		this(major, minor, patch, preRelease, null);
	}

	/**
	 * Creates a new Version with all components specified.
	 *
	 * @param major      the major version number (must be non-negative)
	 * @param minor      the minor version number (must be non-negative)
	 * @param patch      the patch version number (must be non-negative)
	 * @param preRelease the pre-release identifier (may be null)
	 * @param buildMeta  the build metadata (may be null)
	 * @throws IllegalArgumentException if any version number is negative
	 */
	public Version(int major, int minor, int patch, String preRelease, String buildMeta) {
		if (major < 0 || minor < 0 || patch < 0) {
			throw new IllegalArgumentException("Version numbers cannot be negative");
		}

		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preRelease = preRelease;
		this.buildMeta = buildMeta;
	}

	/**
	 * Compares this Version with another Version for order according to SemVer
	 * precedence rules.
	 * 
	 * <p>
	 * Precedence is determined by comparing each dot-separated identifier from left
	 * to right until a difference is found:
	 * </p>
	 * <ol>
	 * <li>Major, minor, and patch versions are compared numerically</li>
	 * <li>Pre-release versions have lower precedence than the associated normal
	 * version</li>
	 * <li>Pre-release versions are compared by each dot-separated identifier:
	 * <ul>
	 * <li>Identifiers consisting of only digits are compared numerically</li>
	 * <li>Identifiers with letters or hyphens are compared lexically</li>
	 * <li>Numeric identifiers have lower precedence than non-numeric</li>
	 * </ul>
	 * </li>
	 * <li>Build metadata does not affect precedence</li>
	 * </ol>
	 *
	 * @param other the Version to be compared
	 * @return a negative integer, zero, or a positive integer as this Version is
	 *         less than, equal to, or greater than the specified Version
	 */
	@Override
	public int compareTo(Version other) {
		// Compare major.minor.patch
		int result = Integer.compare(this.major, other.major);
		if (result != 0)
			return result;

		result = Integer.compare(this.minor, other.minor);
		if (result != 0)
			return result;

		result = Integer.compare(this.patch, other.patch);
		if (result != 0)
			return result;

		// If versions are equal, pre-release versions have lower precedence
		if (this.preRelease == null && other.preRelease == null)
			return 0;
		if (this.preRelease == null)
			return 1; // No pre-release > pre-release
		if (other.preRelease == null)
			return -1;

		// Compare pre-release versions
		String[] thisParts = this.preRelease.split("\\.");
		String[] otherParts = other.preRelease.split("\\.");

		int length = Math.min(thisParts.length, otherParts.length);
		for (int i = 0; i < length; i++) {
			String thisPart = thisParts[i];
			String otherPart = otherParts[i];

			boolean thisIsNum = thisPart.matches("\\d+");
			boolean otherIsNum = otherPart.matches("\\d+");

			if (thisIsNum && otherIsNum) {
				// Compare numerically
				int thisNum = Integer.parseInt(thisPart);
				int otherNum = Integer.parseInt(otherPart);
				result = Integer.compare(thisNum, otherNum);
			} else {
				// Compare lexically
				result = thisPart.compareTo(otherPart);
			}

			if (result != 0)
				return result;
		}

		// If all parts match, longer version has higher precedence
		return Integer.compare(thisParts.length, otherParts.length);
	}

	/**
	 * Determines if this Version is compatible with another Version according to
	 * SemVer rules.
	 * 
	 * <p>
	 * Compatibility Rules:
	 * </p>
	 * <ul>
	 * <li>For versions 0.y.z (development versions):
	 * <ul>
	 * <li>Only exact matches are compatible</li>
	 * <li>Changes may break API at any time</li>
	 * </ul>
	 * </li>
	 * <li>For versions ≥ 1.0.0:
	 * <ul>
	 * <li>Major versions must match exactly</li>
	 * <li>The other version's minor must not be greater than this version's
	 * minor</li>
	 * <li>Patch versions do not affect compatibility</li>
	 * </ul>
	 * </li>
	 * </ul>
	 *
	 * @param v the Version to check for compatibility
	 * @return true if the versions are compatible, false otherwise
	 * @throws NullPointerException if v is null
	 */
	public boolean isCompatibleWith(Version libraryVersion) {
		Objects.requireNonNull(libraryVersion, "Library version cannot be null");

		// For 0.x.x versions, require exact match of major and minor
		if (this.major == 0) {
			return this.major == libraryVersion.major &&
					this.minor == libraryVersion.minor;
		}

		// For stable versions (1.0.0 and above):
		// 1. Major versions must match exactly
		// 2. Library's minor version must be >= than what we require
		return this.major == libraryVersion.major &&
				this.minor <= libraryVersion.minor;
	}

	/**
	 * Returns a string representation of this Version in SemVer format.
	 * 
	 * <p>
	 * Format: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
	 * </p>
	 *
	 * @return the formatted version string
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(major).append('.').append(minor).append('.').append(patch);

		if (preRelease != null) {
			sb.append('-').append(preRelease);
		}

		if (buildMeta != null) {
			sb.append('+').append(buildMeta);
		}

		return sb.toString();
	}

	/**
	 * Compares this Version with another object for equality.
	 * 
	 * <p>
	 * Two Versions are considered equal if their major, minor, patch, and
	 * pre-release components are equal. Build metadata is not considered in
	 * equality comparisons.
	 * </p>
	 *
	 * @param obj the object to compare with
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Version))
			return false;
		Version other = (Version) obj;

		return this.major == other.major &&
				this.minor == other.minor &&
				this.patch == other.patch &&
				Objects.equals(this.preRelease, other.preRelease);
		// Note: Build metadata is not part of equality comparison per SemVer spec
	}

	/**
	 * Returns a hash code value for this Version.
	 * 
	 * <p>
	 * The hash code is computed from the major, minor, patch, and pre-release
	 * components. Build metadata is not included in the hash code computation.
	 * </p>
	 *
	 * @return a hash code value for this Version
	 */
	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, preRelease);
		// Note: Build metadata is not part of hashCode per SemVer spec
	}

	/**
	 * Returns the major version number.
	 * 
	 * @return the major version number
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Returns the minor version number.
	 * 
	 * @return the minor version number
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Returns the patch version number.
	 * 
	 * @return the patch version number
	 */
	public int getPatch() {
		return patch;
	}

	/**
	 * Returns the pre-release identifier, if any.
	 * 
	 * @return the pre-release identifier, or null if none exists
	 */
	public String getPreRelease() {
		return preRelease;
	}

	/**
	 * Returns the build metadata, if any.
	 * 
	 * @return the build metadata, or null if none exists
	 */
	public String getBuildMetadata() {
		return buildMeta;
	}
}