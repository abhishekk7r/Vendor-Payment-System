package com.example.offlinePayment.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    private String userName;
    private String userEmail;
}