package com.moa.api.grid.config;

/*****************************************************************************
 CLASS NAME    : AsyncConfig
 DESCRIPTION   : Grid 비동기 처리용 ThreadPool 설정 및 예외 핸들링 구성
 AUTHOR        : 방대혁
 ******************************************************************************/

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * ThreadPool 튜닝:
 * - Core: 10개 (기본 유지 스레드)
 * - Max: 50개 (최대 동시 작업)
 * - Queue: 100개 (대기 가능한 작업)
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final GridProperties properties;

    @Bean(name = "gridAsyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        GridProperties.Async asyncConfig = properties.getAsync();

        executor.setCorePoolSize(asyncConfig.getCorePoolSize());
        executor.setMaxPoolSize(asyncConfig.getMaxPoolSize());
        executor.setQueueCapacity(asyncConfig.getQueueCapacity());
        executor.setThreadNamePrefix(asyncConfig.getThreadNamePrefix());

        // 종료 대기 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // 거부 정책: 큐가 가득 차면 호출 스레드에서 실행 (Graceful Degradation)
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        log.info("Async executor initialized - core: {}, max: {}, queue: {}",
                asyncConfig.getCorePoolSize(),
                asyncConfig.getMaxPoolSize(),
                asyncConfig.getQueueCapacity()
        );

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async exception in method [{}] with params {}",
                    method.getName(),
                    params,
                    throwable
            );
        };
    }
}
