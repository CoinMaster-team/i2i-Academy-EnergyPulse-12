package com.coinmaster.energypulse.home.service;

import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.dto.CreateApplianceRequest;
import com.coinmaster.energypulse.home.dto.CreateHomeRequest;
import com.coinmaster.energypulse.home.dto.HomeResponse;
import com.coinmaster.energypulse.home.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.home.event.HomeRegistrationPublisher;
import com.coinmaster.energypulse.home.mapper.HomeMapper;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private HomeMapper homeMapper;

    @Mock
    private HomeRegistrationPublisher registrationPublisher;

    @InjectMocks
    private HomeService homeService;

    @Test
    void shouldCreateHomeAndPublishRegistrationEvent() {
        CreateHomeRequest request = validRequest();

        HomeResponse expectedResponse = mock(HomeResponse.class);

        when(homeRepository.saveAndFlush(any(Home.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(homeMapper.toResponse(any(Home.class)))
                .thenReturn(expectedResponse);

        HomeResponse actualResponse = homeService.createHome(request);

        assertSame(expectedResponse, actualResponse);

        verify(homeRepository).saveAndFlush(any(Home.class));

        ArgumentCaptor<HomeRegistrationEvent> eventCaptor = ArgumentCaptor.forClass(HomeRegistrationEvent.class);

        verify(registrationPublisher).publish(eventCaptor.capture());

        HomeRegistrationEvent event = eventCaptor.getValue();

        assertEquals("Test Home", event.homeName());
        assertEquals(1, event.schemaVersion());
        assertEquals(0, event.energyQuotaKwh().compareTo(new BigDecimal("500.0000")));
        assertEquals(0, event.budgetLimit().compareTo(new BigDecimal("1000.00")));
        assertEquals(1, event.appliances().size());
        assertEquals(
                "Refrigerator",
                event.appliances().get(0).name());

    }

    @Test
    void shouldRejectPenaltyTariffNotGreaterThanBaseTariff() {
        CreateHomeRequest request = new CreateHomeRequest(
                "Invalid Tariff Home",
                "invalid@energypulse.com",
                new BigDecimal("500.0000"),
                new BigDecimal("1000.00"),
                new BigDecimal("5.000000"),
                new BigDecimal("3.000000"),
                List.of(validAppliance("Refrigerator")));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> homeService.createHome(request));

        assertEquals("INVALID_TARIFF", exception.getCode());

        verifyNoInteractions(
                homeRepository,
                homeMapper,
                registrationPublisher);
    }

    @Test
    void shouldRejectDuplicateApplianceNamesIgnoringCaseAndSpaces() {
        CreateHomeRequest request = new CreateHomeRequest(
                "Duplicate Appliance Home",
                "duplicate@energypulse.com",
                new BigDecimal("500.0000"),
                new BigDecimal("1000.00"),
                new BigDecimal("2.000000"),
                new BigDecimal("4.000000"),
                List.of(
                        validAppliance("Refrigerator"),
                        validAppliance(" refrigerator ")));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> homeService.createHome(request));

        assertEquals(
                "DUPLICATE_APPLIANCE_NAME",
                exception.getCode());

        verifyNoInteractions(
                homeRepository,
                homeMapper,
                registrationPublisher);
    }

    @Test
    void shouldAddApplianceAndRepublishHomeTopology() {
        UUID homeId = UUID.randomUUID();
        Home home = validHome();
        home.addAppliance(
                "Refrigerator",
                new BigDecimal("500.00"),
                new BigDecimal("100.00"),
                new BigDecimal("450.00"));
        HomeResponse expectedResponse = mock(HomeResponse.class);

        when(homeRepository.findById(homeId)).thenReturn(Optional.of(home));
        when(homeRepository.saveAndFlush(home)).thenReturn(home);
        when(homeMapper.toResponse(home)).thenReturn(expectedResponse);

        HomeResponse actualResponse = homeService.addAppliance(
                homeId,
                validAppliance("Washing Machine"));

        assertSame(expectedResponse, actualResponse);
        assertEquals(2, home.getAppliances().size());

        ArgumentCaptor<HomeRegistrationEvent> eventCaptor =
                ArgumentCaptor.forClass(HomeRegistrationEvent.class);
        verify(registrationPublisher).publish(eventCaptor.capture());
        assertEquals(2, eventCaptor.getValue().appliances().size());
    }

    @Test
    void shouldRejectDuplicateApplianceWhenAddingToExistingHome() {
        UUID homeId = UUID.randomUUID();
        Home home = validHome();
        home.addAppliance(
                "Refrigerator",
                new BigDecimal("500.00"),
                new BigDecimal("100.00"),
                new BigDecimal("450.00"));

        when(homeRepository.findById(homeId)).thenReturn(Optional.of(home));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> homeService.addAppliance(
                        homeId,
                        validAppliance(" refrigerator ")));

        assertEquals("DUPLICATE_APPLIANCE_NAME", exception.getCode());
        verifyNoInteractions(homeMapper, registrationPublisher);
    }

    private CreateHomeRequest validRequest() {
        return new CreateHomeRequest(
                "Test Home",
                "test@energypulse.com",
                new BigDecimal("500.0000"),
                new BigDecimal("1000.00"),
                new BigDecimal("2.000000"),
                new BigDecimal("4.000000"),
                List.of(validAppliance("Refrigerator")));
    }

    private CreateApplianceRequest validAppliance(String name) {
        return new CreateApplianceRequest(
                name,
                new BigDecimal("500.00"),
                new BigDecimal("100.00"),
                new BigDecimal("650.00"));
    }

    private Home validHome() {
        return new Home(
                "Test Home",
                "test@energypulse.com",
                new BigDecimal("500.0000"),
                new BigDecimal("1000.00"),
                new BigDecimal("2.000000"),
                new BigDecimal("4.000000"));
    }
}
