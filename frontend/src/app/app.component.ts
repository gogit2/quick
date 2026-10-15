import { DOCUMENT } from '@angular/common';
import { Component, afterNextRender, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class AppComponent {
  constructor() {
    // Colour transitions are enabled only after the first frame (see styles/_motion.scss).
    const doc = inject(DOCUMENT);
    afterNextRender(() => requestAnimationFrame(() => doc.documentElement.classList.add('qf-ready')));
  }
}
