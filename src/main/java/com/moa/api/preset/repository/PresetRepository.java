package com.moa.api.preset.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.exception.PresetException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Preset Repository (JdbcTemplate 기반)
 * -----------------------------------------------------------
 * - 복잡한 동적쿼리, JSONB 처리 등을 위해 JPA 대신 JDBC 직접 사용.
 * - INSERT, SELECT, UPDATE, DELETE 모두 직접 SQL 사용.
 * - JSONB <-> Map 변환은 ObjectMapper + PGobject 기반으로 처리.
 * AUTHOR        : 방대혁
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PresetRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    /**
     * Preset 생성 (INSERT)
     * -----------------------------------------------------------
     * - config(Map) → JSONB 변환 후 저장
     * - RETURNING preset_id 를 사용하여 생성된 키 반환
     */
    public Integer insert(
            Long memberId,
            String name,
            String type,
            Map<String, Object> config,
            boolean favorite,
            PresetOrigin origin
    ) {
        log.debug("Inserting preset: memberId={}, name={}, type={}, origin={}",
                memberId, name, type, origin);

        try {
            PGobject jsonb = convertToJsonb(config); // Map → JSONB 변환

            String sql = """
                INSERT INTO public.presets
                  (member_id, preset_name, preset_type, config, favorite, preset_origin)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING preset_id
                """;

            Integer presetId = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    memberId, name, type, jsonb, favorite, origin.name()
            );

            log.info("Preset inserted successfully: presetId={}, memberId={}", presetId, memberId);

            return presetId;

        } catch (Exception e) {
            log.error("Failed to insert preset: memberId={}, name={}", memberId, name, e);
            throw new PresetException(PresetException.ErrorCode.PRESET_CREATION_FAILED, e);
        }
    }


    /**
     * Preset 목록 조회 (기본 type 필터)
     * -----------------------------------------------------------
     * - 출력: PresetItemDTO 리스트
     * - 정렬 기준: updated_at DESC, created_at DESC
     * - 페이징: LIMIT / OFFSET
     */
    public List<PresetItemDTO> findByMember(Long memberId, String type, int page, int size) {
        log.debug("Finding presets: memberId={}, type={}, page={}, size={}",
                memberId, type, page, size);

        try {
            String sql = """
                SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                FROM presets
                WHERE member_id = ?
                """;

            List<Object> params = new ArrayList<>();
            params.add(memberId);

            // 타입 필터 적용
            if (type != null && !type.isBlank()) {
                sql += " AND preset_type = ? ";
                params.add(type);
            }

            // 최신순 정렬
            sql += " ORDER BY updated_at DESC NULLS LAST, created_at DESC ";
            // 페이징
            sql += " LIMIT ? OFFSET ? ";

            params.add(size);
            params.add(page * size);

            List<PresetItemDTO> result = jdbcTemplate.query(
                    sql,
                    getRowMapper(),
                    params.toArray()
            );

            log.debug("Found {} presets for memberId={}", result.size(), memberId);
            return result;

        } catch (Exception e) {
            log.error("Failed to find presets: memberId={}, type={}", memberId, type, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * Preset 목록 조회 (Origin 필터 포함)
     * -----------------------------------------------------------
     * - origin(SEARCH / EXPORT) 조건을 추가한 버전
     */
    public List<PresetItemDTO> findByMember(
            Long memberId,
            String type,
            String origin,
            int page,
            int size
    ) {
        log.debug("Finding presets with origin filter: memberId={}, type={}, origin={}, page={}, size={}",
                memberId, type, origin, page, size);

        try {
            String sql = """
                SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                FROM presets
                WHERE member_id = ?
                """;

            List<Object> params = new ArrayList<>();
            params.add(memberId);

            if (type != null && !type.isBlank()) {
                sql += " AND preset_type = ? ";
                params.add(type);
            }

            if (origin != null && !origin.isBlank()) {
                sql += " AND preset_origin = ? ";
                params.add(origin);
            }

            // 정렬 + 페이징
            sql += " ORDER BY updated_at DESC NULLS LAST, created_at DESC ";
            sql += " LIMIT ? OFFSET ? ";

            params.add(size);
            params.add(page * size);

            return jdbcTemplate.query(sql, getRowMapper(), params.toArray());

        } catch (Exception e) {
            log.error("Failed to find presets with origin: memberId={}, type={}, origin={}",
                    memberId, type, origin, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * Preset 개수 조회 (type 필터)
     */
    public long countByMember(Long memberId, String type) {
        log.debug("Counting presets: memberId={}, type={}", memberId, type);

        try {
            String sql = "SELECT count(*) FROM presets WHERE member_id = ? ";

            Long count;
            if (type != null && !type.isBlank()) {
                sql += " AND preset_type = ? ";
                count = jdbcTemplate.queryForObject(sql, Long.class, memberId, type);
            } else {
                count = jdbcTemplate.queryForObject(sql, Long.class, memberId);
            }

            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("Failed to count presets: memberId={}, type={}", memberId, type, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * Preset 개수 조회 (type + origin 필터)
     */
    public long countByMember(Long memberId, String type, String origin) {
        log.debug("Counting presets with origin: memberId={}, type={}, origin={}",
                memberId, type, origin);

        try {
            String sql = "SELECT count(*) FROM presets WHERE member_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(memberId);

            if (type != null && !type.isBlank()) {
                sql += " AND preset_type = ? ";
                params.add(type);
            }

            if (origin != null && !origin.isBlank()) {
                sql += " AND preset_origin = ? ";
                params.add(origin);
            }

            Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("Failed to count presets with origin: memberId={}, type={}, origin={}",
                    memberId, type, origin, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * Preset 단건 조회 (소유자 확인 포함)
     * -----------------------------------------------------------
     * - preset_id AND member_id 조건을 동시에 검사하여
     *   “본인 소유 Preset인지" 강하게 보장함.
     */
    public Optional<PresetItemDTO> findOneForOwner(Long memberId, Integer presetId) {
        log.debug("Finding preset for owner: memberId={}, presetId={}", memberId, presetId);

        try {
            String sql = """
                SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                FROM presets
                WHERE preset_id = ? AND member_id = ?
                """;

            List<PresetItemDTO> result = jdbcTemplate.query(sql, getRowMapper(), presetId, memberId);

            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));

        } catch (Exception e) {
            log.error("Failed to find preset: presetId={}, memberId={}", presetId, memberId, e);
            throw new PresetException(PresetException.ErrorCode.DATABASE_ERROR, e);
        }
    }


    /**
     * Favorite 상태 변경
     * -----------------------------------------------------------
     * - updated_at = now() 자동 갱신
     * - 영향받은 row 수 반환 (0이면 업데이트 실패)
     */
    public int updateFavorite(Long memberId, Integer presetId, boolean favorite) {
        log.debug("Updating favorite: presetId={}, memberId={}, favorite={}",
                presetId, memberId, favorite);

        try {
            String sql = """
                UPDATE presets 
                SET favorite = ?, updated_at = now() 
                WHERE preset_id = ? AND member_id = ?
                """;

            return jdbcTemplate.update(sql, favorite, presetId, memberId);

        } catch (Exception e) {
            log.error("Failed to update favorite: presetId={}, memberId={}", presetId, memberId, e);
            throw new PresetException(PresetException.ErrorCode.PRESET_UPDATE_FAILED, e);
        }
    }


    /**
     * Preset 삭제
     */
    public int deleteOne(Long memberId, Integer presetId) {
        log.debug("Deleting preset: presetId={}, memberId={}", presetId, memberId);

        try {
            String sql = "DELETE FROM presets WHERE preset_id = ? AND member_id = ?";
            return jdbcTemplate.update(sql, presetId, memberId);

        } catch (Exception e) {
            log.error("Failed to delete preset: presetId={}, memberId={}", presetId, memberId, e);
            throw new PresetException(PresetException.ErrorCode.PRESET_DELETE_FAILED, e);
        }
    }


    /**
     * RowMapper — ResultSet → PresetItemDTO 변환
     * -----------------------------------------------------------
     * - JSONB(config) → JsonNode 변환 포함
     */
    private RowMapper<PresetItemDTO> getRowMapper() {
        return (rs, rowNum) -> {
            PGobject configObj = (PGobject) rs.getObject("config");
            JsonNode config = parseJsonbToNode(configObj);

            Timestamp updatedAt = rs.getTimestamp("updated_at");

            return new PresetItemDTO(
                    rs.getInt("preset_id"),
                    rs.getString("preset_name"),
                    rs.getString("preset_type"),
                    config,
                    rs.getBoolean("favorite"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    updatedAt != null ? updatedAt.toLocalDateTime() : null
            );
        };
    }


    /**
     * Map → PGobject(JSONB)
     */
    private PGobject convertToJsonb(Map<String, Object> config) {
        try {
            PGobject jsonb = new PGobject();
            jsonb.setType("jsonb");
            jsonb.setValue(objectMapper.writeValueAsString(config));
            return jsonb;

        } catch (Exception e) {
            log.error("Failed to convert config to JSONB", e);
            throw new PresetException(
                    PresetException.ErrorCode.INVALID_CONFIG,
                    "설정을 JSON으로 변환할 수 없습니다: " + e.getMessage()
            );
        }
    }

    /**
     * JSONB → JsonNode 변환
     */
    private JsonNode parseJsonbToNode(PGobject pgObject) {
        try {
            if (pgObject == null || pgObject.getValue() == null) {
                return objectMapper.nullNode();
            }
            return objectMapper.readTree(pgObject.getValue());
        } catch (Exception e) {
            log.warn("Failed to parse JSONB to JsonNode, returning null node", e);
            return objectMapper.nullNode();
        }
    }
}
