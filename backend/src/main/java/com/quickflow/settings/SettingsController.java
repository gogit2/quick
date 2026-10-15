package com.quickflow.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "getSettings", summary = "Get the settings of the single user")
    @ApiResponse(responseCode = "200", description = "Settings")
    public Settings get() {
        return service.get();
    }

    @PutMapping
    @Operation(operationId = "updateSettings", summary = "Replace the settings",
            description = "Invalid time zone ids and unknown start pages are rejected with 400.")
    @ApiResponse(responseCode = "200", description = "Saved settings")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public Settings update(@RequestBody SettingsUpdate body) {
        return service.update(body);
    }
}
