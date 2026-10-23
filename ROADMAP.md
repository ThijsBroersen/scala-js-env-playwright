# Maintenance roadmap

Last reviewed: 2026-10-17 (dependency bump + CI verification).

## Completed (this maintenance round)

- [x] Bump sbt 2.0.1 → 2.0.10
- [x] Bump Scala 3.8.4 → 3.10.0
- [x] Bump Playwright Java 1.61.0 → 1.63.0
- [x] Bump cats-effect 3.7.0 → 3.7.1, jimfs 1.3.1 → 1.3.2
- [x] Refresh sbt plugins (ci-release, scalafmt, scalafix, pgp, mima, github-actions)
- [x] Enable MiMa baseline for the 0.3.x line (`0.3.2`)
- [x] Align README compatibility notes and package names with the codebase

## P0 — correctness & API completeness

| Item | Notes |
|------|--------|
| **Implement `exitJSStatement` / process exit** | Scala.js `RunTests` / `ComTests` skip exit-related cases (`JSEnv needs exitJSStatement`). Needed for parity with Node/Selenium envs and for clean test shutdown. |
| **KeepAlive / browser lifecycle** | README marks KeepAlive as WIP; document supported `RunConfig` options and implement predictable teardown (avoid flaky parallel tests). |
| **WebKit on Linux** | Driver hang on some Linux setups; WebKit suite gated by `PLAYWRIGHT_WEBKIT_TESTS`. Investigate Playwright/WebKit upgrades, timeouts, and optional CI job on macOS. |

## P1 — ecosystem & distribution

| Item | Notes |
|------|--------|
| **Mill first-class support** | README: “not yet part of Mill”; upstream `JsEnvConfig.Playwright` integration and a Mill example project. |
| **Track Scala.js releases** | `scalajs-js-envs` / test-kit pinned at 1.6.0 (current on Maven). Re-check on each Scala.js release for binary compatibility (see `ResourcesFactory` conflict message). |
| **Playwright version policy** | Document supported Playwright range; consider aligning driver JAR (`DriverJar.java`) with Playwright’s bundled driver expectations after bumps. |
| **Release 0.3.3+** | After dependency-only changes: run `mimaReportBinaryIssues`, tag, publish; update consumer snippet in README. |

## P2 — quality & developer experience

| Item | Notes |
|------|--------|
| **README examples** | Use consistent imports (`io.github.thijsbroersen.jsenv.playwright.*`) and sbt 2.x / Scala 3 settings throughout. |
| **Reduce log noise in tests** | Expected validation failures log at ERROR today; tune levels or test-only logging. |
| **Dependabot / Renovate** | Automate Maven Central bumps for library + plugin deps (manual maintenance round is error-prone). |
| **Optional JDK matrix** | CI uses JDK 21 only; consider JDK 17 smoke job if LTS compatibility is a stated goal. |
| **Scalafix / formatting** | `.scalafix.conf` is minimal (`RemoveUnused`); expand rules or wire into CI if desired. |

## P3 — features (from README / gaps)

| Item | Notes |
|------|--------|
| **CommonJS / legacy module kinds** | Explicitly unsupported; either document forever or evaluate emulation path. |
| **Custom launch options** | Partially documented; ensure parity across chrome/firefox/webkit factories and Mill/sbt config. |
| **Selenium coexistence** | Guava version pin documented for mixed setups; add integration test or doc recipe. |

## Suggested next actions (ordered)

1. Implement `exitJSStatement` and un-skip jsenv test-kit exit tests.
2. Open upstream Mill PR or publish Mill plugin module once API is stable.
3. Add Renovate (or GitHub Dependabot for sbt) + monthly `test` + `mimaReportBinaryIssues` on main.
4. Schedule WebKit investigation (macOS CI job or Playwright issue tracker).
5. Patch release **0.3.3** with dependency updates after MiMa is green on main.

## Dependency watchlist

| Dependency | Current | Watch |
|------------|---------|--------|
| Scala 3 | 3.10.0 | New 3.x minors |
| sbt | 2.0.10 | 2.0.x |
| Playwright | 1.63.0 | Frequent 1.x releases |
| scalajs-js-envs | 1.6.0 | Tied to Scala.js toolchain |
| cats-effect | 3.7.1 | 3.x patches |
| sbt-release | 1.4.0 | 1.6.0 still RC-only on Maven |
