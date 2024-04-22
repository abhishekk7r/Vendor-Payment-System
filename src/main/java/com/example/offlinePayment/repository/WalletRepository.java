package com.example.offlinePayment.repository;


import com.example.offlinePayment.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}