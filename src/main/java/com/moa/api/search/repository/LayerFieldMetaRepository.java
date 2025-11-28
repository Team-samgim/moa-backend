/**
 * 작성자: 정소영
 */
package com.moa.api.search.repository;

import com.moa.api.search.entity.LayerFieldMeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LayerFieldMetaRepository
        extends JpaRepository<LayerFieldMeta, String>,
        LayerFieldMetaRepositoryCustom {

    // JpaRepository의 기본 메서드 + Custom 메서드 모두 사용 가능
}