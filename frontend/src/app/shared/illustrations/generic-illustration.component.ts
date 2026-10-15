import { Component } from '@angular/core';

// Fallback empty-state picture: a document with a sparkle.
@Component({
  selector: 'qf-generic-illustration',
  standalone: true,
  template: `
    <svg viewBox="0 0 160 120" width="160" height="120" aria-hidden="true" focusable="false">
      <path class="page" d="M48 16 H96 L116 36 V104 H48 Z" />
      <path class="page" d="M96 16 V36 H116" />
      <line class="line" x1="60" y1="56" x2="104" y2="56" />
      <line class="line" x1="60" y1="72" x2="96" y2="72" />
      <circle class="accent" cx="114" cy="96" r="10" />
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

    .accent {
      fill: var(--qf-accent);
    }
  `,
})
export class GenericIllustrationComponent {}
