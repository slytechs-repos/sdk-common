/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
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
package com.slytechs.sdk.common.memory;

import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * Marker interface for native C-like structures with memory layout. Provides a
 * set of memory layouts for typical primitives used in C language environments
 * and for both network, little endianness and alignment constrainTs.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface MemoryStructure {

	ValueLayout S8 = ValueLayout.JAVA_BYTE;
	ValueLayout S16 = ValueLayout.JAVA_SHORT;
	ValueLayout S32 = ValueLayout.JAVA_INT;
	ValueLayout S64 = ValueLayout.JAVA_LONG;

	ValueLayout S8_BE = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout S16_BE = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout S32_BE = ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout S64_BE = ValueLayout.JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN);

	ValueLayout S8_LE = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout S16_LE = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout S32_LE = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout S64_LE = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN);

	ValueLayout S8_A1 = ValueLayout.JAVA_BYTE.withByteAlignment(1);
	ValueLayout S16_A1 = ValueLayout.JAVA_SHORT.withByteAlignment(1);
	ValueLayout S32_A1 = ValueLayout.JAVA_INT.withByteAlignment(1);
	ValueLayout S64_A1 = ValueLayout.JAVA_LONG.withByteAlignment(1);

	ValueLayout S8_BE_A1 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout S16_BE_A1 = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout S32_BE_A1 = ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout S64_BE_A1 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);

	ValueLayout S8_LE_A1 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout S16_LE_A1 = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout S32_LE_A1 = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout S64_LE_A1 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);

	ValueLayout U8 = ValueLayout.JAVA_BYTE;
	ValueLayout U16 = ValueLayout.JAVA_SHORT;
	ValueLayout U32 = ValueLayout.JAVA_INT;
	ValueLayout U64 = ValueLayout.JAVA_LONG;

	ValueLayout U8_BE = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout U16_BE = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout U32_BE = ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN);
	ValueLayout U64_BE = ValueLayout.JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN);

	ValueLayout U8_LE = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout U16_LE = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout U32_LE = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
	ValueLayout U64_LE = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN);

	ValueLayout U8_A1 = ValueLayout.JAVA_BYTE.withByteAlignment(1);
	ValueLayout U16_A1 = ValueLayout.JAVA_SHORT.withByteAlignment(1);
	ValueLayout U32_A1 = ValueLayout.JAVA_INT.withByteAlignment(1);
	ValueLayout U64_A1 = ValueLayout.JAVA_LONG.withByteAlignment(1);

	ValueLayout U8_BE_A1 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout U16_BE_A1 = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout U32_BE_A1 = ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);
	ValueLayout U64_BE_A1 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.BIG_ENDIAN).withByteAlignment(1);

	ValueLayout U8_LE_A1 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout U16_LE_A1 = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout U32_LE_A1 = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);
	ValueLayout U64_LE_A1 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN).withByteAlignment(1);

}