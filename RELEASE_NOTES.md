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
