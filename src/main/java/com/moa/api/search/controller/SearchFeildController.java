package com.moa.api.search.controller;

import com.moa.api.search.dto.FieldDTO;
import com.moa.api.search.dto.FieldWithOpsDTO;
import com.moa.api.search.dto.OperatorDTO;
import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.service.SearchExecuteService;
import com.moa.api.search.service.SearchFeildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchFeildController {

    private final SearchFeildService metaService;
    private final SearchExecuteService executeService;

    @GetMapping("/field")
    public Map<String, Object> meta(
            @RequestParam(defaultValue = "HTTP_PAGE") String layer,
            @RequestParam(defaultValue = "byField") String view   // 기본: byField
    ) {
        if ("byField".equalsIgnoreCase(view)) {
            List<FieldWithOpsDTO> fields = metaService.listFieldsWithOperators(layer);
            return Map.of("fields", fields);
        }
        // normalized 모드도 필요하면 지원
        List<FieldDTO> fields = metaService.listFields(layer);
        Map<String, List<OperatorDTO>> ops = metaService.operatorsByType();
        return Map.of("fields", fields, "operatorsByType", ops);
    }

    @PostMapping("/execute")
    public ResponseEntity<SearchDTO> execute(@RequestBody SearchDTO req) {
        return ResponseEntity.ok(executeService.execute(req));
    }
}
