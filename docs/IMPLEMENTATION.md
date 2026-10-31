# Implementation record

Base: 73374cd. Branch: codex/timetable-refresh. User delegated feature/UI choices and requested separate issue commits.

- Planning: preserve native Kotlin/Views/Room and existing data; no remote publication.
- Build baseline before fix: clean tracked snapshot failed with GradleWrapperMain ClassNotFoundException; local debug/lint succeeded using ignored Gradle files.
- Import cancellation: ImportSafetyTest failed with expected 1 course / actual 0, then passed after removing the pre-picker delete. Replacement now uses a Room transaction; full suite passed.
- Import validation: BackupCodec fixtures RED (missing boundary) → GREEN; complete suite 8 tests passed. Invalid records never reach Room.
- Details click: oneTapOpensCourseDetails RED→GREEN. Interval: submitRejectsZeroLengthCourse RED (stored 1) → GREEN (stored 0).
- Scheduling: ReminderTime boundary cases RED→GREEN (week rollover, advance offset, previous-day reminder, DST); complete suite passed. Real AlarmManager/PendingIntent dispatch replaces immediate notify calls.
- Reminder preference: disabled refresh RED (2 alarms) → GREEN (0 alarms). Draft recreation RED→GREEN. Live list RED (1 stale row after clear) → GREEN (0); shared Application database and lifecycle Flow observation fix the root cause.
- Numeric overflow: ReminderInputTest RED (NumberFormatException) → GREEN; full regression suite passed. Ten original review issues now have individual fix commits.
- Data boundary: concurrentConflictingSavesOnlyInsertOnce, adjacentCoursesDoNotConflict and failedReplacementRetainsOldCourses RED→GREEN. Validation + conflict check + write now share one Room transaction. File reads/writes run on IO with a bounded read.
- UI redesign: 21 regression tests passed, debug APK built, Lint passed. Emulator verified empty timetable and editor. Material roles cover light/dark, bounded content width, insets/keyboard, inline validation and localized strings. Import now previews count before confirmation.
- Features: TimetableTest RED→GREEN for ongoing/upcoming selection, weekends, week rollover and search. DuplicateCourseTest RED→GREEN (empty draft → copied fields); UndoCourseTest RED→GREEN. VersionedBackupTest RED→GREEN, including Unicode/multiline roundtrip, empty backups and unknown-version rejection. Full suite: 30 tests passed; debug build and Lint passed.
- Final independent review: all six important findings and the minor midnight-date finding were repaired in separate commits. Targeted regressions increased the suite to 39 local tests. The native suite now has 7 passing tests, including notification-entry Undo and compact/keyboard layout.
- Manual UI verification: actual light/dark, Chinese/English, 1.3 font scale and wide-screen captures. Follow-up fixes label toolbar Back, keep the selected weekday visible after width changes, and wrap editor weekday choices so Sunday is not hidden off-screen.
- Final build/Lint/device checks passed; Lint retains 112 warnings and has 0 errors. A tracked-only JDK 17 checkout also passed local tests, APK build and Lint. Full results and issue-to-commit mapping are in VERIFICATION.md and CHANGES.md.

- 2026-10-31 Lint follow-up: original 112 warnings and new checker findings resolved in 19 focused commits. Debug/Release reports contain 0 issues with warnings treated as errors and the old permission suppression removed. Tracked-only JDK 17 checkout passed 41 tests and both APK builds (102 executed tasks); API 33 and API 37 each passed all 7 device tests. See LINT_CLEANUP.md for the commit map, raw results, toolchain versions and the upstream Gradle/Kotlin deprecation notice.
