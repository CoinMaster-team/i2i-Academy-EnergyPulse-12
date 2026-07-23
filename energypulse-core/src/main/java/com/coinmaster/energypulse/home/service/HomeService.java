package com.coinmaster.energypulse.home.service;

import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
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

    @Transactional
    public HomeResponse addAppliance(
            UUID homeId,
            CreateApplianceRequest request) {
        Home home = homeRepository.findById(homeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HOME_NOT_FOUND",
                        "Home not found: " + homeId));

        validateApplianceRange(request);

        String normalizedName = normalizeApplianceName(request.name());
        boolean duplicateName = home.getAppliances()
                .stream()
                .map(appliance -> normalizeApplianceName(appliance.getName()))
                .anyMatch(normalizedName::equals);

        if (duplicateName) {
            throw new BusinessRuleException(
                    "DUPLICATE_APPLIANCE_NAME",
                    "Appliance names must be unique within a home.");
        }

        home.addAppliance(
                request.name(),
                request.safeLimitWatt(),
                request.simulationMinWatt(),
                request.simulationMaxWatt());

        Home savedHome = homeRepository.saveAndFlush(home);
        registrationPublisher.publish(createRegistrationEvent(savedHome));

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
            validateApplianceRange(appliance);

            String normalizedName = normalizeApplianceName(appliance.name());

            if (!applianceNames.add(normalizedName)) {
                throw new BusinessRuleException(
                        "DUPLICATE_APPLIANCE_NAME",
                        "Appliance names must be unique within a home.");
            }
        }
    }

    private void validateApplianceRange(CreateApplianceRequest appliance) {
        if (appliance.simulationMaxWatt()
                .compareTo(appliance.simulationMinWatt()) <= 0) {
            throw new BusinessRuleException(
                    "INVALID_SIMULATION_RANGE",
                    "Simulation maximum watt must be greater than minimum watt.");
        }
    }

    private String normalizeApplianceName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
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
                home.getEnergyQuotaKwh(),
                home.getBudgetLimit(),
                home.getAccumulatedEnergyKwh(),
                home.getAccumulatedCost(),
                applianceEvents);
    }
}
