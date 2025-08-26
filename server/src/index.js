/* eslint-disable no-console */
require('dotenv').config();
const express = require('express');
const cors = require('cors');

let admin = null;
let firestore = null;
let messaging = null;

function initializeFirebaseAdmin() {
  try {
    // Lazy require to avoid hard crash if package missing
    admin = require('firebase-admin');

    const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.FIREBASE_SERVICE_ACCOUNT_PATH;

    if (serviceAccountPath) {
      const serviceAccount = require(serviceAccountPath);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
      console.log('[firebase] Initialized with service account at', serviceAccountPath);
    } else {
      admin.initializeApp({
        credential: admin.credential.applicationDefault()
      });
      console.log('[firebase] Initialized with application default credentials');
    }

    firestore = admin.firestore();
    messaging = admin.messaging();
  } catch (error) {
    console.warn('[firebase] Admin SDK not initialized. Provide credentials to enable DB/FCM.', error.message);
  }
}

initializeFirebaseAdmin();

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', firebaseReady: Boolean(firestore && messaging) });
});

// List users from Firestore users collection
app.get('/api/users', async (req, res) => {
  if (!firestore) {
    return res.status(501).json({ error: 'Firestore not configured on server' });
  }
  try {
    const snapshot = await firestore.collection('users').limit(200).get();
    const users = snapshot.docs.map(doc => {
      const data = doc.data() || {};
      return {
        uid: doc.id,
        email: data.email || null,
        displayName: data.displayName || null,
        lastSeenAt: data.lastSeenAt || null,
        fcmTokensCount: Array.isArray(data.fcmTokens) ? data.fcmTokens.length : 0
      };
    });
    res.json({ users });
  } catch (err) {
    console.error('Failed to list users', err);
    res.status(500).json({ error: 'Failed to list users' });
  }
});

// Helper: recursively delete a document and its subcollections
async function deleteDocumentRecursively(docRef) {
  // Delete subcollections first
  const subcollections = await docRef.listCollections();
  for (const sub of subcollections) {
    const batchSize = 250;
    // Delete documents in batches and recurse
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const snapshot = await sub.limit(batchSize).get();
      if (snapshot.empty) break;
      for (const subDoc of snapshot.docs) {
        await deleteDocumentRecursively(subDoc.ref);
      }
      if (snapshot.size < batchSize) break;
    }
  }
  // Finally delete the document itself
  await docRef.delete().catch(() => {});
}

// Wipe a user: send FCM wipe and delete Firestore user doc + subcollections
app.post('/api/wipe/:uid', async (req, res) => {
  if (!firestore || !messaging) {
    return res.status(501).json({ error: 'Firestore/FCM not configured on server' });
  }
  const { uid } = req.params;
  if (!uid) return res.status(400).json({ error: 'uid required' });

  try {
    const userDocRef = firestore.collection('users').doc(uid);
    const userSnap = await userDocRef.get();
    if (!userSnap.exists) {
      return res.status(404).json({ error: 'User not found' });
    }
    const userData = userSnap.data() || {};
    let tokens = [];

    if (Array.isArray(userData.fcmTokens)) {
      tokens = userData.fcmTokens.filter(Boolean);
    } else {
      // Try tokens subcollection fallback
      const tokensSnap = await userDocRef.collection('tokens').get();
      tokens = tokensSnap.docs.map(d => d.get('token')).filter(Boolean);
    }

    // Send wipe command first
    if (tokens.length > 0) {
      try {
        const resp = await messaging.sendEachForMulticast({
          tokens,
          data: {
            action: 'wipe',
            reason: req.body?.reason || 'Admin initiated wipe'
          }
        });
        console.log(`[wipe] Sent to ${tokens.length} tokens. success=${resp.successCount} failure=${resp.failureCount}`);
      } catch (fcme) {
        console.warn('[wipe] FCM send failed', fcme.message);
      }
    } else {
      console.log('[wipe] No FCM tokens for user; proceeding with deletion only');
    }

    // Delete Firestore user data
    await deleteDocumentRecursively(userDocRef);

    res.json({ status: 'ok', uid, deleted: true, notified: tokens.length });
  } catch (err) {
    console.error('[wipe] Failed', err);
    res.status(500).json({ error: 'Wipe failed' });
  }
});

// Serve admin UI
app.use('/', express.static(require('path').join(__dirname, '..', 'public', 'admin')));

app.listen(PORT, () => {
  console.log(`Server listening on http://localhost:${PORT}`);
});