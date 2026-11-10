package com.temple.erp.repository;

import com.temple.erp.model.RestaurantMenuItem;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantMenuRepository extends MongoRepository<RestaurantMenuItem, String> {
    List<RestaurantMenuItem> findByAvailableTrue();
}
