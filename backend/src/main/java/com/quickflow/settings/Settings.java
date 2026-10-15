package com.quickflow.settings;

/** Response body of GET and PUT /api/settings (contract schema Settings). */
public record Settings(
        String displayName,
        String email,
        String timeZone,
        boolean notificationsEnabled,
        StartPage defaultStartPage,
        boolean configured) {
}
