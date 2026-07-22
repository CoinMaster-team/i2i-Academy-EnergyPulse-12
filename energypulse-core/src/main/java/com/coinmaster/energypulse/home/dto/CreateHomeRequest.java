package com.coinmaster.energypulse.home.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateHomeRequest(

        @NotBlank(message = "Home name is required.") @Size(max = 120, message = "Home name must not exceed 120 characters.") String name,

        @NotBlank(message = "Contact email is required.") @Email(message = "Contact email must be valid.") @Size(max = 254, message = "Contact email must not exceed 254 characters.") String contactEmail,

        @NotNull(message = "Energy quota is required.") @Positive(message = "Energy quota must be greater than zero.") @Digits(integer = 10, fraction = 4, message = "Energy quota has an invalid number format.") BigDecimal energyQuotaKwh,

        @NotNull(message = "Budget limit is required.") @Positive(message = "Budget limit must be greater than zero.") @Digits(integer = 12, fraction = 2, message = "Budget limit has an invalid number format.") BigDecimal budgetLimit,

        @NotNull(message = "Base tariff is required.") @PositiveOrZero(message = "Base tariff cannot be negative.") @Digits(integer = 8, fraction = 6, message = "Base tariff has an invalid number format.") BigDecimal baseTariff,

        @NotNull(message = "Penalty tariff is required.") @Positive(message = "Penalty tariff must be greater than zero.") @Digits(integer = 8, fraction = 6, message = "Penalty tariff has an invalid number format.") BigDecimal penaltyTariff,

        @NotEmpty(message = "At least one appliance is required.") List<@Valid CreateApplianceRequest> appliances) {
}
