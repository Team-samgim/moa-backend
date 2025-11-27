package com.moa.api.preset.controller;

import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.dto.PresetListResponseDTO;
import com.moa.api.preset.service.MyPagePresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ==============================================================
 * MyPagePresetController
 * --------------------------------------------------------------
 * '마이페이지 > 내가 저장한 Preset' 관련 API를 제공.
 *
 * 주요 기능:
 *   - Preset 목록 조회 (Pagination)
 *   - Preset 즐겨찾기(on/off)
 *   - Preset 삭제
 *
 * 설계 방향:
 *   - Authentication에서 memberId를 직접 꺼내는 구조 유지
 *   - 서비스 레이어에 모든 비즈니스 로직 위임
 *   - Controller는 단순 입출력 + 로깅만 담당
 * ==============================================================
 * AUTHOR        : 방대혁
 */
@Slf4j
@RestController
@RequestMapping("/api/mypage/presets")
@RequiredArgsConstructor
@Validated
public class MyPagePresetController {

    private final MyPagePresetService myPagePresetService;

    /**
     * ==============================================================
     * [GET] My Preset 목록 조회
     * --------------------------------------------------------------
     * - 페이지 기반 조회(page, size)
     * - type/origin 필터링 제공
     *   · type   : SEARCH / PIVOT / CHART
     *   · origin : USER / EXPORT
     *
     * @param auth   인증된 사용자 정보
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기 (default=10)
     * @param type   검색 프리셋 타입(Optional)
     * @param origin 프리셋 생성 출처(Optional)
     *
     * @return PresetListResponseDTO (items, page, totalPages 등 포함)
     * ==============================================================
     */
    @GetMapping
    public ResponseEntity<PresetListResponseDTO> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String origin
    ) {
        Long userId = (Long) auth.getPrincipal();

        log.info("GET /api/mypage/presets - userId={}, page={}, size={}, type={}, origin={}",
                userId, page, size, type, origin);

        // 비즈니스 로직 위임
        PresetListResponseDTO response = myPagePresetService.findMyPresets(
                userId,
                page,
                size,
                type,
                origin
        );

        log.debug("Found {} presets for userId={} (page {}/{})",
                response.getItems().size(), userId, response.getPage(), response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * ==============================================================
     * [PATCH] Preset 즐겨찾기 상태 변경
     * --------------------------------------------------------------
     * - RequestBody 예: {"favorite": true}
     * - Boolean.TRUE.equals() 로 null-safe 처리
     *
     * @param auth     인증 정보
     * @param presetId Preset ID
     * @param body     favorite=true/false
     *
     * @return 수정된 PresetItemDTO
     * ==============================================================
     */
    @PatchMapping("/{presetId}/favorite")
    public ResponseEntity<PresetItemDTO> toggleFavorite(
            Authentication auth,
            @PathVariable("presetId") Integer presetId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = (Long) auth.getPrincipal();

        log.info("PATCH /api/mypage/presets/{}/favorite - userId={}", presetId, userId);

        // null-safe Boolean 처리
        boolean favorite = Boolean.TRUE.equals(body.get("favorite"));

        PresetItemDTO response = myPagePresetService.setFavorite(userId, presetId, favorite);

        log.info("Favorite updated: presetId={}, favorite={}", presetId, favorite);

        return ResponseEntity.ok(response);
    }

    /**
     * ==============================================================
     * [DELETE] Preset 삭제
     * --------------------------------------------------------------
     * - userId와 presetId의 소유 검증은 Service 레이어에서 수행
     * - 성공 시 {"ok": true} 형태로 응답
     *
     * @param auth     인증 정보
     * @param presetId Preset ID
     * @return { "ok": true }
     * ==============================================================
     */
    @DeleteMapping("/{presetId}")
    public ResponseEntity<Map<String, Object>> delete(
            Authentication auth,
            @PathVariable("presetId") Integer presetId
    ) {
        Long userId = (Long) auth.getPrincipal();

        log.info("DELETE /api/mypage/presets/{} - userId={}", presetId, userId);

        myPagePresetService.delete(userId, presetId);

        log.info("Preset deleted successfully: presetId={}", presetId);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
