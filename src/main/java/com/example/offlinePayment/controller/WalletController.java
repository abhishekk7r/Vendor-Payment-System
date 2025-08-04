package com.example.offlinePayment.controller;

import com.example.offlinePayment.model.User;
import com.example.offlinePayment.model.Wallet;
import com.example.offlinePayment.repository.UserRepository;
import com.example.offlinePayment.repository.WalletRepository;
import com.example.offlinePayment.service.ValidationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidationService validationService;

    @PostMapping("/add-money/{userId}")
    public ResponseEntity<String> addMoneyToWallet(
            @PathVariable("userId") String userId,
            @RequestParam("amount") Double amount) {
        
        if (!validationService.isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        
        if (!validationService.isValidAmount(amount)) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = user.getWallet();
        if (wallet == null) {
            wallet = new Wallet(user);
            user.setWallet(wallet);
        }

        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        return ResponseEntity.ok("Money added to wallet successfully. New balance: " + wallet.getBalance());
    }

    @GetMapping("/check-balance/{userId}")
    public ResponseEntity<WalletBalanceResponse> checkWalletBalance(@PathVariable("userId") String userId) {
        if (!validationService.isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = user.getWallet();
        if (wallet == null) {
            return ResponseEntity.ok(new WalletBalanceResponse(0.0, 0.0));
        }

        return ResponseEntity.ok(new WalletBalanceResponse(wallet.getBalance(), wallet.getOfflineBalance()));
    }

    public static class WalletBalanceResponse {
        private double onlineBalance;
        private double offlineBalance;

        public WalletBalanceResponse(double onlineBalance, double offlineBalance) {
            this.onlineBalance = onlineBalance;
            this.offlineBalance = offlineBalance;
        }

        public double getOnlineBalance() { return onlineBalance; }
        public double getOfflineBalance() { return offlineBalance; }
    }

    @PostMapping("/transfer-to-offline/{userId}")
    public ResponseEntity<String> transferMoney(
            @PathVariable("userId") String userId,
            @RequestParam("amount") Double amount) {
        
        if (!validationService.isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid user ID.");
        }
        
        if (!validationService.isValidAmount(amount)) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = user.getWallet();
        if (wallet == null) {
            throw new RuntimeException("Wallet not found for the user.");
        }

        // Check if there is enough balance to transfer
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds in the wallet. Available balance: " + wallet.getBalance());
        }

        if (wallet.getCodes().isEmpty()) {
            generateAndAddCodes(wallet);
        }

        // Transfer money from balance to offline balance
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setOfflineBalance(wallet.getOfflineBalance() + amount);
        walletRepository.save(wallet);

        return ResponseEntity.ok("Money transferred successfully. Offline balance: " + wallet.getOfflineBalance());
    }

    private void generateAndAddCodes(Wallet wallet) {
        int numberOfCodes = 5;
        for (int i = 0; i < numberOfCodes; i++) {
            String randomCode = generateRandomCode();
            wallet.getCodes().add(randomCode);
        }
    }

    private String generateRandomCode() {
        int codeLength = 8;
        return RandomStringUtils.randomAlphanumeric(codeLength);
    }

    @GetMapping("/get-codes/{userId}")
    public ResponseEntity<Set<String>> getWalletCodes(@PathVariable("userId") String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = user.getWallet();
        if (wallet == null || wallet.getCodes().isEmpty()) {
            return ResponseEntity.ok(new HashSet<>()); // Return an empty set if no codes are available
        }

        return ResponseEntity.ok(wallet.getCodes());
    }
}
