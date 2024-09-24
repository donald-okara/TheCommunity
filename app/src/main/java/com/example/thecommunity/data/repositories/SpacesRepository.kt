package com.example.thecommunity.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.SpaceMember
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.model.UserSpaces
import com.example.thecommunity.presentation.communities.JoinedStatusState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlin.reflect.full.memberProperties

class SpaceRepository(
    private val context : Context,
    val db: FirebaseFirestore,
    val firestore: FirebaseFirestore,
    val storage : FirebaseStorage,
    private val firebaseService: FirebaseService,
    private val userRepository: UserRepository,
    coroutineScope: CoroutineScope
) {
    private val Tag = "SpacesRepository"

    /**
     * CREATE
     */

    /**
     * Create a new space
     */
    suspend fun requestNewSpace(
        parentCommunityId: String,
        spaceName: String,
        profilePictureUri: Uri?,
        bannerUri: Uri?,
        approvalStatus: String = "Pending",
        description: String?,
        membersRequireApproval: Boolean,
        selectedLeaders: List<UserData>,
        selectedEditors: List<UserData>
    ) {
        try {
            val currentUser = userRepository.getCurrentUser()
            currentUser?.let { user ->
                val members = mutableListOf<Map<String, SpaceMember>>(
                    mapOf(user.userId to SpaceMember("leader", "Approved"))
                ).apply {
                    addAll(selectedLeaders.map { mapOf(it.userId to SpaceMember("leader", "Approved")) })
                    addAll(selectedEditors.map { mapOf(it.userId to SpaceMember("editor", "Approved")) })
                }

                val spaceId = db.collection("spaces").document().id // Generate a new ID

                val bannerUrl = bannerUri?.let {
                    firebaseService.uploadImageToStorage(
                        it,
                        "spaces/banners/${parentCommunityId}/${spaceId}.jpg"
                    )
                }
                val profileUrl = profilePictureUri?.let {
                    firebaseService.uploadImageToStorage(
                        it,
                        "spaces/profileImages/${parentCommunityId}/${spaceId}.jpg"
                    )
                }

                val space = Space(
                    id = spaceId,
                    name = spaceName,
                    parentCommunity = parentCommunityId,
                    bannerUri = bannerUrl,
                    profileUri = profileUrl,
                    description = description,
                    approvalStatus = approvalStatus,
                    membersRequireApproval = membersRequireApproval,
                    members = members
                )

                try {
                    // Start a batch
                    val batch = db.batch()

                    // Add the new space to the spaces collection
                    val spaceRef = db.collection("spaces").document(spaceId)
                    batch.set(spaceRef, space)

                    // Update each user's spaces directly
                    members.forEach { member ->
                        val (userId, spaceMember) = member.entries.first()
                        val userRef = db.collection("users").document(userId)

                        // Fetch the current user's spaces to update
                        val userDoc = userRef.get().await()
                        val currentSpaces = userDoc.get("spaces") as? List<Map<String, UserSpaces>> ?: emptyList()
                        val updatedSpaces = currentSpaces.toMutableList().apply {
                            removeIf { it.containsKey(spaceId) }
                            add(mapOf(spaceId to UserSpaces(spaceMember.role, spaceMember.approvalStatus)))
                        }

                        batch.update(userRef, "spaces", updatedSpaces)
                    }

                    // Update the parent community's list of spaces
                    val communityRef = db.collection("communities").document(parentCommunityId)
                    val communityDoc = communityRef.get().await()
                    val currentSpaces = communityDoc.get("spaces") as? List<Map<String, String>> ?: emptyList()
                    val updatedSpaces = currentSpaces.toMutableList().apply {
                        removeIf { it.containsKey(spaceId) }
                        add(mapOf(spaceId to approvalStatus))
                    }

                    batch.update(communityRef, "spaces", updatedSpaces)

                    // Commit the batch
                    batch.commit().await()

                    Log.d(Tag, "Space request successful")
                } catch (e: Exception) {
                    Log.e(Tag, "Error adding space $spaceId", e)
                }
            } ?: run {
                Log.e(Tag, "User not authenticated or username is null")
            }
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error creating space", e)
        }
    }



    suspend fun editSpace(
        spaceId: String,
        newSpaceName: String?,
        newBannerUri: Uri?,
        newProfileUri: Uri?,
        newRequiresApproval : Boolean,
        parentCommunityId: String,
        newAboutUs: String?
    ) {
        try {
            val spaceRef = db.collection("spaces").document(spaceId)
            val batch = db.batch()

            // Prepare updates for the community document
            val updates = mutableMapOf<String, Any?>()

            newSpaceName?.let { updates["name"] = it }
            newAboutUs?.let { updates["description"] = it }
            newRequiresApproval.let { updates["membersRequireApproval"] = it }

            // Handle image uploads
            if (newBannerUri != null) {
                val newBannerUrl = firebaseService.uploadImageToStorage(newBannerUri,
                    "spaces/banners/${parentCommunityId}/${spaceId}.jpg"
                )
                newBannerUrl?.let { url -> updates["bannerUri"] = url }
            } else {
                updates["bannerUri"] = FieldValue.delete()
                Log.d(Tag, "Banner URL removed for space $spaceId")
            }

            if (newProfileUri != null) {
                val newProfileUrl = firebaseService.uploadImageToStorage(newProfileUri,
                    "spaces/profileImages/${parentCommunityId}/${spaceId}.jpg"
                )
                newProfileUrl?.let { url -> updates["profileUri"] = url }
            } else {
                updates["profileUri"] = FieldValue.delete()
                Log.d(Tag, "Profile URL removed for space $spaceId")
            }

            // Apply updates to the community document if there are any
            if (updates.isNotEmpty()) {
                batch.update(spaceRef, updates)
            }

            // Commit the batch
            batch.commit().await()
            Toast.makeText(context, "Space updated successfully.", Toast.LENGTH_SHORT).show()
            Log.d(Tag, "Space $spaceId updated successfully.")

        } catch (e: Exception) {
            Log.e(Tag, "Error updating community $spaceId", e)
            Toast.makeText(context, "Error updating space. Try again later.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * READ
     */

    fun getSpaceMembersFlow(
        spaceId: String,
        pageSize: Int = 15 // Number of documents to fetch per page
    ): Flow<List<UserData>> = flow {
        try {
            val communityRef = firestore.collection("spaces").document(spaceId)
            val snapshot = communityRef.get().await()

            // Retrieve the list of member IDs from the community document
            val memberIds = snapshot.get("members") as? List<Map<String, SpaceMember>> ?: emptyList()

            Log.d(Tag, "MemberIds are $memberIds")
            if (memberIds.isNotEmpty()) {
                // Extract user IDs from member entries
                val userIds = memberIds.mapNotNull { it.keys.firstOrNull() }

                Log.d(Tag, "User Ids are: $userIds")

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

                    Log.d(Tag, "Members data fetched and paginated.")
                    emit(users) // Emit the list of UserData

                } else {
                    emit(emptyList()) // Emit an empty list if there are no user IDs
                }

            } else {
                emit(emptyList()) // Emit an empty list if there are no members
            }

        } catch (e: Exception) {
            Log.e(Tag, "Error fetching community members", e)
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }.flowOn(Dispatchers.IO) // Ensure this runs on a background thread


    fun observeJoinedStatus(
        userId: String, spaceId: String
    ): Flow<JoinedStatusState> = flow {
        emit(JoinedStatusState.Loading) // Emit loading state initially
        try {
            val spaceRef = db.collection("spaces").document(spaceId)

            // Fetch the document and check the membership status
            val snapshot = spaceRef.get().await()
            val isUserJoined =
                snapshot.toObject(Space::class.java)?.members?.any { member ->
                    member.keys.first() == userId
                } ?: false

            emit(JoinedStatusState.Success(isUserJoined)) // Emit success state with the joined status

        } catch (e: Exception) {
            Log.e(Tag,"Error fetching or observing joined status", e)
            emit(JoinedStatusState.Error(e.message ?: "Unknown error occurred")) // Emit error state
        }
    }
        .buffer()
        .flowOn(Dispatchers.IO) // Ensure this runs on a background thread


    /**
     * Room Database
     */

    suspend fun getSpaceById(spaceId: String): Space? {
        return try {
            val documentSnapshot = firestore.collection("spaces")
                .document(spaceId)
                .get()
                .await()

            // Convert the document snapshot to a Community object
            documentSnapshot.toObject(Space::class.java)
        } catch (e: Exception) {
            Log.e(Tag, "Error fetching space by ID", e)
            null
        }
    }


    suspend fun getSpaceByStatus(status: String,parentCommunity: String): List<Space> {
        return try {
            val snapshot = firestore.collection("spaces")
                .whereEqualTo(
                    "status", status,
                )
                .whereEqualTo(
                    "parentCommunity", parentCommunity

                )
                .get()
                .await()
            snapshot.toObjects(Space::class.java)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching communities by status", e)
            emptyList()
        }
    }

    fun observeLiveSpacesByCommunity(
        communityId: String
    ): Flow<List<Space>> = flow {
        try {
            val snapshot = firestore.collection("spaces")
                .whereEqualTo("parentCommunity", communityId)
                .whereEqualTo("approvalStatus", "Live")
                .get()
                .await()

            val spaces = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Space::class.java)
            }

            emit(spaces)
        } catch (exception: Exception) {
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }


    fun observePendingSpacesByCommunity(
        communityId: String
    ): Flow<List<Space>> = flow {
        try {
            val snapshot = firestore.collection("spaces")
                .whereEqualTo("parentCommunity", communityId)
                .whereEqualTo("approvalStatus", "Pending")
                .get()
                .await()

            val spaces = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Space::class.java)
            }

            emit(spaces)
        } catch (exception: Exception) {
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }

    fun observeRejectedSpacesByCommunity(
        communityId: String
    ): Flow<List<Space>> = flow {
        try {
            val snapshot = firestore.collection("spaces")
                .whereEqualTo("parentCommunity", communityId)
                .whereEqualTo("approvalStatus", "Rejected")
                .get()
                .await()

            val spaces = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Space::class.java)
            }

            emit(spaces)
        } catch (exception: Exception) {
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }

    /**
     * UPDATE
     */

    /**
     * Add a member to a space
     */
    suspend fun addMembersToSpace(
        userId: String,
        spaceId: String,
        role: String = "member"
    ) {
        try {
            // Fetch the current space document
            val spaceDoc = db.collection("spaces").document(spaceId).get().await()
            val space = spaceDoc.toObject(Space::class.java)

            space?.let {
                val updatedMembers = it.members.toMutableList()
                var isUpdated = false

                if (it.membersRequireApproval) {
                    // Check if the user is already in the approval list
                    val userInMembersList = updatedMembers.any { member -> member.containsKey(userId) }
                    if (!userInMembersList) {
                        // Add user with pending approval status
                        updatedMembers.add(mapOf(userId to SpaceMember(role, "Pending")))
                        isUpdated = true
                    }
                } else {
                    // Check if the user is already a member
                    val userInMembersList = updatedMembers.any { member -> member.containsKey(userId) }
                    if (!userInMembersList) {
                        updatedMembers.add(mapOf(userId to SpaceMember(role, "Approved")))
                        isUpdated = true
                    }
                }

                if (isUpdated) {
                    // Update the space document with the modified list
                    val batch = db.batch()

                    // Update the space document
                    val spaceRef = db.collection("spaces").document(spaceId)
                    batch.update(
                        spaceRef,
                        "members", updatedMembers
                    )

                    // Update the user's spaces field
                    val userRef = db.collection("users").document(userId)
                    val userDoc = userRef.get().await()
                    val currentSpaces = userDoc.get("spaces") as? List<Map<String, UserSpaces>> ?: emptyList()
                    val updatedSpaces = currentSpaces.toMutableList().apply {
                        removeIf { it.containsKey(spaceId) }
                        add(mapOf(spaceId to UserSpaces(role, if (it.membersRequireApproval) "Pending" else "Approved")))
                    }
                    batch.update(userRef, "spaces", updatedSpaces)

                    // Commit the batch
                    batch.commit().await()

                    Log.d("SpacesRepository", "Added member $userId to space $spaceId with role $role")
                } else {
                    Log.d("SpacesRepository", "User $userId is already in the space $spaceId")
                }
            } ?: run {
                Log.e("SpacesRepository", "Space $spaceId not found")
            }
        } catch (e: Exception) {
            Log.e("SpacesRepository", "Error adding member $userId to space $spaceId", e)
        }
    }


    /**
     * Update spaces fields
     */
    suspend fun syncSpacesModelWithFirebase() {
        try {
            val spacesCollection = db.collection("spaces").get().await()

            for (document in spacesCollection.documents) {
                val spacesData = document.data ?: continue

                // Create a new instance of the Community data class
                val space = Space()

                // Find missing fields by checking the properties of the Community data class
                val missingFields = Space::class.memberProperties
                    .filter { property ->
                        // Check if the field is missing in the document
                        !spacesData.containsKey(property.name)
                    }
                    .associate { property ->
                        // Associate the missing property with its default value
                        property.name to property.get(space)
                    }

                if (missingFields.isNotEmpty()) {
                    // Update the document with missing fields
                    db.collection("spaces")
                        .document(document.id)
                        .update(missingFields)
                        .await()

                    Log.d("FirebaseSync", "Document ${document.id} updated with missing fields: $missingFields")
                } else {
                    Log.d("FirebaseSync", "Document ${document.id} is up to date")
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing community model with Firebase", e)
        }
    }


    suspend fun demoteMemberInSpaces(
        userId: String,
        spaceId: String
    ) {
        /**
         * Can be used by space leader to demote user to member or by member to remove themselves as leader or editor
         */
        try {
            // Fetch the current space document
            val spaceDoc = db.collection("spaces").document(spaceId).get().await()
            val space = spaceDoc.toObject(Space::class.java)

            space?.let {
                // Check the current number of leaders
                val leaderCount = it.members.count { member -> member.values.first().role == "leader" }

                // If the user is a leader, ensure there is more than one leader
                val userRole = it.members.find { member -> member.keys.first() == userId }?.values?.first()?.role
                if (userRole == "leader" && leaderCount <= 1) {
                    Log.e("SpaceRepository", "Cannot demote user $userId: they are the only leader in space $spaceId")
                    Toast.makeText(context, "Cannot demote the only leader in the space.", Toast.LENGTH_LONG).show()
                    return
                }

                // Update the members list
                val updatedMembers = it.members.map { member ->
                    val (id, spaceMember) = member.entries.first()
                    if (id == userId && (spaceMember.role == "leader" || spaceMember.role == "editor")) {
                        mapOf(id to SpaceMember("member", "Approved")) // Demote to member
                    } else {
                        member // Keep the existing role for others
                    }
                }

                // Fetch the user's document to update their spaces field
                val userDoc = db.collection("users").document(userId).get().await()
                val userSpaces = userDoc.get("spaces") as? MutableList<Map<String, UserSpaces>> ?: mutableListOf()

                // Update the user's spaces list
                val updatedUserSpaces = userSpaces.apply {
                    removeIf { it.containsKey(spaceId) } // Remove the old role
                    add(mapOf(spaceId to UserSpaces("member", "Approved"))) // Add the new role as "member"
                }

                // Update the space document with the modified members list
                val batch = db.batch()

                // Update the space document
                val spaceRef = db.collection("spaces").document(spaceId)
                batch.update(spaceRef, "members", updatedMembers)

                // Update the user's spaces field
                val userRef = db.collection("users").document(userId)
                batch.update(userRef, "spaces", updatedUserSpaces)

                // Commit the batch
                batch.commit().await()

                Log.d("SpaceRepository", "Demoted member $userId in space $spaceId to member")
            } ?: run {
                Log.e("SpaceRepository", "Space $spaceId not found")
            }
        } catch (e: Exception) {
            Log.e("SpaceRepository", "Error demoting member $userId in space $spaceId", e)
        }
    }



    /**
     * Add leaders or editors
     */

    suspend fun promoteMemberInSpace(
        userId: String,
        spaceId: String,
        role: String // Pass "editor" or "leader"
    ) {
        try {
            // Fetch the current space document
            val spaceDoc = db.collection("spaces").document(spaceId).get().await()
            val space = spaceDoc.toObject(Space::class.java)

            space?.let {
                // Update the members list to promote the user
                val updatedMembers = it.members.map { member ->
                    val (id, currentRole) = member.entries.first()
                    if (id == userId) {
                        mapOf(id to SpaceMember(role, "Approved")) // Promote to the specified role
                    } else {
                        member // Keep the existing role for others
                    }
                }

                // Create a batch
                val batch = db.batch()

                // Update the space document in the batch
                val spaceRef = db.collection("spaces").document(spaceId)
                batch.update(spaceRef, "members", updatedMembers)

                // Update the user's role in their spaces field
                val userRef = db.collection("users").document(userId)

                // Fetch the current user document
                val userDoc = userRef.get().await()
                val userSpaces = userDoc.get("spaces") as? List<Map<String, UserSpaces>>

                userSpaces?.let { spacesList ->
                    val updatedSpaces = spacesList.map { spaceEntry ->
                        if (spaceEntry.keys.first() == spaceId) {
                            mapOf(spaceId to UserSpaces(role, "Approved")) // Promote the role to the specified role
                        } else {
                            spaceEntry // Keep other entries unchanged
                        }
                    }

                    // Update the user's spaces field with the modified list in the batch
                    batch.update(userRef, "spaces", updatedSpaces)
                }

                // Commit the batch
                batch.commit().await()

                // Notify user of success
                Toast.makeText(context, "Member promoted to $role successfully.", Toast.LENGTH_SHORT).show()

                Log.d(
                    "SpaceRepository",
                    "Promoted member $userId in space $spaceId to $role"
                )
            } ?: run {
                Log.e("SpaceRepository", "Space $spaceId not found")
            }
        } catch (e: Exception) {
            Log.e(
                "SpaceRepository",
                "Error promoting member $userId in space $spaceId to $role",
                e
            )
        }
    }



    /**
     * DELETE
     */

    suspend fun deleteSpace(
        spaceId: String
    ) {
        val storage = storage.reference

        try {
            // Step 1: Fetch the space document
            val spaceDoc = db.collection("spaces").document(spaceId).get().await()
            val space = spaceDoc.toObject(Space::class.java)

            space?.let {
                val parentCommunityId = it.parentCommunity

                // Step 2: Delete profile image and banner image from storage
                val deleteProfileDeferred = space.profileUri?.let { profileUri ->
                    CoroutineScope(Dispatchers.IO).async {
                        try {
                            val profileRef = storage.child(
                                "spaces/profileImages/${parentCommunityId}/${spaceId}.jpg"
                            )
                            profileRef.delete().await()
                        } catch (e: Exception) {
                            Log.e("SpaceRepository", "Failed to delete profile image for space $spaceId", e)
                        }
                    }
                }

                val deleteBannerDeferred = space.bannerUri?.let { bannerUri ->
                    CoroutineScope(Dispatchers.IO).async {
                        try {
                            val bannerRef = storage.child(
                                "spaces/banners/${parentCommunityId}/${spaceId}.jpg"
                            )
                            bannerRef.delete().await()
                        } catch (e: Exception) {
                            Log.e("SpaceRepository", "Failed to delete banner image for space $spaceId", e)
                        }
                    }
                }

                // Await completion of image deletions
                deleteProfileDeferred?.await()
                deleteBannerDeferred?.await()

                // Step 3: Remove space from all users
                val users = it.members.mapNotNull { member -> member.keys.firstOrNull() }
                val batch = db.batch()

                users.forEach { userId ->
                    val userDoc = db.collection("users").document(userId).get().await()

                    // Remove the spaceId from 'spaces' field with any role
                    val userSpaces = userDoc.get("spaces") as? MutableList<Map<String, UserSpaces>> ?: mutableListOf()
                    val updatedSpaces = userSpaces.apply {
                        removeIf { it.containsKey(spaceId) } // Remove spaceId with any role
                    }

                    // Add update operations to the batch for the user document
                    batch.update(
                        db.collection("users").document(userId),
                        mapOf(
                            "spaces" to updatedSpaces
                        )
                    )
                }

                // Commit the batch
                batch.commit().await()

                // Step 4: Remove space from the community
                try {
                    val communityDoc = db.collection("communities").document(parentCommunityId).get().await()
                    val communitySpaces = communityDoc.get("spaces") as? MutableList<Map<String, String>> ?: mutableListOf()

                    // Remove the spaceId from the community's 'spaces' field with any status
                    val updatedCommunitySpaces = communitySpaces.apply {
                        removeIf { it.containsKey(spaceId) } // Remove spaceId with any status
                    }

                    db.collection("communities").document(parentCommunityId)
                        .update("spaces", updatedCommunitySpaces)
                        .await()

                } catch (e: Exception) {
                    Log.e("SpaceRepository", "Failed to remove space $spaceId from community ${space.parentCommunity}", e)
                }

                // Step 5: Delete the space document
                try {
                    db.collection("spaces").document(spaceId).delete().await()
                    Log.d("SpaceRepository", "Successfully deleted space $spaceId")
                } catch (e: Exception) {
                    Log.e("SpaceRepository", "Failed to delete space document for $spaceId", e)
                }
            } ?: run {
                Log.e("SpaceRepository", "Space $spaceId not found")
            }
        } catch (e: Exception) {
            Log.e("SpaceRepository", "Error deleting space $spaceId", e)
        }
    }




    suspend fun removeUserFromSpace(
        spaceId: String,
        userId: String
    ) {
        val db = FirebaseFirestore.getInstance()

        try {
            // Fetch the current space document
            val spaceDoc = db.collection("spaces").document(spaceId).get().await()
            val space = spaceDoc.toObject(Space::class.java)

            space?.let {
                val batch = db.batch()

                // Check if the user is a leader
                val userRole = it.members.find { member -> member.keys.first() == userId }?.values?.first()

                // If the user is a leader, check if they are the only leader
                if (userRole?.role == "leader" && it.members.count { member -> member.values.first().role == "leader" } <= 1) {
                    Log.e("SpaceRepository", "Cannot remove user, they are the only leader in the space")
                    Toast.makeText(context, "Cannot remove the only leader in the space.", Toast.LENGTH_LONG).show()
                    return
                }

                // Update the members list by removing the user
                val updatedMembers = it.members.filter { member ->
                    member.keys.first() != userId
                }

                // Add update operations to the batch for the space document
                batch.update(
                    db.collection("spaces").document(spaceId),
                    mapOf(
                        "members" to updatedMembers
                    )
                )

                // Fetch the user's document to update their spaces field
                val userDoc = db.collection("users").document(userId).get().await()

                // Remove the spaceId from 'spaces' field
                val userSpaces = userDoc.get("spaces") as? MutableList<Map<String, UserSpaces>> ?: mutableListOf()
                val updatedSpaces = userSpaces.apply {
                    removeIf { it.containsKey(spaceId) } // Remove spaceId with any role
                }

                // Add update operations to the batch for the user document
                batch.update(
                    db.collection("users").document(userId),
                    mapOf(
                        "spaces" to updatedSpaces
                    )
                )

                // Commit the batch
                batch.commit().await()

                // Notify success
                Toast.makeText(context, "User removed from space successfully", Toast.LENGTH_SHORT).show()
                Log.d("SpaceRepository", "Removed user $userId from space $spaceId")
            } ?: run {
                Log.e("SpaceRepository", "Space $spaceId not found")
            }
        } catch (e: Exception) {
            Log.e("SpaceRepository", "Error removing user $userId from space $spaceId", e)
        }
    }



    /**
     * CleanUp
     */
    suspend fun cleanUpSpacesWithMissingParent() {
        try {
            val spacesSnapshot = db.collection("spaces").get().await()
            val spaces = spacesSnapshot.documents.mapNotNull { it.toObject(Space::class.java) }

            for (space in spaces) {
                if (space.parentCommunity.isBlank()) {
                    try {
                        deleteSpace(space.id)

                        Log.d("Cleanup", "Successfully deleted space with missing parent: ${space.id}")
                    }
                    catch (e: Exception) {
                        Log.e("Cleanup", "Failed to delete space with ID ${space.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Cleanup", "Error fetching spaces for cleanup", e)
        }
    }

    /**
     * LEGACY
     */

//
//
//    fun getLiveSpacesByCommunity(communityId: String): CollectionReference {
//        return firestore.collection("communities")
//            .document(communityId)
//            .collection("spaces")
//    }
//
//    suspend fun getPendingSpacesByCommunity(communityId: String): List<Space> {
//        return try {
//            val snapshot = db.collection("spaces")
//                .whereEqualTo("parentCommunity", communityId)
//                .whereEqualTo("approvalStatus", "Pending")
//                .get()
//                .await()
//            snapshot.toObjects(Space::class.java)
//        } catch (e: Exception) {
//            Log.e("SpacesRepository", "Error fetching pending spaces for community $communityId", e)
//            emptyList()
//        }
//    }
//
//    suspend fun getRejectedSpacesByCommunity(communityId: String): List<Space> {
//        return try {
//            val snapshot = db.collection("spaces")
//                .whereEqualTo("parentCommunity", communityId)
//                .whereEqualTo("approvalStatus", "Rejected")
//                .get()
//                .await()
//            snapshot.toObjects(Space::class.java)
//        } catch (e: Exception) {
//            Log.e("SpacesRepository", "Error fetching rejected spaces for community $communityId", e)
//            emptyList()
//        }
//    }
//
//
//
//
//
//
//
//    private suspend fun updateCommunityCounts() {
//        // Fetch the pending communities and update the count
//        val pendingSpaces = getPendingSpaces()
//        val liveSpaces = getLiveSpaces()
//        val rejectedSpaces = getRejectedSpaces()
//
//        pendingCount.intValue = pendingSpaces.size
//        pendingCount.intValue = liveSpaces.size
//        pendingCount.intValue = rejectedSpaces.size
//    }
//
//    private suspend fun getPendingSpaces(): List<Space> {
//        return try {
//            val snapshot = db.collection("spaces")
//                .whereEqualTo("status", "Pending")
//                .get()
//                .await()
//            snapshot.toObjects(Space::class.java)
//        } catch (e: Exception) {
//            Log.e("SpacesRepository", "Error fetching pending spaces", e)
//            emptyList()
//        }
//    }
//
//    suspend fun getLiveSpaces(): List<Space> {
//        return try {
//            val snapshot = db.collection("spaces")
//                .whereEqualTo("status", "Live")
//                .get()
//                .await()
//            snapshot.toObjects(Space::class.java)
//        } catch (e: Exception) {
//            Log.e("SpacesRepository", "Error fetching live spaces", e)
//            emptyList()
//        }
//    }
//
//    private suspend fun getRejectedSpaces(): List<Community> {
//        return try {
//            val snapshot = db.collection("spaces")
//                .whereEqualTo("status", "Rejected")
//                .get()
//                .await()
//            snapshot.toObjects(Community::class.java)
//        } catch (e: Exception) {
//            Log.e("CommunityRepository", "Error fetching rejected communities", e)
//            emptyList()
//        }
//    }

}