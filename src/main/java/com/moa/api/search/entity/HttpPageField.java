package com.moa.api.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "http_page_fields")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HttpPageField {

    @Id
    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    // 편의 메서드 (필요시)
    public HttpPageField(String fieldKey, String dataType) {
        this.fieldKey = fieldKey;
        this.dataType = dataType;
    }
}