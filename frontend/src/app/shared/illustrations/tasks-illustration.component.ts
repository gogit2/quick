import { Component } from '@angular/core';

// Empty-state picture for Tasks: a clipboard with one ticked item.
@Component({
  selector: 'qf-tasks-illustration',
  standalone: true,
  template: `
    <svg viewBox="0 0 160 120" width="160" height="120" aria-hidden="true" focusable="false">
      <rect class="frame" x="44" y="22" width="72" height="84" rx="10" />
      <rect class="clip" x="62" y="14" width="36" height="16" rx="4" />
      <line class="line" x1="58" y1="50" x2="102" y2="50" />
      <line class="line" x1="58" y1="66" x2="98" y2="66" />
      <line class="line" x1="58" y1="82" x2="92" y2="82" />
      <circle class="accent" cx="112" cy="94" r="11" />
    </svg>
  `,
  styles: `
    :host {
      display: block;
    }

    svg {
      display: block;
      color: var(--qf-text-muted);
    }

    .frame {
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
    }

    .clip {
      fill: var(--qf-outline);
    }

    .line {
      stroke: var(--qf-outline);
      stroke-width: 3;
      stroke-linecap: round;
    }

    .accent {
      fill: var(--qf-accent);
    }
  `,
})
export class TasksIllustrationComponent {}
