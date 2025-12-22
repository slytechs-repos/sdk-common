package com.slytechs.jnet.core.api.detail.render;

import com.slytechs.jnet.core.api.detail.DataDetail;
import com.slytechs.jnet.core.api.detail.DetailNode;
import com.slytechs.jnet.core.api.detail.ExpertDetail;
import com.slytechs.jnet.core.api.detail.FieldDetail;
import com.slytechs.jnet.core.api.detail.HeaderDetail;
import com.slytechs.jnet.core.api.detail.SectionDetail;

/**
 * Visitor for rendering DetailNode trees.
 */
public interface DetailVisitor<T> {
    
    T visit(DetailNode node);
    
    default T visitHeader(HeaderDetail header) { return visit(header); }
    default T visitField(FieldDetail field) { return visit(field); }
    default T visitSection(SectionDetail section) { return visit(section); }
    default T visitData(DataDetail data) { return visit(data); }
    default T visitExpert(ExpertDetail expert) { return visit(expert); }
}

