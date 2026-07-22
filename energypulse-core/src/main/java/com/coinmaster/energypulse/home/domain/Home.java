package com.coinmaster.energypulse.home.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "homes")
public class Home {

    @OneToMany(mappedBy = "home", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appliance> appliances = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    @Column(name = "energy_quota_kwh", nullable = false, precision = 14, scale = 4)
    private BigDecimal energyQuotaKwh;

    @Column(name = "budget_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "base_tariff", nullable = false, precision = 14, scale = 6)
    private BigDecimal baseTariff;

    @Column(name = "penalty_tariff", nullable = false, precision = 14, scale = 6)
    private BigDecimal penaltyTariff;

    @Column(name = "accumulated_energy_kwh", nullable = false, precision = 18, scale = 6)
    private BigDecimal accumulatedEnergyKwh;

    @Column(name = "accumulated_cost", nullable = false, precision = 18, scale = 6)
    private BigDecimal accumulatedCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Home() {
        // Required by JPA.
    }

    public Home(
            String name,
            String contactEmail,
            BigDecimal energyQuotaKwh,
            BigDecimal budgetLimit,
            BigDecimal baseTariff,
            BigDecimal penaltyTariff) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.energyQuotaKwh = energyQuotaKwh;
        this.budgetLimit = budgetLimit;
        this.baseTariff = baseTariff;
        this.penaltyTariff = penaltyTariff;
        this.accumulatedEnergyKwh = BigDecimal.ZERO;
        this.accumulatedCost = BigDecimal.ZERO;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;

        if (this.accumulatedEnergyKwh == null) {
            this.accumulatedEnergyKwh = BigDecimal.ZERO;
        }

        if (this.accumulatedCost == null) {
            this.accumulatedCost = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Appliance addAppliance(
            String name,
            BigDecimal safeLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt) {
        Appliance appliance = new Appliance(
                this,
                name,
                safeLimitWatt,
                simulationMinWatt,
                simulationMaxWatt);

        this.appliances.add(appliance);
        return appliance;
    }

    public void updateAccumulatedUsage(
            BigDecimal accumulatedEnergyKwh,
            BigDecimal accumulatedCost) {
        this.accumulatedEnergyKwh = accumulatedEnergyKwh;
        this.accumulatedCost = accumulatedCost;
    }

    public List<Appliance> getAppliances() {
        return Collections.unmodifiableList(appliances);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public BigDecimal getEnergyQuotaKwh() {
        return energyQuotaKwh;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public BigDecimal getBaseTariff() {
        return baseTariff;
    }

    public BigDecimal getPenaltyTariff() {
        return penaltyTariff;
    }

    public BigDecimal getAccumulatedEnergyKwh() {
        return accumulatedEnergyKwh;
    }

    public BigDecimal getAccumulatedCost() {
        return accumulatedCost;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}