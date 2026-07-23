package com.coinmaster.energypulse.notification.controller;

import com.coinmaster.energypulse.notification.dto.NotificationResponse;
import com.coinmaster.energypulse.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Console", description = "AI recommendation and email delivery history.")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns notification history ordered from newest to oldest.")
    @ApiResponse(responseCode = "200", description = "Notification history returned.")
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications());
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification details returned."),
            @ApiResponse(responseCode = "404", description = "Notification not found.")
    })
    public ResponseEntity<NotificationResponse> getNotification(
            @Parameter(description = "Notification UUID.", required = true)
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.getNotification(notificationId));
    }
}
