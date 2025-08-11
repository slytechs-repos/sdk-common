package com.slytechs.jnet.core.api.memory;
public interface MemoryEditable extends Memory {
    // Marker only; no methods.
    // Implemented by MemoryBuffer for mutable bounds (dataOffset/dataEnd) and gap expansions.
    // Usage: MemoryEditor.edit(MemoryEditable), MemoryBuffer ops like put/get require this.
}