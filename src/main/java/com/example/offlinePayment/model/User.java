package com.example.offlinePayment.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="table_user")
public class User {
    @Id
    private String userId;
    
    private String userName;
    
    private String userEmail;
    
    private boolean isApproved;
    
    private Long approvalTimestamp;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;
    
    private Boolean user_status;
    
    private Boolean user_enrolled;
    
    private Boolean user_enrollapproved;
    
    private Double user_latitude;
    
    private Double user_longitude;
    
    @Enumerated(EnumType.STRING)
    private Role role;
}