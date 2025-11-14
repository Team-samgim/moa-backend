package com.moa.api.detail.uri.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "http_uri_sample")
public class HttpUriSample {

    @Id
    @Column(name = "row_key")
    private String rowKey;

    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }
}