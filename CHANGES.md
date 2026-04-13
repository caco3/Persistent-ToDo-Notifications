# Change Log

## 2026-04-13

### Notification ordering
- **`TodoForegroundService.kt`**: Reversed notification posting order (`todos.reversed().forEachIndexed`).
  Previously the closest future event was posted first and appeared at the bottom of the group; now it is posted last and appears at the top.

### Next recurrence date in main app
- **`TodoItem.kt`**: Added `nextDtStart: Long?` field (defaults to `null`).
- **`CalendarTodoSource.kt`**: For each recurring event in the final result list, computes `nextDtStart` via `findNextInstanceAfter` (anchor = end-of-day of current instance or now, whichever is later) and stores it on the `TodoItem`.
- **`TodoAdapter.kt`**: Split date row into three parts — current date/time (`textTodoDate`), recurring icon (`iconRecurring`), and next occurrence (`textTodoNextDate` showing `-> d. MMMM yyyy`).
- **`item_todo.xml`**: Added `textTodoNextDate` `TextView` after `iconRecurring` in the horizontal row.
- **`NotificationHelper.kt`**: Reverted `buildTodoNotification` to plain date/time only (no next-instance lookup in notifications).

### Recurring icon styling
- **`item_todo.xml`**: Set `android:alpha="1.0"` and `android:tint="@android:color/black"` on `iconRecurring` for better visibility.

### Bug fix: recurring event re-appears after mark-as-done
- **`CalendarTodoSource.kt`** (`getTodos`): Changed `todos.map` to `todos.mapNotNull`. For recurring events where no instance exists in the query window AND `handledUntil >= todo.dtStart`, the event is now excluded (`null`) instead of falling through with its stale base `dtStart`.
  - Root cause: yearly events (e.g. April 2) have their next occurrence (April 2, 2027) outside the `daysAfter` window. Without an entry in `instanceMap`, the event kept its raw `CalendarContract.Events.DTSTART` (April 2, 2026), which still passed the range filter.
- Added `Log.d` statements for debugging the instance/handledUntil resolution per event.
