package com.example.offlinePayment.integration;

import com.example.offlinePayment.controller.TransactionController;
import com.example.offlinePayment.model.User;
import com.example.offlinePayment.model.Vendor;
import com.example.offlinePayment.model.Wallet;
import com.example.offlinePayment.repository.UserRepository;
import com.example.offlinePayment.repository.VendorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for payment system
 * SDE2 Skill: Integration testing, Test containers, End-to-end testing
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    private User testUser;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = User.builder()
                .userId("test-user-123")
                .userName("Test User")
                .userEmail("test@example.com")
                .isApproved(true)
                .build();

        Wallet wallet = new Wallet(testUser);
        wallet.setBalance(1000.0);
        wallet.setOfflineBalance(500.0);
        testUser.setWallet(wallet);

        testVendor = Vendor.builder()
                .vendorId(123)
                .name("Test Vendor")
                .latitude(40.7128)
                .longitude(-74.0060)
                .status(true)
                .build();

        userRepository.save(testUser);
        vendorRepository.save(testVendor);
    }

    @Test
    void testSuccessfulOnlinePayment() throws Exception {
        TransactionController.PaymentRequestOnline request = new TransactionController.PaymentRequestOnline();
        request.setUserId("test-user-123");
        request.setVendorId(123);
        request.setAmount(100.0);
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);

        mockMvc.perform(post("/api/transactions/make-payment-online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment successful."));
    }

    @Test
    void testFlaggedPaymentDueToDistance() throws Exception {
        TransactionController.PaymentRequestOnline request = new TransactionController.PaymentRequestOnline();
        request.setUserId("test-user-123");
        request.setVendorId(123);
        request.setAmount(100.0);
        request.setLatitude(50.0); // Far from vendor
        request.setLongitude(-80.0);

        mockMvc.perform(post("/api/transactions/make-payment-online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isOk())
                .andExpect(content().string("Payment flagged. Payment made from > 20 km"));
    }

    @Test
    void testInsufficientBalance() throws Exception {
        TransactionController.PaymentRequestOnline request = new TransactionController.PaymentRequestOnline();
        request.setUserId("test-user-123");
        request.setVendorId(123);
        request.setAmount(2000.0); // More than available balance
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);

        mockMvc.perform(post("/api/transactions/make-payment-online")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}