package com.slytechs.jnet.core.api.detail;
public interface HeaderOption {
    
    int optionId();
    
    int optionOffset();
    
    int optionLength();
    
    boolean isPresent();
    
    String optionName();
    
    void buildDetail(DetailBuilder.FieldContainer f);  // Changed from HeaderBuilder
}