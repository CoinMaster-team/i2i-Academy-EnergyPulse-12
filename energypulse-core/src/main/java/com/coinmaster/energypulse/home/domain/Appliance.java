package com.coinmaster.energypulse.home.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "appliances", uniqueConstraints = {
        @UniqueConstraint(name = "uq_appliances_home_name", columnNames = { "home_id", "name" })
})
public class Appliance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false, updatable = false)
    private Home home;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "safe_limit_watt", nullable = false, precision = 12, scale = 2)
    private BigDecimal safeLimitWatt;

    @Column(name = "simulation_min_watt", nullable = false, precision = 12, scale = 2)
    private BigDecimal simulationMinWatt;

    @Column(name = "simulation_max_watt", nullable = false, precision = 12, scale = 2)
    private BigDecimal simulationMaxWatt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Appliance() {
        // Required by JPA.
    }

    public Appliance(
            Home home,
            String name,
            BigDecimal safeLimitWatt,
            BigDecimal simulationMinWatt,
            BigDecimal simulationMaxWatt) {
        this.home = home;
        this.name = name;
        this.safeLimitWatt = safeLimitWatt;
        this.simulationMinWatt = simulationMinWatt;
        this.simulationMaxWatt = simulationMaxWatt;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSafeLimitWatt() {
        return safeLimitWatt;
    }

    public BigDecimal getSimulationMinWatt() {
        return simulationMinWatt;
    }

    public BigDecimal getSimulationMaxWatt() {
        return simulationMaxWatt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}