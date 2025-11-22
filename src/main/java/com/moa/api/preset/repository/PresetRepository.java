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
 * Preset Repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PresetRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Preset 삽입
     *
     * @return 생성된 preset_id
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
            // Config를 PostgreSQL JSONB로 변환
            PGobject jsonb = convertToJsonb(config);

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
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_CREATION_FAILED,
                    e
            );
        }
    }

    /**
     * 회원의 Preset 목록 조회
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

            if (type != null && !type.isBlank()) {
                sql += " AND preset_type = ? ";
                params.add(type);
            }

            sql += " ORDER BY updated_at DESC NULLS LAST, created_at DESC ";
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
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * 회원의 Preset 목록 조회 (Origin 필터 포함)
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

            sql += " ORDER BY updated_at DESC NULLS LAST, created_at DESC ";
            sql += " LIMIT ? OFFSET ? ";

            params.add(size);
            params.add(page * size);

            List<PresetItemDTO> result = jdbcTemplate.query(
                    sql,
                    getRowMapper(),
                    params.toArray()
            );

            log.debug("Found {} presets for memberId={} with origin filter", result.size(), memberId);

            return result;

        } catch (Exception e) {
            log.error("Failed to find presets with origin: memberId={}, type={}, origin={}",
                    memberId, type, origin, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * Preset 개수 조회
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

            log.debug("Found {} presets for memberId={}", count, memberId);

            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("Failed to count presets: memberId={}, type={}", memberId, type, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * Preset 개수 조회 (Origin 필터 포함)
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

            log.debug("Found {} presets for memberId={} with origin filter", count, memberId);

            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("Failed to count presets with origin: memberId={}, type={}, origin={}",
                    memberId, type, origin, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * 특정 Preset 조회 (소유자 확인)
     */
    public Optional<PresetItemDTO> findOneForOwner(Long memberId, Integer presetId) {
        log.debug("Finding preset for owner: memberId={}, presetId={}", memberId, presetId);

        try {
            String sql = """
                SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                FROM presets
                WHERE preset_id = ? AND member_id = ?
                """;

            List<PresetItemDTO> result = jdbcTemplate.query(
                    sql,
                    getRowMapper(),
                    presetId,
                    memberId
            );

            if (result.isEmpty()) {
                log.debug("Preset not found: presetId={}, memberId={}", presetId, memberId);
                return Optional.empty();
            }

            log.debug("Found preset: presetId={}", presetId);
            return Optional.of(result.get(0));

        } catch (Exception e) {
            log.error("Failed to find preset: presetId={}, memberId={}", presetId, memberId, e);
            throw new PresetException(
                    PresetException.ErrorCode.DATABASE_ERROR,
                    e
            );
        }
    }

    /**
     * Favorite 상태 변경
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

            int affected = jdbcTemplate.update(sql, favorite, presetId, memberId);

            if (affected > 0) {
                log.info("Updated favorite: presetId={}, favorite={}", presetId, favorite);
            } else {
                log.warn("No preset updated: presetId={}, memberId={}", presetId, memberId);
            }

            return affected;

        } catch (Exception e) {
            log.error("Failed to update favorite: presetId={}, memberId={}",
                    presetId, memberId, e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_UPDATE_FAILED,
                    e
            );
        }
    }

    /**
     * Preset 삭제
     */
    public int deleteOne(Long memberId, Integer presetId) {
        log.debug("Deleting preset: presetId={}, memberId={}", presetId, memberId);

        try {
            String sql = "DELETE FROM presets WHERE preset_id = ? AND member_id = ?";

            int affected = jdbcTemplate.update(sql, presetId, memberId);

            if (affected > 0) {
                log.info("Deleted preset: presetId={}, memberId={}", presetId, memberId);
            } else {
                log.warn("No preset deleted: presetId={}, memberId={}", presetId, memberId);
            }

            return affected;

        } catch (Exception e) {
            log.error("Failed to delete preset: presetId={}, memberId={}",
                    presetId, memberId, e);
            throw new PresetException(
                    PresetException.ErrorCode.PRESET_DELETE_FAILED,
                    e
            );
        }
    }

    /**
     * RowMapper 반환
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
     * Map을 PostgreSQL JSONB로 변환
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
     * PostgreSQL JSONB를 JsonNode로 파싱
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