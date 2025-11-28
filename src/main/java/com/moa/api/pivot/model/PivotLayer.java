// 작성자: 최이서
package com.moa.api.pivot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PivotLayer {

    HTTP_PAGE("HTTP_PAGE", "http_page_sample", "http_page_fields", "ts_server_nsec"),
    ETHERNET("Ethernet", "ethernet_sample", "ethernet_fields", "ts_server_nsec"),
    TCP("TCP", "tcp_sample", "tcp_fields", "ts_server_nsec"),
    HTTP_URI("HTTP_URI", "http_uri_sample", "http_uri_fields", "ts_server_nsec");

    private final String code;
    private final String dataTable;
    private final String fieldsTable;
    private final String defaultTimeField;

    public static PivotLayer from(String raw) {
        if (raw == null || raw.isBlank()) {
            return HTTP_PAGE; // 기본값
        }
        String normalized = raw.trim();
        for (PivotLayer l : values()) {
            if (l.code.equalsIgnoreCase(normalized)) {
                return l;
            }
        }
        throw new IllegalArgumentException("Unsupported layer: " + raw);
    }
}
