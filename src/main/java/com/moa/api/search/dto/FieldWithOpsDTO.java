package com.moa.api.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FieldWithOpsDTO {
    private String key;        // 예: "src_ip"
    private String label;      // 예: "src_ip"
    private String dataType;   // 예: "IP"
    private Integer orderNo;   // 정렬용(옵션)
    private List<OperatorDTO> operators; // 이 필드에서 쓸 수 있는 연산자들
}
