package com.moa.api.detail.page.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/*****************************************************************************
 CLASS NAME    : HttpPageSample
 DESCRIPTION   : http_page_sample 테이블 매핑용 JPA 엔티티
 - HTTP Page 상세 조회 시 기본 식별/요약 정보 조회에 사용
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * HTTP Page Sample Entity
 * http_page_sample 테이블 매핑
 */
@Entity
@Table(name = "http_page_sample")
@Getter
@Setter
public class HttpPageSample {

    /**
     * Row Key (PK)
     * 형식: {timestamp}#{src_ip}#{dst_ip}#{src_port}#{dst_port}#{page_idx}
     */
    @Id
    @Column(name = "row_key")
    private String rowKey;

    // 기본 정보 (필수 필드만 매핑)

    /** 출발지 IP */
    @Column(name = "src_ip")
    private String srcIp;

    /** 목적지 IP */
    @Column(name = "dst_ip")
    private String dstIp;

    /** 출발지 Port */
    @Column(name = "src_port")
    private Integer srcPort;

    /** 목적지 Port */
    @Column(name = "dst_port")
    private Integer dstPort;

    /** HTTP 메서드 (GET/POST/...) */
    @Column(name = "http_method")
    private String httpMethod;

    /** HTTP URI */
    @Column(name = "http_uri")
    private String httpUri;

    /** HTTP 응답 코드 (200, 404 등) */
    @Column(name = "http_res_code")
    private String httpResCode;

    /** 페이지 기준 세션 수 */
    @Column(name = "page_session_cnt")
    private Long pageSessionCnt;
}
