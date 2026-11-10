package com.temple.erp.repository;

import com.temple.erp.model.RoomBooking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomBookingRepository extends MongoRepository<RoomBooking, String> {
    Optional<RoomBooking> findByBookingId(String bookingId);
    List<RoomBooking> findByUserIdOrderByIdDesc(String userId);
    List<RoomBooking> findAllByOrderByIdDesc();
    Optional<RoomBooking> findByIdAndUserId(String id, String userId);
}
