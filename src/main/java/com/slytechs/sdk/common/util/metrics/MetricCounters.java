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
package com.slytechs.sdk.common.util.metrics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Metric utility methods
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class MetricCounters {

	/**
	 * Find named handle inside a class.
	 *
	 * @param name      the counter field name
	 * @param classType the counter class type
	 * @return the var handle bound to the field
	 */
	public static VarHandle findHandle(String name, Class<?> classType) {
		try {
			return MethodHandles.lookup()
					.findVarHandle(classType, name, long.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ExceptionInInitializerError(
					"VarHandle lookup failed for field: " + name);
		}
	}

	private MetricCounters() {}
}
