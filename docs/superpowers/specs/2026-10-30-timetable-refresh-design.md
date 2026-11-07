# TimeSync EduAlert refresh

## Intent and authorization
The owner requested every review issue fixed, code and UI/UX improved, useful new features selected by the implementer, and a separate commit for each issue. Implementation and routine design choices are delegated; no publication is requested.

## Approach
Retain Kotlin + Android Views + Room rather than migrating UI frameworks. This keeps stored data and the existing toolchain usable while allowing a substantial redesign. A small data/domain layer will own course validation, backup replacement and weekly reminder calculation. Screens render persisted data and delegate writes. A Compose rewrite and a minimal cosmetic patch were considered; the first adds migration risk without a user benefit, the second leaves the state/backup problems intact.

## Constraints
- Preserve applicationId com.ams.timesyncedualertv2, Room database courses.db version 1, existing course IDs and legacy JSON arrays.
- Keep minSdk 33 / compileSdk 34 / targetSdk 34 for this refresh; dependency/toolchain upgrades are separate work.
- No account, cloud service or analytics. Course data stays local except for explicit file export.
- English and Simplified Chinese UI, system light/dark themes, native Back, 48dp touch targets, keyboard-safe scrolling, font scaling.
- Each of the ten review issues gets its own fix commit, with meaningful regression coverage where applicable.

## User experience
Homepage uses a clear app bar, a next-class summary, seven day tabs, chronological course rows and one Add course FAB. Search filters names and locations. Course editor uses labelled Material fields, seven selectable weekdays, time pickers, a concise color palette and visible validation. Details offer edit, duplicate and delete with undo. Settings separates reminder controls from backup/restore and destructive actions. Empty states explain how to add a course.

## Behavior
Imports parse and validate the entire file before showing a course-count preview; replacement is transactional and cancellation never writes. Export uses a versioned document and imports accept the original array format. Invalid records, unsupported versions, duplicate IDs and oversized files fail without altering data.

Reminders use a real AlarmManager PendingIntent per course and weekday, schedule the next occurrence in the device timezone, respect the app switch and system permission, and reschedule after delivery, reboot, clock/timezone changes and course edits. Use exact alarms only when allowed, with an honest inexact fallback otherwise. No immediate test notifications masquerading as scheduled reminders.

## New features selected by implementer
Weekend classes; upcoming/ongoing class summary; course search; duplicate course draft; undo deletion. These are incremental local timetable workflows with no external service dependency.

## Verification
JUnit tests cover strict times/intervals, backup parsing, reminder rollover/DST and next-class selection. Device tests cover import cancellation, database replacement, one-tap details, rotation, reactive lists, preferences and undo. Build and lint run on the final branch; emulator screenshots verify light/dark, larger text and a wider layout.
