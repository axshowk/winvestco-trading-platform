package in.winvestco.portfolio_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Holding entity representing a stock holding within a portfolio.
 * Each holding tracks quantity, average price, and total invested amount.
 */
@Entity
@Table(name = "holdings", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_holdings_portfolio_symbol", columnNames = {"portfolio_id", "symbol"})
    },
    indexes = {
        @Index(name = "idx_holdings_portfolio_id", columnList = "portfolio_id"),
        @Index(name = "idx_holdings_symbol", columnList = "symbol")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "portfolio")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Holding implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    @NotBlank
    @Size(max = 20)
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Size(max = 100)
    @Column(name = "company_name", length = 100)
    private String companyName;

    @Size(max = 10)
    @Column(name = "exchange", length = 10)
    @Builder.Default
    private String exchange = "NSE";

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @NotNull
    @Positive
    @Column(name = "average_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal averagePrice;

    @Column(name = "total_invested", precision = 18, scale = 4)
    private BigDecimal totalInvested;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Calculate and update total invested amount
     */
    public void calculateTotalInvested() {
        if (quantity != null && averagePrice != null) {
            this.totalInvested = quantity.multiply(averagePrice);
        }
    }
}
