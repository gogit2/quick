import { Pipe, PipeTransform } from '@angular/core';

// Formats a number of seconds as `1h 05m 03s` (or `5m 03s` below one hour).
// Null, undefined and negative values render as an empty string.
@Pipe({ name: 'restTime', standalone: true })
export class RestTimePipe implements PipeTransform {
  transform(seconds: number | null | undefined): string {
    if (seconds === null || seconds === undefined || seconds < 0) {
      return '';
    }
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    const pad = (value: number) => String(value).padStart(2, '0');
    return hours > 0 ? `${hours}h ${pad(minutes)}m ${pad(secs)}s` : `${minutes}m ${pad(secs)}s`;
  }
}
