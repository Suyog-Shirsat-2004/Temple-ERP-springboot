package com.temple.erp.repository;

import com.temple.erp.model.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByEmail(String email);
    Optional<User> findByEmailOrUsername(String email, String username);
    Optional<User> findFirstByEmailOrUsername(String email, String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
