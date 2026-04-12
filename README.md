# Todo Notifications

An Android app that reads events from a DAVx5-synced **"ToDo" calendar** and displays them as **persistent notifications** in the notification shade. In case they get swiped away by accident, they will show up after some seconds.

<video src="https://github.com/user-attachments/assets/3493ffd2-f941-403a-b5a1-96239d1fc2fc" autoplay loop muted playsinline></video>

## Features

- Reads todos from the Android calendars (DAVx5 calendars)
- One notification card per todo
- Notifications are restored automatically within ~3 seconds if dismissed
- **Tap a notification card** to open an action dialog with:
  - **Mark as Done** — deletes the event (non-recurring), or advances to the next occurrence (recurring); the button label shows the next recurrence date
  - **Snooze** — hide the notification for a given time
  - **Open in Calendar** — opens the event in [Business Calendar 2](https://play.google.com/store/apps/details?id=com.appgenix.bizcal) (fallback: system calendar)
- **Recurring event support** — recurring events are detected automatically and shown with a recurring icon; completing one advances it to the next occurrence instead of deleting it
- **Delete** a todo directly from the app's list view
- **Multiple calendars** — add or remove any number of device calendars as todo sources (Settings → Calendar)
- Filters (via **⋮ overflow menu → Settings**):
  - **Show ToDos from before 2026** toggle
  - **Days before today** slider (0–365, default: 30)
  - **Days after today** slider (0–365, default: 30)
- **Demo mode** (via Settings → Development) — shows synthetic dummy todos (no calendar required)
- Notifications update automatically when the calendar changes

## Screenshots

| App list view | Settings | Notifications (collapsed) | Notifications (expanded) |
|:---:|:---:|:---:|:---:|
| ![App list](screenshots/main%20screen.jpg) | ![Settings](screenshots/settings.jpg) | ![Collapsed](screenshots/collapsed%20notifications.jpg) | ![Expanded](screenshots/expanded%20notifications.jpg) |

## How notification persistence works

- A **foreground service** (`TodoForegroundService`) keeps a summary notification alive permanently.
- Individual per-todo notifications use `setOngoing(true)` but can be dismissed on Android 14+.
- A **watchdog** runs every 3 seconds: it checks `NotificationManager.getActiveNotifications()` and reposts any missing individual notifications via `nm.notify()`.
- To avoid Android's notification rate-limiting, individual notifications are posted with a 250 ms stagger between each.
- On device reboot, `BootReceiver` starts `TodoForegroundService`. `startForeground()` is called immediately in `onStartCommand()` before any calendar query, satisfying Android's 5-second foreground service deadline even when the calendar ContentProvider is slow to become available at boot.

## Requirements

- **Android 8.0+ (API 26+)**
- Android SDK with Build Tools 34 (installed automatically by `build.sh`, or via Android Studio)
- JDK 17+ (for `build.sh`)
- DAVx5 with a calendar named exactly **"ToDo"** synced to the device

## Settings

Accessible via the **⋮ overflow menu → Settings** and the **About** item in the toolbar.

**Filters**

| Setting | Description |
|---|---|
| Show ToDos from before 2026 | Include events with a start date before 2026 |
| Days before today | Slider (0–365, default: 30) — how many days into the past to include |
| Days after today | Slider (0–365, default: 30) — how many days into the future to include |

**Calendar**

Add or remove any number of device calendars as todo sources. Tap **+ Add** to pick from the calendars available on the device; tap the remove button next to an entry to stop reading from that calendar. Existing single-name settings are migrated automatically.

**Development**

| Setting | Description |
|---|---|
| Demo mode | Show synthetic dummy todos — no calendar required (useful for screenshots/testing) |

## Installation

1. Download the latest APK from the [Releases](https://github.com/caco3/Persistent-ToDo-Notifications/releases) page.
2. On your device, enable **Install from unknown sources** (Settings → Apps → Special app access) if not already allowed.
3. Open the downloaded `.apk` to install.
4. Grant **Calendar** and **Notification** permissions when prompted.

## Development

### Option A — Android Studio

1. **Open in Android Studio:** `File → Open → select the TodoNotifications folder`
2. **Sync Gradle:** click "Sync Now" when prompted.
3. **Build and run** on a device or emulator (API 26+).
4. **Grant permissions** when prompted: Calendar (read + write) and Notifications.

> `local.properties` is generated automatically by Android Studio. Do not commit it.

### Option B (Linux) — `build.sh` (no Android Studio required)

`build.sh` downloads all required SDK components and builds the APK entirely from the command line.

**Prerequisites:** `java` (JDK 17+), `curl`, `unzip`

```bash
# Build debug APK
./build.sh

# Build and immediately flash to a connected device via adb
./build.sh -f
```

What the script does (each step is skipped on subsequent runs if already done):

1. Downloads Android command-line tools → `~/android-sdk`
2. Accepts SDK licences
3. Installs `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`
4. Downloads Gradle 8.4 → `~/.gradle-dist/gradle-8.4`
5. Generates a `./gradlew` wrapper for future manual use
6. Writes `local.properties` pointing to the SDK
7. Builds `app/build/outputs/apk/debug/*.apk`

With `-f`, the script also runs `adb install -r <apk>` to flash the built APK onto a connected device or emulator.

## Project Structure

```
build.sh                              # CLI build + flash script (no Android Studio needed)
app/src/main/
├── java/com/example/todonotifications/
│   ├── TodoItem.kt                   # Data class (id, title, dtStart, isRecurring)
│   ├── CalendarTodoSource.kt         # Reads events from the calendar(s); recurring instance resolution
│   ├── AppPreferences.kt             # SharedPreferences: filters, snooze state, handled-until, calendar names
│   ├── NotificationHelper.kt         # Builds summary + per-todo notifications
│   ├── NotificationActionReceiver.kt # Handles ACTION_REPOST, ACTION_DELETE_TODO, ACTION_SNOOZE_TODO, ACTION_DONE_RECURRING; schedules exact alarms
│   ├── TodoForegroundService.kt      # Foreground service + 3s watchdog
│   ├── BootReceiver.kt               # Restarts service after reboot
│   ├── MainActivity.kt               # Main UI (list, overflow menu)
│   ├── SettingsActivity.kt           # Settings screen (filters, multiple calendars, demo mode)
│   ├── TodoActionActivity.kt         # Action dialog: Mark as Done / Snooze / Open in Calendar
│   ├── SnoozePickerActivity.kt       # Snooze duration picker dialog
│   ├── TodoAdapter.kt                # RecyclerView adapter
│   └── TodoPreferences.kt            # Per-todo preference helpers
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_settings.xml
    │   ├── item_todo.xml
    │   ├── dialog_todo_action.xml    # Action dialog layout (Done / Snooze / Open in Calendar)
    │   ├── item_calendar_name.xml    # Row in the multi-calendar list
    │   ├── notification_collapsed.xml
    │   ├── notification_expanded.xml
    │   └── notification_row.xml
    ├── menu/
    │   └── menu_main.xml             # Overflow menu items
    ├── values/
    │   ├── strings.xml
    │   ├── themes.xml
    │   └── colors.xml
    └── drawable/                     # Vector icons (incl. ic_recurring.xml, ic_snooze.xml)
```

## Permissions Used

| Permission | Purpose |
|---|---|
| `READ_CALENDAR` | Read events from the "ToDo" calendar |
| `WRITE_CALENDAR` | Delete events from app / notification |
| `POST_NOTIFICATIONS` | Show notifications (required at runtime on Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Restart notification service after reboot |
| `FOREGROUND_SERVICE` | Keep the summary notification alive |

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

# Copyright

Copyright (c) 2026 George Ruinelli <caco3@ruinelli.ch>
