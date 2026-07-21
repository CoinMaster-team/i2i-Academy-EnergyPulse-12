package com.coinmaster.energypulse.home.mapper;

import com.coinmaster.energypulse.home.domain.Appliance;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.dto.ApplianceResponse;
import com.coinmaster.energypulse.home.dto.HomeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HomeMapper {

    public HomeResponse toResponse(Home home) {
        List<ApplianceResponse> appliances = home.getAppliances()
                .stream()
                .map(this::toApplianceResponse)
                .toList();

        return new HomeResponse(
                home.getId(),
                home.getName(),
                home.getContactEmail(),
                home.getEnergyQuotaKwh(),
                home.getBudgetLimit(),
                home.getBaseTariff(),
                home.getPenaltyTariff(),
                home.getAccumulatedEnergyKwh(),
                home.getAccumulatedCost(),
                appliances,
                home.getCreatedAt(),
                home.getUpdatedAt());
    }

    private ApplianceResponse toApplianceResponse(Appliance appliance) {
        return new ApplianceResponse(
                appliance.getId(),
                appliance.getName(),
                appliance.getSafeLimitWatt(),
                appliance.getSimulationMinWatt(),
                appliance.getSimulationMaxWatt());
    }
}