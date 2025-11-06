package com.moa.api.preset.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.api.preset.entity.Preset;
import com.moa.api.preset.entity.PresetOrigin;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PresetRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    public Integer insert(Long memberId,
                          String name,
                          String type,
                          Map<String,Object> config,
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
}
