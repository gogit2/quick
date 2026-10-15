package com.quickflow.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.FieldError;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsServiceTest {

    private AppSettingsRepository repository;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppSettingsRepository.class);
        AppSettingsEntity row = new AppSettingsEntity(1L);
        row.setDisplayName("QuickFlow User");
        row.setTimeZone("UTC");
        row.setNotificationsEnabled(true);
        row.setDefaultStartPage(StartPage.DASHBOARD);
        row.setConfigured(false);
        when(repository.findById(1L)).thenReturn(Optional.of(row));
        when(repository.save(any(AppSettingsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new SettingsService(repository);
    }

    @Test
    void defaultsBeforeFirstSave() {
        Settings settings = service.get();

        assertThat(settings.displayName()).isEqualTo("QuickFlow User");
        assertThat(settings.email()).isNull();
        assertThat(settings.timeZone()).isEqualTo("UTC");
        assertThat(settings.notificationsEnabled()).isTrue();
        assertThat(settings.defaultStartPage()).isEqualTo(StartPage.DASHBOARD);
        assertThat(settings.configured()).isFalse();
    }

    @Test
    void updatePersistsAndMarksConfigured() {
        Settings saved = service.update(new SettingsUpdate(
                " Alex ", "alex@example.com", "Europe/Berlin", false, StartPage.TASKS));

        assertThat(saved.displayName()).isEqualTo("Alex");
        assertThat(saved.email()).isEqualTo("alex@example.com");
        assertThat(saved.timeZone()).isEqualTo("Europe/Berlin");
        assertThat(saved.notificationsEnabled()).isFalse();
        assertThat(saved.defaultStartPage()).isEqualTo(StartPage.TASKS);
        assertThat(saved.configured()).isTrue();
    }

    @Test
    void blankEmailIsStoredAsNull() {
        Settings saved = service.update(update("Alex", "   ", "UTC"));

        assertThat(saved.email()).isNull();
    }

    @Test
    void invalidTimeZoneIsRejectedWithFieldError() {
        assertFieldError(update("Alex", null, "Mars/Olympus"), "timeZone");
    }

    @Test
    void blankTimeZoneIsRejected() {
        assertFieldError(update("Alex", null, " "), "timeZone");
    }

    @Test
    void blankDisplayNameIsRejected() {
        assertFieldError(update("   ", null, "UTC"), "displayName");
    }

    @Test
    void displayNameLongerThan100IsRejected() {
        assertFieldError(update("x".repeat(101), null, "UTC"), "displayName");
    }

    @Test
    void invalidEmailIsRejected() {
        assertFieldError(update("Alex", "not-an-email", "UTC"), "email");
    }

    @Test
    void missingNotificationsFlagIsRejected() {
        assertFieldError(new SettingsUpdate("Alex", null, "UTC", null, StartPage.DASHBOARD), "notificationsEnabled");
    }

    @Test
    void nothingIsSavedWhenValidationFails() {
        assertThatThrownBy(() -> service.update(update("   ", null, "Mars/Olympus")))
                .isInstanceOf(BadRequestException.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }

    private static SettingsUpdate update(String displayName, String email, String timeZone) {
        return new SettingsUpdate(displayName, email, timeZone, true, StartPage.DASHBOARD);
    }

    private void assertFieldError(SettingsUpdate update, String field) {
        assertThatThrownBy(() -> service.update(update))
                .isInstanceOfSatisfying(BadRequestException.class, ex -> {
                    List<FieldError> errors = ex.getFieldErrors();
                    assertThat(errors).extracting(FieldError::field).contains(field);
                });
    }
}
