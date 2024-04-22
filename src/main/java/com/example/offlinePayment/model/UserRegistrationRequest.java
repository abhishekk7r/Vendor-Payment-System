package com.example.offlinePayment.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UserRegistrationRequest {

    private String userName;
    private String userEmail;

    // constructor, getters, setters
}
