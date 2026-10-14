# Contract issues

Dev loops never edit `specs/**/contracts/**`. They log problems here and keep building to the contract as written.
The verifier resolves small, compatible issues itself (and logs the resolution); anything else is escalated to the user.

Format:
```
## CI-<n> · <Mxx> · <role> · <ISO time> · status: open | resolved-by-verify | escalated | resolved-by-user
- Operation: <METHOD /path> (or schema name)
- Problem: <what is wrong or missing>
- Proposed change: <exact change>
- Workaround in code meanwhile: <none | what>
- Resolution: <filled by verify or user>
```

