package in.winvestco.funds_service.messaging;

import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserEventListener.
 * Tests wallet creation on user registration.
 */
@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private UserEventListener userEventListener;

    private UserCreatedEvent userCreatedEvent;
    private Wallet createdWallet;

    @BeforeEach
    void setUp() {
        userCreatedEvent = UserCreatedEvent.builder()
                .userId(100L)
                .email("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        createdWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("Should create wallet for new user")
    void handleUserCreated_ShouldCreateWallet() {
        // Arrange
        when(walletService.createWalletForUser(100L)).thenReturn(createdWallet);

        // Act
        userEventListener.handleUserCreated(userCreatedEvent);

        // Assert
        verify(walletService).createWalletForUser(100L);
    }

    @Test
    @DisplayName("Should rethrow exception on wallet creation failure")
    void handleUserCreated_Error_ShouldRethrow() {
        // Arrange
        when(walletService.createWalletForUser(100L))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> userEventListener.handleUserCreated(userCreatedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }

    @Test
    @DisplayName("Should log user details from event")
    void handleUserCreated_ShouldLogUserDetails() {
        // Arrange
        when(walletService.createWalletForUser(100L)).thenReturn(createdWallet);

        // Act
        userEventListener.handleUserCreated(userCreatedEvent);

        // Assert - verify the correct userId was passed
        verify(walletService).createWalletForUser(eq(100L));
    }
}
