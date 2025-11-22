package com.moa.api.data.controller;

import com.moa.api.data.entity.HttpPageSampleFull;
import com.moa.api.data.repository.HttpPageSampleInsertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/page-samples")
@RequiredArgsConstructor
public class InternalPageSampleController {

    private final HttpPageSampleInsertRepository repository;

    @PostMapping("/batch")
    public ResponseEntity<Void> receiveBatch(@RequestBody List<HttpPageSampleFull> samples) {
        log.info("🔵 내부 API: 배치 {}개 수신", samples.size());

        try {
            for (HttpPageSampleFull sample : samples) {
                LocalDateTime now = LocalDateTime.now();

                double unixTimestamp = now.atZone(ZoneId.systemDefault())
                        .toEpochSecond()
                        + (now.getNano() / 1_000_000_000.0);

                String rowKey = String.format("%.9f", unixTimestamp);

                sample.setRowKey(rowKey);
                sample.setTsServer(now);
                sample.setTsServerNsec(unixTimestamp);

                log.debug("  → row_key: {}", rowKey);  // ← 각 데이터마다
            }

            repository.saveAll(samples);
            log.info("✅ DB 저장 완료: {}개", samples.size());
            log.info("   첫 번째: {}, 마지막: {}",
                    samples.get(0).getRowKey(),
                    samples.get(samples.size() - 1).getRowKey());  // ← 요약

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ 배치 저장 실패", e);
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody HttpPageSampleFull sample) {
        log.info("🔵 내부 API: 단건 데이터 수신");

        try {
            repository.save(sample);
            log.info("✅ DB 저장 완료");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ 데이터 저장 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}