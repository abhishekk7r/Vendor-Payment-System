package com.example.offlinePayment.service;

import com.example.offlinePayment.model.User;
import com.example.offlinePayment.repository.UserRepository;
import com.example.offlinePayment.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegisterUser() {
        // Given
        String userName = "John Doe";
        String userEmail = "john@example.com";
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String userId = userService.registerUser(userName, userEmail);

        // Then
        assertNotNull(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testApproveUser() {
        // Given
        String userId = "test-user-id";
        User user = User.builder()
                .userId(userId)
                .userName("Test User")
                .userEmail("test@example.com")
                .isApproved(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.approveUser(userId);

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
        assertTrue(user.isApproved());
        assertNotNull(user.getApprovalTimestamp());
    }

    @Test
    void testIsUserApproved() {
        // Given
        String userId = "test-user-id";
        User approvedUser = User.builder()
                .userId(userId)
                .isApproved(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(approvedUser));

        // When
        boolean isApproved = userService.isUserApproved(userId);

        // Then
        assertTrue(isApproved);
        verify(userRepository, times(1)).findById(userId);
    }
}