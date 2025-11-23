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
 * MyPage Preset Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/mypage/presets")
@RequiredArgsConstructor
@Validated
public class MyPagePresetController {

    private final MyPagePresetService myPagePresetService;

    /**
     * 내 Preset 목록 조회
     *
     * @param auth 인증 정보
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param type Preset 타입 (SEARCH, PIVOT, CHART)
     * @param origin Preset Origin (USER, EXPORT)
     * @return Preset 목록
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
     * Preset Favorite 상태 변경
     *
     * @param auth 인증 정보
     * @param presetId Preset ID
     * @param body favorite 값 포함
     * @return 수정된 Preset
     */
    @PatchMapping("/{presetId}/favorite")
    public ResponseEntity<PresetItemDTO> toggleFavorite(
            Authentication auth,
            @PathVariable("presetId") Integer presetId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = (Long) auth.getPrincipal();

        log.info("PATCH /api/mypage/presets/{}/favorite - userId={}", presetId, userId);

        boolean favorite = Boolean.TRUE.equals(body.get("favorite"));

        PresetItemDTO response = myPagePresetService.setFavorite(userId, presetId, favorite);

        log.info("Favorite updated: presetId={}, favorite={}", presetId, favorite);

        return ResponseEntity.ok(response);
    }

    /**
     * Preset 삭제
     *
     * @param auth 인증 정보
     * @param presetId Preset ID
     * @return 성공 응답
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