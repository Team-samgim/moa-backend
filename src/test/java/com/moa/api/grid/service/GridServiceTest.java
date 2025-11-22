package com.moa.api.grid.service;

import com.moa.api.grid.dto.AggregateRequestDTO;
import com.moa.api.grid.dto.FilterResponseDTO;
import com.moa.api.grid.exception.GridException;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.grid.validation.GridValidator;
import com.moa.api.search.service.SearchExecuteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GridService 통합 테스트
 */
@ExtendWith(MockitoExtension.class)
class GridServiceTest {

    @Mock
    private GridRepositoryImpl gridRepository;

    @Mock
    private SearchExecuteService executeService;

    @Mock
    private GridValidator validator;

    @InjectMocks
    private GridService gridService;

    @Test
    void getDistinctValues_정상_호출() {
        // given
        String layer = "http_page";
        String field = "src_ip";
        String filterModel = "{}";

        var mockPage = new com.moa.api.grid.dto.DistinctPageDTO(
                List.of("192.168.1.1", "10.0.0.1"),
                0L, 0, 100, null
        );

        when(gridRepository.getDistinctValuesPaged(
                anyString(), anyString(), anyString(), anyBoolean(),
                anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString()
        )).thenReturn(mockPage);

        // when
        FilterResponseDTO result = gridService.getDistinctValues(
                layer, field, filterModel, "", 0, 100, false, null, "DESC", null
        );

        // then
        assertThat(result.getField()).isEqualTo(field);
        assertThat(result.getValues()).hasSize(2);
        assertThat(result.getHasMore()).isFalse();

        // Validation 호출 확인
        verify(validator).validateLayer(layer);
        verify(validator).validateFilterModel(filterModel);
        verify(validator).validateBaseSpec(null);
    }

    @Test
    void getDistinctValues_잘못된_레이어() {
        // given
        String invalidLayer = "invalid_layer";
        doThrow(new GridException(GridException.ErrorCode.LAYER_NOT_FOUND))
                .when(validator).validateLayer(invalidLayer);

        // when & then
        assertThatThrownBy(() ->
                gridService.getDistinctValues(
                        invalidLayer, "field", "{}", "", 0, 100, false, null, "DESC", null
                )
        ).isInstanceOf(GridException.class);
    }

    @Test
    void aggregate_정상_호출() {
        // given
        AggregateRequestDTO req = new AggregateRequestDTO();
        req.setLayer("http_page");
        req.setMetrics(Map.of(
                "src_ip", createMetricSpec("string", List.of("count", "distinct"))
        ));

        var mockResponse = new com.moa.api.grid.dto.AggregateResponseDTO(
                Map.of("src_ip", Map.of("count", 100L, "distinct", 10L))
        );

        when(gridRepository.aggregate(any())).thenReturn(mockResponse);

        // when
        var result = gridService.aggregate(req);

        // then
        assertThat(result.getAggregates()).containsKey("src_ip");

        // Validation 호출 확인
        verify(validator).validateLayer("http_page");
        verify(validator).validateFilterModel(null);
        verify(validator).validateBaseSpec(null);
    }

    @Test
    void aggregate_잘못된_필터_모델() {
        // given
        AggregateRequestDTO req = new AggregateRequestDTO();
        req.setLayer("http_page");
        req.setFilterModel("{ invalid json }");

        doThrow(new GridException(GridException.ErrorCode.INVALID_FILTER_MODEL))
                .when(validator).validateFilterModel(anyString());

        // when & then
        assertThatThrownBy(() -> gridService.aggregate(req))
                .isInstanceOf(GridException.class)
                .hasMessageContaining("필터 모델");
    }

    @Test
    void hasSelfFilter_자기_필터_감지() {
        // given
        String filterModel = """
            {
                "src_ip": {
                    "mode": "checkbox",
                    "values": ["192.168.1.1"]
                }
            }
            """;

        // when
        gridService.getDistinctValues(
                "http_page", "src_ip", filterModel, "", 0, 100, false, null, "DESC", null
        );

        // then - includeSelf가 자동으로 true가 되어야 함
        verify(gridRepository).getDistinctValuesPaged(
                anyString(), anyString(), anyString(),
                eq(true),  // includeSelf가 true로 호출되어야 함
                anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString()
        );
    }

    private AggregateRequestDTO.MetricSpec createMetricSpec(String type, List<String> ops) {
        AggregateRequestDTO.MetricSpec spec = new AggregateRequestDTO.MetricSpec();
        spec.setType(type);
        spec.setOps(ops);
        return spec;
    }
}