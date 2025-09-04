package com.example.offlinePayment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SimpleTestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello! The application is working!";
    }
}