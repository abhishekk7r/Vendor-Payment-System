package com.example.offlinePayment.controller;


import com.example.offlinePayment.exceptions.UserAlreadyApprovedException;
import com.example.offlinePayment.exceptions.UserNotFoundException;
import com.example.offlinePayment.model.User;
import com.example.offlinePayment.model.UserRegistrationRequest;
import com.example.offlinePayment.service.UserService;
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

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationRequest registrationRequest) {
        String userId = userService.registerUser(registrationRequest.getUserName(), registrationRequest.getUserEmail());
        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable String userId) {
        if (!userService.isUserApproved(userId)) {
            userService.approveUser(userId);
            return new ResponseEntity<>("User approved successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User is already approved", HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/checkWaitingPeriod")
    public ResponseEntity<String> checkWaitingPeriod(@RequestParam String userId) {
        int waitingPeriodMinutes = 15;
        if (userService.isWaitingPeriodOver(userId, waitingPeriodMinutes)) {
            return new ResponseEntity<>("Waiting period is over. User can use functionalities now.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User still in the waiting period.", HttpStatus.OK);
        }
    }


}
