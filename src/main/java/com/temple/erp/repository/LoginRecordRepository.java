package com.temple.erp.repository;

import com.temple.erp.model.LoginRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LoginRecordRepository extends MongoRepository<LoginRecord, String> {
    Page<LoginRecord> findByUserIdOrderByLoginAtDesc(String userId, Pageable pageable);
    Page<LoginRecord> findAllByOrderByLoginAtDesc(Pageable pageable);
}
