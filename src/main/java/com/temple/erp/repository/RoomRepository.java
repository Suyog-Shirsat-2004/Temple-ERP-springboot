package com.temple.erp.repository;

import com.temple.erp.model.Room;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByStatus(String status);
}
