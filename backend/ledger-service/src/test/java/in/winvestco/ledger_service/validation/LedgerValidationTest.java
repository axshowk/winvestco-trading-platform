package in.winvestco.ledger_service.validation;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.testdata.LedgerTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ledger Validation Tests")
class LedgerValidationTest {

    private Validator validator;
    private CreateLedgerEntryRequest validRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        validRequest = LedgerTestDataFactory.createTestRequest();
    }

    @Test
    @DisplayName("Should validate correct request")
    void validRequest_ShouldPassValidation() {
        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Should reject null wallet ID")
    void nullWalletId_ShouldFailValidation() {
        // Given
        validRequest.setWalletId(null);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("walletId") &&
                v.getMessage().contains("must not be null")));
    }

    @Test
    @DisplayName("Should reject null entry type")
    void nullEntryType_ShouldFailValidation() {
        // Given
        validRequest.setEntryType(null);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("entryType") &&
                v.getMessage().contains("must not be null")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100.00", "0.00", "-0.01"})
    @DisplayName("Should reject non-positive amounts")
    void nonPositiveAmount_ShouldFailValidation(String amount) {
        // Given
        validRequest.setAmount(new BigDecimal(amount));

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("amount") &&
                v.getMessage().contains("must be positive")));
    }

    @Test
    @DisplayName("Should reject null balance before")
    void nullBalanceBefore_ShouldFailValidation() {
        // Given
        validRequest.setBalanceBefore(null);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("balanceBefore") &&
                v.getMessage().contains("must not be null")));
    }

    @Test
    @DisplayName("Should reject null balance after")
    void nullBalanceAfter_ShouldFailValidation() {
        // Given
        validRequest.setBalanceAfter(null);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("balanceAfter") &&
                v.getMessage().contains("must not be null")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    @DisplayName("Should accept blank reference ID")
    void blankReferenceId_ShouldPassValidation(String referenceId) {
        // Given
        validRequest.setReferenceId(referenceId);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.stream().noneMatch(v -> 
                v.getPropertyPath().toString().equals("referenceId")));
    }

    @Test
    @DisplayName("Should reject overly long reference ID")
    void tooLongReferenceId_ShouldFailValidation() {
        // Given
        validRequest.setReferenceId("a".repeat(101)); // Exceeds 100 character limit

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("referenceId") &&
                v.getMessage().contains("size must be between")));
    }

    @Test
    @DisplayName("Should reject overly long reference type")
    void tooLongReferenceType_ShouldFailValidation() {
        // Given
        validRequest.setReferenceType("a".repeat(51)); // Exceeds 50 character limit

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("referenceType") &&
                v.getMessage().contains("size must be between")));
    }

    @Test
    @DisplayName("Should reject overly long description")
    void tooLongDescription_ShouldFailValidation() {
        // Given
        validRequest.setDescription("a".repeat(501)); // Exceeds 500 character limit

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("description") &&
                v.getMessage().contains("size must be between")));
    }

    @Test
    @DisplayName("Should validate decimal precision")
    void invalidDecimalPrecision_ShouldFailValidation() {
        // Given - amount with more than 4 decimal places
        validRequest.setAmount(new BigDecimal("100.12345"));

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("amount") &&
                v.getMessage().contains("numeric value out of range")));
    }

    @Test
    @DisplayName("Should validate extremely large amounts")
    void extremelyLargeAmount_ShouldFailValidation() {
        // Given - amount exceeding NUMERIC(18,4) limits
        validRequest.setAmount(new BigDecimal("99999999999999999.9999"));

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("amount") &&
                v.getMessage().contains("numeric value out of range")));
    }

    @Test
    @DisplayName("Should validate all entry types")
    void allEntryTypes_ShouldPassValidation() {
        for (LedgerEntryType entryType : LedgerEntryType.values()) {
            // Given
            CreateLedgerEntryRequest request = LedgerTestDataFactory.createTestRequest();
            request.setEntryType(entryType);

            // When
            Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.isEmpty(), 
                    "Entry type " + entryType + " should be valid: " + 
                    violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
        }
    }

    @Test
    @DisplayName("Should validate minimum amount precision")
    void minimumAmount_ShouldPassValidation() {
        // Given
        validRequest.setAmount(new BigDecimal("0.0001")); // Smallest valid amount

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should validate maximum amount precision")
    void maximumAmount_ShouldPassValidation() {
        // Given
        validRequest.setAmount(new BigDecimal("99999999999999.9999")); // Largest valid amount

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should validate negative balance values")
    void negativeBalanceValues_ShouldPassValidation() {
        // Given - negative balances are allowed (overdraft scenarios)
        validRequest.setBalanceBefore(new BigDecimal("-100.00"));
        validRequest.setBalanceAfter(new BigDecimal("-50.00"));

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should validate zero balance values")
    void zeroBalanceValues_ShouldPassValidation() {
        // Given
        validRequest.setBalanceBefore(BigDecimal.ZERO);
        validRequest.setBalanceAfter(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should validate multiple violations")
    void multipleValidationErrors_ShouldReturnAllViolations() {
        // Given
        validRequest.setWalletId(null);
        validRequest.setEntryType(null);
        validRequest.setAmount(new BigDecimal("-100.00"));
        validRequest.setBalanceBefore(null);
        validRequest.setBalanceAfter(null);
        validRequest.setReferenceId("a".repeat(101));
        validRequest.setReferenceType("a".repeat(51));
        validRequest.setDescription("a".repeat(501));

        // When
        Set<ConstraintViolation<CreateLedgerEntryRequest>> violations = validator.validate(validRequest);

        // Then
        assertEquals(8, violations.size());
        
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        
        assertTrue(violatedFields.contains("walletId"));
        assertTrue(violatedFields.contains("entryType"));
        assertTrue(violatedFields.contains("amount"));
        assertTrue(violatedFields.contains("balanceBefore"));
        assertTrue(violatedFields.contains("balanceAfter"));
        assertTrue(violatedFields.contains("referenceId"));
        assertTrue(violatedFields.contains("referenceType"));
        assertTrue(violatedFields.contains("description"));
    }

    @Test
    @DisplayName("Should validate LedgerEntry entity constraints")
    void ledgerEntryEntity_ShouldValidateConstraints() {
        // Given
        LedgerEntry entry = LedgerEntry.builder()
                .id(null)
                .walletId(null)
                .entryType(null)
                .amount(null)
                .balanceBefore(null)
                .balanceAfter(null)
                .referenceId("a".repeat(101))
                .referenceType("a".repeat(51))
                .description("a".repeat(501))
                .createdAt(null)
                .build();

        // When
        Set<ConstraintViolation<LedgerEntry>> violations = validator.validate(entry);

        // Then
        // Note: ID is auto-generated, so it can be null for new entities
        // Other required fields should be validated
        assertTrue(violations.size() >= 5); // At least walletId, entryType, amount, balanceBefore, balanceAfter
    }

    @Test
    @DisplayName("Should validate valid LedgerEntry entity")
    void validLedgerEntry_ShouldPassValidation() {
        // Given
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry();

        // When
        Set<ConstraintViolation<LedgerEntry>> violations = validator.validate(entry);

        // Then
        assertTrue(violations.isEmpty());
    }
}
