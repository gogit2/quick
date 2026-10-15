import { Component } from '@angular/core';

// Empty-state picture for Learning: an open book with a bookmark ribbon.
@Component({
  selector: 'qf-learning-illustration',
  standalone: true,
  template: `
    <svg viewBox="0 0 160 120" width="160" height="120" aria-hidden="true" focusable="false">
      <path class="page" d="M80 36 C64 30 50 30 36 34 V94 C50 90 64 90 80 96 Z" />
      <path class="page" d="M80 36 C96 30 110 30 124 34 V94 C110 90 96 90 80 96 Z" />
      <line class="line" x1="46" y1="52" x2="68" y2="56" />
      <line class="line" x1="46" y1="66" x2="68" y2="70" />
      <path class="ribbon" d="M112 18 H122 V46 L117 41 L112 46 Z" />
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

    .page {
      fill: none;
      stroke: currentColor;
      stroke-width: 2;
      stroke-linejoin: round;
    }

    .line {
      stroke: var(--qf-outline);
      stroke-width: 3;
      stroke-linecap: round;
    }

    .ribbon {
      fill: var(--qf-accent);
    }
  `,
})
export class LearningIllustrationComponent {}
