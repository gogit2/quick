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

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskDueFilter = 'TODAY' | 'OVERDUE' | 'THIS_WEEK' | 'NO_DUE_DATE';

export type TaskSort = 'CREATED_DESC' | 'CREATED_ASC' | 'DUE_DATE_ASC' | 'DUE_DATE_DESC';

export interface Task {
  id: number;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string | null;
  createdAt: string;
  updatedAt: string;
  archived: boolean;
  overdue: boolean;
}

export interface TaskCreate {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string | null;
}

export interface TaskUpdate {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string | null;
}
