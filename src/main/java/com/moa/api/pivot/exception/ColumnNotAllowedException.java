// 작성자: 최이서
package com.moa.api.pivot.exception;

public class ColumnNotAllowedException extends RuntimeException {
    public ColumnNotAllowedException(String column) {
        super("Column not allowed: " + column);
    }
}
