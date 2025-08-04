package com.example.offlinePayment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter

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
    //@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
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
    
    public Wallet() {
        this.balance = 0.0;
        this.offlineBalance = 0.0;
        this.codes = new HashSet<>();
    }


    // private Wallet wallet;



}