package com.moa.api.grid.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterRequestDTO {
    private String layer = "ethernet";
    private int offset = 0;
    private int limit = 100;
    private String sortField;
    private String sortDirection;
    private String filterModel;
}