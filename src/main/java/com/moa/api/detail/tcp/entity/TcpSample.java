package com.moa.api.detail.tcp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/*****************************************************************************
 CLASS NAME    : TcpSample
 DESCRIPTION   : tcp_sample 테이블의 row_key 컬럼을 매핑하는 최소 엔티티
 AUTHOR        : 방대혁
 ******************************************************************************/
/**
 * TCP Sample Entity
 * tcp_sample 테이블 매핑 (row_key 기본 키만 보유)
 */
@Entity
@Table(name = "tcp_sample")
public class TcpSample {

    @Id
    @Column(name = "row_key")
    private String rowKey;

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }
}
