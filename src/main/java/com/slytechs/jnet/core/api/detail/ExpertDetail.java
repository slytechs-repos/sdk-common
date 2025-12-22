package com.slytechs.jnet.core.api.detail;

import java.util.List;
import java.util.Map;

/**
 * Expert info node - analysis annotations
 */
public record ExpertDetail(
    ExpertLevel level,              // INFO, WARN, ERROR, CRITICAL
    String message,                 // Human readable message
    String actionType,              // "addKey", "followStream", null if no action
    Map<String, Object> actionData  // Parameters for action
) implements DetailNode {
    
    @Override
    public String name() { return level.name(); }
    
    @Override
    public List<DetailNode> children() { return List.of(); }
    
    // Convenience constructor without action
    public ExpertDetail(ExpertLevel level, String message) {
        this(level, message, null, Map.of());
    }
}

