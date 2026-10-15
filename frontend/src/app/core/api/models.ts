// TypeScript mirrors of the contract schemas in specs/001-quickflow/contracts/openapi.yaml.
// Names, enum values and nullability follow the contract exactly. Do not rename here.

export type StartPage = 'DASHBOARD' | 'TASKS' | 'HABITS' | 'LEARNING' | 'PLANS';

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: FieldError[];
}

export interface Settings {
  displayName: string;
  email?: string | null;
  timeZone: string;
  notificationsEnabled: boolean;
  defaultStartPage: StartPage;
  configured: boolean;
}

export interface SettingsUpdate {
  displayName: string;
  email?: string | null;
  timeZone: string;
  notificationsEnabled: boolean;
  defaultStartPage: StartPage;
}
