package com.slytechs.jnet.core.api.detail;

import com.slytechs.jnet.core.api.detail.DetailBuilder.SectionBuilder;

public interface DetailableComponent {
    void buildDetail(SectionBuilder s);
}