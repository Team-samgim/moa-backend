package com.moa.api.search.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * 작성자: 정소영
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonPropertyOrder({"key", "label", "labelKo", "dataType", "isInfo", "orderNo", "operators"})
public class FieldWithOpsDTO {
    private String key;
    private String label;
    private String labelKo;
    private String dataType;
    private boolean isInfo;
    private Integer orderNo;
    private List<OperatorDTO> operators;
}