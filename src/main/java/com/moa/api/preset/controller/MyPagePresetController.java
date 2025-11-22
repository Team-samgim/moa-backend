package com.moa.api.preset.controller;

import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.dto.PresetListResponseDTO;
import com.moa.api.preset.service.MyPagePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mypage/presets")
@RequiredArgsConstructor
public class MyPagePresetController {

    private final MyPagePresetService service;

    @GetMapping
    public PresetListResponseDTO list(Authentication auth,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String origin) {
        Long userId = (Long) auth.getPrincipal();
        return service.findMyPresets(userId, page, size, type, origin);
    }

    @PatchMapping("/{id}/favorite")
    public PresetItemDTO toggleFavorite(Authentication auth,
                                        @PathVariable("id") Integer id,
                                        @RequestBody Map<String, Object> body) {
        Long userId = (Long) auth.getPrincipal();
        boolean favorite = Boolean.TRUE.equals(body.get("favorite"));
        return service.setFavorite(userId, id, favorite);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(Authentication auth,
                                      @PathVariable("id") Integer id) {
        Long userId = (Long) auth.getPrincipal();
        service.delete(userId, id);
        return Map.of("ok", true);
    }
}