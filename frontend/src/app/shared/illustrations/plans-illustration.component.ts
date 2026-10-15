import { Component } from '@angular/core';

// Empty-state picture for Plans: a timeline with two steps reached.
@Component({
  selector: 'qf-plans-illustration',
  standalone: true,
  template: `
    <svg viewBox="0 0 160 120" width="160" height="120" aria-hidden="true" focusable="false">
      <rect class="bar" x="30" y="30" width="60" height="14" rx="7" />
      <rect class="bar" x="30" y="78" width="100" height="14" rx="7" />
      <line class="track" x1="24" y1="60" x2="136" y2="60" />
      <circle class="done" cx="38" cy="60" r="7" />
      <circle class="done" cx="80" cy="60" r="7" />
      <circle class="next" cx="122" cy="60" r="7" />
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

    .bar {
      fill: var(--qf-outline);
    }

    .track {
      stroke: var(--qf-outline);
      stroke-width: 2;
    }

    .done {
      fill: var(--qf-accent);
    }

    .next {
      fill: var(--qf-surface);
      stroke: var(--qf-accent);
      stroke-width: 2;
    }
  `,
})
export class PlansIllustrationComponent {}
