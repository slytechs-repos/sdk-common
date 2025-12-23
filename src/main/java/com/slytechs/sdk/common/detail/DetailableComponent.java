package com.slytechs.sdk.common.detail;

import com.slytechs.sdk.common.detail.DetailBuilder.SectionBuilder;

public interface DetailableComponent {
    void buildDetail(SectionBuilder s);
}