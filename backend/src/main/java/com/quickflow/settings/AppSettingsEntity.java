package com.quickflow.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The single settings row (id always 1). */
@Entity
@Table(name = "settings")
public class AppSettingsEntity {

    @Id
    private Long id;

    private String displayName;

    private String email;

    private String timeZone;

    private boolean notificationsEnabled;

    @Enumerated(EnumType.STRING)
    private StartPage defaultStartPage;

    private boolean configured;

    protected AppSettingsEntity() {
    }

    public AppSettingsEntity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public StartPage getDefaultStartPage() {
        return defaultStartPage;
    }

    public void setDefaultStartPage(StartPage defaultStartPage) {
        this.defaultStartPage = defaultStartPage;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
