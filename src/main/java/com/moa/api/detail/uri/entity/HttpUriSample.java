package com.moa.api.detail.uri.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/*****************************************************************************
 * CLASS NAME    : HttpUriSample
 * DESCRIPTION   : http_uri_sample 테이블 매핑 엔티티
 * AUTHOR        : 방대혁
 *****************************************************************************/
/**
 * HTTP URI 샘플 엔티티
 */
@Entity
@Table(name = "http_uri_sample")
public class HttpUriSample {

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
