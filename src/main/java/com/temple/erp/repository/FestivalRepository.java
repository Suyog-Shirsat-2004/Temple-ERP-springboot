package com.temple.erp.repository;

import com.temple.erp.model.Festival;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FestivalRepository extends MongoRepository<Festival, String> {
    List<Festival> findAllByOrderByEventDateDesc();
}
