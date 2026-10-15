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

export type HabitFrequency = 'DAILY' | 'WEEKLY';

export interface Habit {
  id: number;
  name: string;
  description?: string | null;
  frequency: HabitFrequency;
  createdAt: string;
  active: boolean;
  // DAILY: completed today. WEEKLY: completed on any day of the current Monday-Sunday week.
  doneForCurrentPeriod: boolean;
  currentStreak: number;
  totalCompletions: number;
  periodStart: string;
  periodEnd: string;
}

export interface HabitCreate {
  name: string;
  description?: string | null;
  frequency: HabitFrequency;
}

export interface HabitUpdate {
  name: string;
  description?: string | null;
  frequency: HabitFrequency;
  active: boolean;
}

export interface HabitCompletion {
  id: number;
  habitId: number;
  completionDate: string;
  createdAt: string;
}

// completionDate is optional: the server uses today in the user's time zone when it is left out.
export interface HabitCompletionCreate {
  completionDate?: string | null;
}

// Derived from the milestones when the card has any; set by hand only when it has none.
export type LearningStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';

export interface LearningMilestone {
  id: number;
  learningCardId: number;
  title: string;
  done: boolean;
  targetDate?: string | null;
  completedAt?: string | null;
}

export interface LearningNote {
  id: number;
  learningCardId: number;
  text: string;
  createdAt: string;
}

export interface LearningCard {
  id: number;
  title: string;
  description?: string | null;
  status: LearningStatus;
  createdAt: string;
  milestones: LearningMilestone[];
  notes: LearningNote[];
}

export interface LearningCardCreate {
  title: string;
  description?: string | null;
  status?: LearningStatus;
}

// status is only accepted while the card has no milestones; otherwise the server answers 409.
export interface LearningCardUpdate {
  title: string;
  description?: string | null;
  status?: LearningStatus;
}

export interface LearningMilestoneCreate {
  title: string;
  targetDate?: string | null;
}

export interface LearningMilestoneUpdate {
  title: string;
  done: boolean;
  targetDate?: string | null;
}

export interface LearningNoteCreate {
  text: string;
}

// status is computed on every read from the clock and item completion, never stored.
export type PlanStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';

// ACTIVE_UPCOMING = NOT_STARTED + IN_PROGRESS.
export type PlanGroup = 'ACTIVE_UPCOMING' | 'COMPLETED';

export type PlanItemSourceType = 'TASK' | 'HABIT' | 'LEARNING_RESOURCE';

export interface PlanItem {
  id: number;
  sourceType: PlanItemSourceType;
  sourceId: number;
  title: string;
  done: boolean;
}

export interface Plan {
  id: number;
  title: string;
  estimatedDurationMinutes: number;
  startDateTime: string;
  endDateTime: string;
  priorityOrder: number;
  status: PlanStatus;
  createdAt: string;
  startNotified: boolean;
  durationExceedsWindow: boolean;
  totalItems: number;
  doneItems: number;
  completionPercent: number;
  // Seconds until endDateTime, only while status is IN_PROGRESS (BR12); null otherwise.
  restSeconds?: number | null;
  items: PlanItem[];
}

export interface PlanItemCreate {
  sourceType: PlanItemSourceType;
  sourceId: number;
}

export interface PlanCreate {
  title: string;
  estimatedDurationMinutes: number;
  startDateTime: string;
  endDateTime: string;
  priorityOrder: number;
  items: PlanItemCreate[];
}

export interface PlanItemUpdate {
  done: boolean;
}
