package in.winvestco.ledger_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.repository.LedgerEntryRepository;
import in.winvestco.ledger_service.testdata.LedgerTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LedgerController Integration Tests")
class LedgerControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        
        ledgerEntryRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should record new ledger entry")
    void recordEntry_ShouldCreateAndReturnEntry() throws Exception {
        // Given
        CreateLedgerEntryRequest request = LedgerTestDataFactory.createTestRequest();
        
        // When & Then
        mockMvc.perform(post("/api/v1/ledger/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value(request.getWalletId()))
                .andExpect(jsonPath("$.entryType").value(request.getEntryType().name()))
                .andExpect(jsonPath("$.amount").value(request.getAmount().doubleValue()))
                .andExpect(jsonPath("$.balanceBefore").value(request.getBalanceBefore().doubleValue()))
                .andExpect(jsonPath("$.balanceAfter").value(request.getBalanceAfter().doubleValue()))
                .andExpect(jsonPath("$.referenceId").value(request.getReferenceId()))
                .andExpect(jsonPath("$.referenceType").value(request.getReferenceType()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should return 400 for invalid request data")
    void recordEntry_ShouldReturnBadRequestForInvalidData() throws Exception {
        // Given
        CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                .walletId(null) // Invalid: null wallet ID
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("-100")) // Invalid: negative amount
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("-100"))
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/ledger/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get entry by ID")
    void getEntry_ShouldReturnEntry() throws Exception {
        // Given
        LedgerEntry savedEntry = ledgerEntryRepository.save(LedgerTestDataFactory.createTestEntry());
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/entries/{id}", savedEntry.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedEntry.getId()))
                .andExpect(jsonPath("$.walletId").value(savedEntry.getWalletId()))
                .andExpect(jsonPath("$.entryType").value(savedEntry.getEntryType().name()))
                .andExpect(jsonPath("$.amount").value(savedEntry.getAmount().doubleValue()));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should return 404 when entry not found")
    void getEntry_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/entries/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get paginated wallet entries")
    void getWalletEntries_ShouldReturnPaginatedResults() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 15);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}", 1L)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get all wallet entries")
    void getAllWalletEntries_ShouldReturnAllEntries() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 5);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/all", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].walletId").value(1L));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get latest wallet entry")
    void getLatestEntry_ShouldReturnMostRecentEntry() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/latest", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(1L))
                .andExpect(jsonPath("$.entryType").exists())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should return 404 when no entries exist for wallet")
    void getLatestEntry_ShouldReturnNotFoundForEmptyWallet() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/latest", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get entries by type for wallet")
    void getEntriesByType_ShouldReturnFilteredEntries() throws Exception {
        // Given
        List<LedgerEntry> depositEntries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3, LedgerEntryType.DEPOSIT);
        List<LedgerEntry> withdrawalEntries = LedgerTestDataFactory.createTestEntriesSeries(1L, 2, LedgerEntryType.WITHDRAWAL);
        ledgerEntryRepository.saveAll(depositEntries);
        ledgerEntryRepository.saveAll(withdrawalEntries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/type/{entryType}", 1L, LedgerEntryType.DEPOSIT)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].entryType", everyItem(equalTo("DEPOSIT"))));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get entries by reference ID")
    void getEntriesByReference_ShouldReturnMatchingEntries() throws Exception {
        // Given
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry();
        entry.setReferenceId("REF-12345");
        ledgerEntryRepository.save(entry);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/reference/{referenceId}", "REF-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referenceId").value("REF-12345"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should sum amounts by type for reconciliation")
    void getSumByType_ShouldReturnCorrectSum() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3, LedgerEntryType.DEPOSIT);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/sum/{entryType}", 1L, LedgerEntryType.DEPOSIT))
                .andExpect(status().isOk())
                .andExpect(content().string("300.0000")); // 3 entries * 100 each
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should count entries for wallet")
    void countEntries_ShouldReturnCorrectCount() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 5);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/count", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should get audit entries in date range")
    void getAuditEntries_ShouldReturnEntriesInDateRange() throws Exception {
        // Given
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry();
        entry.setCreatedAt(Instant.now());
        ledgerEntryRepository.save(entry);
        
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now().plusSeconds(3600);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/audit")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should get wallet audit entries in date range")
    void getWalletAuditEntries_ShouldReturnEntriesInDateRange() throws Exception {
        // Given
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry();
        ledgerEntryRepository.save(entry);
        
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now().plusSeconds(3600);
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/audit", 1L)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Should get wallet balance at specific time")
    void getBalanceAt_ShouldReturnBalanceAtTimestamp() throws Exception {
        // Given
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry();
        entry.setCreatedAt(Instant.now().minusSeconds(1800)); // 30 minutes ago
        ledgerEntryRepository.save(entry);
        
        Instant timestamp = Instant.now();
        
        // When & Then
        mockMvc.perform(get("/api/v1/ledger/wallet/{walletId}/balance-at", 1L)
                .param("timestamp", timestamp.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.0000"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("Should rebuild wallet state from all events")
    void rebuildState_ShouldRebuildWalletState() throws Exception {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createMixedTransactionHistory(1L);
        ledgerEntryRepository.saveAll(entries);
        
        // When & Then
        mockMvc.perform(post("/api/v1/ledger/wallet/{walletId}/rebuild", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("9590.0000")); // Expected balance from mixed transactions
    }

    @Test
    @DisplayName("Should return 401 for unauthorized access")
    void endpoints_ShouldRequireAuthentication() throws Exception {
        // Test various endpoints without authentication
        mockMvc.perform(get("/api/v1/ledger/entries/1"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/v1/ledger/wallet/1"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/api/v1/ledger/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
