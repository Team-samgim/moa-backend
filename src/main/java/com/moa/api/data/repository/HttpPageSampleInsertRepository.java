/**
 * 작성자: 정소영
 */
package com.moa.api.data.repository;

import com.moa.api.data.entity.HttpPageSampleFull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HttpPageSampleInsertRepository extends JpaRepository<HttpPageSampleFull, String> {
    // 기본 CRUD 메서드는 JpaRepository가 제공
    // saveAll(), save(), findAll(), findById() 등
}