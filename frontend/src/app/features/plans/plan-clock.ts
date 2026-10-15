import { Plan } from '../../core/api/models';

// Time rules for plans on the client. `now` always comes from ClockService (constitution V).
// Only IN_PROGRESS plans count down (BR12); the server status stays the source of truth
// for NOT_STARTED / IN_PROGRESS / COMPLETED, these helpers only refine the live display.

// Whole seconds until the end of the plan, never negative. Null unless the plan is IN_PROGRESS.
export function restSecondsAt(plan: Plan, now: Date): number | null {
  if (plan.status !== 'IN_PROGRESS') {
    return null;
  }
  const remainingMs = Date.parse(plan.endDateTime) - now.getTime();
  return Math.max(0, Math.floor(remainingMs / 1000));
}

// True while the clock is inside the plan window and the server says IN_PROGRESS.
export function isInWindowAt(plan: Plan, now: Date): boolean {
  if (plan.status !== 'IN_PROGRESS') {
    return false;
  }
  const time = now.getTime();
  return time >= Date.parse(plan.startDateTime) && time < Date.parse(plan.endDateTime);
}
