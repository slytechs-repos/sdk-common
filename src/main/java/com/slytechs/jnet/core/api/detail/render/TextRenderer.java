package com.slytechs.jnet.core.api.detail.render;

import java.util.List;

import com.slytechs.jnet.core.api.detail.DataDetail;
import com.slytechs.jnet.core.api.detail.Detail;
import com.slytechs.jnet.core.api.detail.DetailFlags;
import com.slytechs.jnet.core.api.detail.DetailNode;
import com.slytechs.jnet.core.api.detail.ExpertDetail;
import com.slytechs.jnet.core.api.detail.FieldDetail;
import com.slytechs.jnet.core.api.detail.HeaderDetail;
import com.slytechs.jnet.core.api.detail.SectionDetail;
import com.slytechs.jnet.core.api.format.HexDump;

/**
 * Renders DetailNode tree to formatted text string.
 */
public class TextRenderer {
    
    private final Detail detailLevel;
    private final TextStyle style;
    
    // Context tracking using abbreviations
    private String currentHeaderAbbr = "";
    private String currentSectionAbbr = "";
    
    public TextRenderer() {
        this(Detail.HIGH, TextStyle.STANDARD);
    }
    
    public TextRenderer(Detail detailLevel) {
        this(detailLevel, TextStyle.STANDARD);
    }
    
    public TextRenderer(Detail detailLevel, TextStyle style) {
        this.detailLevel = detailLevel;
        this.style = style;
    }
    
    public String render(List<DetailNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (DetailNode node : nodes) {
            renderNode(sb, node, 0);
        }
        return sb.toString();
    }
    
    public String render(DetailNode node) {
        StringBuilder sb = new StringBuilder();
        renderNode(sb, node, 0);
        return sb.toString();
    }
    
    private void renderNode(StringBuilder sb, DetailNode node, int depth) {
        switch (node) {
            case HeaderDetail h -> renderHeader(sb, h, depth);
            case FieldDetail f -> renderField(sb, f, depth);
            case SectionDetail s -> renderSection(sb, s, depth);
            case DataDetail d -> renderData(sb, d, depth);
            case ExpertDetail e -> renderExpert(sb, e, depth);
        }
    }
    
    private void renderHeader(StringBuilder sb, HeaderDetail h, int depth) {
        // Set context using abbreviation
        currentHeaderAbbr = h.abbr();
        currentSectionAbbr = "";
        
        // Header line - style chooses name vs abbr
        sb.append(style.headerLine(h.name(), h.abbr()));
        if (h.summary() != null && !h.summary().isEmpty()) {
            sb.append(": ").append(h.summary());
        }
        if (h.offset() >= 0) {
            sb.append(" offset=").append(h.offset());
            sb.append(" length=").append(h.length());
        }
        sb.append("\n");
        
        // Flags
        String indent = style.indent(depth);
        renderFlags(sb, indent, h.flags());
        
        // Children
        for (DetailNode child : h.children()) {
            renderNode(sb, child, depth + 1);
        }
        
        // Blank line after header
        sb.append("\n");
    }
    
    private void renderFlags(StringBuilder sb, String prefix, int flags) {
        if ((flags & DetailFlags.REASSEMBLED) != 0) {
            sb.append(prefix).append("    ** REASSEMBLED **\n");
        }
        if ((flags & DetailFlags.DECRYPTED) != 0) {
            sb.append(prefix).append("    ** DECRYPTED **\n");
        }
        if ((flags & DetailFlags.TRUNCATED) != 0) {
            sb.append(prefix).append("    ** TRUNCATED **\n");
        }
    }
    
    private void renderField(StringBuilder sb, FieldDetail f, int depth) {
        if (detailLevel == Detail.SUMMARY) return;
        
        String prefix = style.fieldPrefix(currentHeaderAbbr, currentSectionAbbr, depth);
        sb.append(prefix);
        sb.append(style.formatFieldName(f.name()));
        sb.append(" = ");
        sb.append(f.display());
        sb.append("\n");
        
        // Sub-fields only for HIGH detail
        if (detailLevel == Detail.HIGH) {
            for (DetailNode child : f.children()) {
                renderNode(sb, child, depth + 1);
            }
        }
    }
    
    private void renderSection(StringBuilder sb, SectionDetail s, int depth) {
        // Sections only for HIGH detail
        if (detailLevel != Detail.HIGH) return;
        
        // Save previous section and set new
        String prevSection = currentSectionAbbr;
        currentSectionAbbr = s.abbr();
        
        // Section header line
        String prefix = style.fieldPrefix(currentHeaderAbbr, currentSectionAbbr, depth);
        sb.append(prefix);
        sb.append(style.formatFieldName(s.name() + ":"));
        sb.append("\n");
        
        // Children
        for (DetailNode child : s.children()) {
            renderNode(sb, child, depth + 1);
        }
        
        // Restore previous section
        currentSectionAbbr = prevSection;
    }
    
    private void renderData(StringBuilder sb, DataDetail d, int depth) {
        if (detailLevel.compareTo(Detail.HEXDUMP) < 0) return;
        
        String prefix = style.dataIndent(currentHeaderAbbr, currentSectionAbbr, depth);
        sb.append(prefix);
        sb.append(style.formatFieldName(d.name()));
        sb.append(": ").append(d.length()).append(" bytes\n");
        
        HexDump.render(sb, d.data(), (int) d.offset(), (int) d.length(), prefix);
    }
    
    private void renderExpert(StringBuilder sb, ExpertDetail e, int depth) {
        String prefix = style.fieldPrefix(currentHeaderAbbr, currentSectionAbbr, depth);
        sb.append(prefix);
        sb.append("[").append(e.level()).append("] ");
        sb.append(e.message());
        sb.append("\n");
    }
}