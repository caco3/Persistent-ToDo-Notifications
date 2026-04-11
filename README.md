# Todo Notifications

An Android app that keeps your todos as **persistent (ongoing) notifications** — they cannot be accidentally swiped away.

## Features

- Add todos from the app or directly from the notification
- Ongoing notification in the notification shade that **cannot be dismissed by swiping**
- Expanded notification view shows up to 6 pending todos at once
- "Done" action button on the notification to complete the first todo without opening the app
- Todos persist across app restarts (SharedPreferences)
- Notification is automatically restored after device reboot
- Clean Material 3 UI

## How ongoing notifications work

The notification uses `setOngoing(true)` via `NotificationCompat.Builder`. This flag prevents the user from dismissing the notification with a swipe. The notification stays visible until:
- The app explicitly cancels it, or
- The user force-stops the app from system settings

On device reboot, `BootReceiver` re-posts the notification automatically.

## Requirements

- **Android 8.0+ (API 26+)**
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK with Build Tools 34

## Setup

1. **Open in Android Studio:**
   ```
   File → Open → select the TodoNotifications folder
   ```

2. **Sync Gradle:** Android Studio will prompt to sync — click "Sync Now".

3. **Build and run** on a device or emulator (API 26+).

4. **Grant notification permission** when prompted (required on Android 13+).

> **Note:** `local.properties` is generated automatically by Android Studio with your local SDK path. Do not commit it.

## Project Structure

```
app/src/main/
├── java/com/example/todonotifications/
│   ├── TodoItem.kt                  # Data class
│   ├── TodoPreferences.kt           # SharedPreferences storage
│   ├── NotificationHelper.kt        # Builds and posts the ongoing notification
│   ├── NotificationActionReceiver.kt # Handles notification button actions
│   ├── BootReceiver.kt              # Restores notification after reboot
│   ├── MainActivity.kt              # Main UI
│   └── TodoAdapter.kt               # RecyclerView adapter
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   └── item_todo.xml
    ├── values/
    │   ├── strings.xml
    │   ├── themes.xml
    │   └── colors.xml
    └── drawable/                    # Vector icons
```

## Permissions Used

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Show notifications (required to request at runtime on Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-post notification after device reboot |
