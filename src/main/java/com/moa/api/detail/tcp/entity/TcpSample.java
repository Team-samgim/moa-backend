package com.moa.api.detail.tcp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tcp_sample")
public class TcpSample {

    @Id
    @Column(name = "row_key")
    private String rowKey;

    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }
}