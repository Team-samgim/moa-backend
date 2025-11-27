package com.moa.api.data.scheduler;

import com.moa.api.data.service.EthernetSampleArchiveService;
import com.moa.api.data.service.HttpPageSampleArchiveService;
import com.moa.api.data.service.HttpUriSampleArchiveService;
import com.moa.api.data.service.TcpSampleArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*****************************************************************************
 CLASS NAME    : SampleArchiveScheduler
 DESCRIPTION   : 상세 트래픽 샘플(http_page / http_uri / tcp / ethernet)을
 주기적으로 S3에 아카이브하고 원본 DB에서 삭제하는 스케줄러
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * Sample Archive Scheduler
 *
 * 매일 오전 9시 9분에 7일 이전 데이터를 S3에 아카이브하고 삭제
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SampleArchiveScheduler {

    private final HttpPageSampleArchiveService httpPageSampleArchiveService;
    private final TcpSampleArchiveService tcpSampleArchiveService;
    private final HttpUriSampleArchiveService httpUriSampleArchiveService;
    private final EthernetSampleArchiveService ethernetSampleArchiveService;

    /**
     * 아카이브 스케줄러
     *
     * 매일 오전 9시 9분 실행 (Asia/Seoul 타임존)
     * - http_page_sample
     * - tcp_sample
     * - http_uri_sample
     * - ethernet_sample
     * 각 테이블별 7일 이전 데이터를 S3로 이동 후 원본 삭제
     */
    @Scheduled(cron = "0 9 9 * * *", zone = "Asia/Seoul")
    public void archiveJob() {
        log.info("========================================");
        log.info("Archive Scheduler Started");
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        try {
            // 1) HTTP Page Sample
            runSafe("http_page_sample", httpPageSampleArchiveService::archiveOlderThan7Days);

            // 2) TCP Sample
            runSafe("tcp_sample", tcpSampleArchiveService::archiveOlderThan7Days);

            // 3) HTTP URI Sample
            runSafe("http_uri_sample", httpUriSampleArchiveService::archiveOlderThan7Days);

            // 4) Ethernet Sample
            runSafe("ethernet_sample", ethernetSampleArchiveService::archiveOlderThan7Days);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("========================================");
            log.info("Archive Scheduler Completed Successfully");
            log.info("Total Elapsed Time: {} ms", elapsedTime);
            log.info("========================================");

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.error("========================================");
            log.error("Archive Scheduler Failed");
            log.error("Total Elapsed Time: {} ms", elapsedTime);
            log.error("========================================", e);
        }
    }

    /**
     * 안전하게 아카이브 작업 실행
     *
     * 개별 테이블 아카이브 실패 시에도 다른 테이블은 계속 실행
     */
    private void runSafe(String name, Runnable task) {
        log.info("----------------------------------------");
        log.info("[{}] Archive Starting", name);

        long startTime = System.currentTimeMillis();

        try {
            task.run();

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("[{}] Archive Completed Successfully ({}ms)", name, elapsedTime);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.error("[{}] Archive Failed ({}ms)", name, elapsedTime, e);
        }
    }
}
