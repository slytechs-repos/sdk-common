package com.slytechs.sdk.common.detail;

import java.util.List;

/**
 * Header node in detail tree.
 */
public record HeaderDetail(
        String name,
        String abbr,           // Added
        String summary,
        int protocolId,
        long offset,
        long length,
        int flags,
        List<DetailNode> children
) implements DetailNode {}