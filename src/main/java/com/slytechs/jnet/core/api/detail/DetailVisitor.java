package com.slytechs.jnet.core.api.detail;

import java.util.List;

/**
 * Visitor interface for processing DetailNode trees.
 * 
 * Provides type-safe traversal with default implementations that
 * recursively visit children.
 *
 * @param <T> the return type of visit operations
 */
public interface DetailVisitor<T> {
    
    /**
     * Visit any node type - dispatches to specific visit method.
     */
    default T visit(DetailNode node) {
        return switch (node) {
            case HeaderDetail h -> visitHeader(h);
            case FieldDetail f -> visitField(f);
            case SectionDetail s -> visitSection(s);
            case DataDetail d -> visitData(d);
            case ExpertDetail e -> visitExpert(e);
        };
    }
    
    /**
     * Visit a header node.
     */
    T visitHeader(HeaderDetail header);
    
    /**
     * Visit a field node.
     */
    T visitField(FieldDetail field);
    
    /**
     * Visit a section node.
     */
    T visitSection(SectionDetail section);
    
    /**
     * Visit a data/hexdump node.
     */
    T visitData(DataDetail data);
    
    /**
     * Visit an expert info node.
     */
    T visitExpert(ExpertDetail expert);
    
    /**
     * Visit all children of a node.
     */
    default List<T> visitChildren(DetailNode node) {
        return node.children().stream()
            .map(this::visit)
            .toList();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ABSTRACT ADAPTER - Provides defaults for simple visitors
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Abstract adapter with default implementations that return null.
     * Extend this for visitors that only care about specific node types.
     */
    abstract class Adapter<T> implements DetailVisitor<T> {
        
        @Override
        public T visitHeader(HeaderDetail header) {
            visitChildren(header);
            return null;
        }
        
        @Override
        public T visitField(FieldDetail field) {
            visitChildren(field);
            return null;
        }
        
        @Override
        public T visitSection(SectionDetail section) {
            visitChildren(section);
            return null;
        }
        
        @Override
        public T visitData(DataDetail data) {
            return null;
        }
        
        @Override
        public T visitExpert(ExpertDetail expert) {
            return null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VOID ADAPTER - For visitors that don't return values
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Adapter for visitors that perform actions but don't return values.
     */
    abstract class VoidAdapter implements DetailVisitor<Void> {
        
        @Override
        public Void visitHeader(HeaderDetail header) {
            onHeader(header);
            visitChildren(header);
            return null;
        }
        
        @Override
        public Void visitField(FieldDetail field) {
            onField(field);
            visitChildren(field);
            return null;
        }
        
        @Override
        public Void visitSection(SectionDetail section) {
            onSection(section);
            visitChildren(section);
            return null;
        }
        
        @Override
        public Void visitData(DataDetail data) {
            onData(data);
            return null;
        }
        
        @Override
        public Void visitExpert(ExpertDetail expert) {
            onExpert(expert);
            return null;
        }
        
        // Override these for specific behavior
        protected void onHeader(HeaderDetail header) {}
        protected void onField(FieldDetail field) {}
        protected void onSection(SectionDetail section) {}
        protected void onData(DataDetail data) {}
        protected void onExpert(ExpertDetail expert) {}
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLLECTING VISITOR - Collects specific node types
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Visitor that collects all nodes of a specific type.
     */
    static <N extends DetailNode> List<N> collect(List<DetailNode> roots, Class<N> nodeType) {
        List<N> result = new java.util.ArrayList<>();
        
        var collector = new VoidAdapter() {
            @Override
            public Void visit(DetailNode node) {
                if (nodeType.isInstance(node)) {
                    result.add(nodeType.cast(node));
                }
                return super.visit(node);
            }
        };
        
        for (DetailNode root : roots) {
            collector.visit(root);
        }
        
        return result;
    }
    
    /**
     * Collect all fields from a detail tree.
     */
    static List<FieldDetail> collectFields(List<DetailNode> roots) {
        return collect(roots, FieldDetail.class);
    }
    
    /**
     * Collect all expert items from a detail tree.
     */
    static List<ExpertDetail> collectExperts(List<DetailNode> roots) {
        return collect(roots, ExpertDetail.class);
    }
    
    /**
     * Find first node matching predicate.
     */
    static <N extends DetailNode> java.util.Optional<N> find(
            List<DetailNode> roots, 
            Class<N> nodeType, 
            java.util.function.Predicate<N> predicate) {
        
        for (N node : collect(roots, nodeType)) {
            if (predicate.test(node)) {
                return java.util.Optional.of(node);
            }
        }
        return java.util.Optional.empty();
    }
}