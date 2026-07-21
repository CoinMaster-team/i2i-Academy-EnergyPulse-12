package com.coinmaster.energypulse.consumption.repository;

import com.coinmaster.energypulse.consumption.domain.ConsumptionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsumptionSnapshotRepository
        extends JpaRepository<ConsumptionSnapshot, UUID> {

    List<ConsumptionSnapshot> findAllByHome_IdAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
            UUID homeId,
            OffsetDateTime from,
            OffsetDateTime to);
}