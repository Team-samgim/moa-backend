package com.moa.api.detail.page.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * HTTP Page Sample Entity
 * http_page_sample 테이블 매핑
 *
 * 주의: 이 엔티티는 조회 전용입니다.
 * 실제 컬럼은 235개이지만, Repository의 @Query로 직접 조회하므로
 * Entity에는 최소한의 필드만 정의합니다.
 */
@Entity
@Table(name = "http_page_sample")
@Getter
@Setter
public class HttpPageSample {

    @Id
    @Column(name = "row_key")
    private String rowKey;

    // 기본 정보 (필수 필드만)
    @Column(name = "src_ip")
    private String srcIp;

    @Column(name = "dst_ip")
    private String dstIp;

    @Column(name = "src_port")
    private Integer srcPort;

    @Column(name = "dst_port")
    private Integer dstPort;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "http_uri")
    private String httpUri;

    @Column(name = "http_res_code")
    private String httpResCode;

    @Column(name = "page_session_cnt")
    private Long pageSessionCnt;

    // 나머지 232개 컬럼은 Repository의 @Query에서 직접 조회
    // HttpPageRowSlice 인터페이스로 매핑됨
}