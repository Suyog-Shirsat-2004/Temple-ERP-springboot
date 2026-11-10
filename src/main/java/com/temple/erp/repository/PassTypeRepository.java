package com.temple.erp.repository;

import com.temple.erp.model.PassType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PassTypeRepository extends MongoRepository<PassType, String> {
    List<PassType> findByIsActiveTrueOrderByPriceAsc();
    @Query("{ 'pass_type': { $regex: ?0, $options: 'i' }, 'is_active': true }")
    Optional<PassType> findActiveByPassTypeRegex(String regex);
}
