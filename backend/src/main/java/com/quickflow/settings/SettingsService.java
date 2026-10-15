package com.quickflow.settings;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.FieldError;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for the single-user settings. Plain class: unit-testable without Spring. */
@Service
public class SettingsService {

    static final long SETTINGS_ID = 1L;
    static final int DISPLAY_NAME_MAX = 100;
    static final int EMAIL_MAX = 200;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AppSettingsRepository repository;

    public SettingsService(AppSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Settings get() {
        return toDto(load());
    }

    @Transactional
    public Settings update(SettingsUpdate update) {
        List<FieldError> errors = validate(update);
        if (!errors.isEmpty()) {
            throw new BadRequestException("Validation failed", errors);
        }
        AppSettingsEntity entity = load();
        entity.setDisplayName(update.displayName().trim());
        entity.setEmail(blankToNull(update.email()));
        entity.setTimeZone(update.timeZone());
        entity.setNotificationsEnabled(update.notificationsEnabled());
        entity.setDefaultStartPage(update.defaultStartPage());
        entity.setConfigured(true);
        return toDto(repository.save(entity));
    }

    static List<FieldError> validate(SettingsUpdate update) {
        List<FieldError> errors = new ArrayList<>();
        if (update.displayName() == null || update.displayName().isBlank()) {
            errors.add(new FieldError("displayName", "must not be blank"));
        } else if (update.displayName().trim().length() > DISPLAY_NAME_MAX) {
            errors.add(new FieldError("displayName", "must be at most 100 characters"));
        }
        String email = blankToNull(update.email());
        if (email != null && (email.length() > EMAIL_MAX || !EMAIL.matcher(email).matches())) {
            errors.add(new FieldError("email", "must be a valid email address"));
        }
        if (!isValidZone(update.timeZone())) {
            errors.add(new FieldError("timeZone", "must be a valid IANA time zone id"));
        }
        if (update.notificationsEnabled() == null) {
            errors.add(new FieldError("notificationsEnabled", "must not be null"));
        }
        if (update.defaultStartPage() == null) {
            errors.add(new FieldError("defaultStartPage", "must not be null"));
        }
        return errors;
    }

    private static boolean isValidZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return false;
        }
        try {
            ZoneId.of(timeZone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AppSettingsEntity load() {
        return repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("settings row " + SETTINGS_ID + " is missing"));
    }

    private static Settings toDto(AppSettingsEntity e) {
        return new Settings(
                e.getDisplayName(),
                e.getEmail(),
                e.getTimeZone(),
                e.isNotificationsEnabled(),
                e.getDefaultStartPage(),
                e.isConfigured());
    }
}
