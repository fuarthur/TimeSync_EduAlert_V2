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
