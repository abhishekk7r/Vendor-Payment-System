package com.example.offlinePayment.service;

import com.example.offlinePayment.model.User;
import com.example.offlinePayment.model.Wallet;
import com.example.offlinePayment.repository.UserRepository;
import com.example.offlinePayment.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    public String registerUser(String userName, String userEmail) {
        String userId = UUID.randomUUID().toString();
        User newUser = User.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .isApproved(false)
                .approvalTimestamp(null)
                .user_status(true)
                .user_enrolled(false)
                .user_enrollapproved(false)
                .build();
        
        // Create wallet for user
        Wallet wallet = new Wallet(newUser);
        newUser.setWallet(wallet);
        
        // Save user (wallet will be saved due to cascade)
        userRepository.save(newUser);
        return userId;
    }

    public void approveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setApproved(true);
        user.setApprovalTimestamp(System.currentTimeMillis());
        userRepository.save(user);
    }

    public boolean isUserApproved(String userId) {
        User user = userRepository.findById(userId)
                .orElse(null);
        return user != null && user.isApproved();
    }

    public boolean isWaitingPeriodOver(String userId, int waitingPeriodMinutes) {
        User user = userRepository.findById(userId)
                .orElse(null);
        
        if (user != null && user.isApproved() && user.getApprovalTimestamp() != null) {
            long currentTimeMillis = System.currentTimeMillis();
            long approvalTimestamp = user.getApprovalTimestamp();
            long waitingPeriodMillis = waitingPeriodMinutes * 60 * 1000L;
            return (currentTimeMillis - approvalTimestamp) >= waitingPeriodMillis;
        }
        return false;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}
