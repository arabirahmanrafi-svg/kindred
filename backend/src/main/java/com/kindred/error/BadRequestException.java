package com.kindred.error;

import java.util.Map;

/** A business-rule validation failure tied to specific form fields. */
public class BadRequestException extends RuntimeException {

    private final Map<String, String> fields;

    public BadRequestException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
