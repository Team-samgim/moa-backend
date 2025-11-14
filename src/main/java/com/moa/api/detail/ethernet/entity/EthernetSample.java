package com.moa.api.detail.ethernet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ethernet_sample")
public class EthernetSample {

    @Id
    @Column(name = "row_key")
    private String rowKey;

    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }
}