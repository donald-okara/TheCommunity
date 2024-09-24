package com.example.thecommunity.data.repositories

import android.content.Context
import android.util.Log
import com.example.thecommunity.data.model.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val context: Context,
    coroutineScope: CoroutineScope // Add CoroutineScope for coroutine management

) {
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> get() = _isAdmin

    init {
        // Initialize the admin status when the repository is created
        coroutineScope.launch{
            fetchAdminStatus()
        }

    }

    /**
     * CREATE
     */
    //Create user functionality is in sign_in package

    /**
     * READ
     */

    /**
     * Fetch admin status
     */
    private suspend fun fetchAdminStatus() {
        val userId = getCurrentUserId()
        if (userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    val userData = userDoc.toObject(UserData::class.java)
                    _isAdmin.value = userData?.admin ?: false
                } catch (e: Exception) {
                    Log.e("UserRepository", "Error fetching admin status", e)
                    _isAdmin.value = false
                }
            }
        } else {
            _isAdmin.value = false
        }
    }

    suspend fun refreshAdminStatus() {
        fetchAdminStatus()
    }

    suspend fun fetchUsersByCommunity(
        communityId: String
    ): List<UserData> {
        return try {
            // Fetch all users
            val usersSnapshot = db.collection("users")
                .whereNotEqualTo("userId", getCurrentUserId())
                .get()
                .await()
            val users = usersSnapshot.toObjects(UserData::class.java)

            // Filter users who belong to the specified community
            users.filter { user ->
                user.communities.any { community -> community["communityId"] == communityId }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching users by community: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch users
     */
    suspend fun fetchUsers(): List<UserData> {
        return try {
            val snapshot = db.collection("users")
                .whereNotEqualTo("userId", getCurrentUserId())
                .get()
                .await()
            snapshot.toObjects(UserData::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching users", e)
            emptyList()
        }
    }

    suspend fun fetchUsersByIds(
        userIds: List<String>
    ): List<UserData> {
        return try {
            val users = db.collection("users")
                .whereIn("userId", userIds)
                .get()
                .await()
            users.toObjects(UserData::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching users by IDs", e)
            emptyList()
        }
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): UserData? {
        val authUser = auth.currentUser
        val firestoreUserData = authUser?.let {
            val userDoc = db.collection("users")
                .document(it.uid)
                .get(Source.DEFAULT)
                .await()
            userDoc.toObject(UserData::class.java)
        }

        return firestoreUserData?.apply {
            this.userId = authUser.uid
            this.username = authUser.displayName ?: this.username
            this.profilePictureUrl = authUser.photoUrl?.toString() ?: this.profilePictureUrl
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * UPDATE
     */

    /**
     * Update user communities and space
     */
    suspend fun updateUserCommunities(userId: String, communityId: String, role: String) {
        try {
            // Fetch the current user document
            val userDocRef = db.collection("users").document(userId)
            val document = userDocRef.get().await()

            if (document.exists()) {
                val userData = document.toObject(UserData::class.java)
                userData?.let { user ->
                    val updatedCommunities = user.communities.toMutableList()
                    val existingCommunityIndex = updatedCommunities.indexOfFirst { community -> community.keys.first() == communityId }

                    if (existingCommunityIndex != -1) {
                        // Update the role if community already exists
                        updatedCommunities[existingCommunityIndex] = mapOf(communityId to role)
                    } else {
                        // Add new community with role
                        updatedCommunities.add(mapOf(communityId to role))
                    }

                    // Set updated data back to the user document
                    userDocRef.set(user.copy(communities = updatedCommunities)).await()
                    Log.d("UserRepository", "User data updated successfully for userId: $userId" + "for community : $communityId")
                } ?: Log.w("UserRepository", "No user data found for userId: $userId")
            } else {
                Log.w("UserRepository", "No such user data found for userId: $userId")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user data", e)
        }
    }



    /**
     * Update user role in community
     */
    suspend fun updateUserRoleInCommunity(
        userId: String,
        communityId: String,
        newRole: String) {
        try {
            val userDocRef = db.collection("users").document(userId)
            val document = userDocRef.get().await()

            if (document.exists()) {
                val userData = document.toObject(UserData::class.java)
                userData?.let {
                    val updatedCommunities = it.communities.toMutableList()

                    // Check if the user already has a role in the community
                    val communityIndex = updatedCommunities.indexOfFirst { community -> community.keys.first() == communityId }

                    if (communityIndex != -1) {
                        // Update the existing role
                        updatedCommunities[communityIndex] = mapOf(communityId to newRole)
                    } else {
                        // Add a new role for the community
                        updatedCommunities.add(mapOf(communityId to newRole))
                    }

                    it.communities = updatedCommunities

                    // Save the updated user data
                    userDocRef.set(it).await()
                    Log.d("UserRepository", "User role updated successfully for userId: $userId in community: $communityId")
                } ?: Log.w("UserRepository", "No user data found for userId: $userId")
            } else {
                Log.w("UserRepository", "No such user data found for userId: $userId")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user role in community", e)
        }
    }



    /**
     * DELETE
     */
    suspend fun removeCommunityFromUser(
        userId: String,
        communityId: String
    ) {
        try {
            // Fetch the user document
            val userDoc = db.collection("users").document(userId).get().await()
            val user = userDoc.toObject(UserData::class.java)

            user?.let {
                // Remove the community from the user's list of communities
                val updatedCommunities = it.communities.filter { community ->
                    community.keys.firstOrNull() != communityId
                }

                // Update the user document with the modified communities list
                db.collection("users").document(userId)
                    .update("communities", updatedCommunities)
                    .await()

                Log.d("UserRepository", "Removed community $communityId from user $userId")
            } ?: run {
                Log.e("UserRepository", "User $userId not found")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing community $communityId from user $userId", e)
        }
    }



    /**
     * CleanUp
     */

    suspend fun cleanUpUserCommunities() {
        try {
            val usersSnapshot = db.collection("users").get().await()
            val users = usersSnapshot.documents.mapNotNull { it.toObject(UserData::class.java) }

            for (user in users) {
                val existingCommunities = user.communities.map { it.keys.firstOrNull() }.filterNotNull()

                var operationsSuccessful = true

                for (communityId in existingCommunities) {
                    try {
                        val communityDoc = db.collection("communities").document(communityId).get().await()
                        if (!communityDoc.exists()) {
                            // Community does not exist, remove it from user's communities
                            val updatedCommunities = user.communities.filterNot { it.keys.firstOrNull() == communityId }
                            db.collection("users").document(user.userId)
                                .update("communities", updatedCommunities).await()

                            Log.d("Cleanup", "Removed non-existing community $communityId from user ${user.userId}")
                        }
                    } catch (e: Exception) {
                        Log.e("Cleanup", "Error checking existence of community $communityId for user ${user.userId}", e)
                        operationsSuccessful = false
                    }
                }

                if (!operationsSuccessful) {
                    Log.e("Cleanup", "Failed to clean up communities for user ${user.userId}")
                }
            }
        } catch (e: Exception) {
            Log.e("Cleanup", "Error fetching users for cleanup", e)
        }
    }

}
