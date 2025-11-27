package com.moa.api.detail.ethernet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/*****************************************************************************
 CLASS NAME    : EthernetSample
 DESCRIPTION   : ethernet_sample 테이블 매핑용 JPA 엔티티
 - 상세 조회 시 rowKey 존재 여부 확인 등에 사용
 AUTHOR        : 방대혁
 ******************************************************************************/
@Entity
@Table(name = "ethernet_sample")
public class EthernetSample {

    /**
     * ethernet_sample 테이블의 기본 키
     * - 상세 메트릭 조회 시 식별자로 사용
     */
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
