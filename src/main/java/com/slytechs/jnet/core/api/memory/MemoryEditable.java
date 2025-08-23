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
package com.slytechs.jnet.core.api.memory;

/**
 * The Interface MemoryEditable.
 */
public interface MemoryEditable extends Memory {
    // Marker only; no methods.
    // Implemented by MemoryBuffer for mutable bounds (dataOffset/dataEnd) and gap expansions.
    // Usage: MemoryEditor.edit(MemoryEditable), MemoryBuffer ops like put/get require this.
}