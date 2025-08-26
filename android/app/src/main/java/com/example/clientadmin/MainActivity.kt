package com.example.clientadmin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
	private lateinit var auth: FirebaseAuth
	private lateinit var db: FirebaseFirestore

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)

		auth = FirebaseAuth.getInstance()
		db = FirebaseFirestore.getInstance()

		val statusText = findViewById<TextView>(R.id.statusText)
		val emailInput = findViewById<EditText>(R.id.emailInput)
		val passwordInput = findViewById<EditText>(R.id.passwordInput)
		val loginButton = findViewById<Button>(R.id.loginButton)
		val signOutButton = findViewById<Button>(R.id.signOutButton)

		fun refreshStatus() {
			val user = auth.currentUser
			statusText.text = if (user == null) {
				"Not signed in"
			} else {
				"Signed in as ${user.email ?: user.uid}"
			}
			signOutButton.isEnabled = user != null
		}

		fun registerTokenAndUser() {
			val user = auth.currentUser ?: return
			val userDoc = db.collection("users").document(user.uid)
			val data = hashMapOf(
				"email" to (user.email ?: ""),
				"displayName" to (user.displayName ?: ""),
				"lastSeenAt" to System.currentTimeMillis()
			)
			userDoc.set(data, com.google.firebase.firestore.SetOptions.merge())

			FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
				if (!token.isNullOrBlank()) {
					userDoc.update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
				}
			}
		}

		loginButton.setOnClickListener {
			val email = emailInput.text.toString().trim()
			val password = passwordInput.text.toString()
			if (email.isEmpty() || password.length < 6) {
				statusText.text = "Enter email and password (>=6 chars)"
				return@setOnClickListener
			}

			auth.signInWithEmailAndPassword(email, password)
				.addOnSuccessListener {
					refreshStatus()
					registerTokenAndUser()
				}
				.addOnFailureListener {
					auth.createUserWithEmailAndPassword(email, password)
						.addOnSuccessListener { result ->
							result.user?.updateProfile(userProfileChangeRequest { displayName = email.substringBefore('@') })
							refreshStatus()
							registerTokenAndUser()
						}
						.addOnFailureListener { e -> statusText.text = "Auth failed: ${e.message}" }
				}
		}

		signOutButton.setOnClickListener {
			com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
				.addOnCompleteListener {
					auth.signOut()
					refreshStatus()
				}
		}

		refreshStatus()
	}
}