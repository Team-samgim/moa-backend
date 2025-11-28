package com.moa.api.search.dto;

/**
 * 작성자: 정소영
 */
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OperatorDTO {
    private String opCode;     // 예: "LIKE"
    private String label;      // 예: "포함"
    private int valueArity;    // 0 | 1 | 2 | -1(가변목록)
    private boolean isDefault; // 기본 선택 여부
    private int orderNo;       // 정렬 순서
}