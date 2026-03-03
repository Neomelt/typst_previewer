# Contributing

## Branch Strategy
- New feature: `feat/<scope>-<short-name>`
- Bug fix: `fix/<scope>-<short-name>`
- Chore/docs: `chore/<scope>-<short-name>`

Do not commit new feature work directly on `main`.

## Commit Convention
Use Conventional Commits:
- `feat:` new feature
- `fix:` bug fix
- `test:` tests
- `docs:` docs only
- `chore:` tooling/config
- `refactor:` code cleanup without behavior change

Examples:
- `feat(app): add typ/pdf import actions`
- `test(parser): cover heading extraction edge cases`

## Local Quality Gate
Run before every commit:
```bash
./scripts/dev-check.sh
```

Current check includes:
- `./gradlew testDebugUnitTest`

## PR Checklist
- [ ] Feature branch created
- [ ] Tests pass locally
- [ ] Worklog updated (`WORKLOG.md`)
- [ ] Scope matches commit message
- [ ] No secrets committed
