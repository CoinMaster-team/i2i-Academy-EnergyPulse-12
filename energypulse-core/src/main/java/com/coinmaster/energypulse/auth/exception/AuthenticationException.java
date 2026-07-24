package com.coinmaster.energypulse.auth.exception;

public class AuthenticationException extends RuntimeException {

    private final String code;

    public AuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
