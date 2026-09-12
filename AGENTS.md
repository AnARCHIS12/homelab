# AGENTS.md - Homelab

This repository is the single working copy for Homelab Android development.

## Repository Flow

- Work from this repository root only.
- `origin` is the primary GitHub remote (`https://github.com/AnARCHIS12/homelab.git`).
- Keep `main` tracking `origin/main`.

## Branch Strategy

- Use `main` for normal owner-directed changes, release preparation, and release follow-up commits.
- Create a short-lived branch for larger/riskier work or feature development.
- Prefer branch names like `feat/service-name`, `fix/issue-name`, `docs/topic`, or `ci/topic`.

## Development Rules

- Make focused commits with clear messages:
  - `feat: ...`
  - `fix: ...`
  - `docs: ...`
  - `ci: ...`
  - `chore: ...`
- For release-bound changes, update `versionCode` and `versionName` in `HomelabAndroid/app/build.gradle.kts`.
- Do not commit generated release binaries (`.apk`, `.aab`) directly to git.

## Build Checks

Android compile check:

```bash
cd HomelabAndroid
./gradlew :app:compileDebugKotlin --console=plain
```

## Test Checks

Android unit and security tests:

```bash
cd HomelabAndroid
./gradlew :app:testDebugUnitTest --console=plain
```

Assemble signed release APK:

```bash
cd HomelabAndroid
./gradlew :app:assembleRelease --console=plain
```

## Release Flow

- Creating or pushing a git tag `v*` (e.g. `v1.6.2`) triggers `.github/workflows/release.yml`.
- The workflow automatically runs unit tests, compiles the release APK, creates the GitHub Release, and attaches `Homelab.apk`.
- The workflow also updates `app-version.json` with the new version and direct APK download URL.
