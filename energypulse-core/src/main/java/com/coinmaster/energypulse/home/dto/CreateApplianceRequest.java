package com.coinmaster.energypulse.home.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApplianceRequest(

        @NotBlank(message = "Appliance name is required.") @Size(max = 120, message = "Appliance name must not exceed 120 characters.") String name,

        @NotNull(message = "Safe limit watt is required.") @Positive(message = "Safe limit watt must be greater than zero.") @Digits(integer = 10, fraction = 2, message = "Safe limit watt has an invalid number format.") BigDecimal safeLimitWatt,

        @NotNull(message = "Simulation minimum watt is required.") @PositiveOrZero(message = "Simulation minimum watt cannot be negative.") @Digits(integer = 10, fraction = 2, message = "Simulation minimum watt has an invalid number format.") BigDecimal simulationMinWatt,

        @NotNull(message = "Simulation maximum watt is required.") @Positive(message = "Simulation maximum watt must be greater than zero.") @Digits(integer = 10, fraction = 2, message = "Simulation maximum watt has an invalid number format.") BigDecimal simulationMaxWatt) {
}