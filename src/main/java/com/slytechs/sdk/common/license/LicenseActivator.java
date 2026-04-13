package com.slytechs.sdk.common.license;
@FunctionalInterface
public interface LicenseActivator {
    void activate() throws LicenseException;
}