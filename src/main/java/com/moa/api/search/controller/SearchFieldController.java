/**
 * 작성자: 정소영
 */
package com.moa.api.search.controller;

import com.moa.api.search.dto.FieldWithOpsDTO;
import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.service.SearchExecuteService;
import com.moa.api.search.service.SearchFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchFieldController {

    private final SearchFieldService metaService;
    private final SearchExecuteService executeService;

    @GetMapping("/field")
    public Map<String, Object> meta(
            @RequestParam(defaultValue = "HTTP_PAGE") String layer
    ) {
        List<FieldWithOpsDTO> fields = metaService.listFieldsWithOperators(layer);
        return Map.of("fields", fields);
    }

    @PostMapping("/execute")
    public ResponseEntity<SearchDTO> execute(@RequestBody SearchDTO req) {
        return ResponseEntity.ok(executeService.execute(req));
    }
}
