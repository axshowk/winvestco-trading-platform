package in.winvestco.funds_service.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LedgerClientIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private LedgerClient ledgerClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        registry.add("ledger-service.ribbon.listOfServers", () -> "localhost:" + wireMockServer.port());
        registry.add("feign.client.config.ledger-service.url", () -> "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
    }

    @Test
    void recordEntry_ShouldCreateLedgerEntry() {
        String jsonResponse = """
                {
                    "id": 1,
                    "walletId": 100,
                    "entryType": "CREDIT",
                    "amount": 500.00,
                    "balanceBefore": 1000.00,
                    "balanceAfter": 1500.00,
                    "referenceId": "ref-123",
                    "referenceType": "DEPOSIT",
                    "description": "Test deposit",
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;

        wireMockServer.stubFor(post("/api/v1/ledger/entries")
                .withHeader("X-Idempotency-Key", equalTo("idemp-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                .walletId(100L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .referenceId("ref-123")
                .referenceType("DEPOSIT")
                .description("Test deposit")
                .build();

        LedgerEntryDTO result = ledgerClient.recordEntry("idemp-123", request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getWalletId());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
    }

    @Test
    void getEntry_ShouldReturnEntry() {
        String jsonResponse = """
                {
                    "id": 1,
                    "walletId": 100,
                    "entryType": "CREDIT",
                    "amount": 500.00,
                    "balanceBefore": 1000.00,
                    "balanceAfter": 1500.00,
                    "referenceId": "ref-123",
                    "referenceType": "DEPOSIT",
                    "description": "Test deposit",
                    "createdAt": "2024-01-01T00:00:00Z"
                }
                """;

        wireMockServer.stubFor(get("/api/v1/ledger/entries/1")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        LedgerEntryDTO result = ledgerClient.getEntry(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ref-123", result.getReferenceId());
    }

    @Test
    void getWalletEntries_ShouldReturnPaginatedEntries() {
        String jsonResponse = """
                {
                    "content": [
                        {
                            "id": 1,
                            "walletId": 100,
                            "entryType": "CREDIT",
                            "amount": 500.00,
                            "balanceBefore": 1000.00,
                            "balanceAfter": 1500.00,
                            "referenceId": "ref-123",
                            "referenceType": "DEPOSIT",
                            "description": "Test deposit",
                            "createdAt": "2024-01-01T00:00:00Z"
                        }
                    ],
                    "totalElements": 1,
                    "totalPages": 1
                }
                """;

        wireMockServer.stubFor(get("/api/v1/ledger/wallet/100?page=0&size=50")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        var result = ledgerClient.getWalletEntries(100L, 0, 50);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllWalletEntries_ShouldReturnList() {
        String jsonResponse = """
                [
                    {
                        "id": 1,
                        "walletId": 100,
                        "entryType": "CREDIT",
                        "amount": 500.00,
                        "balanceBefore": 1000.00,
                        "balanceAfter": 1500.00,
                        "referenceId": "ref-123",
                        "referenceType": "DEPOSIT",
                        "description": "Test deposit",
                        "createdAt": "2024-01-01T00:00:00Z"
                    }
                ]
                """;

        wireMockServer.stubFor(get("/api/v1/ledger/wallet/100/all")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        var result = ledgerClient.getAllWalletEntries(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getLatestEntry_ShouldReturnLatest() {
        String jsonResponse = """
                {
                    "id": 2,
                    "walletId": 100,
                    "entryType": "DEBIT",
                    "amount": 200.00,
                    "balanceBefore": 1500.00,
                    "balanceAfter": 1300.00,
                    "referenceId": "ref-124",
                    "referenceType": "WITHDRAWAL",
                    "description": "Test withdrawal",
                    "createdAt": "2024-01-02T00:00:00Z"
                }
                """;

        wireMockServer.stubFor(get("/api/v1/ledger/wallet/100/latest")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        LedgerEntryDTO result = ledgerClient.getLatestEntry(100L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("ref-124", result.getReferenceId());
    }

    @Test
    void getEntriesByReference_ShouldReturnList() {
        String jsonResponse = """
                [
                    {
                        "id": 1,
                        "walletId": 100,
                        "entryType": "CREDIT",
                        "amount": 500.00,
                        "balanceBefore": 1000.00,
                        "balanceAfter": 1500.00,
                        "referenceId": "ref-123",
                        "referenceType": "DEPOSIT",
                        "description": "Test deposit",
                        "createdAt": "2024-01-01T00:00:00Z"
                    }
                ]
                """;

        wireMockServer.stubFor(get("/api/v1/ledger/reference/ref-123")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        var result = ledgerClient.getEntriesByReference("ref-123");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ref-123", result.get(0).getReferenceId());
    }

    @Test
    void recordEntry_WhenServiceUnavailable_ShouldFallback() {
        wireMockServer.stubFor(post("/api/v1/ledger/entries")
                .willReturn(aResponse()
                        .withStatus(503)));

        CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                .walletId(100L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .balanceBefore(new BigDecimal("1000.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .build();

        LedgerEntryDTO result = ledgerClient.recordEntry("idemp-key", request);

        assertNotNull(result);
    }
}
