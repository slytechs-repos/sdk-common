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
package com.slytechs.sdk.common.license;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;

import com.cryptlex.lexactivator.FeatureEntitlement;
import com.cryptlex.lexactivator.LexActivator;
import com.cryptlex.lexactivator.LexActivatorException;

/**
 * License utility class providing activation state checks, feature entitlement
 * queries, and implicit activation support for all Sly Technologies SDKs.
 *
 * <p>
 * Activation state is always verified directly with Cryptlex — no local boolean
 * flags are trusted to prevent trivial bypass.
 * </p>
 */
public final class License {

	/**
	 * Functional interface for implicit license activation. Each SDK passes its own
	 * {@code activateLicense()} method reference.
	 *
	 * <p>
	 * Usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * License.ensureActivated(Pcap::activateLicense, logger);
	 * }</pre>
	 */
	@FunctionalInterface
	public interface LicenseActivator {
		void activate() throws LicenseException;
	}

	/** Lock for one-time implicit activation. */
	private static final Object ACTIVATION_LOCK = new Object();

	/**
	 * Prevent instantiation.
	 */
	private License() {}

	/**
	 * Ensures the license is activated, calling the provided activator if no active
	 * license is detected. Safe to call from every SDK constructor — the Cryptlex
	 * check is the gate, no local boolean state is trusted.
	 *
	 * <p>
	 * Thread-safe. Under contention, only one thread calls the activator; others
	 * wait and then re-check with Cryptlex directly.
	 * </p>
	 *
	 * <p>
	 * The logger passed in ensures activation messages appear under the calling SDK
	 * class name rather than {@code License}.
	 * </p>
	 *
	 * <p>
	 * Usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * // In Pcap constructor
	 * License.ensureActivated(Pcap::activateLicense, logger);
	 *
	 * // In NetCapture constructor
	 * License.ensureActivated(NetCapture::activateLicense, logger);
	 * }</pre>
	 *
	 * @param activator the SDK-specific activation method reference
	 * @param logger    the calling class logger
	 */
	public static void ensureActivated(LicenseActivator activator, Logger logger) {
		// Fast path — ask Cryptlex directly, no local state trusted
		if (isActivated())
			return;

		synchronized (ACTIVATION_LOCK) {
			// Another thread may have activated while we waited for the lock
			if (isActivated())
				return;

			try {
				activator.activate();
			} catch (LicenseException e) {
				logger.warn("Implicit license activation failed: {}", e.getMessage());
			}
		}
	}

	/**
	 * Checks if is feature enabled.
	 *
	 * @param featureName the feature name
	 * @return true, if is feature enabled
	 */
	public static boolean isFeatureEnabled(String featureName) {
		try {
			FeatureEntitlement ent = LexActivator.GetFeatureEntitlement(featureName);
			return ent != null && "yes".equals(ent.value);
		} catch (LexActivatorException | UnsupportedEncodingException e) {
			return false;
		}
	}

	/**
	 * Checks if the license is currently active and genuine per Cryptlex.
	 *
	 * @return true, if is activated
	 */
	public static boolean isActivated() {
		try {
			return LexActivator.IsLicenseGenuine() == LexActivator.LA_OK;
		} catch (LexActivatorException e) {
			return false;
		}
	}

	/**
	 * Checks if is feature enabled.
	 *
	 * @param featureName the feature name
	 * @return true, if is feature enabled
	 */
	public static boolean isActivated(String productName, String productData) {
		try {
			LexActivator.SetProductData(productData);
			if (LexActivator.IsLicenseGenuine() == LexActivator.LA_OK)
				return true;
		} catch (LexActivatorException e) {
			return false;
		}

		try {
			FeatureEntitlement ent = LexActivator.GetFeatureEntitlement(productName);
			return ent != null && "yes".equals(ent.value);
		} catch (LexActivatorException | UnsupportedEncodingException e) {
			return false;
		}
	}

	/**
	 * Checks if the active license is a commercial license.
	 *
	 * @return true, if is commercial
	 */
	public static boolean isCommercial() {
		return isFeatureEnabled("commercial-use");
	}

	/**
	 * Gets the licensee name from the active license.
	 *
	 * @return the licensee name, or null if unavailable
	 */
	public static String getLicensee() {
		try {
			return LexActivator.GetLicenseUserName();
		} catch (LexActivatorException | UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Gets a human-readable edition string for the active license.
	 *
	 * @return "Commercial", "Community", or "Offline"
	 */
	public static String getEdition() {
		if (!isActivated())
			return "Offline";
		if (isCommercial())
			return "Commercial";
		return "Community";
	}

	/**
	 * Require feature.
	 *
	 * @param featureName the feature name
	 * @throws LicenseException the license exception
	 */
	public static void requireFeature(String featureName) throws LicenseException {
		if (!isFeatureEnabled(featureName))
			throw new LicenseException("Feature not licensed: " + featureName);
	}
}