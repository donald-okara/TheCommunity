package com.example.thecommunity.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.communities.JoinedStatusState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

/**
 * CAUTION
 *
 * Dear Dev,
 *
 * Trust me, all this works. The database updates, deletes as needed. Any refactoring of those fields will make your and everyone else's jobs a living hell because you will have to delete the whole database.
 *
 * By all means, add new fields. But DO NOT DELETE OR REFACTOR existing ones.
 */
class CommunityRepository(
    private val context: Context,
    val db: FirebaseFirestore,
    val firestore: FirebaseFirestore,
    private val firebaseService: FirebaseService,
    var userRepository: UserRepository,
    val storage: FirebaseStorage,
    val coroutineScope: CoroutineScope
) {

    /**
     * CREATE
     */

    /**
     * Request new community
     */
    suspend fun requestNewCommunity(//Batch transaction
        communityName: String,
        communityType: String,
        bannerUri: Uri?,
        profileUri: Uri?,
        aboutUs: String?,
        selectedLeaders: List<UserData>,
        selectedEditors: List<UserData>
    ) {
        val currentUser = userRepository.getCurrentUser()
        currentUser?.let { user ->
            val members = mutableListOf<Map<String, String>>(
                mapOf(user.userId to "leader")
            ).apply {
                addAll(selectedLeaders.map { mapOf(it.userId to "leader") })
                addAll(selectedEditors.map { mapOf(it.userId to "editor") })
            }
            val communityId = db.collection("communities").document().id // Generate a new ID

            // Upload images and get URLs
            val bannerUrl =
                bannerUri?.let { firebaseService.uploadImageToStorage(it, "communities/banners/$communityId.jpg") }
            val profileUrl = profileUri?.let {
                firebaseService.uploadImageToStorage(
                    it,
                    "communities/profileImages/$communityId.jpg"
                )
            }

            val community = Community(
                id = communityId,
                name = communityName,
                type = communityType,
                members = members,
                communityBannerUrl = bannerUrl,
                profileUrl = profileUrl,
                aboutUs = aboutUs,
                location = null,
            )

            val batch = db.batch() // Create a new batch

            try {
                // Step 1: Add the community document to the batch
                val communityRef = db.collection("communities").document(communityId)
                batch.set(communityRef, community)

                // Step 2: Update each user’s communities field in the batch
                members.forEach { member ->
                    val (userId, role) = member.entries.first()
                    val userRef = db.collection("users").document(userId)

                    // Retrieve the current user's communities field
                    val userSnapshot = userRef.get().await()
                    val existingCommunities = userSnapshot.get("communities") as? List<Map<String, String>> ?: emptyList()

                    // Add the new community to the list
                    val updatedCommunities = existingCommunities.toMutableList().apply {
                        add(mapOf(communityId to role))
                    }

                    // Update the user document with the new list
                    batch.update(userRef, "communities", updatedCommunities)
                }

                // Step 3: Commit the batch
                batch.commit().await()

                // Notify user of success
                Toast.makeText(
                    context,
                    "Community $communityName created successfully. Please wait for the admin's approval.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.w("CommunityRepository", "Error creating community", e)
                Toast.makeText(
                    context,
                    "Community $communityName could not be created at this time. Try again later.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } ?: run {
            Log.e("CommunityRepository", "User not authenticated or username is null")
        }
    }


    /**
     * READ
     */

    /**
     *Observe community details
     */

    fun getAllMembers(community: Community): Task<List<UserData>> {
        val memberIds = community.members.map { it.keys.first() } // Extract user IDs from the members list

        if (memberIds.isEmpty()) {
            return Tasks.forResult(emptyList())
        }

        val allMembers = mutableListOf<UserData>()
        val tasks = mutableListOf<Task<DocumentSnapshot>>()

        // Fetch each user by their ID
        memberIds.forEach { userId ->
            val task = db.collection("users").document(userId).get()
            tasks.add(task)
        }

        // Return a task that completes when all individual tasks complete
        return Tasks.whenAllSuccess<DocumentSnapshot>(tasks).continueWith { task ->
            task.result.forEach { document ->
                if (document.exists()) {
                    val user = document.toObject(UserData::class.java)
                    user?.let { allMembers.add(it) }
                }
            }
            allMembers // This will be the result of the task
        }
    }

    fun filterMembersByRole(community: Community, members: List<UserData>, role: String): List<UserData> {
        return members.filter { member ->
            community.members.any { it[member.userId] == role }
        }
    }


    fun getCommunityMembersFlow(
        communityId: String,
        pageSize: Int = 15 // Number of documents to fetch per page
    ): Flow<List<UserData>> = flow {
        try {
            val communityRef = firestore.collection("communities").document(communityId)
            val snapshot = communityRef.get().await()

            // Retrieve the list of member IDs from the community document
            val memberIds = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()

            Log.d("CommunityRepository", "MemberIds are $memberIds")
            if (memberIds.isNotEmpty()) {
                // Extract user IDs from member entries
                val userIds = memberIds.mapNotNull { it.keys.firstOrNull() }

                Log.d("CommunityRepository", "User Ids are: $userIds")

                // Check if userIds is not empty before querying
                if (userIds.isNotEmpty()) {
                    // Query the users collection with pagination
                    val userCollectionRef = firestore.collection("users")

                    val query = userCollectionRef
                        .whereIn("userId", userIds) // Filter by user IDs
                        .limit(pageSize.toLong()) // Limit to pageSize documents
                        .get()
                        .await()

                    // Convert query snapshot to list of UserData
                    val users = query.documents.mapNotNull { document ->
                        document.toObject(UserData::class.java)
                    }

                    Log.d("CommunityRepository", "Members data fetched and paginated.")
                    emit(users) // Emit the list of UserData

                } else {
                    emit(emptyList()) // Emit an empty list if there are no user IDs
                }

            } else {
                emit(emptyList()) // Emit an empty list if there are no members
            }

        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching community members", e)
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }.flowOn(Dispatchers.IO) // Ensure this runs on a background thread

    suspend fun getCommunityById(communityId: String): Community? {
        return try {
            val documentSnapshot = firestore.collection("communities")
                .document(communityId)
                .get(Source.DEFAULT)
                .await()

            // Convert the document snapshot to a Community object
            documentSnapshot.toObject(Community::class.java)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching community by ID", e)
            null
        }
    }


    fun getCommunitySpacesFlow(communityId: String): Flow<List<Map<String, String>>> = flow {
        try {
            val communityRef = firestore.collection("communities").document(communityId)
            val snapshot = communityRef.get().await()

            // Retrieve the spaces list and filter for live spaces based on approval status
            val allSpaces = snapshot.get("spaces") as? List<Map<String, String>> ?: emptyList()
            val liveSpaces = allSpaces.filter { space ->
                space.values.contains("Live") // Filter spaces by "live" approval status
            }

            emit(liveSpaces) // Emit the filtered list of live spaces
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching live community spaces", e)
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }.flowOn(Dispatchers.IO) // Ensure this runs on a background thread

    /**
     * Observe user isJoined status
     */
    fun observeJoinedStatus(
        userId: String, communityId: String
    ): Flow<JoinedStatusState> = flow {
        emit(JoinedStatusState.Loading) // Emit loading state initially
        var isDataLoaded = false
        try {
            val communityRef = db.collection("communities").document(communityId)
            val snapshot = communityRef.get().await()
            val isUserJoined =
                snapshot.toObject(Community::class.java)?.members?.any { member ->
                    member.keys.first() == userId
                } ?: true

            if (!isDataLoaded) {
                emit(JoinedStatusState.Success(isUserJoined))
                isDataLoaded = true
            }
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "Error fetching or observing joined status", e)
            if (!isDataLoaded) {
                emit(JoinedStatusState.Error(e.message ?: "Unknown error occurred"))
                isDataLoaded = true
            }
        }
    }.flowOn(Dispatchers.IO) // Ensure this runs on a background thread

    // Use a single ImageLoader instance
    private val imageLoader by lazy { ImageLoader(context) }

    suspend fun generateThumbnail(url: String?, context: Context): String? {
        if (url.isNullOrEmpty()) return null

        // Check if thumbnail already exists
        val existingThumbnail = checkIfThumbnailExists(url, context)
        if (existingThumbnail != null) return existingThumbnail

        val request = ImageRequest.Builder(context)
            .data(url)
            .size(480) // Set the thumbnail size
            .build()

        val drawable = (imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap

        return drawable?.let { bitmap ->
            saveBitmapToLocalStorage(bitmap, context)
        }
    }

    private fun checkIfThumbnailExists(url: String, context: Context): String? {
        val directory = File(context.filesDir, "thumbnails")
        if (!directory.exists()) return null

        val fileName = "${url.hashCode()}.jpg"
        val file = File(directory, fileName)

        return if (file.exists()) file.absolutePath else null
    }

    fun saveBitmapToLocalStorage(bitmap: Bitmap, context: Context): String {
        val fileName = "${System.currentTimeMillis()}.jpg"
        val directory = File(context.filesDir, "thumbnails")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return file.absolutePath
    }

    fun observeAllCommunities(): Flow<List<Community>> {
        return firebaseService.observeAllCommunitiesRefresh()
    }

    fun getCommunitiesByStatus (status : String): Flow<List<Community>> {
        return firebaseService.observeCommunitiesByStatusRefresh(status)
    }

    /**
     * UPDATE
     */

    /**
     * Add members
     */


    suspend fun updateUserAndAddMemberToCommunity(
        userId: String,
        communityId: String,
        role: String = "member"
    ) {
        val db = FirebaseFirestore.getInstance()

        try {
            // Create a Firestore batch
            val batch = db.batch()

            // Fetch the current community document
            val communityDocRef = db.collection("communities").document(communityId)
            val communityDoc = communityDocRef.get().await()
            val community = communityDoc.toObject(Community::class.java)

            if (community != null) {
                // Check if the user is already in the community and has the same or higher role
                val existingMember = community.members.find { it.keys.first() == userId }
                if (existingMember != null && existingMember.values.first() != "member") {
                    // User is already a leader or editor; no need to update the role
                    Log.d("CommunityRepository", "User $userId already has a role in the community $communityId")
                } else {
                    // Add the user with the specified role if not already a member or update role
                    val updatedMembers = community.members.toMutableList().apply {
                        if (existingMember != null) {
                            // Update role if user is already in the community
                            val updatedIndex = indexOfFirst { it.keys.first() == userId }
                            this[updatedIndex] = mapOf(userId to role)
                        } else {
                            // Add new member with the role
                            add(mapOf(userId to role))
                        }
                    }

                    // Update the community document in the batch
                    batch.update(
                        communityDocRef,
                        "members", updatedMembers
                    )
                }
            } else {
                Log.e("CommunityRepository", "Community $communityId not found")
                return
            }

            // Fetch the current user document
            val userDocRef = db.collection("users").document(userId)
            val userDoc = userDocRef.get().await()

            if (userDoc.exists()) {
                val userData = userDoc.toObject(UserData::class.java)
                userData?.let { user ->
                    // Check if the community is already in the user's list
                    val existingCommunity = user.communities.find { it.keys.first() == communityId }
                    if (existingCommunity != null && existingCommunity.values.first() != "member") {
                        // Community is already in the user's list with a role that should not be demoted
                        Log.d("UserRepository", "Community $communityId already has a role in user $userId")
                    } else {
                        // Update the user's communities list
                        val updatedCommunities = user.communities.toMutableList().apply {
                            val existingCommunityIndex = indexOfFirst { it.keys.first() == communityId }
                            if (existingCommunityIndex != -1) {
                                // Update the role if community already exists
                                this[existingCommunityIndex] = mapOf(communityId to role)
                            } else {
                                // Add new community with role
                                add(mapOf(communityId to role))
                            }
                        }

                        // Update the user's communities field with the modified list in the batch
                        batch.update(
                            userDocRef,
                            "communities",
                            updatedCommunities
                        )
                    }
                } ?: Log.w("UserRepository", "No user data found for userId: $userId")
            } else {
                Log.w("UserRepository", "No such user data found for userId: $userId")
                return
            }

            // Commit the batch
            batch.commit().await()
            Toast.makeText(
                context,
                "Welcome to ${community.name ?: "the community"}",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(
                "CommunityRepository",
                "Successfully updated user $userId and community $communityId with role $role"
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not join the community at this time. Please try again later",
                Toast.LENGTH_LONG
            ).show()
            Log.e(
                "CommunityRepository",
                "Error updating user $userId and community $communityId",
                e
            )
        }
    }

    /**
     * Demote user to member
     */

    suspend fun demoteMemberInCommunity(//Batch transaction
        userId: String,
        communityId: String
    ) {
        try {
            // Fetch the current community document
            val communityDoc = db.collection("communities").document(communityId).get().await()
            val community = communityDoc.toObject(Community::class.java)

            community?.let {
                // Check the current number of leaders
                val leaderCount = it.members.count { member -> member.values.first() == "leader" }

                // If the user is a leader, ensure there is more than one leader
                val userRole =
                    it.members.find { member -> member.keys.first() == userId }?.values?.first()
                if (userRole == "leader" && leaderCount <= 1) {
                    Log.e(
                        "CommunityRepository",
                        "Cannot demote user $userId: they are the only leader in community $communityId"
                    )
                    Toast.makeText(
                        context,
                        "Cannot demote the only leader in the community.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Update the members list
                val updatedMembers = it.members.map { member ->
                    val (id, role) = member.entries.first()
                    if (id == userId && (role == "leader" || role == "editor")) {
                        mapOf(id to "member") // Demote to member
                    } else {
                        member // Keep the existing role for others
                    }
                }

                // Create a batch
                val batch = db.batch()

                // Update the community document in the batch
                val communityRef = db.collection("communities").document(communityId)
                batch.update(communityRef, "members", updatedMembers)

                // Update the user's role in their communities field
                val userRef = db.collection("users").document(userId)

                // Fetch the current user document
                val userDoc = userRef.get().await()
                val userCommunities = userDoc.get("communities") as? List<Map<String, String?>>

                userCommunities?.let { communitiesList ->
                    val updatedCommunities = communitiesList.map { communityEntry ->
                        if (communityEntry["communityId"] == communityId) {
                            mapOf(communityId to "member") // Demote the role to member
                        } else {
                            communityEntry // Keep other entries unchanged
                        }
                    }

                    // Update the user's communities field with the modified list in the batch
                    batch.update(userRef, "communities", updatedCommunities)
                }

                // Commit the batch
                batch.commit().await()

                // Notify user of success
                Toast.makeText(context, "Member demoted successfully.", Toast.LENGTH_SHORT).show()

                Log.d(
                    "CommunityRepository",
                    "Demoted member $userId in community $communityId to member"
                )
            } ?: run {
                Log.e("CommunityRepository", "Community $communityId not found")
            }
        } catch (e: Exception) {
            Log.e(
                "CommunityRepository",
                "Error demoting member $userId in community $communityId",
                e
            )
        }
    }


    /**
     * Add leaders or editors
     */

    suspend fun promoteMemberInCommunity(//Batch transaction
        userId: String,
        communityId: String,
        role: String // Pass "editor" or "leader"
    ) {
        try {
            // Fetch the current community document
            val communityDoc = db.collection("communities").document(communityId).get().await()
            val community = communityDoc.toObject(Community::class.java)

            community?.let {
                // Update the members list to promote the user
                val updatedMembers = it.members.map { member ->
                    val (id, currentRole) = member.entries.first()
                    if (id == userId) {
                        mapOf(id to role) // Promote to the specified role
                    } else {
                        member // Keep the existing role for others
                    }
                }

                // Create a batch
                val batch = db.batch()

                // Update the community document in the batch
                val communityRef = db.collection("communities").document(communityId)
                batch.update(communityRef, "members", updatedMembers)

                // Update the user's role in their communities field
                val userRef = db.collection("users").document(userId)

                // Fetch the current user document
                val userDoc = userRef.get().await()
                val userCommunities = userDoc.get("communities") as? List<Map<String, String?>>

                userCommunities?.let { communitiesList ->
                    val updatedCommunities = communitiesList.map { communityEntry ->
                        if (communityEntry["communityId"] == communityId) {
                            mapOf(communityId to role) // Promote the role to the specified role
                        } else {
                            communityEntry // Keep other entries unchanged
                        }
                    }

                    // Update the user's communities field with the modified list in the batch
                    batch.update(userRef, "communities", updatedCommunities)
                }

                // Commit the batch
                batch.commit().await()

                // Notify user of success
                Toast.makeText(context, "Member promoted to $role successfully.", Toast.LENGTH_SHORT).show()

                Log.d(
                    "CommunityRepository",
                    "Promoted member $userId in community $communityId to $role"
                )
            } ?: run {
                Log.e("CommunityRepository", "Community $communityId not found")
            }
        } catch (e: Exception) {
            Log.e(
                "CommunityRepository",
                "Error promoting member $userId in community $communityId to $role",
                e
            )
        }
    }


    /**
     * Update community fields
     */


    suspend fun editCommunity(
        communityId: String,
        newCommunityName: String?,
        newCommunityType: String?,
        newBannerUri: Uri?,
        newProfileUri: Uri?,
        newAboutUs: String?
    ) {
        try {
            val communityRef = db.collection("communities").document(communityId)
            val batch = db.batch()

            // Prepare updates for the community document
            val updates = mutableMapOf<String, Any?>()
            newCommunityName?.let { updates["name"] = it }
            newCommunityType?.let { updates["type"] = it }
            newAboutUs?.let { updates["aboutUs"] = it }

            // Handle image uploads
            if (newBannerUri != null) {
                val newBannerUrl = firebaseService.uploadImageToStorage(newBannerUri, "communities/banners/$communityId.jpg")
                newBannerUrl?.let { url -> updates["communityBannerUrl"] = url }
            } else {
                updates["communityBannerUrl"] = FieldValue.delete()
                Log.d("CommunityRepository", "Banner URL removed for community $communityId")
            }

            if (newProfileUri != null) {
                val newProfileUrl = firebaseService.uploadImageToStorage(newProfileUri, "communities/profileImages/$communityId.jpg")
                newProfileUrl?.let { url -> updates["profileUrl"] = url }
            } else {
                updates["profileUrl"] = FieldValue.delete()
                Log.d("CommunityRepository", "Profile URL removed for community $communityId")
            }

            // Apply updates to the community document if there are any
            if (updates.isNotEmpty()) {
                batch.update(communityRef, updates)
            }

            // Commit the batch
            batch.commit().await()
            Toast.makeText(context, "Community updated successfully.", Toast.LENGTH_SHORT).show()
            Log.d("CommunityRepository", "Community $communityId updated successfully.")

        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error updating community $communityId", e)
            Toast.makeText(context, "Error updating community. Try again later.", Toast.LENGTH_LONG).show()
        }
    }





    /**
     * DELETE
     */

    /**
     * Remove User
     */
    /**
     * Remove User from Community
     */
    suspend fun removeUserFromCommunity(//Batch transaction
        communityId: String,
        userId: String
    ) {
        try {
            // Fetch the current community document
            val communityDoc = db.collection("communities").document(communityId).get().await()
            val community = communityDoc.toObject(Community::class.java)

            community?.let {
                // Check if the user is a leader
                val userRole =
                    it.members.find { member -> member.keys.first() == userId }?.values?.first()

                // If the user is a leader, check if they are the only leader
                if (userRole == "leader" && it.members.count { member -> member.values.first() == "leader" } <= 1) {
                    Log.e(
                        "CommunityRepository",
                        "Cannot remove user, they are the only leader in the community"
                    )
                    Toast.makeText(
                        context,
                        "Cannot remove the only leader in the community.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Update the members list by removing the user
                val updatedMembers = it.members.filter { member -> member.keys.first() != userId }

                // Create a batch
                val batch = db.batch()

                // Update the community document in the batch
                val communityRef = db.collection("communities").document(communityId)
                batch.update(communityRef, "members", updatedMembers)
//
                // Update the user's communities field in their user document
                val userRef = db.collection("users").document(userId)

                // Fetch the current user document
                val userDoc = userRef.get().await()
                val userCommunities = userDoc.get("communities") as? List<Map<String, String?>>

                userCommunities?.let { communitiesList ->
                    val updatedCommunities = communitiesList.filterNot { community ->
                        community.keys.first()== communityId
//                                        communityEntry.keys.first() == communityId
                    }

                    // Update the user's communities field with the modified list in the batch
                    batch.update(userRef, "communities", updatedCommunities)
                }

                // Commit the batch
                batch.commit().await()

                Log.d("CommunityRepository", "Removed user $userId from community $communityId")
            } ?: run {
                Log.e("CommunityRepository", "Community $communityId not found")
            }
        } catch (e: Exception) {
            Log.e(
                "CommunityRepository",
                "Error removing user $userId from community $communityId",
                e
            )
        }
    }


    /**
     * Remove space from community
     */
    suspend fun removeSpaceFromCommunity(
        communityId: String,
        spaceId: String
    ) {
        /**
         * CAUTION : Should only be called from spaceRepository.deleteSpace
         */
        val db = FirebaseFirestore.getInstance()

        try {
            // Fetch the community document
            val communityDoc = db.collection("communities").document(communityId).get().await()
            val community = communityDoc.toObject(Community::class.java)

            community?.let {
                // Remove the space from the list of spaces
                val updatedSpaces = it.spaces.filter { space ->
                    space.keys.firstOrNull() != spaceId
                }

                // Update the community document with the modified spaces list
                db.collection("communities").document(communityId)
                    .update("spaces", updatedSpaces)
                    .await()

                Log.d(
                    "CommunityRepository",
                    "Successfully removed space $spaceId from community $communityId"
                )
            } ?: run {
                Log.e("CommunityRepository", "Community $communityId not found")
            }
        } catch (e: Exception) {
            Log.e(
                "CommunityRepository",
                "Error removing space $spaceId from community $communityId",
                e
            )
        }
    }

    /**
     * Delete community
     */
    suspend fun deleteCommunity(
        communityId: String
    ) {
        val storage = storage.reference

        try {
            // Fetch the community document
            val communityDoc = db.collection("communities").document(communityId).get().await()
            val community = communityDoc.toObject(Community::class.java)

            community?.let { community1 ->
                var batch = db.batch()  // Initialize a Firestore batch
                var operationCount = 0  // Keep track of the number of operations in the batch

                // Remove the community from all users
                val userRemovalTasks = community1.members.mapNotNull { member ->
                    val userId = member.keys.firstOrNull()
                    userId?.let {
                        CoroutineScope(Dispatchers.IO).async {
                            try {
                                val userRef = db.collection("users").document(userId)
                                val userDoc = userRef.get().await()
                                val userData = userDoc.toObject(UserData::class.java)

                                userData?.let { user ->
                                    val updatedCommunities = user.communities.filterNot { communityEntry ->
                                        communityEntry.keys.first() == communityId
                                    }

                                    // Update user's communities field
                                    batch.update(userRef, "communities", updatedCommunities)
                                    operationCount++

                                    // Check if we need to commit the batch (limit of 500 operations)
                                    if (operationCount == 500) {
                                        batch.commit().await()
                                        batch = db.batch()  // Start a new batch
                                        operationCount = 0
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("DeleteCommunity", "Failed to remove community $communityId from user $userId", e)
                            }
                        }
                    }
                }

                // Await completion of all user removal tasks
                userRemovalTasks.awaitAll()

                // Remove spaces within the community (similarly split the batch if needed)

                // (The space deletion tasks would follow a similar batching logic as above)

                // Delete profile image and banner from storage
                try {
                    community1.profileUrl?.let { profileUrl ->
                        val profileRef = storage.child("communities/profileImages/${communityId}.jpg")
                        profileRef.delete().await()
                    }
                } catch (e: Exception) {
                    Log.e("DeleteCommunity", "Failed to delete profile image for community $communityId", e)
                }

                try {
                    community1.communityBannerUrl?.let { bannerUrl ->
                        val bannerRef = storage.child("communities/banners/${communityId}.jpg")
                        bannerRef.delete().await()
                    }
                } catch (e: Exception) {
                    Log.e("DeleteCommunity", "Failed to delete banner image for community $communityId", e)
                }

                // Delete the community document
                batch.delete(db.collection("communities").document(communityId))
                operationCount++

                // Commit any remaining operations in the final batch
                if (operationCount > 0) {
                    batch.commit().await()
                }

                Log.d("DeleteCommunity", "Successfully deleted community $communityId")
            } ?: run {
                Log.e("DeleteCommunity", "Community document $communityId not found")
            }
        } catch (e: Exception) {
            Log.e("DeleteCommunity", "Failed to delete community $communityId", e)
        }
    }


}


    /**
     * LEGACY
     */

    /**
     *Legacy update community counts
     */


    /**
     *Legacy fetch community details functions
     */

