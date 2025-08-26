package com.example.clientadmin

import android.app.Application
import com.google.firebase.FirebaseApp

class ClientAdminApp : Application() {
	override fun onCreate() {
		super.onCreate()
		FirebaseApp.initializeApp(this)
	}
}