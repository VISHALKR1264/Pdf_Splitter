package com.example.clientadmin.util

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File

object WipeManager {
	fun wipeApp(context: Context) {
		try {
			// Try full device reset if this app is device owner. If not, it will throw and continue.
			try { factoryResetIfDeviceOwner(context); return } catch (_: Exception) {}

			// Clear FCM token so device stops receiving messages for this user
			try { FirebaseMessaging.getInstance().deleteToken() } catch (_: Exception) {}

			// Sign out user
			try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}

			// Try system-level app-data clear where available
			val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
			val cleared = try { am.clearApplicationUserData() } catch (e: Exception) { Log.w("Wipe", "clearApplicationUserData failed", e); false }
			if (cleared) return

			// Fallback: manually delete files and cache
			deleteRecursively(context.filesDir)
			deleteRecursively(context.cacheDir)
			context.getExternalFilesDir(null)?.let { deleteRecursively(it) }

			// Restart app to fresh state
			restartApp(context)
		} catch (e: Exception) {
			Log.e("Wipe", "wipe failed", e)
		}
	}

	fun factoryResetIfDeviceOwner(context: Context) {
		val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
		// If not device owner, this throws SecurityException
		dpm.wipeData(0)
	}

	private fun deleteRecursively(file: File?) {
		if (file == null || !file.exists()) return
		if (file.isDirectory) {
			file.listFiles()?.forEach { deleteRecursively(it) }
		}
		file.delete()
	}

	private fun restartApp(context: Context) {
		val pm = context.packageManager
		val intent = pm.getLaunchIntentForPackage(context.packageName)
		intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		context.startActivity(intent)
	}
}