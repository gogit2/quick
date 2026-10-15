import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { ApiRequestError } from '../../core/api/error.interceptor';
import { Settings, StartPage } from '../../core/api/models';
import { SettingsStore } from '../../core/settings.store';
import { ThemeService } from '../../core/theme.service';
import { UiCardComponent } from '../../shared/ui-card.component';
import { UiPageHeaderComponent } from '../../shared/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/ui-skeleton.component';

interface SettingsForm {
  displayName: FormControl<string>;
  email: FormControl<string>;
  timeZone: FormControl<string>;
  notificationsEnabled: FormControl<boolean>;
  defaultStartPage: FormControl<StartPage>;
}

// The backend default is always offered, since some browsers leave "UTC" out of the list.
function timeZoneOptions(): string[] {
  const zones = Intl.supportedValuesOf('timeZone');
  return zones.includes('UTC') ? zones : ['UTC', ...zones];
}

@Component({
  selector: 'qf-settings-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    MatSlideToggleModule,
    UiCardComponent,
    UiPageHeaderComponent,
    UiSkeletonComponent,
  ],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
})
export class SettingsPageComponent implements OnInit {
  private readonly store = inject(SettingsStore);
  readonly theme = inject(ThemeService);

  readonly startPages: { value: StartPage; label: string }[] = [
    { value: 'DASHBOARD', label: 'Dashboard' },
    { value: 'TASKS', label: 'Tasks' },
    { value: 'HABITS', label: 'Habits' },
    { value: 'LEARNING', label: 'Learning Resources' },
    { value: 'PLANS', label: 'Todo Plans' },
  ];

  readonly timeZones = signal<string[]>(timeZoneOptions());
  readonly loading = signal(false);
  // Set when the settings could not be read, so the form is not presented as the saved values.
  readonly loadFailed = signal(false);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly errorText = signal<string | null>(null);

  readonly form = new FormGroup<SettingsForm>({
    displayName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)],
    }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(200)] }),
    timeZone: new FormControl('UTC', { nonNullable: true, validators: [Validators.required] }),
    notificationsEnabled: new FormControl(true, { nonNullable: true }),
    defaultStartPage: new FormControl<StartPage>('DASHBOARD', { nonNullable: true, validators: [Validators.required] }),
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadFailed.set(false);
    this.store.refresh().subscribe({
      next: (settings) => {
        this.loading.set(false);
        this.apply(settings);
      },
      error: () => {
        this.loading.set(false);
        this.loadFailed.set(true);
      },
    });
  }

  save(): void {
    this.saved.set(false);
    this.errorText.set(null);
    // Re-run validators so that server errors from the last save do not block a new attempt.
    Object.values(this.form.controls).forEach((control) => control.updateValueAndValidity());
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    const email = value.email.trim();
    this.saving.set(true);
    this.store
      .save({
        displayName: value.displayName,
        email: email === '' ? null : email,
        timeZone: value.timeZone,
        notificationsEnabled: value.notificationsEnabled,
        defaultStartPage: value.defaultStartPage,
      })
      .subscribe({
        next: (settings) => {
          this.apply(settings);
          this.saving.set(false);
          this.saved.set(true);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          if (err instanceof ApiRequestError && err.body) {
            for (const fieldError of err.body.fieldErrors) {
              this.form.get(fieldError.field)?.setErrors({ server: fieldError.message });
            }
          } else {
            this.errorText.set('Settings could not be saved.');
          }
        },
      });
  }

  errorMessage(name: keyof SettingsForm): string {
    const control = this.form.controls[name];
    if (control.hasError('server')) {
      return control.getError('server');
    }
    if (control.hasError('required') || control.hasError('pattern')) {
      return 'This field is required.';
    }
    if (control.hasError('maxlength')) {
      return 'This value is too long.';
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address.';
    }
    return '';
  }

  private apply(settings: Settings): void {
    if (!this.timeZones().includes(settings.timeZone)) {
      this.timeZones.update((zones) => [...zones, settings.timeZone]);
    }
    this.form.reset({
      displayName: settings.displayName,
      email: settings.email ?? '',
      timeZone: settings.timeZone,
      notificationsEnabled: settings.notificationsEnabled,
      defaultStartPage: settings.defaultStartPage,
    });
  }
}
