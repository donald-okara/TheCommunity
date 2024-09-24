package com.example.thecommunity.presentation.sign_in

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thecommunity.data.model.SignInResult
import com.example.thecommunity.data.model.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class SignInViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    fun onSignInResult(result: SignInResult){
        _state.update {
            val isSuccessful = result.data != null
            Log.d("SignInResult", "Sign-in result: Success = $isSuccessful, Error = ${result.errorMessage}")
            it.copy(
                isSignInSuccessful = isSuccessful,
                signInError = result.errorMessage
            )
        }
    }

    fun setLoading(isLoading: Boolean) {
        _state.update {
            it.copy(isLoading = isLoading)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            Log.d("SignInWithEmail", "Attempting sign-in with email: $email")

            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                Log.d("SignInWithEmail", "Sign-in successful: ${user != null}")

                user?.let {
                    try {
                        updateFirestoreUser(it)
                    } catch (e: Exception) {
                        Log.e("SignInWithEmail", "Error adding/updating user in Firestore: ${e.message}", e)
                    }
                }

                _state.update { it.copy(isLoading = false, isSignInSuccessful = user != null) }
            } catch (e: Exception) {
                Log.e("SignInWithEmail", "Sign-in failed: ${e.message}", e)
                _state.update { it.copy(isLoading = false, signInError = e.message) }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            Log.d("SignUpWithEmail", "Attempting sign-up with email: $email")

            try {
                // Check if the email is already in use
                val signInMethods = firebaseAuth.fetchSignInMethodsForEmail(email).await()
                if (signInMethods.signInMethods?.isNotEmpty() == true) {
                    Log.d("SignUpWithEmail", "Email is already in use.")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            signUpError = "Email is already in use."
                        )
                    }
                    return@launch
                }

                // Proceed with sign-up
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                Log.d("SignUpWithEmail", "Sign-up successful: ${user != null}")

                if (user != null) {
                    addToFirestore(user)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            signUpError = null,
                            isSignInSuccessful = true
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            signUpError = "Sign up failed",
                            isSignInSuccessful = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SignUpWithEmail", "Sign-up failed: ${e.message}", e)
                _state.update { it.copy(isLoading = false, signUpError = e.message) }
            }
        }
    }

    private suspend fun addToFirestore(user: FirebaseUser?) {
        user?.let {
            try {
                updateFirestoreUser(it)
            } catch (e: Exception) {
                Log.e("AddToFirestore", "Error adding/updating user in Firestore: ${e.message}", e)
            }
        }
    }

    fun getSignedInUser(): UserData? {
        val user = firebaseAuth.currentUser
        val userData = user?.let {
            UserData(
                userId = it.uid,
                username = it.displayName ?: "Default Username",
                alias = null,
                profilePictureUrl = it.photoUrl?.toString(),
                headerImageUrl = null,
                dateOfBirth = null,
                biography = null,
                biographyBackgroundImageUrl = null,
                communities = emptyList(),
                spaces = emptyList(),
                events = emptyList()
            )
        }
        Log.d("GetSignedInUser", "Current signed-in user: $userData")
        return userData
    }

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
            Log.d("UpdateFirestoreUser", "User document created in Firestore.")
        } else {
            // Document exists, perform updates as needed
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
                Log.d("UpdateFirestoreUser", "User document updated in Firestore.")
            } else {
                Log.d("UpdateFirestoreUser", "No updates necessary for document.")
            }
        }
    }

    fun resetState(){
        _state.update { SignInState() }
    }
}
