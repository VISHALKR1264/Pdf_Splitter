# Admin Wipe Server

This server exposes:
- GET /api/users: list users from Firestore `users` collection
- POST /api/wipe/:uid: send FCM data message `{action: 'wipe'}` and recursively delete `users/{uid}` doc & subcollections
- Serves a minimal admin UI at `/`

## Setup

1. Create a Firebase project and a service account key with Firestore and FCM access. Download JSON.
2. Copy `.env.example` to `.env` and set `FIREBASE_SERVICE_ACCOUNT_PATH` to the absolute path of the JSON file, or set `GOOGLE_APPLICATION_CREDENTIALS` in your environment.
3. Install dependencies:

```bash
cd server
npm install
```

4. Start the server:

```bash
npm start
```

Open `http://localhost:8080/` for the admin panel.

Notes:
- Users are read from `users` collection. Each user doc may include an array field `fcmTokens` for device tokens, or a subcollection `tokens` with docs `{token: string}`.
- The wipe endpoint sends the FCM command (if tokens exist) and then deletes the Firestore user doc recursively.