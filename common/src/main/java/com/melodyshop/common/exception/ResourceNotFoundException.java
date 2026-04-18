package com.melodyshop.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("Không tìm thấy %s với %s: %s", resource, field, value));
    }
}
