package com.example.offlinePayment.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="wallet_table")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int walletId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ElementCollection
    @CollectionTable(name = "wallet_codes", joinColumns = @JoinColumn(name = "wallet_id"))
    @Column(name = "code")
    @Builder.Default
    private Set<String> codes = new HashSet<>();

    private double balance;
    
    private double offlineBalance;

    public Wallet(User user) {
        this.user = user;
        this.balance = 0.0;
        this.offlineBalance = 0.0;
        this.codes = new HashSet<>();
    }
    
    public Wallet(int walletId) {
        this.walletId = walletId;
        this.balance = 0.0;
        this.offlineBalance = 0.0;
        this.codes = new HashSet<>();
    }
}