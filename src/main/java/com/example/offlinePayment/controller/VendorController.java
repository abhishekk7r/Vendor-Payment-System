package com.example.offlinePayment.controller;

import com.example.offlinePayment.model.Admin;
import com.example.offlinePayment.model.Vendor;
import com.example.offlinePayment.model.Wallet;
import com.example.offlinePayment.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorRepository vendorRepository;



    @PostMapping("/register")
    public ResponseEntity<String> registerVendor(@RequestBody Vendor vendor) {
        if (vendorRepository.existsById(vendor.getVendorId())) {
            return ResponseEntity.badRequest().body("Vendor with the same name already exists.");
        }
        vendor.setStatus(false);
        vendorRepository.save(vendor);

        return ResponseEntity.ok("Vendor registration request submitted. Waiting for approval.");
    }

    @PostMapping("/approve/{adminId}/{vendorId}")
    public ResponseEntity<String> approveVendor(
            @PathVariable int adminId,
            @PathVariable int vendorId,
            @RequestParam int walletId) {
//        Admin admin = adminRepository.findById(adminId)
//                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        Wallet storeWallet = vendor.getStoreWallet();
        if (storeWallet == null) {
            storeWallet = new Wallet(walletId);
            vendor.setStoreWallet(storeWallet);
        }


        vendor.setStatus(true); // Approve the vendor
        vendorRepository.save(vendor);

        return ResponseEntity.ok("Vendor approved successfully.");
    }

    @PostMapping("/transfer-to-personal/{vendorId}")
    public ResponseEntity<String> transferMoney(
            @PathVariable int vendorId,
            @RequestParam("amount") Double amount,
            @RequestParam int personalWalletId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getStoreWallet() == null) {
            return ResponseEntity.badRequest().body("Store wallet not found for the vendor.");
        }

        Wallet personalWallet = vendor.getPersonalWallet();
        if (personalWallet == null) {
            personalWallet = new Wallet(personalWalletId);
            vendor.setPersonalWallet(personalWallet);
        }

        // Transfer money from store wallet to personal wallet
        vendor.getStoreWallet().setBalance(vendor.getStoreWallet().getBalance() - amount);
        vendor.getPersonalWallet().setBalance(vendor.getPersonalWallet().getBalance() + amount);

        vendorRepository.save(vendor);

        return ResponseEntity.ok("Money transferred from store wallet to personal wallet.");
    }

}
