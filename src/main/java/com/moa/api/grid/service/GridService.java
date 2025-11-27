package com.moa.api.grid.service;

import com.moa.api.grid.dto.*;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.grid.validation.GridValidator;
import com.moa.api.search.dto.SearchDTO;
import com.moa.api.search.service.SearchExecuteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Grid 서비스
 *
 * AUTHOR        : 방대혁
 *
 * 역할:
 * - DISTINCT 필터 값 조회
 * - 집계(Aggregate) 실행
 * - SearchSpec 기반 그리드 데이터 조회
 * - 공통 Validation(GridValidator) 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GridService {

    private final GridRepositoryImpl gridRepository;
    private final SearchExecuteService executeService;

    /** 레이어 / 필터 / baseSpec 등 공통 검증 담당 */
    private final GridValidator validator;

    /** 필터 JSON 파싱용 ObjectMapper (self-contained 사용) */
    private final ObjectMapper om = new ObjectMapper();

    /**
     * DISTINCT 값 조회
     *
     * 사용처:
     * - 그리드 필터 팝업에서 특정 컬럼의 유니크한 값 목록을 가져올 때 사용
     *
     * 처리 흐름:
     * 1) layer / filterModel / baseSpec 에 대해 Validation 수행
     * 2) filterModel 안에 자기 자신 컬럼에 대한 필터 조건이 있는지 체크(hasSelfFilter)
     * 3) includeSelf 플래그를 계산해서 Repository 에 전달
     * 4) GridRepositoryImpl.getDistinctValuesPaged 호출
     * 5) DistinctPageDTO 를 FilterResponseDTO 로 변환해 반환
     */
    public FilterResponseDTO getDistinctValues(
            String layer,
            String field,
            String filterModel,
            String search,
            int offset,
            int limit,
            boolean includeSelfFromClient,
            String orderBy,
            String order,
            String baseSpecJson) {

        // 1) 입력값 Validation
        validator.validateLayer(layer);
        validator.validateFilterModel(filterModel);
        validator.validateBaseSpec(baseSpecJson);

        // 2) 클라이언트에서 includeSelf 요청 + 필터모델 안에 자기 필드 조건이 있는지 검사
        boolean includeSelf = includeSelfFromClient || hasSelfFilter(filterModel, field);

        // 3) Repository 에서 DISTINCT 페이지 조회
        var page = gridRepository.getDistinctValuesPaged(
                layer, field, filterModel, includeSelf, search,
                offset, limit, orderBy, order, baseSpecJson
        );

        // String 리스트를 Object 리스트로 래핑 (프론트 일관성 유지용)
        List<Object> values = page.values().stream()
                .map(v -> (Object) v)
                .toList();

        // 4) 페이징 정보 포함 응답 구성
        return FilterResponseDTO.builder()
                .field(field)
                .values(values)
                .total(page.total())
                .offset(page.offset())
                .limit(page.limit())
                .nextOffset(page.nextOffset())
                .hasMore(page.nextOffset() != null)
                .build();
    }

    /**
     * filterModel 안에 특정 field 에 대한 자기 필터가 있는지 검사
     *
     * 기준:
     * - filterModel JSON 최상위 key 중 safeFieldName 이 동일한 필드를 찾음
     * - 해당 노드의 mode 가 checkbox/condition 인 경우 "자기 필터"로 간주
     *
     * 예:
     * {
     *   "http_host": { "mode": "checkbox", ... },
     *   "country": { "mode": "condition", ... }
     * }
     */
    private boolean hasSelfFilter(String filterModel, String field) {
        if (filterModel == null || filterModel.isBlank()
                || field == null || field.isBlank()) {
            return false;
        }

        try {
            JsonNode root = om.readTree(filterModel);
            String target = safeFieldName(field);

            for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
                String k = it.next();
                if (!safeFieldName(k).equals(target)) {
                    continue;
                }

                JsonNode node = root.get(k);
                String mode = node.path("mode").asText("");
                if ("checkbox".equalsIgnoreCase(mode)
                        || "condition".equalsIgnoreCase(mode)) {
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            // 필터 JSON 파싱 실패 시 자기필터 여부 판단은 포기하고 false 처리
            log.debug("[GridService] hasSelfFilter parse fail: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 컬럼명 안전 변환
     *
     * - 프론트에서 "http_host-label" 처럼 suffix 가 붙는 경우가 있어
     *   실제 컬럼명 비교 시 '-' 이전까지만 사용
     */
    private String safeFieldName(String f) {
        if (f == null) return "";
        int i = f.indexOf('-');
        return (i >= 0) ? f.substring(0, i) : f;
    }

    /**
     * 집계 실행 서비스
     *
     * 사용처:
     * - HTTP_PAGE / HTTP_URI / L4_TCP / ETHERNET 레벨에서
     *   count / sum / avg / min / max / distinct / topN 등 집계 정보를 요청할 때
     *
     * 처리 흐름:
     * 1) layer / filterModel / baseSpec Validation
     * 2) GridRepositoryImpl.aggregate 에 위임
     */
    public AggregateResponseDTO aggregate(AggregateRequestDTO req) {
        // Validation (레이어, 필터 JSON, baseSpec JSON)
        validator.validateLayer(req.getLayer());
        validator.validateFilterModel(req.getFilterModel());
        validator.validateBaseSpec(req.getBaseSpecJson());

        return gridRepository.aggregate(req);
    }

    /**
     * 컬럼 메타데이터 조회
     *
     * - 해당 layer 의 모든 컬럼명, 타입(frontend 타입), 한글 라벨을 반환
     * - 프론트에서 컬럼 선택 UI, 검색/필터 UI 구성 시 사용
     */
    public List<SearchResponseDTO.ColumnDTO> getColumnsWithType(String layer) {
        validator.validateLayer(layer);
        return gridRepository.getColumnsWithType(layer);
    }

    /**
     * SearchSpec 기반 그리드 데이터 조회
     *
     * 사용처:
     * - 프론트에서 SearchDTO 형태로 검색/필터/기간/정렬 정보까지 묶어서 요청
     * - executeService.execute 로 실제 SQL 구성 및 실행
     * - 컬럼 메타데이터 조회 후, 요청된 컬럼 순서대로 ColumnDTO 목록을 구성
     *
     * 주의:
     * - layer 파라미터가 비어있으면 기본 "http_page" 사용
     * - 요청된 columns 가 없으면 모든 컬럼 반환
     */
    public SearchResponseDTO getGridDataBySearchSpec(SearchDTO req) {
        log.info("[GridService] 받은 요청 layer={}, columns={}, conditions={}, time={}",
                req.getLayer(), req.getColumns(), req.getConditions(), req.getTime());

        // 레이어 Validation
        validator.validateLayer(req.getLayer());

        // 실제 검색 실행 (조건 → SQL 변환 → 실행 → rows/total 채운 SearchDTO 반환)
        SearchDTO out = executeService.execute(req);

        log.info("[GridService] rows={}, total={}",
                (out.getRows() != null ? out.getRows().size() : null),
                out.getTotal());

        if (out.getRows() != null && !out.getRows().isEmpty()) {
            log.debug("[GridService] 첫 row keys={}", out.getRows().get(0).keySet());
        }

        // 레이어 기본값 보정
        String layer = (req.getLayer() == null || req.getLayer().isBlank())
                ? "http_page"
                : req.getLayer();

        // 전체 컬럼 메타 로딩
        var allColumns = gridRepository.getColumnsWithType(layer);
        log.info("[GridService] 전체 컬럼 수={}", allColumns.size());

        // 프론트에서 요청한 컬럼만 필터링 (순서 보존)
        List<String> requested = req.getColumns();
        List<SearchResponseDTO.ColumnDTO> filteredColumns;

        if (requested != null && !requested.isEmpty()) {
            // name → ColumnDTO 맵
            Map<String, SearchResponseDTO.ColumnDTO> index =
                    allColumns.stream()
                            .collect(Collectors.toMap(
                                    SearchResponseDTO.ColumnDTO::getName,
                                    c -> c,
                                    (a, b) -> a
                            ));

            filteredColumns = requested.stream()
                    .map(index::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("[GridService] 필터링된 컬럼 수={}", filteredColumns.size());
        } else {
            // 요청된 컬럼이 없으면 전체 컬럼 반환
            filteredColumns = allColumns;
        }

        // 최종 응답 DTO 구성
        return SearchResponseDTO.builder()
                .layer(layer)
                .columns(filteredColumns)
                .rows(out.getRows())
                .total(out.getTotal())
                .build();
    }
}
