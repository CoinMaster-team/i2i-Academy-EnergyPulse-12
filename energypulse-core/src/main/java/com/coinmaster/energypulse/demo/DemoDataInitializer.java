package com.coinmaster.energypulse.demo;

import com.coinmaster.energypulse.home.dto.CreateApplianceRequest;
import com.coinmaster.energypulse.home.dto.CreateHomeRequest;
import com.coinmaster.energypulse.home.dto.HomeResponse;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import com.coinmaster.energypulse.home.service.HomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@Order(10)
@ConditionalOnProperty(
        prefix = "app.demo-data",
        name = "enabled",
        havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DemoDataInitializer.class);

    private final HomeRepository homeRepository;
    private final HomeService homeService;
    private final JdbcTemplate jdbcTemplate;
    private final String contactEmail;

    public DemoDataInitializer(
            HomeRepository homeRepository,
            HomeService homeService,
            JdbcTemplate jdbcTemplate,
            @Value("${app.demo-data.contact-email:demo@energypulse.local}") String contactEmail) {
        this.homeRepository = homeRepository;
        this.homeService = homeService;
        this.jdbcTemplate = jdbcTemplate;
        this.contactEmail = contactEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (homeRepository.count() > 0) {
            LOGGER.info("Demo data initialization skipped because homes already exist.");
            return;
        }

        HomeResponse homeA = homeService.createHome(new CreateHomeRequest(
                "Home A",
                contactEmail,
                new BigDecimal("350.0000"),
                new BigDecimal("900.00"),
                new BigDecimal("2.250000"),
                new BigDecimal("3.750000"),
                List.of(
                        appliance("Refrigerator", "320", "120", "240"),
                        appliance("Washing Machine", "2200", "350", "1800"),
                        appliance("Air Conditioner", "2400", "650", "1950"))));

        HomeResponse homeB = homeService.createHome(new CreateHomeRequest(
                "Home B",
                contactEmail,
                new BigDecimal("280.0000"),
                new BigDecimal("720.00"),
                new BigDecimal("2.100000"),
                new BigDecimal("3.500000"),
                List.of(
                        appliance("Television", "450", "80", "280"),
                        appliance("Dishwasher", "2100", "400", "1700"),
                        appliance("Water Heater", "2600", "900", "2200"))));

        seedHistory(
                homeA.id(),
                new BigDecimal("2.250000"),
                List.of("7.20", "8.10", "6.80", "9.40", "8.70", "10.20", "9.10"));
        seedHistory(
                homeB.id(),
                new BigDecimal("2.100000"),
                List.of("5.40", "6.10", "5.90", "7.20", "6.80", "7.50", "6.90"));

        LOGGER.info("Demo homes and seven-day consumption history initialized.");
    }

    private CreateApplianceRequest appliance(
            String name,
            String safeLimitWatt,
            String simulationMinWatt,
            String simulationMaxWatt) {
        return new CreateApplianceRequest(
                name,
                new BigDecimal(safeLimitWatt),
                new BigDecimal(simulationMinWatt),
                new BigDecimal(simulationMaxWatt));
    }

    private void seedHistory(
            UUID homeId,
            BigDecimal tariff,
            List<String> dailyEnergyValues) {
        BigDecimal cumulativeEnergy = BigDecimal.ZERO;
        BigDecimal cumulativeCost = BigDecimal.ZERO;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate firstDay = now.toLocalDate()
                .minusDays(dailyEnergyValues.size() - 1L);

        for (int index = 0; index < dailyEnergyValues.size(); index++) {
            BigDecimal dailyEnergy = new BigDecimal(dailyEnergyValues.get(index));
            cumulativeEnergy = cumulativeEnergy.add(dailyEnergy);
            cumulativeCost = cumulativeCost.add(dailyEnergy.multiply(tariff));

            LocalDate snapshotDay = firstDay.plusDays(index);
            OffsetDateTime capturedAt = snapshotDay.equals(now.toLocalDate())
                    ? now.minusMinutes(1)
                    : snapshotDay.atTime(23, 0).atOffset(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            INSERT INTO consumption_snapshots (
                                home_id,
                                total_energy_kwh,
                                total_cost,
                                captured_at
                            ) VALUES (?, ?, ?, ?)
                            """,
                    homeId,
                    cumulativeEnergy,
                    cumulativeCost,
                    capturedAt);
        }

        jdbcTemplate.update(
                """
                        UPDATE homes
                        SET accumulated_energy_kwh = ?,
                            accumulated_cost = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                cumulativeEnergy,
                cumulativeCost,
                homeId);
    }
}
