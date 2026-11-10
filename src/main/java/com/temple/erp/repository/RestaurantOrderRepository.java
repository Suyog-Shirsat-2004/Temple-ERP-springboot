package com.temple.erp.repository;

import com.temple.erp.model.RestaurantOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantOrderRepository extends MongoRepository<RestaurantOrder, String> {
    Optional<RestaurantOrder> findByOrderId(String orderId);
    List<RestaurantOrder> findByUserIdOrderByIdDesc(String userId);
    List<RestaurantOrder> findAllByOrderByIdDesc();
}
