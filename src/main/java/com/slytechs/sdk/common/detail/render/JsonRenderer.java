package com.slytechs.sdk.common.detail.render;

import java.util.List;
import java.util.Map;

import com.slytechs.sdk.common.detail.DataDetail;
import com.slytechs.sdk.common.detail.DetailNode;
import com.slytechs.sdk.common.detail.ExpertDetail;
import com.slytechs.sdk.common.detail.FieldDetail;
import com.slytechs.sdk.common.detail.HeaderDetail;
import com.slytechs.sdk.common.detail.SectionDetail;

/**
 * Renders DetailNode tree to JSON string using only standard Java.
 * No external dependencies (no Gson, Jackson, etc.)
 */
public class JsonRenderer {
    
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;
    private final boolean prettyPrint;
    
    public JsonRenderer() {
        this(true);
    }
    
    public JsonRenderer(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════
    
    public String render(List<DetailNode> nodes) {
        sb.setLength(0);
        indent = 0;
        writeArray(nodes);
        return sb.toString();
    }
    
    public String render(DetailNode node) {
        sb.setLength(0);
        indent = 0;
        writeNode(node);
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NODE RENDERING
    // ═══════════════════════════════════════════════════════════════════════
    
    private void writeNode(DetailNode node) {
        switch (node) {
            case HeaderDetail h -> writeHeader(h);
            case FieldDetail f -> writeField(f);
            case SectionDetail s -> writeSection(s);
            case DataDetail d -> writeData(d);
            case ExpertDetail e -> writeExpert(e);
        }
    }
    
    private void writeHeader(HeaderDetail h) {
        objectStart();
        writeProperty("type", "header");
        writeProperty("name", h.name());
        writeProperty("summary", h.summary());
        writeProperty("protocolId", h.protocolId());
        if (h.offset() >= 0) {
            writeProperty("offset", h.offset());
            writeProperty("length", h.length());
        }
        writeProperty("flags", h.flags());
        writeChildrenProperty(h.children());
        objectEnd();
    }
    
    private void writeField(FieldDetail f) {
        objectStart();
        writeProperty("type", "field");
        writeProperty("name", f.name());
        writeValueProperty("value", f.value());
        writeProperty("display", f.display());
        if (f.bitOffset() >= 0) {
            writeProperty("bitOffset", f.bitOffset());
            writeProperty("bitLength", f.bitLength());
            writeProperty("byteOffset", f.byteOffset());
            writeProperty("byteLength", f.byteLength());
        }
        if (!f.children().isEmpty()) {
            writeChildrenProperty(f.children());
        }
        objectEnd();
    }
    
    private void writeSection(SectionDetail s) {
        objectStart();
        writeProperty("type", "section");
        writeProperty("name", s.name());
        writeChildrenProperty(s.children());
        objectEnd();
    }
    
    private void writeData(DataDetail d) {
        objectStart();
        writeProperty("type", "data");
        writeProperty("name", d.name());
        writeProperty("offset", d.offset());
        writeProperty("length", d.length());
        writeProperty("preview", toHexPreview(d.data(), (int) d.offset(), (int) Math.min(32, d.length())));
        objectEnd();
    }
    
    private void writeExpert(ExpertDetail e) {
        objectStart();
        writeProperty("type", "expert");
        writeProperty("level", e.level().name());
        writeProperty("message", e.message());
        if (e.actionType() != null) {
            writeProperty("actionType", e.actionType());
            writeMapProperty("actionData", e.actionData());
        }
        objectEnd();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // JSON STRUCTURE
    // ═══════════════════════════════════════════════════════════════════════
    
    private void objectStart() {
        sb.append('{');
        indent++;
        newline();
    }
    
    private void objectEnd() {
        indent--;
        // Remove trailing comma if present
        removeTrailingComma();
        newline();
        sb.append('}');
    }
    
    private void arrayStart() {
        sb.append('[');
        indent++;
        newline();
    }
    
    private void arrayEnd() {
        indent--;
        removeTrailingComma();
        newline();
        sb.append(']');
    }
    
    private void writeArray(List<DetailNode> nodes) {
        arrayStart();
        for (int i = 0; i < nodes.size(); i++) {
            writeNode(nodes.get(i));
            if (i < nodes.size() - 1) {
                sb.append(',');
            }
            newline();
        }
        arrayEnd();
    }
    
    private void writeChildrenProperty(List<DetailNode> children) {
        writeIndent();
        sb.append("\"children\": ");
        if (children.isEmpty()) {
            sb.append("[]");
        } else {
            arrayStart();
            for (int i = 0; i < children.size(); i++) {
                writeNode(children.get(i));
                if (i < children.size() - 1) {
                    sb.append(',');
                }
                newline();
            }
            arrayEnd();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROPERTY WRITING
    // ═══════════════════════════════════════════════════════════════════════
    
    private void writeProperty(String name, String value) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escapeJson(value)).append('"');
        }
        sb.append(',');
        newline();
    }
    
    private void writeProperty(String name, int value) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        sb.append(value);
        sb.append(',');
        newline();
    }
    
    private void writeProperty(String name, long value) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        sb.append(value);
        sb.append(',');
        newline();
    }
    
    private void writeProperty(String name, boolean value) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        sb.append(value);
        sb.append(',');
        newline();
    }
    
    private void writeValueProperty(String name, Object value) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        writeValue(value);
        sb.append(',');
        newline();
    }
    
    private void writeValue(Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            sb.append('"').append(escapeJson(s)).append('"');
        } else if (value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof byte[] bytes) {
            sb.append('"').append(toHexPreview(bytes, 0, Math.min(32, bytes.length))).append('"');
        } else if (value instanceof java.net.InetAddress addr) {
            sb.append('"').append(addr.getHostAddress()).append('"');
        } else {
            // Fallback - use toString and escape
            sb.append('"').append(escapeJson(value.toString())).append('"');
        }
    }
    
    private void writeMapProperty(String name, Map<String, Object> map) {
        writeIndent();
        sb.append('"').append(escapeJson(name)).append("\": ");
        if (map == null || map.isEmpty()) {
            sb.append("{}");
        } else {
            sb.append('{');
            indent++;
            newline();
            int count = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writeIndent();
                sb.append('"').append(escapeJson(entry.getKey())).append("\": ");
                writeValue(entry.getValue());
                if (++count < map.size()) {
                    sb.append(',');
                }
                newline();
            }
            indent--;
            writeIndent();
            sb.append('}');
        }
        sb.append(',');
        newline();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FORMATTING HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    private void writeIndent() {
        if (prettyPrint) {
            sb.append("  ".repeat(indent));
        }
    }
    
    private void newline() {
        if (prettyPrint) {
            sb.append('\n');
        }
    }
    
    private void removeTrailingComma() {
        // Find last non-whitespace and remove if comma
        int i = sb.length() - 1;
        while (i >= 0 && Character.isWhitespace(sb.charAt(i))) {
            i--;
        }
        if (i >= 0 && sb.charAt(i) == ',') {
            sb.deleteCharAt(i);
        }
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        
        StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.toString();
    }
    
    private String toHexPreview(byte[] data, int offset, int length) {
        if (data == null || length <= 0) return "";
        
        StringBuilder hex = new StringBuilder(length * 2);
        int end = Math.min(offset + length, data.length);
        for (int i = offset; i < end; i++) {
            hex.append(String.format("%02x", data[i] & 0xFF));
        }
        if (offset + length < data.length) {
            hex.append("...");
        }
        return hex.toString();
    }
}