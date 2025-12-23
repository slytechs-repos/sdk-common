package com.slytechs.sdk.common.detail;

import java.util.List;

/**
 * Section node in detail tree.
 */
public record SectionDetail(
        String name,
        String abbr,           // Added
        List<DetailNode> children
) implements DetailNode {}