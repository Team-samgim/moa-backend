package com.moa.api.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "layer_fields")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    // 정적 팩토리 메서드 추가
    public static LayerFieldMeta of(String fieldKey, String dataType, String labelKo, Boolean isInfo) {
        LayerFieldMeta entity = new LayerFieldMeta();
        entity.fieldKey = fieldKey;
        entity.dataType = dataType;
        entity.labelKo = labelKo;
        entity.isInfo = isInfo;
        return entity;
    }

    // 또는 Builder 패턴 사용
    @Builder
    private LayerFieldMeta(String fieldKey, String dataType, String labelKo, Boolean isInfo) {
        this.fieldKey = fieldKey;
        this.dataType = dataType;
        this.labelKo = labelKo;
        this.isInfo = isInfo;
    }
}