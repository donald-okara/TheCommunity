package com.example.thecommunity.presentation.spaces

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.SpaceRepository
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.communities.JoinRequestState
import com.example.thecommunity.presentation.communities.JoinedStatusState
import com.example.thecommunity.presentation.communities.LeaveRequestState
import com.example.thecommunity.presentation.communities.RequestStatus
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SpacesViewModel(
    private val spaceRepository: SpaceRepository,
    private val userRepository: UserRepository,
    private val context : Context,
) : ViewModel() {
//    private val _liveState = MutableStateFlow<SpaceState>(SpaceState.Loading)
//    val liveState: StateFlow<SpaceState> = _liveState

    private val _spaceMembers = MutableStateFlow<List<UserData>>(emptyList())
    val spaceMembers: StateFlow<List<UserData>> get() = _spaceMembers

    private val _users = MutableStateFlow<List<UserData>>(emptyList())
    val users: StateFlow<List<UserData>> = _users

    private val _liveSpacesState = MutableStateFlow<SpaceState>(SpaceState.Loading)
    val liveSpacesState: StateFlow<SpaceState> = _liveSpacesState

    private val _isJoined = MutableStateFlow<JoinedStatusState>(JoinedStatusState.Loading)
    val isJoined: StateFlow<JoinedStatusState> get() = _isJoined

    private val _joinRequestState = MutableStateFlow<JoinRequestState>(JoinRequestState.Idle)
    val joinRequestState: StateFlow<JoinRequestState> = _joinRequestState

    private val _leaveRequestState = MutableStateFlow<LeaveRequestState>(LeaveRequestState.Idle)
    val leaveRequestState: StateFlow<LeaveRequestState> = _leaveRequestState.asStateFlow()

    private val _pendingSpacesState = MutableStateFlow<SpaceState>(SpaceState.Loading)
    val pendingSpacesState: StateFlow<SpaceState> = _pendingSpacesState

    private val _rejectedSpacesState = MutableStateFlow<SpaceState>(SpaceState.Loading)
    val rejectedSpacesState: StateFlow<SpaceState> = _rejectedSpacesState

    private val _requestStatus = MutableStateFlow<RequestStatus>(RequestStatus.Idle)
    val requestStatus: StateFlow<RequestStatus> = _requestStatus
    val pendingCount = mutableIntStateOf(0)

    private var listenerRegistration: ListenerRegistration? = null

    init {
        fetchUsers()
        //fetchLiveSpaces()
    }

    fun clearRequestStatus() {
        _requestStatus.value = RequestStatus.Idle
    }
    fun fetchUsers() {
        viewModelScope.launch {
            _users.value = userRepository.fetchUsers()
        }
    }
    fun clearState() {
        _isJoined.value = JoinedStatusState.Loading
        _liveSpacesState.value = SpaceState.Loading
        listenerRegistration?.remove() // Clean up the listener when ViewModel is cleared

    }

    /**
     * CREATE
     */
    fun requestNewSpace(
        parentCommunityId : String,
        spaceName : String,
        profilePictureUri : Uri?,
        bannerUri : Uri?,
        description : String?,
        membersRequireApproval : Boolean,
        selectedLeaders : List<UserData>,
        selectedEditors : List<UserData>
    ) {
        viewModelScope.launch {
            _requestStatus.value = RequestStatus.Loading
            try {
                spaceRepository.requestNewSpace(
                    parentCommunityId = parentCommunityId,
                    spaceName = spaceName,
                    bannerUri = bannerUri,
                    profilePictureUri = profilePictureUri,
                    selectedLeaders = selectedLeaders,
                    selectedEditors = selectedEditors,
                    membersRequireApproval = membersRequireApproval,
                    description = description

                )
                _requestStatus.value = RequestStatus.Success
            } catch (e: Exception) {
                _requestStatus.value = RequestStatus.Error(e.message ?: "Check your internet and try again")

                Log.e("SpacesViewModel", "Space request failed: ${e.message}")
            }
        }

//        viewModelScope.launch {
//            delay(10000) // 10 seconds delay
//            if (requestJob.isActive) {
//                requestJob.cancel() // Cancel the leave operation if still active
//
//                // Reset state to idle after timeout
//                _requestStatus.value = RequestStatus.Idle
//            }
//        }

    }

    fun editSpace(
        spaceId: String,
        newSpaceName: String?,
        newBannerUri: Uri?,
        newProfileUri: Uri?,
        newRequiresApproval : Boolean,
        parentCommunityId: String,
        newAboutUs: String?
    ){
        viewModelScope.launch {
            spaceRepository.editSpace(
                spaceId = spaceId,
                newSpaceName = newSpaceName,
                newBannerUri = newBannerUri,
                newProfileUri = newProfileUri,
                newRequiresApproval = newRequiresApproval,
                parentCommunityId = parentCommunityId,
                newAboutUs = newAboutUs
            )
        }
    }
    /**
     * READ
     */

    /**
     * Get space by ID
     */
    fun startObservingJoinedStatus(userId: String, spaceId: String) {
        viewModelScope.launch {

            // Cancel any previous listener
            listenerRegistration?.remove()

            listenerRegistration = spaceRepository.observeJoinedStatus(userId, spaceId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, JoinedStatusState.Loading)
                .collect { joinedStatus ->
                    when (joinedStatus) {
                        is JoinedStatusState.Loading -> {
                            // Optionally handle loading state if needed
                        }
                        is JoinedStatusState.Success -> {
                            _isJoined.value = joinedStatus
                        }
                        is JoinedStatusState.Error -> {
                            // Handle error state, maybe show a message
                        }
                    }
                }
        }

    }

    fun startObservingSpaceMembers(spaceId: String) {
        viewModelScope.launch {
            spaceRepository.getSpaceMembersFlow(spaceId).collect { memberList ->
                _spaceMembers.value = memberList
            }
        }
    }

    suspend fun getSpaceById(spaceId: String): Space? {
        return spaceRepository.getSpaceById(spaceId)
    }

    /**
     * Observe spaces by approval status
     */
    fun fetchPendingSpacesByCommunity(communityId: String) {
        viewModelScope.launch {
            _pendingSpacesState.value = SpaceState.Loading
            try {
                spaceRepository.observePendingSpacesByCommunity(communityId)
                    .collect { spaces ->
                        _pendingSpacesState.value = SpaceState.Success(spaces)
                        pendingCount.intValue = spaces.size
                    }
            } catch (e: Exception) {
                Log.e("SpacesViewModel", "Error pending fetching spaces for community $communityId: ${e.message}")
                _pendingSpacesState.value = SpaceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchRejectedSpacesByCommunity(communityId: String) {
        viewModelScope.launch {
            _rejectedSpacesState.value = SpaceState.Loading
            try {
                spaceRepository.observeRejectedSpacesByCommunity(communityId)
                    .collect{spaces->
                        _rejectedSpacesState.value = SpaceState.Success(spaces)
                    }
            } catch (e: Exception) {
                Log.e("SpacesViewModel", "Error rejected fetching spaces for community $communityId: ${e.message}")
                _rejectedSpacesState.value = SpaceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchLiveSpacesByCommunity(communityId: String) {
        viewModelScope.launch {
            try {
                spaceRepository.observeLiveSpacesByCommunity(communityId)
                    .collect { spaces ->
                        _liveSpacesState.value = SpaceState.Success(spaces)
                    }
            } catch (e: Exception) {
                Log.e("SpacesViewModel", "Error fetching live spaces for community $communityId: ${e.message}")
                _liveSpacesState.value = SpaceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Observe users by community
     */
    fun fetchCommunityUsers(communityId: String) {
        viewModelScope.launch {
            try {
                val communityUsers = userRepository.fetchUsersByCommunity(communityId)
                _users.value = communityUsers
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Error fetching community users: ${e.message}")
            }
        }
    }

    /**
     * UPDATE
     */
    fun approveSpace(request: Space) {
        viewModelScope.launch {
            try {
                Log.d("SpacesViewModel", "Attempting to approve space with ID: ${request.id}")

                val db = spaceRepository.db

                // Update the space approval status
                db.collection("spaces").document(request.id)
                    .update("approvalStatus", "Live")
                    .await()

                // Update the space approval status in the community document
                val communityRef = db.collection("communities").document(request.parentCommunity)

                // Fetch the current community document
                val communityDoc = communityRef.get().await()
                val community = communityDoc.toObject(Community::class.java)

                community?.let {
                    // Prepare the updated spaces list
                    val updatedSpaces = it.spaces.map { spaceEntry ->
                        if (spaceEntry.keys.first() == request.id) {
                            mapOf(request.id to "Live") // Update status to "Live"
                        } else {
                            spaceEntry
                        }
                    }

                    // Update the community document with the new spaces list
                    communityRef.update("spaces", updatedSpaces).await()

                    // Fetch updated spaces
                    fetchLiveSpacesByCommunity(request.parentCommunity)
                    fetchPendingSpacesByCommunity(request.parentCommunity)

                } ?: run {
                    Log.e("SpacesViewModel", "Community document not found")
                    _pendingSpacesState.value = SpaceState.Error("Community not found")
                }

            } catch (e: Exception) {
                Log.w("SpacesViewModel", "Error approving space", e)
                _pendingSpacesState.value = SpaceState.Error("Error approving space: ${e.message}")
            }
        }
    }

    fun rejectSpace(request: Space) {
        viewModelScope.launch {
            try {
                Log.d("SpaceViewModel", "Attempting to reject space with ID: ${request.name}")

//                spaceRepository.db.collection("spaces").document(request.name)
//                    .update("status", "Rejected")
//                    .await()
                spaceRepository.deleteSpace(spaceId = request.id)
                fetchPendingSpacesByCommunity(request.parentCommunity)
                fetchRejectedSpacesByCommunity(request.parentCommunity)
            } catch (e: Exception) {
                Log.w("SpaceViewModel", "Error rejecting space", e)
                _pendingSpacesState.value = SpaceState.Error("Error rejecting space: ${e.message}")
            }
        }
    }

    suspend fun deleteSpace(
        space: Space
    ){
        spaceRepository.deleteSpace(
            spaceId = space.id
        )
        fetchLiveSpacesByCommunity(space.parentCommunity)

    }


    suspend fun onJoinSpace(user: UserData, space: Space): Boolean {
        // Create a Job to track the join operation
        return try {
            _joinRequestState.value = JoinRequestState.Loading

            // Perform the join operation
            spaceRepository.addMembersToSpace(user.userId, spaceId = space.id)
            getSpaceById(spaceId = space.id)

            _joinRequestState.value = JoinRequestState.Idle // Reset to idle after successful join
            true
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "Community join failed: ${e.message}")

            // Show a toast to the user
            Toast.makeText(context, "Please check your internet connection and try again.", Toast.LENGTH_LONG).show()

            // Reset state to idle
            _joinRequestState.value = JoinRequestState.Idle
            false
        }finally {
            fetchLiveSpacesByCommunity(space.parentCommunity)
            fetchPendingSpacesByCommunity(space.parentCommunity)
            getSpaceById(spaceId = space.id)
        }
    }

    suspend fun removeUserFromSpace(
        space: Space,
        userId: String
    ) : Boolean {
        // Create a Job to track the leave operation
        Log.d("SpaceViewModel", "Attempting to leave space with ID: ${space.id}")
        val spaceId = space.id
        return try {
            _joinRequestState.value = JoinRequestState.Loading
            _leaveRequestState.value = LeaveRequestState.Loading

            // Perform the remove operation
            spaceRepository.removeUserFromSpace(spaceId = spaceId, userId = userId)

            getSpaceById(spaceId)

            _leaveRequestState.value = LeaveRequestState.Success
            _joinRequestState.value = JoinRequestState.Idle // Reset to idle after successful leave
            true
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "Community leave failed: ${e.message}")

            // Show a toast to the user
            Toast.makeText(context, "Please check your internet connection and try again.", Toast.LENGTH_LONG).show()

            // Reset states to idle instead of error
            _leaveRequestState.value = LeaveRequestState.Idle
            _joinRequestState.value = JoinRequestState.Idle
            false
        }finally {
            fetchLiveSpacesByCommunity(space.parentCommunity)
            fetchPendingSpacesByCommunity(space.parentCommunity)
            getSpaceById(spaceId = space.id)
        }
    }



    suspend fun demoteMemberInSpaces(
        userId: String,
        spaceId: String
    ) : Boolean{
        return try{
            spaceRepository.demoteMemberInSpaces(userId, spaceId)
            true
        }catch (e: Exception){
            Log.e(
                "SpaceViewModel",
                "Error demoting member $userId in space $spaceId",
                e
            )
            false
        }
    }

    suspend fun promoteMemberInSpace(
        userId: String,
        spaceId: String,
        role : String
    ): Boolean{
        return try {
            spaceRepository.promoteMemberInSpace(
                userId = userId,
                spaceId = spaceId,
                role = role
            )
            true

        }catch (e: Exception){
            Log.e(
                "SpaceViewModel",
                "Error promoting member $userId in space $spaceId",
                e
            )
            false
        }
    }

    /**
     * DELETE
     */


    /**
     * LEGACY
     */





}

sealed class SpaceState {
    data object Loading : SpaceState()
    data class Success(val spaces: List<Space>) : SpaceState()
    data class Error(val message: String) : SpaceState()
}