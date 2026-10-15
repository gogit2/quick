package com.quickflow.settings;

/** Request body of PUT /api/settings (contract schema SettingsUpdate). Validated by SettingsService. */
public record SettingsUpdate(
        String displayName,
        String email,
        String timeZone,
        Boolean notificationsEnabled,
        StartPage defaultStartPage) {
}
