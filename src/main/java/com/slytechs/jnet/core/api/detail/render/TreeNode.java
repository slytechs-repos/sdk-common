package com.slytechs.jnet.core.api.detail.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.slytechs.jnet.core.api.detail.ExpertLevel;

/**
 * UI tree node for ExaViewer packet detail panel.
 * 
 * Mutable structure designed for UI binding. Each node can be expanded/collapsed,
 * selected, and provides byte range for hex dump highlighting.
 */
public class TreeNode {
    
    // ═══════════════════════════════════════════════════════════════════════
    // NODE TYPES
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum Type {
        /** Protocol header (e.g., IPv4, TCP) */
        HEADER,
        
        /** Protocol field (e.g., Source Port, TTL) */
        FIELD,
        
        /** Logical section grouping (e.g., Flags, Options) */
        SECTION,
        
        /** Raw data / hex dump */
        DATA,
        
        /** Expert info annotation */
        EXPERT
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private final String name;
    private final Type type;
    private final List<TreeNode> children = new ArrayList<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // DISPLAY PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private String summary;
    private String displayValue;
    private Object rawValue;
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROTOCOL PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private int protocolId = -1;
    private int flags = 0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // BYTE POSITION (for hex highlighting)
    // ═══════════════════════════════════════════════════════════════════════
    
    private long byteOffset = -1;
    private long byteLength = -1;
    private long bitOffset = -1;
    private long bitLength = -1;
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA PROPERTIES (for DATA type)
    // ═══════════════════════════════════════════════════════════════════════
    
    private byte[] data;
    private int dataOffset;
    private int dataLength;
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXPERT PROPERTIES (for EXPERT type)
    // ═══════════════════════════════════════════════════════════════════════
    
    private ExpertLevel expertLevel;
    private String actionType;
    private Map<String, Object> actionData;
    
    // ═══════════════════════════════════════════════════════════════════════
    // UI STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private boolean expandable = false;
    private boolean expanded = false;
    private boolean selected = false;
    private boolean highlighted = false;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public TreeNode(String name) {
        this(name, Type.FIELD);
    }
    
    public TreeNode(String name, Type type) {
        this.name = name;
        this.type = type;
        this.expandable = (type == Type.HEADER || type == Type.SECTION);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CHILDREN
    // ═══════════════════════════════════════════════════════════════════════
    
    public void addChild(TreeNode child) {
        children.add(child);
        if (!expandable && !children.isEmpty()) {
            expandable = true;
        }
    }
    
    public List<TreeNode> children() {
        return Collections.unmodifiableList(children);
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    public int childCount() {
        return children.size();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public String name() {
        return name;
    }
    
    public Type type() {
        return type;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DISPLAY ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public String summary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String displayValue() {
        return displayValue;
    }
    
    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public Object rawValue() {
        return rawValue;
    }
    
    public void setRawValue(Object rawValue) {
        this.rawValue = rawValue;
    }
    
    /**
     * Get display text for UI label.
     * Returns summary for headers, "name = value" for fields.
     */
    public String displayText() {
        return switch (type) {
            case HEADER -> summary != null ? summary : name;
            case FIELD -> displayValue != null ? name + " = " + displayValue : name;
            case SECTION -> name;
            case DATA -> name + " (" + dataLength + " bytes)";
            case EXPERT -> "[" + expertLevel + "] " + name;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROTOCOL ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public int protocolId() {
        return protocolId;
    }
    
    public void setProtocolId(int protocolId) {
        this.protocolId = protocolId;
    }
    
    public int flags() {
        return flags;
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BYTE POSITION ACCESSORS (for hex highlighting)
    // ═══════════════════════════════════════════════════════════════════════
    
    public long byteOffset() {
        return byteOffset;
    }
    
    public long byteLength() {
        return byteLength;
    }
    
    public void setByteRange(long offset, long length) {
        this.byteOffset = offset;
        this.byteLength = length;
    }
    
    public long bitOffset() {
        return bitOffset;
    }
    
    public long bitLength() {
        return bitLength;
    }
    
    public void setBitRange(long offset, long length) {
        this.bitOffset = offset;
        this.bitLength = length;
        // Derive byte range from bit range
        if (offset >= 0 && length >= 0) {
            this.byteOffset = offset / 8;
            this.byteLength = (offset % 8 + length + 7) / 8;
        }
    }
    
    /**
     * Check if this node has valid byte position for highlighting.
     */
    public boolean hasPosition() {
        return byteOffset >= 0 && byteLength > 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA ACCESSORS (for DATA type)
    // ═══════════════════════════════════════════════════════════════════════
    
    public byte[] data() {
        return data;
    }
    
    public int dataOffset() {
        return dataOffset;
    }
    
    public int dataLength() {
        return dataLength;
    }
    
    public void setData(byte[] data, int offset, int length) {
        this.data = data;
        this.dataOffset = offset;
        this.dataLength = length;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXPERT ACCESSORS (for EXPERT type)
    // ═══════════════════════════════════════════════════════════════════════
    
    public ExpertLevel expertLevel() {
        return expertLevel;
    }
    
    public void setExpertLevel(ExpertLevel level) {
        this.expertLevel = level;
    }
    
    public String actionType() {
        return actionType;
    }
    
    public Map<String, Object> actionData() {
        return actionData != null ? actionData : Map.of();
    }
    
    public void setAction(String type, Map<String, Object> data) {
        this.actionType = type;
        this.actionData = data;
    }
    
    public void setAction(String type, Object data) {
        this.actionType = type;
        this.actionData = Map.of("data", data);
    }
    
    public boolean hasAction() {
        return actionType != null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UI STATE ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public boolean isExpandable() {
        return expandable;
    }
    
    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    public void toggleExpanded() {
        if (expandable) {
            this.expanded = !this.expanded;
        }
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isHighlighted() {
        return highlighted;
    }
    
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // TRAVERSAL
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Expand all nodes in tree.
     */
    public void expandAll() {
        if (expandable) {
            expanded = true;
        }
        for (TreeNode child : children) {
            child.expandAll();
        }
    }
    
    /**
     * Collapse all nodes in tree.
     */
    public void collapseAll() {
        expanded = false;
        for (TreeNode child : children) {
            child.collapseAll();
        }
    }
    
    /**
     * Find node at given byte offset.
     * Used for "select field from hex dump click".
     */
    public TreeNode findByOffset(int offset) {
        // Check children first (more specific)
        for (TreeNode child : children) {
            TreeNode found = child.findByOffset(offset);
            if (found != null) {
                return found;
            }
        }
        
        // Check self
        if (hasPosition() && offset >= byteOffset && offset < byteOffset + byteLength) {
            return this;
        }
        
        return null;
    }
    
    /**
     * Clear selection on this node and all descendants.
     */
    public void clearSelection() {
        selected = false;
        for (TreeNode child : children) {
            child.clearSelection();
        }
    }
    
    /**
     * Clear highlighting on this node and all descendants.
     */
    public void clearHighlight() {
        highlighted = false;
        for (TreeNode child : children) {
            child.clearHighlight();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Count total nodes in tree including this node.
     */
    public int totalNodeCount() {
        int count = 1;
        for (TreeNode child : children) {
            count += child.totalNodeCount();
        }
        return count;
    }
    
    /**
     * Get visible node count (expanded nodes only).
     */
    public int visibleNodeCount() {
        int count = 1;
        if (expanded) {
            for (TreeNode child : children) {
                count += child.visibleNodeCount();
            }
        }
        return count;
    }
    
    /**
     * Get depth of this node (0 for root).
     */
    public int depth() {
        return 0; // Would need parent reference to compute
    }
    
    @Override
    public String toString() {
        return switch (type) {
            case HEADER -> "HeaderNode[" + name + "]";
            case FIELD -> "FieldNode[" + name + "=" + displayValue + "]";
            case SECTION -> "SectionNode[" + name + "]";
            case DATA -> "DataNode[" + name + ", " + dataLength + " bytes]";
            case EXPERT -> "ExpertNode[" + expertLevel + ": " + name + "]";
        };
    }
    
    /**
     * Debug string showing tree structure.
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        toTreeString(sb, 0);
        return sb.toString();
    }
    
    private void toTreeString(StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth));
        sb.append(this).append("\n");
        for (TreeNode child : children) {
            child.toTreeString(sb, depth + 1);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create a header node.
     */
    public static TreeNode header(String name) {
        return new TreeNode(name, Type.HEADER);
    }
    
    /**
     * Create a field node.
     */
    public static TreeNode field(String name, String value) {
        TreeNode node = new TreeNode(name, Type.FIELD);
        node.setDisplayValue(value);
        return node;
    }
    
    /**
     * Create a section node.
     */
    public static TreeNode section(String name) {
        return new TreeNode(name, Type.SECTION);
    }
    
    /**
     * Create a data node.
     */
    public static TreeNode data(String name, byte[] data, int offset, int length) {
        TreeNode node = new TreeNode(name, Type.DATA);
        node.setData(data, offset, length);
        return node;
    }
    
    /**
     * Create an expert node.
     */
    public static TreeNode expert(ExpertLevel level, String message) {
        TreeNode node = new TreeNode(message, Type.EXPERT);
        node.setExpertLevel(level);
        return node;
    }
}