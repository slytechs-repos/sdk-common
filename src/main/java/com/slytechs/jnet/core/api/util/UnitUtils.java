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
package com.slytechs.jnet.core.api.util;

/**
 * The Class UnitUtils.
 *
 * @author Sly Technologies
 */
public class UnitUtils {

	/**
	 * The Interface ConvertableUnit.
	 *
	 * @param <T> the generic type
	 * @author Sly Technologies
	 */
	public interface ConvertableUnit<T extends Enum<T>> extends Unit {

		/**
		 * Convert.
		 *
		 * @param source the source
		 * @param unit   the unit
		 * @return the long
		 */
		long convert(long source, T unit);

		/**
		 * Convertf.
		 *
		 * @param source the source
		 * @return the double
		 */
		double convertf(double source);

		/**
		 * Convertf.
		 *
		 * @param source     the source
		 * @param sourceUnit the source unit
		 * @return the double
		 */
		double convertf(double source, T sourceUnit);

	}

	/**
	 * Instantiates a new unit utils.
	 */
	private UnitUtils() {
	}

	/**
	 * Nearest.
	 *
	 * @param <T>   the generic type
	 * @param value the value
	 * @param type  the type
	 * @param base  the base
	 * @return the t
	 */
	public static <T extends Enum<T> & ConvertableUnit<T>> T nearest(long value, Class<T> type, T base) {
		T[] values = type.getEnumConstants();

		for (int i = values.length - 1; i >= 0; i--) {
			T u = values[i];

			if (u.convert(value, base) > 0) {
				return u;
			}
		}

		return base;
	}

	/**
	 * Format.
	 *
	 * @param <T>   the generic type
	 * @param fmt   the fmt
	 * @param value the value
	 * @param type  the type
	 * @param base  the base
	 * @return the string
	 */
	public static <T extends Enum<T> & ConvertableUnit<T>> String format(String fmt,
			long value,
			Class<T> type,
			T base) {
		T unit = nearest(value, type, base);
		return String.format(fmt, unit.convertf(value), unit.getSymbol());
	}

	/**
	 * Values.
	 *
	 * @param <U> the generic type
	 * @param cl  the cl
	 * @return the u[]
	 */
	static <U extends Unit> U[] values(Class<U> cl) {
		if (!(Enum.class.isAssignableFrom(cl)))
			throw new IllegalArgumentException("only enum based units are supported [%s]"
					.formatted(cl));

		U[] values = cl.getEnumConstants();

		return values;
	}

	/**
	 * Parses the units.
	 *
	 * @param <U>           the generic type
	 * @param valueAndUnits the value and units
	 * @param cl            the cl
	 * @return the u
	 */
	static <U extends Unit> U parseUnits(String valueAndUnits, Class<U> cl) {
		U[] values = values(cl);
		String str = valueAndUnits.toLowerCase().strip();

		for (U u : values) {
			String[] symbols = u.getSymbols();
			for (String sym : symbols) {
				String regex = "[^\\s-_@]+[\\s-_@]*\\(?" + sym + "\\)?$";
				if (str.matches(regex))
					return u;
			}
		}

		return null;
	}

	/**
	 * Strip units.
	 *
	 * @param valueAndUnits the value and units
	 * @param cl            the cl
	 * @return the string
	 */
	static String stripUnits(String valueAndUnits, Class<? extends Unit> cl) {
		Unit[] values = values(cl);
		String str = valueAndUnits.toLowerCase();

		for (Unit u : values) {
			String[] symbols = u.getSymbols();
			for (String sym : symbols) {
				String regex = "([^\\s-_@]+)[\\s-_@]*\\(?" + sym + "\\)?$";
				if (str.matches(regex))
					return str.replaceFirst(regex, "$1");
			}
		}

		return valueAndUnits;
	}
}
