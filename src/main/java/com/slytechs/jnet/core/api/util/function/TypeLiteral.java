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
package com.slytechs.jnet.core.api.util.function;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Type literal for preserving generic type information at runtime.
 *
 * @param <T> the generic type
 */
public abstract class TypeLiteral<T> {
	
	/** The type. */
	private final Type type;

	/**
	 * Instantiates a new type literal.
	 */
	protected TypeLiteral() {
		Type superclass = getClass().getGenericSuperclass();
		if (!(superclass instanceof ParameterizedType)) {
			throw new IllegalArgumentException("TypeLiteral must be created with generic type information");
		}
		this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public final Type getType() {
		return type;
	}

	/**
	 * Gets the generic class.
	 *
	 * @return the generic class
	 */
	@SuppressWarnings("unchecked")
	public final Class<T> getGenericClass() {
		Class<T> rawType = (Class<T>) ((ParameterizedType) getType()).getRawType();

		return rawType;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getTypeName(type);
	}

	/**
	 * Recursively builds a string representation of a Type object.
	 *
	 * @param type the type
	 * @return the type name
	 */
	private static String getTypeName(Type type) {
		if (type instanceof Class<?>) {
			return ((Class<?>) type).getName();
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType paramType = (ParameterizedType) type;
			String rawType = getTypeName(paramType.getRawType());
			String args = Arrays.stream(paramType.getActualTypeArguments())
					.map(TypeLiteral::getTypeName)
					.collect(Collectors.joining(", "));
			return rawType + "<" + args + ">";
		}

		if (type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) type;
			Type[] upperBounds = wildcardType.getUpperBounds();
			Type[] lowerBounds = wildcardType.getLowerBounds();

			if (lowerBounds.length > 0) {
				return "? super " + getTypeName(lowerBounds[0]);
			} else if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class)) {
				return "? extends " + getTypeName(upperBounds[0]);
			}
			return "?";
		}

		if (type instanceof TypeVariable) {
			return ((TypeVariable<?>) type).getName();
		}

		if (type instanceof GenericArrayType) {
			return getTypeName(((GenericArrayType) type).getGenericComponentType()) + "[]";
		}

		return type.toString();
	}
}
