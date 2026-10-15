// Verifier helper: lists data-testid literals present at HEAD but missing in the working tree (M10 round 1).
const { execSync } = require('child_process');
const fs = require('fs');
const files = execSync('git diff HEAD --name-only -- frontend/src/app', { encoding: 'utf8' }).split('\n').filter(Boolean);
const re = /(?:data-testid|testId|\[attr\.data-testid\]|\[testId\])="([^"]*)"|data-testid="([^"]*)"|'([a-z]+-[a-z0-9-]+-?)'/g;
const ids = (s) => { const out = new Set(); let m; const r = /(?:data-testid|testId)\]?="([^"]*)"/g; while ((m = r.exec(s))) out.add(m[1]); return out; };
const allNew = new Set();
const newTexts = {};
for (const f of files) { newTexts[f] = fs.existsSync(f) ? fs.readFileSync(f, 'utf8') : ''; }
const everything = Object.values(newTexts).join('\n') + execSync('cat frontend/src/app/shared/*.ts', { encoding: 'utf8' });
for (const f of files) {
  let old = '';
  try { old = execSync('git show HEAD:' + f, { encoding: 'utf8' }); } catch (e) { continue; }
  const o = ids(old), n = ids(newTexts[f]);
  const removed = [...o].filter((x) => !n.has(x));
  console.log('== ' + f + ' old ' + o.size + ' new ' + n.size);
  for (const r of removed) {
    // look for the raw prefix anywhere in new code (ids built in TS or passed through inputs)
    const core = r.replace(/\{\{.*?\}\}/g, '').replace(/'\s*\+.*$/, '').replace(/^'/, '');
    console.log('  REMOVED ' + r + (everything.includes(core) ? '   (prefix "' + core + '" still found)' : '   (NOT FOUND)'));
  }
}
