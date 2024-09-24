package com.example.thecommunity.presentation.sign_in

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.thecommunity.R
import com.example.thecommunity.data.model.SignInResult
import com.example.thecommunity.data.model.UserData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class GoogleAuthUiClient(
    private val context: Context
) {
    private val auth = Firebase.auth
    private val googleSignInClient: GoogleSignInClient = buildGoogleSignInClient()

    // Initiate the Google sign-in intent
    fun signInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // Handle the Google sign-in result
    suspend fun getSignInWithIntent(intent: Intent): SignInResult {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        return try {
            val account = task.getResult(ApiException::class.java)
            val googleCredentials = GoogleAuthProvider.getCredential(account?.idToken, null)
            val user = auth.signInWithCredential(googleCredentials).await().user

            user?.let {
                try {
                    updateFirestoreUser(it)
                } catch (e: Exception) {
                    Log.e("GoogleAuthUiClient", "Error adding/updating user in Firestore: ${e.message}", e)
                }
            }

            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        alias = null,
                        profilePictureUrl = photoUrl?.toString(),
                        headerImageUrl = null,
                        dateOfBirth = null,
                        biography = null,
                        biographyBackgroundImageUrl = null,
                        communities = emptyList(),
                        spaces = emptyList(),
                        events = emptyList()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    // Sign out from Google and Firebase
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    // Update Firestore user details
    private suspend fun updateFirestoreUser(user: FirebaseUser) {
        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection("users").document(user.uid)
        val documentSnapshot = docRef.get().await()

        val userData = UserData(
            userId = user.uid,
            username = user.displayName,
            alias = null,
            profilePictureUrl = user.photoUrl?.toString(),
            headerImageUrl = null,
            dateOfBirth = null,
            biography = null,
            biographyBackgroundImageUrl = null,
            communities = emptyList(),
            spaces = emptyList(),
            events = emptyList()
        )

        if (!documentSnapshot.exists()) {
            // Document doesn't exist, perform initial write
            docRef.set(userData).await()
        } else {
            // Document exists, perform updates as needed (if any)
            val updates = mutableMapOf<String, Any?>()

            UserData::class.memberProperties.forEach { property ->
                property.isAccessible = true
                val fieldName = property.name
                val fieldValue = property.get(userData)

                if (!documentSnapshot.contains(fieldName)) {
                    updates[fieldName] = fieldValue
                }
            }

            if (updates.isNotEmpty()) {
                docRef.update(updates).await()
            } else {
                Log.d("UpdateFirestore", "No updates necessary for document")
            }
        }
    }

    // Build GoogleSignInOptions and create the GoogleSignInClient
    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, googleSignInOptions)
    }
}
