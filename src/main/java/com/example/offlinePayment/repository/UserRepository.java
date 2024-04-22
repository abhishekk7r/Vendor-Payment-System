package com.example.offlinePayment.repository;

import com.example.offlinePayment.model.User;  // Import your custom User class
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserName(String userId);  // Assuming you have a "userName" field in your User class
}
