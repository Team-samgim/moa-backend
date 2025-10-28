package com.moa.api.search.repository;

import com.moa.api.search.entity.HttpPageField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HttpPageFieldRepository extends JpaRepository<HttpPageField, String> {
    List<HttpPageField> findAllByOrderByFieldKeyAsc();
}
