package com.example.offlinePayment.repository;

import com.example.offlinePayment.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {
}
