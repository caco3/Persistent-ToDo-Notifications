# Todo Notifications

An Android app that reads events from a DAVx5-synced **"ToDo" calendar** and displays them as **persistent grouped notifications** in the notification shade.

## Features

- Reads todos from the Android calendar (DAVx5 "ToDo" calendar)
- One notification card per todo — BC2-style grouped individual notifications
- Notifications are restored automatically within ~3 seconds if dismissed (watchdog)
- Tap a notification card to open the event in BC2 calendar app (fallback: system calendar)
- **Delete** a todo directly from the notification card ("Delete" action button) or from the app's list view
- Filters (via overflow menu):
  - **Show ToDos from <2026** toggle
  - **Only show ToDos within ±1 week** toggle
  - **Only show ToDos within ±1 month** toggle
  - **Demo mode** — shows synthetic dummy todos (no calendar required)
- Notifications update automatically when the calendar changes (ContentObserver)
- Notifications reliably restored after device reboot (foreground service started immediately on `BOOT_COMPLETED`)
- About dialog with app version info
- Clean Material 3 UI

## Screenshots

| App list view | Notifications (collapsed) | Notifications (expanded) |
|:---:|:---:|:---:|
| ![App list](screenshots/app_list.png) | ![Notifications collapsed](screenshots/notif_collapsed.png) | ![Notifications expanded](screenshots/notif_expanded.png) |

> Place your screenshots in the `screenshots/` folder with the filenames above.

## How notification persistence works

- A **foreground service** (`TodoForegroundService`) keeps a summary notification alive permanently.
- Individual per-todo notifications use `setOngoing(true)` but can be dismissed on Android 14+.
- A **watchdog** runs every 3 seconds: it checks `NotificationManager.getActiveNotifications()` and reposts any missing individual notifications via `nm.notify()`.
- To avoid Android's notification rate-limiting, individual notifications are posted with a 250 ms stagger between each.
- On device reboot, `BootReceiver` starts `TodoForegroundService`. `startForeground()` is called immediately in `onStartCommand()` before any calendar query, satisfying Android's 5-second foreground service deadline even when the calendar ContentProvider is slow to become available at boot.

## Requirements

- **Android 8.0+ (API 26+)**
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK with Build Tools 34
- DAVx5 with a calendar named exactly **"ToDo"** synced to the device

## Setup

1. **Open in Android Studio:**
   ```
   File → Open → select the TodoNotifications folder
   ```

2. **Sync Gradle:** Android Studio will prompt to sync — click "Sync Now".

3. **Build and run** on a device or emulator (API 26+).

4. **Grant permissions** when prompted: Calendar (read + write) and Notifications.

> **Note:** `local.properties` is generated automatically by Android Studio with your local SDK path. Do not commit it.

## Project Structure

```
app/src/main/
├── java/com/example/todonotifications/
│   ├── TodoItem.kt                   # Data class
│   ├── CalendarTodoSource.kt         # Reads events from the "ToDo" calendar
│   ├── AppPreferences.kt             # SharedPreferences for filter toggles
│   ├── NotificationHelper.kt         # Builds summary + per-todo notifications
│   ├── NotificationActionReceiver.kt # Handles ACTION_REPOST and ACTION_DELETE_TODO
│   ├── TodoForegroundService.kt      # Foreground service + watchdog
│   ├── BootReceiver.kt               # Restarts service after reboot
│   ├── MainActivity.kt               # Main UI with filter toggles and delete
│   └── TodoAdapter.kt                # RecyclerView adapter
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   └── item_todo.xml
    ├── values/
    │   ├── strings.xml
    │   ├── themes.xml
    │   └── colors.xml
    └── drawable/                     # Vector icons
```

## Permissions Used

| Permission | Purpose |
|---|---|
| `READ_CALENDAR` | Read events from the "ToDo" calendar |
| `WRITE_CALENDAR` | Delete events from app / notification |
| `POST_NOTIFICATIONS` | Show notifications (required at runtime on Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Restart notification service after reboot |
| `FOREGROUND_SERVICE` | Keep the summary notification alive |
