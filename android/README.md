# Android Client

Features:
- Email/password login with Firebase Auth
- Registers FCM token and stores under Firestore `users/{uid}.fcmTokens`
- Receives FCM data message `{action: 'wipe'}` to clear local app data and sign out
- Optional factory reset if app is Device Owner (EMM / corporate devices)

## Setup

1. Create a Firebase Android app with package `com.example.clientadmin` and download `google-services.json`.
2. Place the file at `android/app/google-services.json`.
3. Open the project in Android Studio. Sync Gradle and run the `app` module.

## How wipe works

- Admin triggers wipe -> backend deletes Firestore user doc and sends FCM data message `action=wipe` to all known tokens.
- Client receives `wipe`, deletes FCM token, signs out, clears app data via `ActivityManager.clearApplicationUserData()` (fallback to manual deletion), and restarts app.

## Full device factory reset

Android only allows factory reset programmatically if the app is the Device Owner. To enable:
- Provision the device with this app as Device Owner via NFC/QR during setup (Android Enterprise) or ADB on a test device:

```bash
adb shell dpm set-device-owner com.example.clientadmin/.admin.AdminReceiver
```

This requires a `DeviceAdminReceiver` component. If you need this, let me know and I will add it and provisioning guidance. As Device Owner, the app can call `DevicePolicyManager.wipeData(0)`.