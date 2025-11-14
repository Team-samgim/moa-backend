package com.moa.api.preset.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.preset.dto.PresetItemDTO;
import com.moa.api.preset.entity.Preset;
import com.moa.api.preset.entity.PresetOrigin;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PresetRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    public Integer insert(Long memberId,
                          String name,
                          String type,
                          Map<String, Object> config,
                          boolean favorite,
                          PresetOrigin origin) throws Exception {
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(om.writeValueAsString(config));

        String sql = """
                  INSERT INTO public.presets
                    (member_id, preset_name, preset_type, config, favorite, preset_origin)
                  VALUES (?, ?, ?, ?, ?, ?)
                  RETURNING preset_id
                """;

        return jdbc.queryForObject(
                sql,
                Integer.class,
                memberId, name, type, json, favorite, origin.name()  // ← USER/EXPORT
        );
    }

    private final RowMapper<PresetItemDTO> rowMapper = (rs, rn) -> {
        PGobject cfg = (PGobject) rs.getObject("config");
        JsonNode cfgNode;
        try {
            cfgNode = (cfg != null && cfg.getValue() != null) ? om.readTree(cfg.getValue()) : om.nullNode();
        } catch (Exception e) {
            cfgNode = om.nullNode();
        }
        Timestamp u = rs.getTimestamp("updated_at");
        return new PresetItemDTO(
                rs.getInt("preset_id"),
                rs.getString("preset_name"),
                rs.getString("preset_type"),
                cfgNode,
                rs.getBoolean("favorite"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                (u != null) ? u.toLocalDateTime() : null
        );
    };

    public List<PresetItemDTO> findByMember(Long memberId, String type, int page, int size) {
        String base = """
                    SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                    FROM presets
                    WHERE member_id = ?
                """;
        String typeClause = (type == null || type.isBlank()) ? "" : " AND preset_type = ? ";
        String order = " ORDER BY updated_at DESC NULLS LAST, created_at DESC ";
        String limit = " LIMIT ? OFFSET ? ";

        return (typeClause.isEmpty())
                ? jdbc.query(base + order + limit, rowMapper, memberId, size, page * size)
                : jdbc.query(base + typeClause + order + limit, rowMapper, memberId, type, size, page * size);
    }

    public long countByMember(Long memberId, String type) {
        String sql = "SELECT count(*) FROM presets WHERE member_id = ? ";
        if (type != null && !type.isBlank()) sql += " AND preset_type = ? ";
        return (type != null && !type.isBlank())
                ? jdbc.queryForObject(sql, Long.class, memberId, type)
                : jdbc.queryForObject(sql, Long.class, memberId);
    }

    public Optional<PresetItemDTO> findOneForOwner(Long memberId, Integer presetId) {
        String sql = """
                    SELECT preset_id, preset_name, preset_type, config, favorite, created_at, updated_at
                    FROM presets
                    WHERE preset_id = ? AND member_id = ?
                """;
        var list = jdbc.query(sql, rowMapper, presetId, memberId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int updateFavorite(Long memberId, Integer presetId, boolean favorite) {
        String sql = "UPDATE presets SET favorite = ?, updated_at = now() WHERE preset_id = ? AND member_id = ?";
        return jdbc.update(sql, favorite, presetId, memberId);
    }

    public int deleteOne(Long memberId, Integer presetId) {
        String sql = "DELETE FROM presets WHERE preset_id = ? AND member_id = ?";
        return jdbc.update(sql, presetId, memberId);
    }
}
