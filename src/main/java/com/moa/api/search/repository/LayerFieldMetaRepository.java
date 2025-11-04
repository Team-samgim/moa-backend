package com.moa.api.search.repository;

import com.moa.api.search.entity.LayerFieldMeta;

import java.util.List;

public interface LayerFieldMetaRepository {
    // 기본 메서드만 유지 (http_page_fields용 폴백)
    List<LayerFieldMeta> findAllByOrderByFieldKeyAsc();
}
