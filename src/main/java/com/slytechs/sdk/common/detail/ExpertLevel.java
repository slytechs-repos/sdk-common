package com.slytechs.sdk.common.detail;
/**
 * Expert severity levels
 */
public enum ExpertLevel {
    INFO,       // Informational
    NOTE,       // Notable but normal
    WARN,       // Warning - something unusual
    ERROR,      // Error - protocol violation
    CRITICAL    // Critical - security issue, attack
}