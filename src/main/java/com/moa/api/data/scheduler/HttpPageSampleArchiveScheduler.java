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

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class HttpPageSampleArchiveScheduler {

    private final HttpPageSampleArchiveService httpPageSampleArchiveService;
    private final TcpSampleArchiveService tcpSampleArchiveService;
    private final HttpUriSampleArchiveService httpUriSampleArchiveService;
    private final EthernetSampleArchiveService ethernetSampleArchiveService;

    // 매일 9시 9분, 서울 기준
    @Scheduled(cron = "0 9 9 * * *", zone = "Asia/Seoul")
    public void archiveJob() {
        log.info("[archive] 아카이브 스케줄러 시작");

        runSafe("http_page_sample", httpPageSampleArchiveService::archiveOlderThan7Days);
        runSafe("tcp_sample", tcpSampleArchiveService::archiveOlderThan7Days);
        runSafe("http_uri_sample", httpUriSampleArchiveService::archiveOlderThan7Days);
        runSafe("ethernet_sample", ethernetSampleArchiveService::archiveOlderThan7Days);

        log.info("[archive] 아카이브 스케줄러 종료");
    }

    private void runSafe(String name, Runnable task) {
        try {
            log.info("[{}] 아카이브 시작", name);
            task.run();
            log.info("[{}] 아카이브 종료", name);
        } catch (Exception e) {
            log.error("[{}] 아카이브 중 오류 발생", name, e);
        }
    }
}