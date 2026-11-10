package com.temple.erp.repository;

import com.temple.erp.model.Event;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findAllByOrderByEventDateDesc();
}
