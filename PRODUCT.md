# TimeSync EduAlert

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Students maintaining a recurring weekly course schedule. The owner delegated feature and UI choices during the 2026-10-30 refresh.

## Product Purpose
Help people see their classes, maintain a conflict-free schedule, receive optional course reminders, and safely back up or transfer their timetable.

## Operating Context
Native offline Android app. Courses repeat weekly in the device's local timezone. JSON files are explicitly selected through Android's document picker.

## Capabilities and Constraints
Seven weekdays, multiple days per course, start/end time, name, location, optional notes and course color. Local Room storage; no account, cloud service or analytics. Preserve existing database and legacy JSON arrays. English and Simplified Chinese strings, system light/dark themes.

## Product Principles
- Preserve the previous timetable until a replacement is fully validated.
- Let users check their next class and edit a course in a few taps.
- Respect notification preferences and Android permissions.
- Explain errors beside the input and provide recovery from destructive actions.
