package com.slytechs.jnet.core.api.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builder for constructing DetailNode trees.
 */
public class DetailBuilder {

    private final List<DetailNode> roots = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Add a header with name, abbreviation, and content.
     */
    public void header(String name, String abbr, int protocolId, long offset, long length, 
                       Consumer<HeaderBuilder> content) {
        HeaderBuilder hb = new HeaderBuilder(name, abbr, protocolId, offset, length);
        content.accept(hb);
        roots.add(hb.build());
    }

    /**
     * Add a header without offset/length.
     */
    public void header(String name, String abbr, int protocolId, Consumer<HeaderBuilder> content) {
        header(name, abbr, protocolId, -1L, -1L, content);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BUILD
    // ═══════════════════════════════════════════════════════════════════════

    public List<DetailNode> build() {
        return List.copyOf(roots);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIELD CONTAINER INTERFACE
    // ═══════════════════════════════════════════════════════════════════════

    public interface FieldContainer {

        void field(String name, Object value);
        void field(String name, Object value, String display);
        void field(String name, Object value, BitRange bits);
        void field(String name, Object value, String display, BitRange bits);

        void fieldHex(String name, int value);
        void fieldHex(String name, int value, int digits);
        void fieldHex(String name, int value, int digits, BitRange bits);
        void fieldHex(String name, int value, int digits, String description, BitRange bits);
        void fieldf(String name, Object value, String format, Object... args);

        void section(String name, String abbr, Consumer<SectionBuilder> content);

        void data(String name, byte[] data, long offset, long length);
        default void data(byte[] data, long offset, long length) {
            data("Data", data, offset, length);
        }

        void expert(ExpertLevel level, String message);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HEADER BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    public static final class HeaderBuilder implements FieldContainer {

        private final String name;
        private final String abbr;
        private final int protocolId;
        private final long offset;
        private final long length;
        private String summary = "";
        private int flags = 0;
        private final List<DetailNode> children = new ArrayList<>();

        private HeaderBuilder(String name, String abbr, int protocolId, long offset, long length) {
            this.name = name;
            this.abbr = abbr;
            this.protocolId = protocolId;
            this.offset = offset;
            this.length = length;
        }

        // ─── Summary ───

        public void summary(String summary) {
            this.summary = summary;
        }

        public void summaryf(String format, Object... args) {
            this.summary = String.format(format, args);
        }

        // ─── Flags ───

        public void flags(int flags) {
            this.flags = flags;
        }

        public void reassembled() {
            this.flags |= DetailFlags.REASSEMBLED;
        }

        public void decrypted() {
            this.flags |= DetailFlags.DECRYPTED;
        }

        // ─── Fields (simple) ───

        @Override
        public void field(String name, Object value) {
            children.add(new FieldDetail(name, value, String.valueOf(value)));
        }

        @Override
        public void field(String name, Object value, String display) {
            children.add(new FieldDetail(name, value, display));
        }

        @Override
        public void field(String name, Object value, BitRange bits) {
            children.add(new FieldDetail(name, value, String.valueOf(value), bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void field(String name, Object value, String display, BitRange bits) {
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        // ─── Fields (formatted) ───

        @Override
        public void fieldHex(String name, int value) {
            children.add(new FieldDetail(name, value, String.format("0x%X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits) {
            children.add(new FieldDetail(name, value, String.format("0x%0" + digits + "X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits, BitRange bits) {
            String display = String.format("0x%0" + digits + "X", value);
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldHex(String name, int value, int digits, String description, BitRange bits) {
            String hexStr = String.format("0x%0" + digits + "X", value);
            String display = (description != null && !description.isEmpty())
                    ? hexStr + " (" + description + ")"
                    : hexStr;
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldf(String name, Object value, String format, Object... args) {
            children.add(new FieldDetail(name, value, String.format(format, args)));
        }

        // ─── Section (lambda-based) ───

        @Override
        public void section(String name, String abbr, Consumer<SectionBuilder> content) {
            SectionBuilder sb = new SectionBuilder(name, abbr);
            content.accept(sb);
            children.add(sb.build());
        }

        // ─── Expandable Field (lambda-based) ───

        public void expandField(String name, Object value, String display, Consumer<FieldBuilder> content) {
            FieldBuilder fb = new FieldBuilder(name, value, display);
            content.accept(fb);
            children.add(fb.build());
        }

        public void expandField(String name, Object value, String display, BitRange bits,
                Consumer<FieldBuilder> content) {
            FieldBuilder fb = new FieldBuilder(name, value, display);
            fb.bits(bits);
            content.accept(fb);
            children.add(fb.build());
        }

        // ─── Data ───

        @Override
        public void data(String name, byte[] data, long offset, long length) {
            children.add(new DataDetail(name, data, offset, length));
        }

        // ─── Expert ───

        @Override
        public void expert(ExpertLevel level, String message) {
            children.add(new ExpertDetail(level, message));
        }

        public void expert(ExpertLevel level, String message, String action, Map<String, Object> actionData) {
            children.add(new ExpertDetail(level, message, action, actionData));
        }

        private DetailNode build() {
            return new HeaderDetail(name, abbr, summary, protocolId, offset, length, flags, List.copyOf(children));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    public static final class SectionBuilder implements FieldContainer {

        private final String name;
        private final String abbr;
        private final List<DetailNode> children = new ArrayList<>();

        private SectionBuilder(String name, String abbr) {
            this.name = name;
            this.abbr = abbr;
        }

        @Override
        public void field(String name, Object value) {
            children.add(new FieldDetail(name, value, String.valueOf(value)));
        }

        @Override
        public void field(String name, Object value, String display) {
            children.add(new FieldDetail(name, value, display));
        }

        @Override
        public void field(String name, Object value, BitRange bits) {
            children.add(new FieldDetail(name, value, String.valueOf(value), bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void field(String name, Object value, String display, BitRange bits) {
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldHex(String name, int value) {
            children.add(new FieldDetail(name, value, String.format("0x%X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits) {
            children.add(new FieldDetail(name, value, String.format("0x%0" + digits + "X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits, BitRange bits) {
            String display = String.format("0x%0" + digits + "X", value);
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldHex(String name, int value, int digits, String description, BitRange bits) {
            String hexStr = String.format("0x%0" + digits + "X", value);
            String display = (description != null && !description.isEmpty())
                    ? hexStr + " (" + description + ")"
                    : hexStr;
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldf(String name, Object value, String format, Object... args) {
            children.add(new FieldDetail(name, value, String.format(format, args)));
        }

        @Override
        public void section(String name, String abbr, Consumer<SectionBuilder> content) {
            SectionBuilder sb = new SectionBuilder(name, abbr);
            content.accept(sb);
            children.add(sb.build());
        }

        @Override
        public void data(String name, byte[] data, long offset, long length) {
            children.add(new DataDetail(name, data, offset, length));
        }

        @Override
        public void expert(ExpertLevel level, String message) {
            children.add(new ExpertDetail(level, message));
        }

        private DetailNode build() {
            return new SectionDetail(name, abbr, List.copyOf(children));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXPANDABLE FIELD BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    public static final class FieldBuilder implements FieldContainer {

        private final String name;
        private final Object value;
        private final String display;
        private long bitOffset = -1L;
        private long bitLength = -1L;
        private final List<DetailNode> children = new ArrayList<>();

        private FieldBuilder(String name, Object value, String display) {
            this.name = name;
            this.value = value;
            this.display = display;
        }

        public void bits(long bitOffset, long bitLength) {
            this.bitOffset = bitOffset;
            this.bitLength = bitLength;
        }

        public void bits(BitRange bits) {
            this.bitOffset = bits.offset();
            this.bitLength = bits.length();
        }

        @Override
        public void field(String name, Object value) {
            children.add(new FieldDetail(name, value, String.valueOf(value)));
        }

        @Override
        public void field(String name, Object value, String display) {
            children.add(new FieldDetail(name, value, display));
        }

        @Override
        public void field(String name, Object value, BitRange bits) {
            children.add(new FieldDetail(name, value, String.valueOf(value), bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void field(String name, Object value, String display, BitRange bits) {
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldHex(String name, int value) {
            children.add(new FieldDetail(name, value, String.format("0x%X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits) {
            children.add(new FieldDetail(name, value, String.format("0x%0" + digits + "X", value)));
        }

        @Override
        public void fieldHex(String name, int value, int digits, BitRange bits) {
            String display = String.format("0x%0" + digits + "X", value);
            children.add(new FieldDetail(name, value, display, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldHex(String name, int value, int digits, String description, BitRange bits) {
            String hexStr = String.format("0x%0" + digits + "X", value);
            String displayStr = (description != null && !description.isEmpty())
                    ? hexStr + " (" + description + ")"
                    : hexStr;
            children.add(new FieldDetail(name, value, displayStr, bits.offset(), bits.length(), List.of()));
        }

        @Override
        public void fieldf(String name, Object value, String format, Object... args) {
            children.add(new FieldDetail(name, value, String.format(format, args)));
        }

        @Override
        public void section(String name, String abbr, Consumer<SectionBuilder> content) {
            SectionBuilder sb = new SectionBuilder(name, abbr);
            content.accept(sb);
            children.add(sb.build());
        }

        @Override
        public void data(String name, byte[] data, long offset, long length) {
            children.add(new DataDetail(name, data, offset, length));
        }

        @Override
        public void expert(ExpertLevel level, String message) {
            children.add(new ExpertDetail(level, message));
        }

        private DetailNode build() {
            return new FieldDetail(name, value, display, bitOffset, bitLength, List.copyOf(children));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BIT RANGE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    public static BitRange bits(long byteOffset, long byteLength) {
        return new BitRange(byteOffset * 8, byteLength * 8);
    }

    public static BitRange bitsAt(long bitOffset, long bitLength) {
        return new BitRange(bitOffset, bitLength);
    }

    public static BitRange byteAt(long byteOffset) {
        return new BitRange(byteOffset * 8, 8);
    }

    public static BitRange shortAt(long byteOffset) {
        return new BitRange(byteOffset * 8, 16);
    }

    public static BitRange intAt(long byteOffset) {
        return new BitRange(byteOffset * 8, 32);
    }

    public static BitRange longAt(long byteOffset) {
        return new BitRange(byteOffset * 8, 64);
    }

    public record BitRange(long offset, long length) {

        public long byteOffset() {
            return offset / 8;
        }

        public long byteLength() {
            return (offset % 8 + length + 7) / 8;
        }

        public boolean isByteAligned() {
            return offset % 8 == 0 && length % 8 == 0;
        }
    }
}