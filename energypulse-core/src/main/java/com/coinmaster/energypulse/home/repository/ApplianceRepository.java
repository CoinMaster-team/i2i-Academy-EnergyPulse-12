package com.coinmaster.energypulse.home.repository;

import com.coinmaster.energypulse.home.domain.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplianceRepository
        extends JpaRepository<Appliance, UUID> {

    List<Appliance> findAllByHome_Id(UUID homeId);
}