package com.moa.api.pivot.exception;

public class ColumnNotAllowedException extends RuntimeException {
    public ColumnNotAllowedException(String column) {
        super("Column not allowed: " + column);
    }
}
