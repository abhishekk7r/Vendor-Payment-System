package com.example.offlinePayment.controller;

import com.example.offlinePayment.model.Admin;
import com.example.offlinePayment.model.Vendor;
import com.example.offlinePayment.model.Wallet;
import com.example.offlinePayment.repository.VendorRepository;
import com.example.offlinePayment.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ValidationService validationService;



    @PostMapping("/register")
    public ResponseEntity<String> registerVendor(@RequestBody Vendor vendor) {
        // Validate input
        if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor name is required.");
        }
        if (!validationService.isValidCoordinates(vendor.getLatitude(), vendor.getLongitude())) {
            throw new IllegalArgumentException("Invalid coordinates.");
        }

        if (vendorRepository.existsById(vendor.getVendorId())) {
            throw new IllegalArgumentException("Vendor with the same ID already exists.");
        }
        
        vendor.setStatus(false);
        // Initialize wallets
        Wallet storeWallet = new Wallet();
        Wallet personalWallet = new Wallet();
        vendor.setStoreWallet(storeWallet);
        vendor.setPersonalWallet(personalWallet);
        
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
            @RequestParam("amount") Double amount) {
        
        if (!validationService.isValidVendorId(vendorId)) {
            throw new IllegalArgumentException("Invalid vendor ID.");
        }
        if (!validationService.isValidAmount(amount)) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getStoreWallet() == null) {
            throw new RuntimeException("Store wallet not found for the vendor.");
        }

        if (vendor.getStoreWallet().getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance in store wallet. Available: " + 
                vendor.getStoreWallet().getBalance());
        }

        Wallet personalWallet = vendor.getPersonalWallet();
        if (personalWallet == null) {
            personalWallet = new Wallet();
            vendor.setPersonalWallet(personalWallet);
        }

        // Transfer money from store wallet to personal wallet
        vendor.getStoreWallet().setBalance(vendor.getStoreWallet().getBalance() - amount);
        vendor.getPersonalWallet().setBalance(vendor.getPersonalWallet().getBalance() + amount);

        vendorRepository.save(vendor);

        return ResponseEntity.ok("Money transferred from store wallet to personal wallet. Store balance: " + 
            vendor.getStoreWallet().getBalance() + ", Personal balance: " + vendor.getPersonalWallet().getBalance());
    }

}
