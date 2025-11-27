package com.moa.api.grid.config;

/*****************************************************************************
 CLASS NAME    : QueryDslConfig
 DESCRIPTION   : QueryDSL SQLQueryFactory 설정 (PostgreSQL 기반)
 AUTHOR        : 방대혁
 ******************************************************************************/

import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * QueryDSL 설정 클래스
 *
 * - PostgreSQLTemplates 기반 QueryDSL Configuration 생성
 * - SQLQueryFactory Bean 등록
 */
@Configuration
public class QueryDslConfig {

    @Bean
    public SQLQueryFactory sqlQueryFactory(DataSource dataSource) {
        com.querydsl.sql.Configuration querydslConfig =
                new com.querydsl.sql.Configuration(new PostgreSQLTemplates());

        return new SQLQueryFactory(querydslConfig, dataSource);
    }
}
