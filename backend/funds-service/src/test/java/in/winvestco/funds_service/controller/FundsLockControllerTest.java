package in.winvestco.funds_service.controller;

import in.winvestco.common.enums.LockStatus;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.dto.LockFundsRequest;
import in.winvestco.funds_service.dto.ReleaseFundsRequest;
import in.winvestco.funds_service.dto.WalletDTO;
import in.winvestco.funds_service.service.FundsLockService;
import in.winvestco.funds_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FundsLockController.
 * Tests REST endpoints for funds locking operations.
 */
@WebMvcTest(FundsLockController.class)
@AutoConfigureMockMvc
class FundsLockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FundsLockService fundsLockService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Jwt mockJwt;
    private FundsLockDTO testLockDTO;
    private WalletDTO testWalletDTO;

    @BeforeEach
    void setUp() {
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("userId", 100L)
                .claim("sub", "user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        testLockDTO = new FundsLockDTO();
        testLockDTO.setId(1L);
        testLockDTO.setOrderId("ORD-123");
        testLockDTO.setAmount(new BigDecimal("1000.00"));
        testLockDTO.setStatus(LockStatus.LOCKED);

        testWalletDTO = new WalletDTO();
        testWalletDTO.setId(1L);
        testWalletDTO.setUserId(100L);
    }

    @Test
    @DisplayName("POST /locks/lock - Should lock funds successfully")
    void lockFunds_ValidRequest_ShouldReturnLock() throws Exception {
        // Arrange
        LockFundsRequest request = new LockFundsRequest();
        request.setOrderId("ORD-123");
        request.setAmount(new BigDecimal("1000.00"));
        request.setReason("Buy order");

        when(fundsLockService.lockFunds(eq(100L), eq("ORD-123"), eq(new BigDecimal("1000.00")), eq("Buy order")))
                .thenReturn(testLockDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/funds/locks/lock")
                .with(jwt().jwt(mockJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    @DisplayName("POST /locks/unlock - Should release locked funds")
    void unlockFunds_ValidRequest_ShouldReturnReleasedLock() throws Exception {
        // Arrange
        ReleaseFundsRequest request = new ReleaseFundsRequest();
        request.setOrderId("ORD-123");
        request.setReason("Order cancelled");

        testLockDTO.setStatus(LockStatus.RELEASED);
        when(fundsLockService.releaseFunds("ORD-123", "Order cancelled")).thenReturn(testLockDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/funds/locks/unlock")
                .with(jwt().jwt(mockJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    @DisplayName("POST /locks/settle - Should settle locked funds")
    void settleFunds_ValidRequest_ShouldReturnSettledLock() throws Exception {
        // Arrange
        ReleaseFundsRequest request = new ReleaseFundsRequest();
        request.setOrderId("ORD-123");
        request.setReason("Trade executed");

        testLockDTO.setStatus(LockStatus.SETTLED);
        when(fundsLockService.settleFunds("ORD-123", "Trade executed")).thenReturn(testLockDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/funds/locks/settle")
                .with(jwt().jwt(mockJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.status").value("SETTLED"));
    }

    @Test
    @DisplayName("GET /locks - Should return active locks for user")
    void getActiveLocks_ShouldReturnLocksList() throws Exception {
        // Arrange
        when(walletService.getWalletByUserId(100L)).thenReturn(testWalletDTO);
        when(fundsLockService.getActiveLocksForWallet(1L)).thenReturn(List.of(testLockDTO));

        // Act & Assert
        mockMvc.perform(get("/api/v1/funds/locks")
                .with(jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("ORD-123"));
    }

    @Test
    @DisplayName("GET /locks/{orderId} - Should return specific lock")
    void getLockByOrderId_Exists_ShouldReturnLock() throws Exception {
        // Arrange
        when(fundsLockService.getLockByOrderId("ORD-123")).thenReturn(testLockDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/funds/locks/ORD-123")
                .with(jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    @DisplayName("POST /locks/lock - Should return 401 without authentication")
    void lockFunds_Unauthenticated_ShouldReturn401() throws Exception {
        LockFundsRequest request = new LockFundsRequest();
        request.setOrderId("ORD-123");
        request.setAmount(new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/v1/funds/locks/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
