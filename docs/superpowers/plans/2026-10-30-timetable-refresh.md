# Timetable Refresh Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for sequential issue commits and a final independent review.

**Goal:** Repair all reviewed bugs and deliver a reliable, readable local timetable app.
**Architecture:** Retain Room schema; add shared database/repository and validated time/backup/reminder boundaries. Native Material screens observe current data.
**Tech Stack:** Kotlin 2.0.20, Android Views, Room 2.6.1, Material, JUnit, AndroidX device tests.
**Spec:** docs/superpowers/specs/2026-10-30-timetable-refresh-design.md

## Global Constraints
- Preserve applicationId com.ams.timesyncedualertv2, database courses.db version 1 and legacy JSON imports.
- minSdk 33 / compileSdk 34 / targetSdk 34.
- No cloud/account/analytics; separate commits for each review issue.

## Review Focus
- Invalid/cancelled/oversized imports retain all previous courses.
- Alarm triggers around midnight, week rollover, DST and denied permissions.
- Draft restoration and system Back after edits/import/deletion.
- Empty results, long names, dark theme and large fonts.
- Concurrent taps/writes never create overlapping duplicate courses.

## Tasks (ordered, individually committed)
- [x] 1. Build reproducibility: track wrapper/catalogue, stop ignoring tests, add test infrastructure. Verify tracked snapshot wrapper and baseline build.
- [x] 2. Safe import replacement: picker is read-only until validated result; transactional replacement. Regression: cancellation/error keeps original records.
- [x] 3. Strict import validation: central CourseValidation and BackupCodec; reject empty weekdays, malformed times/IDs/nulls. JUnit malformed fixtures RED→GREEN.
- [x] 4. One-tap course details: remove nested listener. Device regression RED→GREEN.
- [x] 5. Course interval validation: reject zero/reversed intervals and blank names. Unit + device regression RED→GREEN.
- [x] 6. Reminder scheduling: pure next-trigger calculator, AlarmManager scheduler, receiver registration/reschedule. Unit rollover cases RED→GREEN and device alarm inspection.
- [x] 7. Reminder preference: enforce enabled state and permission in scheduling boundary. Device disable/reopen regression RED→GREEN.
- [x] 8. Draft restoration: saved instance state for form values, avoid database refill over restored draft. Device recreate regression RED→GREEN.
- [x] 9. Live course lists: DAO Flow and view-lifecycle observation; back-stack navigation uses finish(). Device system Back regression RED→GREEN.
- [x] 10. Numeric input: bounded toIntOrNull parsing and numeric keyboard. Overflow regression RED→GREEN.
- [x] 11. Data architecture: application-level database/repository, atomic conflict-check-and-save, IO backup service, consistent LocalTime use. Run unit/device suites.
- [x] 12. Material UI redesign: unified light/dark tokens, proper app bars, readable list, accessible form/settings, status/error/empty states, localization. Verify emulator captures.
- [x] 13. Seven-day timetable, next-class summary and search. Unit boundary fixtures and device filters.
- [x] 14. Duplicate course and delete undo. Device duplicate draft + restore.
- [x] 15. Versioned backup preview/export, limits and legacy roundtrip. Unit/device replacement roundtrip.
- [x] 16. Final independent review, targeted repairs, build/lint/device suite, screenshots and commit-to-issue changelog.

## Progress
Execution notes and test evidence are recorded in docs/IMPLEMENTATION.md. All issue fixes remain independently identifiable even when later refactors replace their implementation.

## 2026-10-31 Lint follow-up
The user subsequently authorized clearing all remaining Lint warnings. This follow-up updates compile/target SDK to 37 while retaining minSdk 33, the application ID and Room version 1. All warning fixes, updated toolchain checks and API 33/37 verification are recorded in docs/LINT_CLEANUP.md.
