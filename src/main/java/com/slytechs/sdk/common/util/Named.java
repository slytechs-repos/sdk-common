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

/**
 * An interface for objects that can be identified by a name. This interface
 * provides a standard way to access an object's name, which can be used for
 * display, logging, debugging, or identification purposes.
 * 
 * <p>
 * The name returned by {@link #name()} should:
 * </p>
 * <ul>
 * <li>Be immutable for the lifetime of the object
 * <li>Never return null
 * <li>Be unique within its context if used as an identifier
 * <li>Be consistent with equals() and hashCode() if used in collections
 * </ul>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * public class NetworkAdapter implements Named {
 *     private final String adapterName;
 *     
 *     public NetworkAdapter(String name) {
 *         this.adapterName = Objects.requireNonNull(name);
 *     }
 *     
 *     {@literal @}Override
 *     public String name() {
 *         return adapterName;
 *     }
 * }
 * </pre>
 * 
 * <p>
 * Implementations should consider documenting:
 * </p>
 * <ul>
 * <li>The format and constraints of valid names
 * <li>Whether names are case-sensitive
 * <li>Any uniqueness guarantees
 * <li>Whether names can contain special characters
 * </ul>
 *
 * @author Mark Bednarczyk
 * @see java.util.Objects#requireNonNull(Object)
 */
public interface Named {

	/**
	 * To name.
	 *
	 * @param id the id
	 * @return the string
	 */
	static String toName(Object id) {
		if (id instanceof Enum<?> e)
			return e.name();

		else if (id instanceof Named named)
			return named.name();

		return id.toString();
	}

	/**
	 * Returns the name that identifies this object. The returned name should be
	 * non-null and preferably immutable.
	 *
	 * @return a non-null String representing this object's name
	 */
	String name();

	/**
	 * Sets the name that identifies this object. The new name should be non-null.
	 *
	 * @param newName the new name to assign to this object
	 * @throws NullPointerException          if newName is null
	 * @throws UnsupportedOperationException this is an optional operation
	 */
	default void setName(String newName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Generates a default name based on the implementing class. The default name is
	 * derived from the simple name of the class, with optional instance numbering
	 * if multiple instances exist.
	 * 
	 * <p>
	 * For example:
	 * <ul>
	 * <li>A class named "NetworkAdapter" would get the name "NetworkAdapter"
	 * <li>Multiple instances might get "NetworkAdapter-1", "NetworkAdapter-2", etc.
	 * </ul>
	 * </p>
	 * 
	 * @return a default name based on the class name
	 */
	default String defaultName() {
		return getClass().getSimpleName();
	}
}