package com.moa.api.grid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.grid.dto.AggregateRequestDTO;
import com.moa.api.grid.dto.AggregateResponseDTO;
import com.moa.api.grid.dto.DistinctValuesRequestDTO;
import com.moa.api.grid.dto.FilterResponseDTO;
import com.moa.api.grid.service.GridAsyncService;
import com.moa.api.grid.service.GridService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GridController 테스트
 */
@WebMvcTest(GridController.class)
class GridControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GridService gridService;

    @MockitoBean
    private GridAsyncService asyncService;

    @Test
    void getDistinctValuesPost_정상_요청() throws Exception {
        // given
        DistinctValuesRequestDTO request = new DistinctValuesRequestDTO();
        request.setLayer("http_page");
        request.setField("src_ip");
        request.setLimit(100);

        FilterResponseDTO mockResponse = FilterResponseDTO.builder()
                .field("src_ip")
                .values(List.of("192.168.1.1", "10.0.0.1"))
                .total(2L)
                .build();

        when(gridService.getDistinctValues(
                anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyBoolean(), anyString(), anyString(), anyString()
        )).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/grid/filtering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.field").value("src_ip"))
                .andExpect(jsonPath("$.values").isArray())
                .andExpect(jsonPath("$.values.length()").value(2));
    }

    @Test
    void getDistinctValuesPost_필수_필드_누락() throws Exception {
        // given
        DistinctValuesRequestDTO request = new DistinctValuesRequestDTO();
        request.setLayer("http_page");
        // field 누락

        // when & then
        mockMvc.perform(post("/api/grid/filtering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getDistinctValuesPost_limit_범위_초과() throws Exception {
        // given
        DistinctValuesRequestDTO request = new DistinctValuesRequestDTO();
        request.setLayer("http_page");
        request.setField("src_ip");
        request.setLimit(2000); // 최대 1000

        // when & then
        mockMvc.perform(post("/api/grid/filtering")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getDistinctValuesGet_정상_요청() throws Exception {
        // given
        FilterResponseDTO mockResponse = FilterResponseDTO.builder()
                .field("src_ip")
                .values(List.of("192.168.1.1"))
                .total(1L)
                .build();

        when(gridService.getDistinctValues(
                anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyBoolean(), anyString(), anyString(), anyString()
        )).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/grid/filtering")
                        .param("layer", "http_page")
                        .param("field", "src_ip")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.field").value("src_ip"));
    }

    @Test
    void aggregate_정상_요청() throws Exception {
        // given
        AggregateRequestDTO request = new AggregateRequestDTO();
        request.setLayer("http_page");
        request.setMetrics(Map.of(
                "src_ip", createMetricSpec("string", List.of("count"))
        ));

        AggregateResponseDTO mockResponse = new AggregateResponseDTO(
                Map.of("src_ip", Map.of("count", 100L))
        );

        when(asyncService.aggregateSync(any())).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/aggregate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.src_ip.count").value(100));
    }

    @Test
    void aggregate_metrics_누락() throws Exception {
        // given
        AggregateRequestDTO request = new AggregateRequestDTO();
        request.setLayer("http_page");
        // metrics 누락

        // when & then
        mockMvc.perform(post("/api/aggregate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getCacheStats_정상_조회() throws Exception {
        // given
        var mockStats = new GridAsyncService.CacheStats(10, 2, 0.83);
        when(asyncService.getCacheStats()).thenReturn(mockStats);

        // when & then
        mockMvc.perform(get("/api/aggregate/cache-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached_results").value(10))
                .andExpect(jsonPath("$.running_tasks").value(2))
                .andExpect(jsonPath("$.hit_rate").exists());
    }

    private AggregateRequestDTO.MetricSpec createMetricSpec(String type, List<String> ops) {
        AggregateRequestDTO.MetricSpec spec = new AggregateRequestDTO.MetricSpec();
        spec.setType(type);
        spec.setOps(ops);
        return spec;
    }
}