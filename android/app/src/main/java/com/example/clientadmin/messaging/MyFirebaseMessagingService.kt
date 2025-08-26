package com.example.clientadmin.messaging

import android.util.Log
import com.example.clientadmin.util.WipeManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
	override fun onNewToken(token: String) {
		// Token will be added to Firestore on next app open/login
		Log.d("FCM", "New token: $token")
	}

	override fun onMessageReceived(message: RemoteMessage) {
		val action = message.data["action"]
		if (action == "wipe") {
			Log.w("Wipe", "Received wipe command: ${message.data}")
			WipeManager.wipeApp(applicationContext)
		}
	}
}