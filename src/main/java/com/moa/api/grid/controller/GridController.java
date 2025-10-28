package com.moa.api.grid.controller;

import com.moa.api.grid.dto.DistinctValueResponseDTO;
import com.moa.api.grid.dto.FilterRequestDTO;
import com.moa.api.grid.dto.SearchResponseDTO;
import com.moa.api.grid.service.GridService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GridController {

    private final GridService gridService;

    @GetMapping("/randering")
    public SearchResponseDTO getData(@ModelAttribute FilterRequestDTO request) {
        return gridService.getGridData(request);
    }

    @GetMapping("/filtering")
    public DistinctValueResponseDTO getDistinctValues(
            @RequestParam(defaultValue = "ethernet") String layer,
            @RequestParam String field
    ) {
        return gridService.getDistinctValues(layer, field);
    }
}
