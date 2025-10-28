package com.moa.api.search.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldDTO {
    private String key;       // ex) "src_ip"
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String label;     // 없으면 key 사용
    private String dataType;  // "IP" | "NUMBER" | ...
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer orderNo;  // 없으면 null
}