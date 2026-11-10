package com.temple.erp.repository;

import com.temple.erp.model.TempleUpdate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TempleUpdateRepository extends MongoRepository<TempleUpdate, String> {
    List<TempleUpdate> findByStatusOrderByCreatedAtDesc(String status);
    List<TempleUpdate> findByUpdateTypeOrderByCreatedAtDesc(String updateType);
    List<TempleUpdate> findByStatusAndUpdateTypeOrderByCreatedAtDesc(String status, String updateType);
    List<TempleUpdate> findAllByOrderByCreatedAtDesc();
}
