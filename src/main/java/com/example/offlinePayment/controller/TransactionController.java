package com.example.offlinePayment.controller;

import com.example.offlinePayment.model.*;
import com.example.offlinePayment.repository.TransactionRepository;
import com.example.offlinePayment.repository.UserRepository;
import com.example.offlinePayment.repository.VendorRepository;
import com.example.offlinePayment.repository.WalletRepository;
import com.example.offlinePayment.repository.AdminRepository;
import com.example.offlinePayment.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ValidationService validationService;



    @PostMapping("/make-payment-online")
    public ResponseEntity<String> makePayment(@RequestBody PaymentRequestOnline paymentRequest) {
        // Validate input
        if (!validationService.isValidUserId(paymentRequest.getUserId())) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        if (!validationService.isValidVendorId(paymentRequest.getVendorId())) {
            throw new IllegalArgumentException("Invalid vendor ID.");
        }
        if (!validationService.isValidAmount(paymentRequest.getAmount())) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        if (!validationService.isValidCoordinates(paymentRequest.getLatitude(), paymentRequest.getLongitude())) {
            throw new IllegalArgumentException("Invalid coordinates.");
        }

        User user = userRepository.findById(paymentRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vendor vendor = vendorRepository.findById(paymentRequest.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Check if user has sufficient balance
        Wallet userWallet = user.getWallet();
        if (userWallet == null || userWallet.getBalance() < paymentRequest.getAmount()) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + 
                (userWallet != null ? userWallet.getBalance() : 0));
        }

        // Check if the payment is within 20km radius
        if (!isWithinRadius(paymentRequest.getLatitude(), paymentRequest.getLongitude(), vendor.getLatitude(), vendor.getLongitude(), 20)) {
            Transaction transaction = new Transaction();
            transaction.setUserId(paymentRequest.getUserId());
            transaction.setVendorId(paymentRequest.getVendorId());
            transaction.setAmount(paymentRequest.getAmount());
            transaction.setStatus(TransactionStatus.FLAGGED);
            transaction.setPaymentMode(PaymentMode.ONLINE);
            transaction.setTransactionDate(new Date());


            // Update user's wallet balance
            Wallet userWallet2 = user.getWallet();
            userWallet2.setBalance(userWallet2.getBalance() - paymentRequest.getAmount());
            walletRepository.save(userWallet2);

            // Save the transaction
            transactionRepository.save(transaction);

            return ResponseEntity.ok("Payment flagged. Payment made from > 20 km");
        } else {
            Transaction transaction = new Transaction();
            transaction.setUserId(paymentRequest.getUserId());
            transaction.setVendorId(paymentRequest.getVendorId());
            transaction.setAmount(paymentRequest.getAmount());
            transaction.setStatus(TransactionStatus.SUCCESSFUL);
            transaction.setPaymentMode(PaymentMode.ONLINE);
            transaction.setTransactionDate(new Date());


            // Update user's wallet balance
            Wallet userWallet = user.getWallet();
            userWallet.setBalance(userWallet.getBalance() - paymentRequest.getAmount());
            walletRepository.save(userWallet);

            // Update vendor's wallet balance
            Wallet vendorWallet = vendor.getStoreWallet();
            vendorWallet.setBalance(vendorWallet.getBalance() + paymentRequest.getAmount());
            walletRepository.save(vendorWallet);

            // Save the transaction
            transactionRepository.save(transaction);

            return ResponseEntity.ok("Payment successful.");
        }
    }



    @PostMapping("/make-payment-offline")
    public ResponseEntity<String> makePaymentOffline(@RequestBody PaymentRequestOffline paymentRequestOffline) {
        // Validate input
        if (!validationService.isValidUserId(paymentRequestOffline.getUserId())) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        if (!validationService.isValidVendorId(paymentRequestOffline.getVendorId())) {
            throw new IllegalArgumentException("Invalid vendor ID.");
        }
        if (!validationService.isValidAmount(paymentRequestOffline.getAmount())) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        if (!validationService.isValidCoordinates(paymentRequestOffline.getLatitude(), paymentRequestOffline.getLongitude())) {
            throw new IllegalArgumentException("Invalid coordinates.");
        }

        User user = userRepository.findById(paymentRequestOffline.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the provided code matches any of the codes in the user's set
        if (user.getWallet() == null || !user.getWallet().getCodes().contains(paymentRequestOffline.getCode())) {
            throw new IllegalArgumentException("Invalid code. Transaction failed.");
        }

        // Check if user has sufficient offline balance
        if (user.getWallet().getOfflineBalance() < paymentRequestOffline.getAmount()) {
            throw new IllegalArgumentException("Insufficient offline balance. Available: " + 
                user.getWallet().getOfflineBalance());
        }

        Vendor vendor = vendorRepository.findById(paymentRequestOffline.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (!isWithinRadius(paymentRequestOffline.getLatitude(), paymentRequestOffline.getLongitude(), vendor.getLatitude(), vendor.getLongitude(), 20)) {

            Transaction transaction = new Transaction();
            transaction.setUserId(paymentRequestOffline.getUserId());
            transaction.setVendorId(paymentRequestOffline.getVendorId());
            transaction.setAmount(paymentRequestOffline.getAmount());
            transaction.setStatus(TransactionStatus.FLAGGED);
            transaction.setPaymentMode(PaymentMode.OFFLINE);
            transaction.setTransactionDate(new Date());

            // Update user's wallet balance
            Wallet userWallet = user.getWallet();
            userWallet.setOfflineBalance(userWallet.getOfflineBalance() - paymentRequestOffline.getAmount());
            walletRepository.save(userWallet);

            // Update vendor's wallet balance
//            Wallet vendorWallet = vendor.getPersonalWallet();
//            vendorWallet.setBalance(vendorWallet.getBalance() + paymentRequestOffline.getAmount());
//            walletRepository.save(vendorWallet);

            // Save the transaction
            transactionRepository.save(transaction);

            return ResponseEntity.ok("payment flagged. distance > 20km");

        } else {

            Transaction transaction = new Transaction();
            transaction.setUserId(paymentRequestOffline.getUserId());
            transaction.setVendorId(paymentRequestOffline.getVendorId());
            transaction.setAmount(paymentRequestOffline.getAmount());
            transaction.setStatus(TransactionStatus.SUCCESSFUL);
            transaction.setPaymentMode(PaymentMode.OFFLINE);
            transaction.setTransactionDate(new Date());

            // Update user's wallet balance
            Wallet userWallet = user.getWallet();
            userWallet.setOfflineBalance(userWallet.getOfflineBalance() - paymentRequestOffline.getAmount());
            walletRepository.save(userWallet);

            // Update vendor's wallet balance
            Wallet vendorWallet = vendor.getStoreWallet();
            vendorWallet.setBalance(vendorWallet.getBalance() + paymentRequestOffline.getAmount());
            walletRepository.save(vendorWallet);

            // Save the transaction
            transactionRepository.save(transaction);

            return ResponseEntity.ok("Offline payment successful.");
        }
    }

    @GetMapping("/flagged-transactions")
    public ResponseEntity<List<Transaction>> getFlaggedTransactions() {
        // Retrieve flagged transactions for admin review
        List<Transaction> flaggedTransactions = transactionRepository.findByStatus(TransactionStatus.FLAGGED).get();
        return ResponseEntity.ok(flaggedTransactions);
    }

    @PostMapping("/review-transaction/{adminId}/{transactionId}/{approval}")
    public ResponseEntity<String> reviewTransaction(
            @PathVariable Long adminId,
            @PathVariable Long transactionId,
            @PathVariable Boolean approval) {
        // Implement logic for admin review
        Admin admin = adminRepository.findById(Math.toIntExact(adminId))
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() == TransactionStatus.FLAGGED) {
            if (approval) {
                // Approve transaction
                transferAmountToVendor(transaction);
            } else {
                // Reject transaction
                returnAmountToUser(transaction);
            }
        }

        return ResponseEntity.ok("Transaction reviewed successfully.");
    }


    // Helper method to check if a location is within a certain radius
    private boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radius) {
        double earthRadius = 6371; // in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;

        return distance <= radius;
    }

    // Helper method to transfer amount to the vendor's wallet
    private void transferAmountToVendor(Transaction transaction) {
        Vendor vendor = vendorRepository.findById(transaction.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Transfer amount to vendor's wallet
        Wallet vendorWallet = vendor.getStoreWallet();
        vendorWallet.setBalance(vendorWallet.getBalance() + transaction.getAmount());

        // Save the updated vendor's wallet
        walletRepository.save(vendorWallet);

        // Update the transaction status to SUCCESSFUL
        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        transactionRepository.save(transaction);
    }

    // Helper method to return amount to the user's wallet
    private void returnAmountToUser(Transaction transaction) {
        User user = userRepository.findById(transaction.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Return amount to user's wallet
        Wallet userWallet = user.getWallet();
        userWallet.setBalance(userWallet.getBalance() + transaction.getAmount());

        // Save the updated user's wallet
        walletRepository.save(userWallet);

        // Update the transaction status to FAILED
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    @Data
    static class PaymentRequestOnline {
        private String userId;
        private Integer vendorId;
        private Double amount;
        private Double latitude;

        private Double longitude;

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Integer getVendorId() {
            return vendorId;
        }

        public void setVendorId(Integer vendorId) {
            this.vendorId = vendorId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

    }

    @Data
    static class PaymentRequestOffline {
        private String userId;
        private Integer vendorId;
        private Double amount;
        private Double latitude;

        private Double longitude;

        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Integer getVendorId() {
            return vendorId;
        }

        public void setVendorId(Integer vendorId) {
            this.vendorId = vendorId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

    }
}
