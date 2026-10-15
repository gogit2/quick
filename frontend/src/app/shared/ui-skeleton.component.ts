import { Component, computed, input } from '@angular/core';

export type SkeletonVariant = 'line' | 'card' | 'row';

// Placeholder blocks shown while a list loads. The blocks are decorative; one
// visually hidden status text announces the loading state to screen readers.
@Component({
  selector: 'ui-skeleton',
  standalone: true,
  template: `
    <div class="group" aria-hidden="true">
      @for (item of items(); track $index) {
        <div class="block" [class]="variant()"></div>
      }
    </div>
    <span class="visually-hidden" role="status">Loading</span>
  `,
  styles: `
    :host {
      display: block;
    }

    .group {
      display: flex;
      flex-direction: column;
      gap: var(--qf-space-2);
    }

    .block {
      background: var(--qf-surface-raised);
      border-radius: var(--qf-radius-sm);
      animation: qf-skeleton-pulse calc(var(--qf-motion-slow) * 4) var(--qf-ease) infinite;
    }

    .line {
      height: 14px;
      width: 100%;
    }

    .row {
      height: 48px;
      border-radius: var(--qf-radius-sm);
    }

    .card {
      height: 120px;
      border-radius: var(--qf-radius-md);
      border: 1px solid var(--qf-outline);
    }

    @keyframes qf-skeleton-pulse {
      0%,
      100% {
        opacity: 1;
      }
      50% {
        opacity: 0.55;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .block {
        animation: none;
      }
    }

    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip-path: inset(50%);
      white-space: nowrap;
    }
  `,
})
export class UiSkeletonComponent {
  readonly variant = input<SkeletonVariant>('line');
  readonly count = input(1);
  readonly items = computed(() => Array.from({ length: this.count() }, (_, i) => i));
}
