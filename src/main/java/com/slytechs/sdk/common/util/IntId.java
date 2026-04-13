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
 * A common interface for enum tables that map to integer identifiers.
 *
 * <p>
 * This is the standard accessor interface for all {@code *Type} enums that
 * represent wire-level, protocol, or system integer constants. It provides a
 * uniform way to retrieve the underlying integer value regardless of the
 * specific domain.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * {@snippet :
 * public enum EtherType implements EtherTypes, IntId {
 *     IPv4(EtherTypes.IPv4),
 *     IPv6(EtherTypes.IPv6);
 *
 *     private final int value;
 *
 *     EtherType(int value) { this.value = value; }
 *
 *     &#64;Override
 *     public int id() { return value; }
 * }
 *
 * // Uniform access across all type enums
 * EtherType.IPv4.id()          // 0x0800
 * IpType.TCP.id()              // 6
 * IcmpType.ECHO_REQUEST.id()   // 8
 * }
 *
 * <p>
 * For enum tables that do not represent integer identifiers (e.g. colors,
 * encodings, named values), use a domain-specific accessor instead.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.protocol.core.id.EtherType
 */
public interface IntId {

	/**
	 * Returns the integer identifier for this constant as used on the wire or by
	 * the system.
	 *
	 * @return the integer value this constant represents
	 */
	int id();
}
