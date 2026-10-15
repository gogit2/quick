import { Component } from '@angular/core';

// Empty-state picture for Habits: a grid of days, a few already done.
@Component({
  selector: 'qf-habits-illustration',
  standalone: true,
  template: `
    <svg viewBox="0 0 160 120" width="160" height="120" aria-hidden="true" focusable="false">
      <circle class="done" cx="56" cy="40" r="10" />
      <circle class="done" cx="80" cy="40" r="10" />
      <circle class="ring" cx="104" cy="40" r="10" />
      <circle class="done" cx="56" cy="68" r="10" />
      <circle class="ring" cx="80" cy="68" r="10" />
      <circle class="ring" cx="104" cy="68" r="10" />
      <path class="arrow" d="M44 96 H116" />
      <path class="arrow" d="M108 90 L116 96 L108 102" />
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

    .done {
      fill: var(--qf-accent);
    }

    .ring {
      fill: none;
      stroke: var(--qf-outline);
      stroke-width: 2;
    }

    .arrow {
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `,
})
export class HabitsIllustrationComponent {}
