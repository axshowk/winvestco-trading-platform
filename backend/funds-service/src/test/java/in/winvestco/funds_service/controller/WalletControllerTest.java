package in.winvestco.funds_service.controller;

import in.winvestco.common.enums.WalletStatus;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import in.winvestco.funds_service.dto.WalletDTO;
import in.winvestco.funds_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

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
 * Unit tests for WalletController.
 * Tests REST endpoints for wallet operations.
 */
@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc
class WalletControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private WalletService walletService;

        @MockBean
        private JwtDecoder jwtDecoder;

        private WalletDTO testWalletDTO;
        private Jwt mockJwt;

        @BeforeEach
        void setUp() {
                testWalletDTO = new WalletDTO();
                testWalletDTO.setId(1L);
                testWalletDTO.setUserId(100L);
                testWalletDTO.setAvailableBalance(new BigDecimal("10000.00"));
                testWalletDTO.setLockedBalance(new BigDecimal("1000.00"));
                testWalletDTO.setTotalBalance(new BigDecimal("11000.00"));
                testWalletDTO.setCurrency("INR");
                testWalletDTO.setStatus(WalletStatus.ACTIVE);

                mockJwt = Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .claim("userId", 100L)
                                .claim("sub", "user@test.com")
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
        }

        @Test
        @DisplayName("GET /wallet - Should return wallet for authenticated user")
        void getWallet_Authenticated_ShouldReturnWallet() throws Exception {
                // Arrange
                when(walletService.getWalletByUserId(100L)).thenReturn(testWalletDTO);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.userId").value(100))
                                .andExpect(jsonPath("$.availableBalance").value(10000.00))
                                .andExpect(jsonPath("$.lockedBalance").value(1000.00));
        }

        @Test
        @DisplayName("GET /wallet - Should return 401 without authentication")
        void getWallet_Unauthenticated_ShouldReturn401() throws Exception {
                mockMvc.perform(get("/api/v1/funds/wallet"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /wallet/balance - Should return balance summary")
        void getBalanceSummary_ShouldReturnSummary() throws Exception {
                // Arrange
                when(walletService.getWalletByUserId(100L)).thenReturn(testWalletDTO);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet/balance")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.available").value(10000.00))
                                .andExpect(jsonPath("$.locked").value(1000.00))
                                .andExpect(jsonPath("$.total").value(11000.00));
        }

        @Test
        @DisplayName("GET /wallet/ledger - Should return paginated ledger entries")
        void getLedger_ShouldReturnPaginatedEntries() throws Exception {
                // Arrange
                LedgerEntryDTO entry = new LedgerEntryDTO();
                entry.setId(1L);
                entry.setAmount(new BigDecimal("5000.00"));
                Page<LedgerEntryDTO> ledgerPage = new PageImpl<>(List.of(entry));

                when(walletService.getWalletByUserId(100L)).thenReturn(testWalletDTO);
                when(walletService.getLedgerEntries(eq(1L), anyInt(), anyInt())).thenReturn(ledgerPage);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet/ledger")
                                .param("page", "0")
                                .param("size", "20")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].id").value(1));
        }

        @Test
        @DisplayName("GET /wallet/ledger/all - Should return all ledger entries")
        void getAllLedger_ShouldReturnAllEntries() throws Exception {
                // Arrange
                LedgerEntryDTO entry = new LedgerEntryDTO();
                entry.setId(1L);

                when(walletService.getWalletByUserId(100L)).thenReturn(testWalletDTO);
                when(walletService.getAllLedgerEntries(1L)).thenReturn(List.of(entry));

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet/ledger/all")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("POST /wallet/rebuild - Should trigger wallet rebuild")
        void rebuildWallet_ShouldTriggerRebuild() throws Exception {
                // Arrange
                doNothing().when(walletService).rebuildWalletStateFromLedger(100L);

                // Act & Assert
                mockMvc.perform(post("/api/v1/funds/wallet/rebuild")
                                .with(jwt().jwt(mockJwt)))
                                .andExpect(status().isOk());

                verify(walletService).rebuildWalletStateFromLedger(100L);
        }

        @Test
        @DisplayName("Should extract userId from JWT Number claim")
        void extractUserId_NumberClaim_ShouldExtract() throws Exception {
                // Arrange - userId as Number (Long)
                Jwt jwtWithNumber = Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .claim("userId", 200L)
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();

                when(walletService.getWalletByUserId(200L)).thenReturn(testWalletDTO);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet")
                                .with(jwt().jwt(jwtWithNumber)))
                                .andExpect(status().isOk());

                verify(walletService).getWalletByUserId(200L);
        }

        @Test
        @DisplayName("Should extract userId from JWT String claim")
        void extractUserId_StringClaim_ShouldExtract() throws Exception {
                // Arrange - userId as String
                Jwt jwtWithString = Jwt.withTokenValue("token")
                                .header("alg", "RS256")
                                .claim("userId", "300")
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();

                when(walletService.getWalletByUserId(300L)).thenReturn(testWalletDTO);

                // Act & Assert
                mockMvc.perform(get("/api/v1/funds/wallet")
                                .with(jwt().jwt(jwtWithString)))
                                .andExpect(status().isOk());

                verify(walletService).getWalletByUserId(300L);
        }
}
