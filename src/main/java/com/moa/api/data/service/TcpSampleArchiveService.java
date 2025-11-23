package com.moa.api.data.service;

import com.moa.global.aws.S3Props;
import com.moa.global.aws.S3Uploader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * TCP Sample Archive Service
 */
@Service
public class TcpSampleArchiveService extends AbstractSampleArchiveService {

    private static final String TABLE_NAME = "tcp_sample";

    public TcpSampleArchiveService(
            JdbcTemplate jdbcTemplate,
            S3Uploader s3Uploader,
            S3Props s3Props
    ) {
        super(jdbcTemplate, s3Uploader, s3Props);
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }
}