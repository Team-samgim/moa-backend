// Custom 인터페이스
package com.moa.api.search.repository;

import com.moa.api.search.entity.LayerFieldMeta;
import java.util.List;

public interface LayerFieldMetaRepositoryCustom {
    List<LayerFieldMeta> findAllByTableName(String tableName);
    boolean existsTable(String tableName);
}