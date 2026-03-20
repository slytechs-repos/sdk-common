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

import com.cryptlex.lexactivator.FeatureEntitlement;
import com.cryptlex.lexactivator.LexActivator;
import com.cryptlex.lexactivator.LexActivatorException;

/**
 * License utility class.
 */
public final class License {

	/**
	 * Prevent instantiation.
	 */
	private License() {}

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
	 * Checks if is activated.
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
			
			if (LexActivator.IsLicenseGenuine() == LexActivator.LA_OK) {
				return true;
			}
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
	 * Checks if is commercial.
	 *
	 * @return true, if is commercial
	 */
	public static boolean isCommercial() {
		return isFeatureEnabled("commercial-use");
	}

	/**
	 * Gets the licensee.
	 *
	 * @return the licensee
	 */
	public static String getLicensee() {
		try {
			return LexActivator.GetLicenseUserName();
		} catch (LexActivatorException | UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Gets the edition.
	 *
	 * @return the edition
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