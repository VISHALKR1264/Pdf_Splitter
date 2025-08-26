## Client–Admin Mobile App (Android + Admin Panel)

This project includes:
- Android client app (Firebase Auth, Firestore, FCM) that can be remotely wiped
- Node/Express admin server with a minimal web UI to list users and trigger wipes

Important: Full factory reset is only possible on managed devices where the app is Device Owner. Otherwise, the app performs a full in-app data reset and signs the user out, while server deletes all cloud records.

### Prerequisites
- Firebase project with Firestore and Cloud Messaging enabled
- Service account JSON with Firestore Admin + FCM permissions
- Node.js 18+
- Android Studio (to build and run the app)

### Backend setup (Admin server)
1) Put your service account JSON on disk. Copy `server/.env.example` to `server/.env` and set `FIREBASE_SERVICE_ACCOUNT_PATH=/abs/path/to/serviceAccountKey.json`.
2) Install and start the server:
```bash
cd server
npm install
npm start
```
3) Open the admin panel at `http://localhost:8080/`.

The server exposes:
- GET `/api/users`: list `users`
- POST `/api/wipe/:uid`: sends FCM data message `{action: 'wipe'}` to known tokens and recursively deletes `users/{uid}` and subcollections

Recommended Firestore rules (client reads/writes only own profile; server uses Admin SDK and bypasses rules):
```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, update, create: if request.auth != null && request.auth.uid == uid;
      allow delete: if false; // deletion is performed by the admin server
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == uid;
      }
    }
  }
}
```

### Android app setup
1) In Firebase console, register an Android app with package `com.example.clientadmin` and download `google-services.json`.
2) Place it at `android/app/google-services.json` (replace the placeholder file).
3) Open `android/` in Android Studio, sync Gradle, and run the app on a device.

Functions in the client app:
- Login/register using email + password
- Store/update user profile under `users/{uid}` and register FCM token (stored in `fcmTokens` array)
- Receive FCM data `action=wipe` and then:
  - Attempt factory reset if app is Device Owner
  - Otherwise delete FCM token, sign out, clear app data, and restart to fresh state

### Device Owner (optional, for true factory reset)
Android only allows programmatic factory reset if the app is Device Owner (Android Enterprise/EMM scenario). For testing on a dedicated device:
```bash
adb shell dpm set-device-owner com.example.clientadmin/.admin.AdminReceiver
```
If this fails, the device likely needs to be freshly provisioned. In production, use Android Enterprise provisioning (QR/NFC) to set device owner during setup.

### Data model
- `users/{uid}`: `{ email, displayName, lastSeenAt, fcmTokens: string[] }`
- Optional `users/{uid}/...` subcollections for per-user domain data (all deleted by the server wipe)

### Wipe behavior and timing
- Cloud records are removed immediately by the admin server.
- The client wipe is delivered via FCM; if the device is offline, wipe will occur when it reconnects and receives the message. The app also revalidates state on next open.

### Security notes
- Protect the admin server behind authentication (e.g., reverse proxy + SSO) in production.
- Keep the service account JSON secret. Prefer using `GOOGLE_APPLICATION_CREDENTIALS` on the host.
- The server uses Admin SDK and bypasses Firestore security rules; ensure only trusted admins can access it.
