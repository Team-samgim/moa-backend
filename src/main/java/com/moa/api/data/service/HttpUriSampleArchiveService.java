package com.moa.api.data.service;

import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/*****************************************************************************
 CLASS NAME    : HttpUriSampleArchiveService
 DESCRIPTION   : http_uri_sample 테이블의 샘플 데이터를
 AbstractSampleArchiveService 템플릿 로직을 통해
 S3로 아카이브하고 삭제하는 서비스
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP URI Sample Archive Service
 */
@Service
public class HttpUriSampleArchiveService extends AbstractSampleArchiveService {

    /**
     * 아카이브 대상 테이블명
     * - 상위 AbstractSampleArchiveService에서 이 값을 사용해
     *   SELECT / DELETE / S3 경로를 생성
     */
    private static final String TABLE_NAME = "http_uri_sample";

    /**
     * HTTP URI 샘플 아카이브 서비스 생성자
     *
     * @param jdbcTemplate DB 접근용 JdbcTemplate
     * @param s3Uploader   S3 업로드 유틸
     * @param s3Props      S3 설정 정보 (버킷, prefix 등)
     */
    public HttpUriSampleArchiveService(
            JdbcTemplate jdbcTemplate,
            S3Uploader s3Uploader,
            S3Props s3Props
    ) {
        // 공통 아카이브 로직을 제공하는 상위 추상 클래스에 의존성 주입
        super(jdbcTemplate, s3Uploader, s3Props);
    }

    /**
     * 상위 추상 클래스에서 사용할 테이블명 반환
     */
    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }
}
