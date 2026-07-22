package com.coinmaster.energypulse.consumption.domain;

import com.coinmaster.energypulse.home.domain.Home;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "consumption_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uq_consumption_snapshots_home_time", columnNames = { "home_id", "captured_at" })
})
public class ConsumptionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_id", nullable = false, updatable = false)
    private Home home;

    @Column(name = "total_energy_kwh", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalEnergyKwh;

    @Column(name = "total_cost", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalCost;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private OffsetDateTime capturedAt;

    protected ConsumptionSnapshot() {
    }

    public ConsumptionSnapshot(
            Home home,
            BigDecimal totalEnergyKwh,
            BigDecimal totalCost,
            OffsetDateTime capturedAt) {
        this.home = home;
        this.totalEnergyKwh = totalEnergyKwh;
        this.totalCost = totalCost;
        this.capturedAt = capturedAt;
    }

    @PrePersist
    void onCreate() {
        if (capturedAt == null) {
            capturedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public UUID getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public BigDecimal getTotalEnergyKwh() {
        return totalEnergyKwh;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }
}