# TimeSync EduAlert

English | [中文](README_CN.md)

An offline Android timetable for keeping track of classes and what comes next.

## Features

- Seven-day timetable with multiple weekdays per course, time conflict checks, optional location/notes and course colors.
- Live “now / next” class summary and search by course name or location within the selected day.
- Add, edit, duplicate and delete courses; Undo is available after deletion, including when opening a course from a reminder.
- Drafts survive activity recreation. Lists update when a course is edited, imported or cleared.
- Weekly reminders with a configurable 1–1440 minute lead time, notification permission controls and reboot/timezone rescheduling.
- Backup export, import preview and transactional replacement. Cancelled, invalid or failed imports retain the existing timetable.
- Material light/dark themes, English/Simplified Chinese text, keyboard-aware forms and layouts for larger fonts and wider screens.

## Reminders

Turn reminders on in Settings and allow notifications. The precise-timing setting opens Android's Alarms & reminders access. When that access is unavailable, the app uses an inexact system alarm and Android may delay delivery; the status is shown in the app. See [Android's alarm documentation](https://developer.android.com/develop/background-work/services/alarms).

Courses recur in the device's local timezone. Each selected weekday is scheduled separately. A reminder delivery checks current course data and its scheduled timestamp before notifying and rearming.

## Backups and existing data

Existing `courses.db` data and the Room version-1 schema are retained. Imports accept the original course-array JSON and the new document format:

```json
{
  "schemaVersion": 1,
  "format": "timesync-timetable",
  "courses": []
}
```

Import limits are 1 MiB and 1,000 courses. Records must have valid weekdays and `HH:mm` times, a nonempty name and a positive same-day time interval. Duplicate positive IDs and overlapping courses are rejected before preview. The app shows the course count and waits for confirmation before replacing anything. Export supports empty timetables too.

## Build and verify

Requires Android SDK 37.0, Build Tools 36.0.0 and JDK 17. Use an Android Studio version compatible with AGP 9.5, or build through Gradle directly. Set `ANDROID_HOME` or let Android Studio generate `local.properties` for your SDK. The Gradle wrapper and dependency catalogue are committed.

```sh
./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug lintRelease
```

Both build variants are checked explicitly; Lint warnings fail verification.

With a test emulator connected:

```sh
./gradlew connectedDebugAndroidTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

The code uses Kotlin, Android Views/Material, Room, coroutines and Gson. No account, cloud backend, analytics or network permission is required.

## Maintenance notes

- [Lint cleanup, dependency versions and verification](docs/LINT_CLEANUP.md)

- [Issue-to-commit map](docs/CHANGES.md)
- [Measured verification and screenshots](docs/VERIFICATION.md)
- [Implementation plan](docs/superpowers/plans/2026-10-30-timetable-refresh.md)
- [Independent review follow-up](docs/REVIEW.md)

## License

[MIT](LICENSE). Original project: [TimeSync_EduAlert](https://github.com/fuarthur/TimeSync_EduAlert).
