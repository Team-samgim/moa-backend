package com.moa.api.search.repository;

import com.moa.api.search.entity.LayerFieldMeta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class LayerFieldMetaRepositoryImpl implements LayerFieldMetaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LayerFieldMeta> findAllByTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            log.error("Invalid table name: {}", tableName);
            return Collections.emptyList();
        }

        String sql = String.format("""
            SELECT 
                field_key,
                data_type,
                label_ko,
                COALESCE(is_info, false) as is_info
            FROM %s
            ORDER BY field_key
        """, tableName);

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

            return results.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error loading fields from {}: {}", tableName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean existsTable(String tableName) {
        if (!isValidTableName(tableName)) {
            return false;
        }

        String sql = """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = :tableName
            )
        """;

        try {
            Boolean exists = (Boolean) entityManager.createNativeQuery(sql)
                    .setParameter("tableName", tableName)
                    .getSingleResult();
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error checking table existence for {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    private LayerFieldMeta mapToEntity(Object[] row) {
        // 정적 팩토리 메서드 사용
        return LayerFieldMeta.of(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (Boolean) row[3]
        );

        // 또는 Builder 사용
        // return LayerFieldMeta.builder()
        //         .fieldKey((String) row[0])
        //         .dataType((String) row[1])
        //         .labelKo((String) row[2])
        //         .isInfo((Boolean) row[3])
        //         .build();
    }

    private boolean isValidTableName(String tableName) {
        return tableName != null && tableName.matches("^[a-z_]+$");
    }
}