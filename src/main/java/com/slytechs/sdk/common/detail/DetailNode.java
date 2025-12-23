package com.slytechs.sdk.common.detail;

import java.util.List;

/**
 * Immutable node in the packet detail tree.
 * Users can access this via packet.getDetail() and render however they want.
 */
public sealed interface DetailNode 
    permits HeaderDetail, FieldDetail, SectionDetail, DataDetail, ExpertDetail {
    
    /** Node name/label */
    String name();
    
    /** Child nodes (empty for leaf nodes) */
    default List<DetailNode> children() { return List.of(); }
    
    /** Visitor pattern for type-safe processing */
    default <T> T accept(DetailVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

