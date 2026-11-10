package com.temple.erp.repository;

import com.temple.erp.model.DarshanPass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DarshanPassRepository extends MongoRepository<DarshanPass, String> {
    Optional<DarshanPass> findByPassId(String passId);
    Optional<DarshanPass> findByQrToken(String qrToken);
    List<DarshanPass> findByUserIdOrderByIdDesc(String userId);
    List<DarshanPass> findAllByOrderByIdDesc();
    Optional<DarshanPass> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
