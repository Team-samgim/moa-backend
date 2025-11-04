package com.moa.api.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "layer_fields") // 기본값 (실제로는 동적으로 변경됨)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class LayerFieldMeta {

    @Id
    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "label_ko")
    private String labelKo;

    @Column(name = "is_info")
    private Boolean isInfo;

    public LayerFieldMeta(String fieldKey, String dataType) {
        this.fieldKey = fieldKey;
        this.dataType = dataType;
    }
}