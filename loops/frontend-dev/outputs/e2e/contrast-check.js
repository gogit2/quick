/*
 * Contrast check for the Playwright MCP browser_evaluate tool (E2E-M08-05).
 *
 * Usage:
 *   1. browser_evaluate: localStorage.setItem('qf.theme', 'dark')   (or 'light'), then reload the page.
 *   2. browser_evaluate with this whole file. It is ONE function expression.
 *   3. Result: { theme, checked, failures, unknown, focusRing }.
 *      failures must be []. unknown lists elements whose background is an image or gradient.
 *
 * Rules (WCAG 2): text needs 4.5:1, or 3:1 for text of 24px or more, or 18.66px and bold.
 * Icon-only buttons need 3:1. The focus ring is reported against its background (3:1).
 */
() => {
  const parseRgb = (value) => {
    const m = value && value.match(/rgba?\(([^)]+)\)/);
    if (!m) return null;
    const p = m[1].split(/[\s,/]+/).filter(Boolean).map(Number);
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  };
  const blend = (top, bottom) => ({
    r: top.r * top.a + bottom.r * (1 - top.a),
    g: top.g * top.a + bottom.g * (1 - top.a),
    b: top.b * top.a + bottom.b * (1 - top.a),
    a: 1,
  });
  const luminance = ({ r, g, b }) => {
    const lin = (c) => {
      const s = c / 255;
      return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
    };
    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
  };
  const contrast = (c1, c2) => {
    const [hi, lo] = [luminance(c1), luminance(c2)].sort((a, b) => b - a);
    return (hi + 0.05) / (lo + 0.05);
  };
  // Walks up to the first opaque background, compositing translucent layers on the way.
  // Returns null when a layer is an image or gradient.
  const backgroundOf = (el) => {
    const layers = [];
    for (let node = el; node; node = node.parentElement) {
      const cs = getComputedStyle(node);
      if (cs.backgroundImage !== 'none') return null;
      const rgb = parseRgb(cs.backgroundColor);
      if (rgb && rgb.a > 0) {
        layers.push(rgb);
        if (rgb.a >= 1) break;
      }
    }
    let base = { r: 255, g: 255, b: 255, a: 1 };
    if (layers.length && layers[layers.length - 1].a >= 1) base = layers.pop();
    for (let i = layers.length - 1; i >= 0; i--) base = blend(layers[i], base);
    return base;
  };
  const describe = (el) => {
    const id = el.getAttribute('data-testid');
    const tag = el.tagName.toLowerCase();
    if (id) return `${tag}[data-testid="${id}"]`;
    const cls = typeof el.className === 'string' ? el.className.trim().split(/\s+/).filter(Boolean)[0] : '';
    return cls ? `${tag}.${cls}` : tag;
  };
  const visible = (el) => {
    const cs = getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden' || Number(cs.opacity) === 0) return false;
    const r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  };

  let checked = 0;
  const failures = [];
  const unknown = [];
  const measure = (el, text, required) => {
    const bg = backgroundOf(el);
    const cs = getComputedStyle(el);
    const fgRaw = parseRgb(cs.color);
    if (!bg || !fgRaw) {
      unknown.push(describe(el));
      return;
    }
    const fg = fgRaw.a < 1 ? blend(fgRaw, bg) : fgRaw;
    const ratio = Math.round(contrast(fg, bg) * 100) / 100;
    checked++;
    if (ratio < required) {
      failures.push({ selector: describe(el), text: text.slice(0, 40), ratio, required });
    }
  };

  for (const el of document.querySelectorAll('body *')) {
    if (el.closest('script, style') || !visible(el) || el.matches(':disabled')) continue;
    const text = Array.from(el.childNodes)
      .filter((n) => n.nodeType === Node.TEXT_NODE)
      .map((n) => n.textContent)
      .join('')
      .trim();
    if (text) {
      const cs = getComputedStyle(el);
      const size = parseFloat(cs.fontSize);
      const bold = Number(cs.fontWeight) >= 700;
      const large = size >= 24 || (size >= 18.66 && bold);
      measure(el, text, large ? 3 : 4.5);
    } else if (el.matches('button') && (el.getAttribute('aria-label') || el.querySelector('mat-icon'))) {
      measure(el, el.getAttribute('aria-label') || 'icon button', 3);
    }
  }

  // Focus ring: focus the first control and compare its outline with its background.
  let focusRing = null;
  const first = document.querySelector('button, a[href], [tabindex]');
  if (first) {
    first.focus();
    const cs = getComputedStyle(first);
    const ring = parseRgb(cs.outlineColor);
    const bg = backgroundOf(first);
    if (cs.outlineStyle === 'none' || !ring || !bg) {
      focusRing = { selector: describe(first), ratio: null, required: 3 };
    } else {
      const ratio = Math.round(contrast(ring, bg) * 100) / 100;
      focusRing = { selector: describe(first), ratio, required: 3 };
      if (ratio < 3) failures.push({ selector: `${describe(first)}:focus`, text: 'focus ring', ratio, required: 3 });
    }
  }

  return {
    theme: document.documentElement.dataset.theme || 'unset',
    checked,
    failures,
    unknown,
    focusRing,
  };
};
