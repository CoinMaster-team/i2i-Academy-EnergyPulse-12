package com.coinmaster.energypulse.home.service;

import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.dto.CreateApplianceRequest;
import com.coinmaster.energypulse.home.dto.CreateHomeRequest;
import com.coinmaster.energypulse.home.dto.HomeResponse;
import com.coinmaster.energypulse.home.event.ApplianceRegistrationEvent;
import com.coinmaster.energypulse.home.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.home.event.HomeRegistrationPublisher;
import com.coinmaster.energypulse.home.mapper.HomeMapper;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class HomeService {

    private final HomeRepository homeRepository;
    private final HomeMapper homeMapper;
    private final HomeRegistrationPublisher registrationPublisher;

    public HomeService(
            HomeRepository homeRepository,
            HomeMapper homeMapper,
            HomeRegistrationPublisher registrationPublisher) {
        this.homeRepository = homeRepository;
        this.homeMapper = homeMapper;
        this.registrationPublisher = registrationPublisher;
    }

    @Transactional
    public HomeResponse createHome(CreateHomeRequest request) {
        validateTariffs(request);
        validateAppliances(request.appliances());

        Home home = new Home(
                request.name(),
                request.contactEmail(),
                request.energyQuotaKwh(),
                request.budgetLimit(),
                request.baseTariff(),
                request.penaltyTariff());

        request.appliances().forEach(appliance -> home.addAppliance(
                appliance.name(),
                appliance.safeLimitWatt(),
                appliance.simulationMinWatt(),
                appliance.simulationMaxWatt()));

        Home savedHome = homeRepository.saveAndFlush(home);

        HomeRegistrationEvent event = createRegistrationEvent(savedHome);
        registrationPublisher.publish(event);

        return homeMapper.toResponse(savedHome);
    }

    private void validateTariffs(CreateHomeRequest request) {
        if (request.penaltyTariff().compareTo(request.baseTariff()) <= 0) {
            throw new BusinessRuleException(
                    "INVALID_TARIFF",
                    "Penalty tariff must be greater than base tariff.");
        }
    }

    private void validateAppliances(List<CreateApplianceRequest> appliances) {
        Set<String> applianceNames = new HashSet<>();

        for (CreateApplianceRequest appliance : appliances) {
            if (appliance.simulationMaxWatt()
                    .compareTo(appliance.simulationMinWatt()) <= 0) {
                throw new BusinessRuleException(
                        "INVALID_SIMULATION_RANGE",
                        "Simulation maximum watt must be greater than minimum watt.");
            }

            String normalizedName = appliance.name().trim().toLowerCase(Locale.ROOT);

            if (!applianceNames.add(normalizedName)) {
                throw new BusinessRuleException(
                        "DUPLICATE_APPLIANCE_NAME",
                        "Appliance names must be unique within a home.");
            }
        }
    }

    private HomeRegistrationEvent createRegistrationEvent(Home home) {
        List<ApplianceRegistrationEvent> applianceEvents = home.getAppliances()
                .stream()
                .map(appliance -> new ApplianceRegistrationEvent(
                        appliance.getId(),
                        appliance.getName(),
                        appliance.getSafeLimitWatt(),
                        appliance.getSimulationMinWatt(),
                        appliance.getSimulationMaxWatt()))
                .toList();

        return new HomeRegistrationEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                home.getId(),
                home.getName(),
                applianceEvents);
    }
}