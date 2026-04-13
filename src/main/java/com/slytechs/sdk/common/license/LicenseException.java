package com.slytechs.sdk.common.license;

public class LicenseException extends RuntimeException {
    private static final long serialVersionUID = -7432526283544554663L;
	public LicenseException(String message) { super(message); }
    public LicenseException(Throwable cause) { super(cause); }
}