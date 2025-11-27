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
 *
 * AUTHOR        : 방대혁
 *
 * 역할:
 * - Grid 집계(Aggregate)를 비동기/캐싱으로 처리
 * - 동일한 요청에 대해:
 *   1) 이미 완료된 결과가 있으면 캐시에서 즉시 반환
 *   2) 실행 중인 작업이 있으면 해당 Future 재사용
 *   3) 없으면 새로 비동기 작업을 생성해서 실행
 *
 * 특징:
 * - @Async + ThreadPoolTaskExecutor(gridAsyncExecutor) 사용
 * - 요청 내용을 기반으로 SHA-256 해시 → cacheKey 생성
 * - 결과는 10분 동안 메모리 캐시에 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GridAsyncService {

    private final GridRepositoryCustom repository;

    /**
     * 실행 중인 집계 작업 캐시
     * - key: cacheKey (요청 내용 기반 해시)
     * - value: 해당 요청을 처리 중인 CompletableFuture
     * - 동일 요청이 여러 번 들어와도 DB 쿼리는 한 번만 수행되도록 공유
     */
    private final Map<String, CompletableFuture<AggregateResponseDTO>> runningTasks =
            new ConcurrentHashMap<>();

    /**
     * 완료된 집계 결과 캐시
     * - key: cacheKey
     * - value: 캐싱된 결과 + 생성 시각 + TTL 정보
     * - TTL(10분)이 지나면 isExpired()로 만료 여부 판단
     */
    private final Map<String, CachedResult> resultCache =
            new ConcurrentHashMap<>();

    /**
     * 비동기 집계 실행
     *
     * 흐름:
     * 1) cacheKey 생성 (레이어 + 필터 + baseSpec + metrics 기반 SHA-256)
     * 2) resultCache에서 유효한 캐시가 있으면 즉시 CompletableFuture.completedFuture 로 반환
     * 3) runningTasks에 동일 key 작업이 있으면 그 Future 재사용
     * 4) 둘 다 없으면 supplyAsync 로 새 작업 생성, runningTasks에 등록 후 실행
     * 5) 성공 시 resultCache에 결과 넣고, 10분 후 cleanup 예약
     * 6) finally 블록에서 runningTasks 에서 key 제거
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
                        // 실제 집계 로직은 Repository 에 위임
                        AggregateResponseDTO result = repository.aggregate(req);
                        long elapsed = System.currentTimeMillis() - start;

                        log.info("Aggregate completed in {}ms: {}", elapsed, cacheKey);

                        // 결과 캐싱
                        resultCache.put(cacheKey, new CachedResult(result));

                        // 10분 후 캐시 자동 제거 예약
                        scheduleCleanup(cacheKey, 10);

                        return result;

                    } catch (Exception e) {
                        log.error("Aggregate failed: {}", cacheKey, e);
                        throw e;

                    } finally {
                        // 실행 중 작업 목록에서 제거 (성공/실패 상관없이)
                        runningTasks.remove(cacheKey);
                    }
                });

        // 실행 중 작업 등록 (동시에 들어온 요청이 같은 Future 를 보게 됨)
        runningTasks.put(cacheKey, future);

        return future;
    }

    /**
     * 동기 집계 (기존/호환용)
     *
     * - 캐시/비동기 없이 바로 Repository.aggregate 호출
     * - 기존 동기 API나 테스트 코드에서 사용 가능
     */
    public AggregateResponseDTO aggregateSync(AggregateRequestDTO req) {
        return repository.aggregate(req);
    }

    /**
     * 캐시 키 생성 (요청 내용 기반 해시)
     *
     * - layer, filterModel, baseSpecJson, metrics 를 문자열로 합쳐서 SHA-256 해시 계산
     * - URL-safe Base64 인코딩(패딩 없음)으로 Key 생성
     * - 해시 실패 시 fallback 으로 layer + filterModel + 현재 시각(분 단위) 조합 사용
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
            // fallback: 단순 문자열 조합 (충돌 가능성은 있지만, 해시 실패는 드물기 때문에 예외 상황용)
            return String.format("%s-%s-%d",
                    req.getLayer(),
                    req.getFilterModel(),
                    System.currentTimeMillis() / 60000 // 1분 단위 버킷
            );
        }
    }

    /**
     * 캐시 자동 정리 예약
     *
     * - CompletableFuture.delayedExecutor 를 이용해 delayMinutes 분 후 실행
     * - 실행 시 resultCache 에서 해당 key 제거
     * - 별도 스케줄러 없이도 TTL 적용 효과
     */
    private void scheduleCleanup(String key, int delayMinutes) {
        CompletableFuture
                .delayedExecutor(delayMinutes, TimeUnit.MINUTES)
                .execute(() -> {
                    resultCache.remove(key);
                    log.debug("Cache cleaned up: {}", key);
                });
    }

    /**
     * 캐시 상태 조회 (모니터링용)
     *
     * @return
     *  - cachedResults: resultCache.size()
     *  - runningTasks : runningTasks.size()
     *  - hitRate      : 간단한 비율 (resultCache / (cache + running))
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                resultCache.size(),
                runningTasks.size(),
                calculateHitRate()
        );
    }

    /**
     * 단순 캐시 히트율 계산
     * - 현재 캐시에 쌓인 결과 수 / (캐시 결과 + 실행 중 작업 수)
     * - 엄밀한 의미의 히트율은 아니지만, 대략적인 캐시 활용 정도 파악용
     */
    private double calculateHitRate() {
        int total = resultCache.size() + runningTasks.size();
        return total == 0 ? 0.0 : (double) resultCache.size() / total;
    }

    /**
     * 캐싱된 결과 래퍼
     *
     * - result      : 실제 AggregateResponseDTO
     * - createdAt   : 캐시 생성 시각 (millis)
     * - ttlMillis   : TTL(기본 10분)
     */
    private static class CachedResult {
        final AggregateResponseDTO result;
        final long createdAt;
        final long ttlMillis = TimeUnit.MINUTES.toMillis(10);

        CachedResult(AggregateResponseDTO result) {
            this.result = result;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * TTL 기준 만료 여부 판단
         */
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }
    }

    /**
     * 캐시 통계 DTO
     *
     * - cachedResults: 현재 resultCache 크기
     * - runningTasks : 현재 실행 중인 비동기 작업 수
     * - hitRate      : 간단한 캐시 히트율
     */
    public record CacheStats(
            int cachedResults,
            int runningTasks,
            double hitRate
    ) {}
}
