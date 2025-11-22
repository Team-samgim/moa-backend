package com.moa.api.grid.service;

import com.moa.api.grid.dto.AggregateRequestDTO;
import com.moa.api.grid.dto.AggregateResponseDTO;
import com.moa.api.grid.repository.GridRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 비동기 집계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GridAsyncService {

    private final GridRepositoryCustom repository;

    // 실행 중인 작업 캐시
    private final Map<String, CompletableFuture<AggregateResponseDTO>> runningTasks =
            new ConcurrentHashMap<>();

    // 완료된 결과 캐시 (10분간 유지)
    private final Map<String, CachedResult> resultCache =
            new ConcurrentHashMap<>();

    /**
     * 비동기 집계 실행
     */
    @Async("gridAsyncExecutor")
    public CompletableFuture<AggregateResponseDTO> aggregateAsync(
            AggregateRequestDTO req) {

        String cacheKey = generateCacheKey(req);

        // 1. 캐시된 결과가 있으면 즉시 반환
        CachedResult cached = resultCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("✅ Cache HIT for key: {}", cacheKey);
            return CompletableFuture.completedFuture(cached.result);
        }

        // 2. 이미 실행 중인 작업이 있으면 재사용
        CompletableFuture<AggregateResponseDTO> existing = runningTasks.get(cacheKey);
        if (existing != null) {
            log.debug("Task already running for key: {}", cacheKey);
            return existing;
        }

        // 3. 새로운 작업 시작
        CompletableFuture<AggregateResponseDTO> future = CompletableFuture
                .supplyAsync(() -> {
                    long start = System.currentTimeMillis();
                    log.info("Starting aggregate task: {}", cacheKey);

                    try {
                        AggregateResponseDTO result = repository.aggregate(req);
                        long elapsed = System.currentTimeMillis() - start;

                        log.info("Aggregate completed in {}ms: {}", elapsed, cacheKey);

                        // 결과 캐싱
                        resultCache.put(cacheKey, new CachedResult(result));

                        // 10분 후 캐시 자동 제거
                        scheduleCleanup(cacheKey, 10);

                        return result;

                    } catch (Exception e) {
                        log.error("Aggregate failed: {}", cacheKey, e);
                        throw e;

                    } finally {
                        // 실행 중 작업 목록에서 제거
                        runningTasks.remove(cacheKey);
                    }
                });

        // 실행 중 작업 등록
        runningTasks.put(cacheKey, future);

        return future;
    }

    /**
     * 동기 집계 (호환성 유지)
     */
    public AggregateResponseDTO aggregateSync(AggregateRequestDTO req) {
        return repository.aggregate(req);
    }

    /**
     * 캐시 키 생성 (요청 내용 기반 해시)
     */
    private String generateCacheKey(AggregateRequestDTO req) {
        try {
            String content = String.format("%s:%s:%s:%s",
                    req.getLayer(),
                    req.getFilterModel(),
                    req.getBaseSpecJson(),
                    req.getMetrics()
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (Exception e) {
            // fallback: 단순 문자열 조합
            return String.format("%s-%s-%d",
                    req.getLayer(),
                    req.getFilterModel(),
                    System.currentTimeMillis() / 60000 // 1분 단위
            );
        }
    }

    /**
     * 캐시 자동 정리 예약
     */
    private void scheduleCleanup(String key, int delayMinutes) {
        CompletableFuture.delayedExecutor(delayMinutes, TimeUnit.MINUTES)
                .execute(() -> {
                    resultCache.remove(key);
                    log.debug("Cache cleaned up: {}", key);
                });
    }

    /**
     * 캐시 통계 조회 (모니터링용)
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                resultCache.size(),
                runningTasks.size(),
                calculateHitRate()
        );
    }

    private double calculateHitRate() {
        int total = resultCache.size() + runningTasks.size();
        return total == 0 ? 0.0 : (double) resultCache.size() / total;
    }

    /**
     * 캐싱된 결과
     */
    private static class CachedResult {
        final AggregateResponseDTO result;
        final long createdAt;
        final long ttlMillis = TimeUnit.MINUTES.toMillis(10);

        CachedResult(AggregateResponseDTO result) {
            this.result = result;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }
    }

    /**
     * 캐시 통계
     */
    public record CacheStats(
            int cachedResults,
            int runningTasks,
            double hitRate
    ) {}
}