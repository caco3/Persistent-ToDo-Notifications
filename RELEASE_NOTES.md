# Release Notes

## v0.11.0 (since v0.10.0)

### New Features

#### Calendar color support
- Each todo card now shows a colored left accent bar matching its DAVx5 calendar color
- Notifications are tinted with the calendar color (small icon accent)
- Settings calendar list shows a color circle next to each configured calendar name

#### On-device action log
- All "mark as done" and snooze actions are now written to `todo_actions.log` in the app's private storage
- **Settings → Share action log**: share the log as plain text for debugging
- Log rotates automatically at 200 KB

#### Settings improvements
- **Reset all done / snoozes**: new button to clear all recurring done-marks and snooze state at once (useful for testing)
- Filters section: "Show events before 2026" toggle moved to the bottom
- Development section: Demo mode toggle moved to the bottom; Share log and Reset above it

### Bug Fixes

#### Recurring events not disappearing after "mark as done"
- Fixed two separate cases where recurring events would reappear immediately after being marked done:
  1. Events whose next occurrence is outside the query window (e.g. yearly events) — fixed by checking `handledUntil >= dtStart` regardless of window presence
  2. Events whose next occurrence **is** within the query window but all instances are already handled — fixed by removing the `notInWindow &&` guard, so the event is always excluded when `handledUntil >= dtStart` and no valid instance is found

### UI Improvements
- Notification order reversed: closest upcoming event now appears **at the top** of the notification group
- Recurring events in the main list show the next occurrence date (`-> d. MMMM yyyy`)
- Recurring icon moved to appear before the next-occurrence date
- Recurring icon is now solid black for better visibility
- Calendar color accent bar widened to 8dp
- Demo mode now includes recurring event examples (weekly, monthly, quarterly, yearly)



## v0.10.0 (since v0.9.0)
### New Features

**Snooze**
Tap a notification to open the action dialog and choose to snooze a todo for 1 day, 3 days, 1 week, or 1 month. The notification is hidden until the snooze expires; an exact alarm ensures it reappears automatically.

**Recurring event support**
Recurring calendar events are detected automatically and shown with a recurring icon. Completing a recurring todo advances it to the next occurrence instead of deleting it — the "Mark as Done" button shows the date of the next recurrence.

**Notification tap → action dialog**
Tapping a notification now opens an action dialog with three choices: **Mark as Done**, **Snooze**, and **Open in Calendar**. This replaces the previous behaviour of opening the calendar app directly.

**Multiple calendars**
You can now add or remove any number of device calendars as todo sources (Settings → Calendar → **+ Add**). Existing single-calendar configurations are migrated automatically.

### Improvements

- **Flexible date-range filters**: the fixed "±1 week" / "±1 month" toggles in Settings have been replaced with **Days before today** and **Days after today** sliders (0–365 days, default: 30).
- Todo list is re-sorted by date after recurring event instance times are resolved.
- Recurring events correctly respect the date-range filter after their instance date is updated.

### Bug Fixes

- Fixed `handledUntil` timestamp using end-of-day normalisation so a recurring event is not shown again on the same day it was marked as done.
- Fixed "Mark as Done" label showing the current occurrence instead of the *next* one for recurring events.



## v0.9.0

Initial release.
 
## Features
 
- Reads todos from a DAVx5-synced Android calendar (calendar name configurable, default: `ToDo`)
- One persistent notification per todo — automatically restored within ~3 seconds if dismissed
- Tap a notification to open the event in Business Calendar 2 (falls back to system calendar)
- Delete a todo directly from the notification action button or from the app's list view
- Filters: hide old todos (<2026), or narrow to events within ±1 week or ±1 month
- Notifications update automatically when the calendar changes
- Survives device reboot
 
## Requirements
 
- Android 8.0+ (API 26+)
- DAVx5 with a calendar named **ToDo** synced to the device