package com.example.offlinePayment.service;

//import org.springframework.security.core.userdetails.User;


import com.example.offlinePayment.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private List<User> store = new ArrayList<>();

//    public UserService() {
//        store.add(new User(UUID.randomUUID().toString(), "Indrani", "ind@gmail.com"));
//        store.add(new User(UUID.randomUUID().toString(), "Muskan", "mus@gmail.com"));
//        store.add(new User(UUID.randomUUID().toString(), "Utkarsh", "utk@gmail.com"));
//        store.add(new User(UUID.randomUUID().toString(), "Aditya", "adi@gmail.com"));
//
//
//    }

    public String registerUser(String userName, String userEmail) {
        String userId = UUID.randomUUID().toString();
        User newUser = User.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .isApproved(false)
                .approvalTimestamp(null)
                .build();
        store.add(newUser);
        return userId;
    }

    public void approveUser(String userId) {
        for (User user : store) {
            if (user.getUserId().equals(userId)) {
                user.setApproved(true);
                user.setApprovalTimestamp(System.currentTimeMillis());
                break;
            }
        }
    }

    public boolean isUserApproved(String userId) {
        for (User user : store) {
            if (user.getUserId().equals(userId)) {
                return user.isApproved();
            }
        }
        return false;
    }

    public boolean isWaitingPeriodOver(String userId, int waitingPeriodMinutes) {
        for (User user : store) {
            if (user.getUserId().equals(userId) && user.isApproved()) {
                long currentTimeMillis = System.currentTimeMillis();
                long approvalTimestamp = user.getApprovalTimestamp();
                long waitingPeriodMillis = waitingPeriodMinutes * 60 * 1000L;
                return (currentTimeMillis - approvalTimestamp) >= waitingPeriodMillis;
            }
        }
        return false;
    }


    public List<User> getUsers() {
        return this.store;
    }
}
