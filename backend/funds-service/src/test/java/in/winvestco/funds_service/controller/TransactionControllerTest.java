package in.winvestco.funds_service.controller;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import in.winvestco.funds_service.dto.DepositRequest;
import in.winvestco.funds_service.dto.TransactionDTO;
import in.winvestco.funds_service.dto.WithdrawRequest;
import in.winvestco.funds_service.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

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
 * Unit tests for TransactionController.
 * Tests REST endpoints for deposit and withdrawal operations.
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc
class TransactionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private JwtDecoder jwtDecoder;

        private Jwt mockJwt;
        private TransactionDTO testDepositDTO;
        private TransactionDTO testWithdrawalDTO;

        @BeforeEach
        void setUp() {
                mockJwt = Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .claim("userId", 100L)
                                .claim("sub", "user@test.com")
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();

                testDepositDTO = new TransactionDTO();
                testDepositDTO.setId(1L);
                testDepositDTO.setTransactionType(TransactionType.DEPOSIT);
                testDepositDTO.setAmount(new BigDecimal("5000.00"));
                testDepositDTO.setStatus(TransactionStatus.PENDING);
                testDepositDTO.setExternalReference("DEP-ABC123");

                testWithdrawalDTO = new TransactionDTO();
                testWithdrawalDTO.setId(2L);
                testWithdrawalDTO.setTransactionType(TransactionType.WITHDRAWAL);
                testWithdrawalDTO.setAmount(new BigDecimal("2000.00"));
                testWithdrawalDTO.setStatus(TransactionStatus.PENDING);
                testWithdrawalDTO.setExternalReference("WDR-XYZ789");
        }

        @Test
        @DisplayName("POST /deposit - Should initiate deposit")
        void initiateDeposit_ValidRequest_ShouldReturnTransaction() throws Exception {
                // Arrange
                DepositRequest request = new DepositRequest();
                request.setAmount(new BigDecimal("5000.00"));
                request.setDescription("Test deposit");

                when(transactionService.initiateDeposit(eq(100L), any(DepositRequest.class)))
                                .thenReturn(testDepositDTO);

                // Act & Assert
                mockMvc.perform(post("/api/v1/funds/deposit")
                                .with(jwt().jwt(mockJwt))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                                .andExpect(jsonPath("$.amount").value(5000.00))
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /deposit/confirm - Should confirm deposit")
        void confirmDeposit_ValidReference_ShouldReturnCompletedTransaction() throws Exception {
                // Arrange
                testDepositDTO.setStatus(TransactionStatus.COMPLETED);
                when(transactionService.confirmDeposit("DEP-ABC123")).thenReturn(testDepositDTO);

                // Act & Assert
                mockMvc.perform(post("/api/v1/funds/deposit/confirm")
                                .param("reference", "DEP-ABC123")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.externalReference").value("DEP-ABC123"))
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("POST /withdraw - Should initiate withdrawal")
        void initiateWithdrawal_ValidRequest_ShouldReturnTransaction() throws Exception {
                // Arrange
                WithdrawRequest request = new WithdrawRequest();
                request.setAmount(new BigDecimal("2000.00"));
                request.setDescription("Test withdrawal");

                when(transactionService.initiateWithdrawal(eq(100L), any(WithdrawRequest.class)))
                                .thenReturn(testWithdrawalDTO);

                // Act & Assert
                mockMvc.perform(post("/api/v1/funds/withdraw")
                                .with(jwt().jwt(mockJwt))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                                .andExpect(jsonPath("$.amount").value(2000.00))
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /withdraw/complete - Should complete withdrawal")
        void completeWithdrawal_ValidReference_ShouldReturnCompletedTransaction() throws Exception {
                // Arrange
                testWithdrawalDTO.setStatus(TransactionStatus.COMPLETED);
                when(transactionService.completeWithdrawal("WDR-XYZ789")).thenReturn(testWithdrawalDTO);

                // Act & Assert
                mockMvc.perform(post("/api/v1/funds/withdraw/complete")
                                .param("reference", "WDR-XYZ789")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.externalReference").value("WDR-XYZ789"))
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("GET /transactions - Should return paginated transaction history")
        void getTransactions_ShouldReturnPage() throws Exception {
                // Arrange
                Page<TransactionDTO> txPage = new PageImpl<>(List.of(testDepositDTO, testWithdrawalDTO));
                when(transactionService.getTransactionsForUser(eq(100L), any())).thenReturn(txPage);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/transactions")
                                .param("page", "0")
                                .param("size", "20")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("GET /transactions/{reference} - Should return specific transaction")
        void getTransaction_Exists_ShouldReturnTransaction() throws Exception {
                // Arrange
                when(transactionService.getTransactionByReference("DEP-ABC123")).thenReturn(testDepositDTO);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/transactions/DEP-ABC123")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.externalReference").value("DEP-ABC123"))
                                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"));
        }

        @Test
        @DisplayName("POST /deposit - Should return 401 without authentication")
        void initiateDeposit_Unauthenticated_ShouldReturn401() throws Exception {
                DepositRequest request = new DepositRequest();
                request.setAmount(new BigDecimal("5000.00"));

                mockMvc.perform(post("/api/v1/funds/deposit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }
}
