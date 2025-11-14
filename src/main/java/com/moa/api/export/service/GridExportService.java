package com.moa.api.export.service;

import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.dto.ExportGridRequestDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.grid.dto.SqlDTO;
import com.moa.api.grid.repository.GridRepositoryImpl;
import com.moa.api.grid.util.QueryBuilder;
import com.moa.api.member.entity.Member;
import com.moa.api.preset.entity.Preset;
import com.moa.api.preset.entity.PresetOrigin;
import com.moa.api.preset.entity.PresetType;
import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridExportService {

    private final JdbcTemplate jdbcTemplate;
    private final QueryBuilder queryBuilder;
    private final GridRepositoryImpl gridRepository;
    private final ExportFileRepository exportFileRepository;
    private final S3Uploader s3Uploader;
    private final S3Props s3Props;
    private final EntityManager em;

    @Transactional
    public ExportCreateResponseDTO exportCsv(ExportGridRequestDTO req) throws Exception {
        final String bucket = s3Props.getS3().getBucket();
        final String prefix = normalizePrefix(s3Props.getS3().getPrefix());

        // 1) 인증 컨텍스트에서 memberId 보장
        Long memberId = resolveMemberId();                  // req 값 무시
        Member memberRef = em.getReference(Member.class, memberId);

        // 2) 프리셋 확보 (없으면 생성)
        Preset presetRef = (req.getPresetId() == null)
                ? createPresetFromRequest(req, memberRef)              // 새 프리셋 생성(EXPORT)
                : em.getReference(Preset.class, req.getPresetId());

        // 3) 파일명 & objectKey
        final String safeBase = Optional.ofNullable(req.getFileName())
                .map(String::trim).filter(s -> !s.isBlank())
                .map(this::sanitizeFileBase)
                .orElseGet(() -> "grid_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        final String objectKey = buildObjectKey(prefix, safeBase + ".csv");

        // 4) CSV 생성 → 업로드
        Path tmp = Files.createTempFile("grid-export-", ".csv");
        try {
            writeCsvToFile(tmp, req);
            s3Uploader.upload(bucket, objectKey, tmp, "text/csv; charset=utf-8");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignore) {
            }
        }

        // 5) 프리사인드 URL
        String httpUrl = s3Uploader.presign(bucket, objectKey, java.time.Duration.ofMinutes(10));

        // 6) export_files INSERT (연관관계 사용)
        ExportFile row = ExportFile.builder()
                .member(memberRef)           // ★ @ManyToOne(Member)
                .preset(presetRef)           // ★ @ManyToOne(Preset)
                .exportType("GRID")
                .fileFormat("CSV")
                .fileName(safeBase + ".csv")
                .bucket(bucket)
                .objectKey(objectKey)
                .exportStatus("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();
        exportFileRepository.save(row);

        return ExportCreateResponseDTO.builder()
                .exportId(row.getExportId())
                .bucket(bucket)
                .objectKey(objectKey)
                .s3Url("s3://" + bucket + "/" + objectKey)
                .httpUrl(httpUrl)
                .status(row.getExportStatus())
                .build();
    }

    private String normalizePrefix(String p) {
        if (p == null || p.isBlank()) return "";
        String x = p.trim();
        return x.endsWith("/") ? x : x + "/";
    }

    // ★ Member 연관관계로 프리셋 생성
    private Preset createPresetFromRequest(ExportGridRequestDTO req, Member member) {
        String name = Optional.ofNullable(req.getFileName())
                .map(fn -> fn.replace(".csv", ""))
                .orElse("Grid Export " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Map<String, Object> sort = new LinkedHashMap<>();
        if (req.getSortField() != null && !req.getSortField().isBlank()) sort.put("field", req.getSortField());
        if (req.getSortDirection() != null && !req.getSortDirection().isBlank())
            sort.put("direction", req.getSortDirection());

        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("layer", req.getLayer());
        cfg.put("columns", req.getColumns());
        cfg.put("filters", safeJsonNode(req.getFilterModelJson())); // JSON 문자열이면 파싱, 아니면 빈 Map
        cfg.put("baseSpec", safeJsonNode(req.getBaseSpecJson()));
        cfg.put("sort", sort);
        cfg.put("version", 1);

        var om = new ObjectMapper();
        var cfgNode = om.valueToTree(cfg); // JsonNode

        Preset preset = Preset.builder()
                .member(member)              // ★ 연관관계 세팅
                .presetName(name)
                .presetType(PresetType.SEARCH)
                .config(cfgNode)
                .favorite(false)
                .origin(PresetOrigin.EXPORT)
                .createdAt(LocalDateTime.now())
                .build();

        em.persist(preset);   // 영속화
        em.flush();           // ID 즉시 채움(ExportFile FK에 사용)
        return preset;
    }

    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("인증 정보가 없습니다.");
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) return l; // JwtAuthenticationFilter가 m.getId()를 principal로 넣음
        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }

    private Object safeJsonNode(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return new ObjectMapper().readTree(json); // JsonNode
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String sanitizeFileBase(String s) {
        String cleaned = s;
        cleaned = cleaned.replace('\\', ' ');
        cleaned = cleaned.replace('/', ' ');
        cleaned = cleaned.replace('\r', ' ');
        cleaned = cleaned.replace('\n', ' ');
        cleaned = cleaned.replace('\t', ' ');
        cleaned = cleaned.replace('\u0000', ' ');

        cleaned = cleaned.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > 120) cleaned = cleaned.substring(0, 120);
        return cleaned.isBlank() ? "grid" : cleaned;
    }

    private String buildObjectKey(String prefix, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return (prefix + datePath + "/" + UUID.randomUUID() + "/" + fileName).replace("//", "/");
    }

    private void writeCsvToFile(Path path, ExportGridRequestDTO req) throws Exception {
        List<String> cols = Optional.ofNullable(req.getColumns()).orElseThrow(() -> new IllegalArgumentException("columns is required"));
        final String layer = (req.getLayer() == null || req.getLayer().isBlank()) ? "ethernet" : req.getLayer();

        Map<String, String> typeMap = gridRepository.getFrontendTypeMap(layer);
        Map<String, String> temporalMap = gridRepository.getTemporalKindMap(layer);

        // 실존 컬럼만 선택
        List<String> safeCols = cols.stream().filter(c -> typeMap.containsKey(strip(c))).toList();
        if (safeCols.isEmpty()) throw new IllegalArgumentException("no valid columns to export");

        SqlDTO s = queryBuilder.buildSelectSQLForExport(
                layer,
                safeCols,
                req.getSortField(),
                req.getSortDirection(),
                req.getFilterModelJson(),
                req.getBaseSpecJson(),
                typeMap,
                temporalMap
        );

        int fetch = Optional.ofNullable(req.getFetchSize()).orElse(5000);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(path), StandardCharsets.UTF_8));
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(safeCols.toArray(new String[0])))) {

            RowCallbackHandler rch = rs -> {
                List<Object> out = new ArrayList<>(safeCols.size());
                for (String c : safeCols) {
                    Object v = rs.getObject(strip(c)); // 컬럼명 strip 주의
                    if (v instanceof org.postgresql.util.PGobject pg) {
                        out.add(Objects.toString(pg.getValue(), ""));
                    } else {
                        out.add(Objects.toString(v, ""));
                    }
                }
                try {
                    printer.printRecord(out); // <-- IOException 발생 지점
                } catch (IOException e) {
                    // RowCallbackHandler는 IOException을 던질 수 없으므로 래핑
                    throw new UncheckedIOException(e);
                }
            };

            jdbcTemplate.query(con -> {
                con.setAutoCommit(false);
                var ps = con.prepareStatement(s.getSql(),
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_READ_ONLY);
                ps.setFetchSize(fetch);
                int idx = 1;
                for (Object a : s.getArgs()) ps.setObject(idx++, a);
                return ps;
            }, rch);
        }
    }

    private String strip(String c) {
        return c.contains("-") ? c.substring(0, c.indexOf('-')) : c;
    }
}
