package com.moa.api.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class AggregateResponseDTO {
    private Map<String, Object> aggregates; // { field: { count:.., sum:.., top1:{value,count} ... } }
}
