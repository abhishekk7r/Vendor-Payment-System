package com.example.offlinePayment.controller;

import com.example.offlinePayment.exceptions.UserAlreadyApprovedException;
import com.example.offlinePayment.exceptions.UserNotFoundException;
import com.example.offlinePayment.model.User;
import com.example.offlinePayment.model.UserRegistrationRequest;
import com.example.offlinePayment.service.UserService;
import com.example.offlinePayment.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/home")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ValidationService validationService;

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationRequest registrationRequest) {
        // Validate input
        if (!validationService.isValidUserName(registrationRequest.getUserName())) {
            throw new IllegalArgumentException("Invalid user name. Name must be at least 2 characters long.");
        }
        
        if (!validationService.isValidEmail(registrationRequest.getUserEmail())) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        String userId = userService.registerUser(registrationRequest.getUserName(), registrationRequest.getUserEmail());
        logger.info("User registered successfully with ID: {}", userId);
        return new ResponseEntity<>("User registered successfully. User ID: " + userId, HttpStatus.CREATED);
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable String userId) {
        if (!validationService.isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        if (userService.isUserApproved(userId)) {
            throw new UserAlreadyApprovedException("User is already approved");
        }

        userService.approveUser(userId);
        logger.info("User approved successfully: {}", userId);
        return new ResponseEntity<>("User approved successfully", HttpStatus.OK);
    }

    @GetMapping("/checkWaitingPeriod")
    public ResponseEntity<String> checkWaitingPeriod(@RequestParam String userId) {
        if (!validationService.isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        int waitingPeriodMinutes = 15;
        if (userService.isWaitingPeriodOver(userId, waitingPeriodMinutes)) {
            return new ResponseEntity<>("Waiting period is over. User can use functionalities now.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User still in the waiting period.", HttpStatus.OK);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
