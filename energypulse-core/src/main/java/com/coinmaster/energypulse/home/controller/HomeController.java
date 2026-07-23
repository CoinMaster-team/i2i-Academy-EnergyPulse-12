package com.coinmaster.energypulse.home.controller;

import com.coinmaster.energypulse.home.dto.CreateApplianceRequest;
import com.coinmaster.energypulse.home.dto.CreateHomeRequest;
import com.coinmaster.energypulse.home.dto.HomeResponse;
import com.coinmaster.energypulse.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/homes")
@Tag(name = "Home Management", description = "Operations for registering homes and appliances.")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @PostMapping
    @Operation(summary = "Register a new home", description = """
            Registers a home together with its appliance topology,
            persists the data in PostgreSQL and prepares an asset
            registration event for Kafka publication.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Home registered successfully."),
            @ApiResponse(responseCode = "400", description = "Request validation or business rule failure."),
            @ApiResponse(responseCode = "409", description = "Request conflicts with existing data.")
    })
    public ResponseEntity<HomeResponse> createHome(
            @Valid @RequestBody CreateHomeRequest request) {
        HomeResponse response = homeService.createHome(request);

        URI location = URI.create("/api/homes/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PostMapping("/{homeId}/appliances")
    @Operation(
            summary = "Add an appliance",
            description = "Adds an appliance to an existing home and republishes its simulator topology.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appliance added successfully."),
            @ApiResponse(responseCode = "400", description = "Request validation or business rule failure."),
            @ApiResponse(responseCode = "404", description = "Home not found.")
    })
    public ResponseEntity<HomeResponse> addAppliance(
            @PathVariable UUID homeId,
            @Valid @RequestBody CreateApplianceRequest request) {
        return ResponseEntity.ok(homeService.addAppliance(homeId, request));
    }
}
