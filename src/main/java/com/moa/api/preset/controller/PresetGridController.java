package com.moa.api.preset.controller;

import com.moa.api.preset.dto.SaveSearchPresetRequestDTO;
import com.moa.api.preset.dto.SaveSearchPresetResponseDTO;
import com.moa.api.preset.service.PresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presets")
public class PresetGridController {
    private final PresetService service;

    @PostMapping("/search")
    public SaveSearchPresetResponseDTO createSearch(@RequestBody SaveSearchPresetRequestDTO req) throws Exception {
        return service.saveGrid(req);
    }
}