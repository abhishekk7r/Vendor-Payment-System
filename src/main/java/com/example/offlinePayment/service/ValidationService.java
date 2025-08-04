package com.example.offlinePayment.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ValidationService {

    public boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && 
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public boolean isValidAmount(Double amount) {
        return amount != null && amount > 0;
    }

    public boolean isValidUserId(String userId) {
        return StringUtils.hasText(userId);
    }

    public boolean isValidUserName(String userName) {
        return StringUtils.hasText(userName) && userName.trim().length() >= 2;
    }

    public boolean isValidCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }

    public boolean isValidVendorId(Integer vendorId) {
        return vendorId != null && vendorId > 0;
    }
}