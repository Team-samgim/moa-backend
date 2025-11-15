package com.moa.api.export.service;

import com.moa.api.export.dto.ExportCreateResponseDTO;
import com.moa.api.export.dto.ExportPivotRequestDTO;
import com.moa.api.export.entity.ExportFile;
import com.moa.api.export.repository.ExportFileRepository;
import com.moa.api.member.entity.Member;
import com.moa.api.pivot.dto.request.PivotQueryRequestDTO;
import com.moa.api.pivot.dto.response.PivotQueryResponseDTO;
import com.moa.api.pivot.model.PivotQueryContext;
import com.moa.api.pivot.model.TimeWindow;
import com.moa.api.pivot.repository.PivotRepository;
import com.moa.api.pivot.repository.SqlSupport;
import com.moa.api.preset.entity.Preset;
import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PivotExportService {

    private final PivotRepository pivotRepository;
    private final SqlSupport sqlSupport;
    private final ExportFileRepository exportFileRepository;
    private final S3Uploader s3Uploader;
    private final S3Props s3Props;
    private final EntityManager em;

    @Transactional
    public ExportCreateResponseDTO exportCsv(ExportPivotRequestDTO req) throws Exception {
        final String bucket = s3Props.getS3().getBucket();
        final String rootPrefix = normalizePrefix(s3Props.getS3().getPrefix()); // app/exports/dev/

        // 1) 인증 사용자
        Long memberId = resolveMemberId();
        Member memberRef = em.getReference(Member.class, memberId);

        // 2) 프리셋 (프런트에서 이미 savePivot으로 저장했다고 가정)
        if (req.getPresetId() == null) {
            throw new IllegalArgumentException("presetId is required for pivot export");
        }
        Preset presetRef = em.getReference(Preset.class, req.getPresetId());

        // 3) 피벗 쿼리 스펙
        PivotQueryRequestDTO pivotReq = req.getPivot();

        // 4) 전체 피벗 결과 생성 (모든 RowGroupItem까지 로드)
        PivotQueryResponseDTO fullPivot = buildFullPivotForExport(pivotReq);

        // 5) 파일명 + objectKey
        final String safeBase = Optional.ofNullable(req.getFileName())
                .map(String::trim).filter(s -> !s.isBlank())
                .map(this::sanitizeFileBase)
                .orElseGet(() -> "pivot_" + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        final String objectKey = buildObjectKey(rootPrefix, "pivot", memberId, safeBase + ".csv");

        // 6) CSV 생성 → 임시 파일 → 업로드
        Path tmp = Files.createTempFile("pivot-export-", ".csv");
        try {
            writeCsvToFile(tmp, fullPivot);
            s3Uploader.upload(bucket, objectKey, tmp, "text/csv; charset=utf-8");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignore) {}
        }

        // 7) 프리사인드 URL
        String httpUrl = s3Uploader.presign(bucket, objectKey, java.time.Duration.ofMinutes(10));

        // 8) export_files INSERT
        ExportFile row = ExportFile.builder()
                .member(memberRef)
                .preset(presetRef)
                .exportType("PIVOT")
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

    // =============================
    // 1) 전체 피벗 결과 만들기
    // =============================

    public TimeWindow resolveTimeWindow(PivotQueryRequestDTO.TimeDef time) {
        if (time == null || time.getFromEpoch() == null || time.getToEpoch() == null) {
            long nowSec = Instant.now().getEpochSecond();
            return new TimeWindow(nowSec - 3600, nowSec);
        }
        return new TimeWindow(time.getFromEpoch(), time.getToEpoch());
    }

    private PivotQueryResponseDTO buildFullPivotForExport(PivotQueryRequestDTO req) {
        TimeWindow tw = resolveTimeWindow(req.getTime());

        // ctx 생성 (PivotRepositoryImpl.createContext와 같은 내용 구현)
        PivotQueryContext ctx = new PivotQueryContext(
                com.moa.api.pivot.model.PivotLayer.from(req.getLayer()),
                resolveTimeField(req),
                tw,
                req.getFilters(),
                sqlSupport
        );

        // column 필드/값/metric 세팅
        String columnFieldName = (req.getColumn() != null) ? req.getColumn().getField() : null;
        List<String> columnValues = List.of();
        if (columnFieldName != null && !columnFieldName.isBlank()) {
            columnValues = pivotRepository.findTopColumnValues(ctx, columnFieldName);
        }

        // RowGroup summary
        var rowDefs = req.getRows() != null ? req.getRows() : List.<PivotQueryRequestDTO.RowDef>of();
        var valueDefs = req.getValues() != null ? req.getValues() : List.<PivotQueryRequestDTO.ValueDef>of();

        List<PivotQueryResponseDTO.RowGroup> groups =
                pivotRepository.buildRowGroups(ctx, rowDefs, valueDefs, columnFieldName, columnValues);

        // 각 RowGroup의 items 전체 로딩
        for (int i = 0; i < groups.size(); i++) {
            PivotQueryResponseDTO.RowGroup g = groups.get(i);
            String rowField = g.getRowLabel();

            int totalCount = Optional.ofNullable(g.getRowInfo())
                    .map(PivotQueryResponseDTO.RowInfo::getCount)
                    .orElse(0);

            if (totalCount <= 0) {
                g.setItems(List.of());
                continue;
            }

            // offset=0, limit=totalCount → 전체 아이템 로딩
            List<PivotQueryResponseDTO.RowGroupItem> items =
                    pivotRepository.buildRowGroupItems(
                            ctx,
                            rowField,
                            valueDefs,
                            columnFieldName,
                            columnValues,
                            0,
                            totalCount,
                            req.getSort()
                    );

            g.setItems(items);
        }

        // ColumnField 구성
        List<PivotQueryResponseDTO.Metric> metrics = new ArrayList<>();
        for (PivotQueryRequestDTO.ValueDef v : valueDefs) {
            metrics.add(PivotQueryResponseDTO.Metric.builder()
                    .alias(v.getAlias())
                    .field(v.getField())
                    .agg(v.getAgg())
                    .build());
        }

        PivotQueryResponseDTO.ColumnField colField = PivotQueryResponseDTO.ColumnField.builder()
                .name(columnFieldName)
                .values(columnValues)
                .metrics(metrics)
                .build();

        return PivotQueryResponseDTO.builder()
                .columnField(colField)
                .rowGroups(groups)
                .summary(null)
                .build();
    }

    private String resolveTimeField(PivotQueryRequestDTO req) {
        if (req.getTime() != null && req.getTime().getField() != null && !req.getTime().getField().isBlank()) {
            return req.getTime().getField();
        }
        // 레이어별 기본 시간 필드는 PivotLayer에서 가져오면 됨
        return com.moa.api.pivot.model.PivotLayer.from(req.getLayer()).getDefaultTimeField();
    }

    // =============================
    // 2) CSV 작성 (RowGroup + Item 평탄화)
    // =============================

    private void writeCsvToFile(Path path, PivotQueryResponseDTO pivot) throws Exception {
        PivotQueryResponseDTO.ColumnField columnField = pivot.getColumnField();
        List<PivotQueryResponseDTO.RowGroup> groups = pivot.getRowGroups();

        if (columnField == null || groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("no pivot data to export");
        }

        List<String> columnValues = columnField.getValues() != null
                ? columnField.getValues()
                : List.of();
        List<PivotQueryResponseDTO.Metric> metrics = columnField.getMetrics() != null
                ? columnField.getMetrics()
                : List.of();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(path), StandardCharsets.UTF_8));
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // UTF-8 BOM (엑셀 한글 깨짐 방지)
            writer.write('\uFEFF');

            // 1) 헤더 2줄
            List<String> header1 = new ArrayList<>();
            header1.add("#");
            header1.add("rowFieldName");
            header1.add("rowFieldValue");

            for (String colVal : columnValues) {
                String label = (colVal == null || colVal.isBlank()) ? "(empty)" : colVal;
                for (PivotQueryResponseDTO.Metric m : metrics) {
                    header1.add(label);
                }
            }

            List<String> header2 = new ArrayList<>();
            header2.add("");
            header2.add("");
            header2.add("");

            for (int i = 0; i < columnValues.size(); i++) {
                for (PivotQueryResponseDTO.Metric m : metrics) {
                    header2.add(m.getAlias());
                }
            }

            printer.printRecord(header1);
            printer.printRecord(header2);

            // 2) 데이터 행 – RowGroup(필드명) + RowGroupItem(값) 조합을 평탄화
            int rowNo = 1;
            for (PivotQueryResponseDTO.RowGroup g : groups) {
                String rowFieldName = g.getRowLabel();
                List<PivotQueryResponseDTO.RowGroupItem> items = g.getItems();
                if (items == null || items.isEmpty()) continue;

                for (PivotQueryResponseDTO.RowGroupItem item : items) {
                    List<String> line = new ArrayList<>();
                    line.add(String.valueOf(rowNo++));
                    line.add(rowFieldName);
                    line.add(item.getValueLabel());

                    Map<String, Map<String, Object>> cells =
                            item.getCells() != null ? item.getCells() : Map.of();

                    for (String colVal : columnValues) {
                        Map<String, Object> metricMap =
                                cells.getOrDefault(colVal, Map.of());
                        for (PivotQueryResponseDTO.Metric m : metrics) {
                            Object v = metricMap.get(m.getAlias());
                            line.add(v != null ? v.toString() : "");
                        }
                    }

                    printer.printRecord(line);
                }
            }
        }
    }

    // =============================
    // 3) 공통 유틸들 (GridExportService에서 가져온 패턴)
    // =============================

    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("인증 정보가 없습니다.");
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l) return l;
        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }

    private String normalizePrefix(String p) {
        if (p == null || p.isBlank()) return "";
        String x = p.trim();
        return x.endsWith("/") ? x : x + "/";
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
        return cleaned.isBlank() ? "pivot" : cleaned;
    }

    private String buildObjectKey(String rootPrefix, String typePrefix, Long memberId, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();

        String key = String.format(
                "%s%s/%s/%d/%s/%s",
                rootPrefix,
                typePrefix,  // grid / pivot / chart
                datePath,
                memberId,
                uuid,
                fileName
        );
        return key.replace("//", "/");
    }
}
