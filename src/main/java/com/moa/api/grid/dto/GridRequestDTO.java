package com.moa.api.grid.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GridRequestDTO {
    @NotBlank(message = "layer is required")
    private String layer = "";
    private int offset = 0;
    private int limit = 100;
    private String sortField;
    private String sortDirection;
    private String filterModel;
}