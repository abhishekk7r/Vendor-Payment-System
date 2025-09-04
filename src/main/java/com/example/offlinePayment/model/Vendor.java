package com.example.offlinePayment.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="vendor_table")
public class Vendor {
    @Id
    private int vendorId;
    
    private String name;
    
    private Double latitude;
    
    private Double longitude;
    
    private Boolean status;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "store_wallet_id")
    private Wallet storeWallet;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "personal_wallet_id")
    private Wallet personalWallet;
}