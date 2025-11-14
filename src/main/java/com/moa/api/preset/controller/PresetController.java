package com.moa.api.preset.controller;

import com.moa.api.preset.dto.PresetListResponseDTO;
import com.moa.api.preset.dto.SaveSearchPresetRequestDTO;
import com.moa.api.preset.dto.SaveSearchPresetResponseDTO;
import com.moa.api.preset.service.MyPagePresetService;
import com.moa.api.preset.service.PresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presets")
public class PresetController {
    private final PresetService service;
    private final MyPagePresetService myPageService;

    @PostMapping("/search")
    public SaveSearchPresetResponseDTO createSearch(@RequestBody SaveSearchPresetRequestDTO req) throws Exception {
        return service.saveGrid(req);
    }

    @PostMapping("/pivot")
    public SaveSearchPresetResponseDTO createPivot(@RequestBody SaveSearchPresetRequestDTO req) throws Exception {
        return service.savePivot(req);
    }

    @GetMapping("/pivot")
    public PresetListResponseDTO list(Authentication auth,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String origin) {
        Long userId = (Long) auth.getPrincipal();
        return myPageService.findMyPresets(userId, page, size, type, origin);
    }
}