package com.temple.erp.repository;

import com.temple.erp.model.Donation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonationRepository extends MongoRepository<Donation, String> {
    Optional<Donation> findByReceiptNo(String receiptNo);
    List<Donation> findByUserIdOrderByIdDesc(String userId);
    List<Donation> findAllByOrderByIdDesc();
    List<Donation> findByPaymentStatus(String paymentStatus);
}
