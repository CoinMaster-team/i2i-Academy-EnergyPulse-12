package com.coinmaster.energypulse.consumption.controller;

import com.coinmaster.energypulse.consumption.dto.DailyConsumptionResponse;
import com.coinmaster.energypulse.consumption.service.ConsumptionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
@Tag(name = "Consumption History", description = "PostgreSQL-backed daily energy consumption history.")
public class ConsumptionHistoryController {

    private final ConsumptionHistoryService consumptionHistoryService;

    public ConsumptionHistoryController(
            ConsumptionHistoryService consumptionHistoryService) {
        this.consumptionHistoryService = consumptionHistoryService;
    }

    @GetMapping("/{homeId}/consumption-history")
    @Operation(summary = "Get daily consumption history", description = """
            Returns the last consumption snapshot for each day in the
            requested date range. If dates are omitted, the most recent
            seven-day UTC range is used.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily consumption history returned."),
            @ApiResponse(responseCode = "400", description = "Invalid date range."),
            @ApiResponse(responseCode = "404", description = "Home not found.")
    })
    public ResponseEntity<List<DailyConsumptionResponse>> getDailyHistory(
            @Parameter(description = "Registered home UUID.", required = true) @PathVariable UUID homeId,

            @Parameter(description = "Inclusive start date in YYYY-MM-DD format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Inclusive end date in YYYY-MM-DD format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate resolvedTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);

        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(6);

        List<DailyConsumptionResponse> response = consumptionHistoryService.getDailyHistory(
                homeId,
                resolvedFrom,
                resolvedTo);

        return ResponseEntity.ok(response);
    }
}