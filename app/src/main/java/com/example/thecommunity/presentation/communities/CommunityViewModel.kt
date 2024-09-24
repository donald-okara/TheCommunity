package com.example.thecommunity.presentation.communities

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.CommunityRepository
import com.example.thecommunity.data.repositories.EventRepository
import com.example.thecommunity.data.repositories.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val eventsRepository: EventRepository,
    val context : Context,
) : ViewModel() {
    private val _communityState = MutableStateFlow<CommunityState>(CommunityState.Loading)
    val communityState: StateFlow<CommunityState> = _communityState

    private val _pendingState = MutableStateFlow<CommunityState>(CommunityState.Loading)
    val pendingState: StateFlow<CommunityState> = _pendingState

    private val _users = MutableStateFlow<List<UserData>>(emptyList())
    val users: StateFlow<List<UserData>> = _users

    private val _leaveRequestState = MutableStateFlow<LeaveRequestState>(LeaveRequestState.Idle)
    val leaveRequestState: StateFlow<LeaveRequestState> = _leaveRequestState.asStateFlow()

    private val _isJoined = MutableStateFlow<JoinedStatusState>(JoinedStatusState.Loading)
    val isJoined: StateFlow<JoinedStatusState> get() = _isJoined

    private val _joinRequestState = MutableStateFlow<JoinRequestState>(JoinRequestState.Idle)
    val joinRequestState: StateFlow<JoinRequestState> = _joinRequestState

    private val _requestStatus = MutableStateFlow<RequestStatus>(RequestStatus.Idle)
    val requestStatus: StateFlow<RequestStatus> = _requestStatus

    private val _communityMembers = MutableStateFlow<List<UserData>>(emptyList())
    val communityMembers: StateFlow<List<UserData>> get() = _communityMembers

    private val _communitySpaces = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val communitySpaces: StateFlow<List<Map<String, String>>> get() = _communitySpaces

    private var listenerRegistration: ListenerRegistration? = null

    init {
        fetchLiveCommunities()
    }


    /**
     * CREATE
     */

    /**
     * Function to request a new community
     */
    fun requestNewCommunity(
        communityName: String,
        communityType: String,
        bannerUri: Uri?,
        aboutUs : String?,
        profileUri: Uri?,
        selectedLeaders: List<UserData>,
        selectedEditors: List<UserData>
    ) {

        viewModelScope.launch {
            _requestStatus.value = RequestStatus.Loading
            try {
                communityRepository.requestNewCommunity(
                    communityName = communityName,
                    communityType = communityType,
                    bannerUri = bannerUri,
                    profileUri = profileUri,
                    selectedLeaders = selectedLeaders,
                    selectedEditors = selectedEditors,
                    aboutUs = aboutUs
                )
                _requestStatus.value = RequestStatus.Success
            } catch (e: Exception) {
                _requestStatus.value = RequestStatus.Error(e.message ?: "Check your internet and try again")

                Log.e("CommunityViewModel", "Community request failed: ${e.message}")
            }
        }
//        viewModelScope.launch {
//            delay(10000) // 10 seconds delay
//            if (requestJob.isActive) {
//                requestJob.cancel() // Cancel the leave operation if still active
//                // Show toast to user
//                Toast.makeText(
//                    context,
//                    "Please check your internet connection and try again later.",
//                    Toast.LENGTH_LONG
//                ).show()
//                // Reset state to idle after timeout
//                _requestStatus.value = RequestStatus.Idle
//            }
//        }
    }

    fun editCommunity(
        communityId: String,
        communityName: String,
        communityType: String,
        bannerUri: Uri?,
        aboutUs : String?,
        profileUri: Uri?,
    ){
        viewModelScope.launch {
            _requestStatus.value = RequestStatus.Loading
            communityRepository.editCommunity(
                communityId = communityId,
                newCommunityName = communityName,
                newCommunityType = communityType,
                newBannerUri = bannerUri,
                newProfileUri = profileUri,
                newAboutUs = aboutUs,
            )
        }
//        viewModelScope.launch {
//            delay(10000) // 10 seconds delay
//            if (requestJob.isActive) {
//                requestJob.cancel() // Cancel the leave operation if still active
//                // Show toast to user
//                Toast.makeText(
//                    context,
//                    "Please check your internet connection and try again later.",
//                    Toast.LENGTH_LONG
//                ).show()
//                // Reset state to idle after timeout
//                _requestStatus.value = RequestStatus.Idle
//            }
//        }
    }

    fun clearRequestStatus() {
        _requestStatus.value = RequestStatus.Idle
    }

    /**
     * READ
     */

    /**
    * Observe community members
     */
    fun fetchCommunityMembers(communityId: String){
        viewModelScope.launch {
            communityRepository.getCommunityMembersFlow(communityId).collect { members ->
                _communityMembers.value = members
            }
        }

        Log.d("CommunityViewModel", "UserData : ${_communityMembers.value}")
    }

    fun startObservingCommunitySpaces(communityId: String) {
        viewModelScope.launch {
            communityRepository.getCommunitySpacesFlow(communityId).collect { spacesList ->
                _communitySpaces.value = spacesList
            }
        }
    }

    /**
     * Observe community members
     */
    fun startObservingJoinedStatus(userId: String, communityId: String) {
        viewModelScope.launch {

            // Cancel any previous listener
            listenerRegistration?.remove()

            listenerRegistration = communityRepository.observeJoinedStatus(userId, communityId)
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

    /**
     * Fetch users from the repository
     */
    fun fetchUsers() {
        viewModelScope.launch {
            _users.value = userRepository.fetchUsers()
        }
    }

    /**
     * Fetch live communities
     */
// Replaced individual state flows with a single state flow for community data

    fun fetchLiveCommunities() {
        fetchCommunitiesByStatus("Live")
    }

    private fun fetchCommunitiesByStatus(status: String) {
        viewModelScope.launch {
            try {
                val currentState = _communityState.value
                if (currentState is CommunityState.Success && currentState.communities.isEmpty()) {
                    _communityState.value = CommunityState.Loading
                }

                communityRepository.getCommunitiesByStatus(status)
                    .collect { communities ->
                        if (_communityState.value !is CommunityState.Success ||
                            (_communityState.value as CommunityState.Success).communities != communities) {
                            _communityState.value = CommunityState.Success(communities)
                        }
                    }
            } catch (e: Exception) {
                _communityState.value = CommunityState.Error("Error fetching communities: ${e.message}")
            }
        }

    }

    fun fetchPendingRequestsForUser(userId: String) {
        viewModelScope.launch {
            try {
                communityRepository.getCommunitiesByStatus("Pending")
                    .collect { Communities ->
                        val userCommunities = Communities.filter { community ->
                            community.members.any { it.containsKey(userId) }
                        }
                        if (_pendingState.value !is CommunityState.Success ||
                            (_pendingState.value as CommunityState.Success).communities != userCommunities) {
                            _pendingState.value = CommunityState.Success(userCommunities)
                        }
                    }
            } catch (e: Exception) {
                _pendingState.value = CommunityState.Error("Error fetching pending requests: ${e.message}")
            }
        }
    }

    fun fetchLiveRequestsForUser(userId: String) {
        viewModelScope.launch {
            try {
                communityRepository.getCommunitiesByStatus("Live")
                    .collect { Communities ->
                        val userCommunities = Communities.filter { community ->
                            community.members.any { it.containsKey(userId) }
                        }
                        if (_communityState.value !is CommunityState.Success ||
                            (_communityState.value as CommunityState.Success).communities != userCommunities) {
                            _communityState.value = CommunityState.Success(userCommunities)
                        }
                    }
            } catch (e: Exception) {
                _communityState.value = CommunityState.Error("Error fetching live requests: ${e.message}")
            }
        }

    }



    /**
     * Function to get a community by its ID for navigation
     */
    suspend fun getCommunityById(communityId: String): Community? {
        return communityRepository.getCommunityById(communityId = communityId)
    }

    fun getPendingCommunityById(communityId: String): Community? {

        val state = _pendingState.value
        return if (state is CommunityState.Success) {
            state.communities.find { it.id == communityId }
        } else {
            null
        }
    }

    /**
     * UPDATE
     */

    /**
     * Function to join a community
     */
    suspend fun onJoinCommunity(user: UserData, community: Community): Boolean {
        return try {
            _joinRequestState.value = JoinRequestState.Loading

            // Perform the join operation
            communityRepository.updateUserAndAddMemberToCommunity(user.userId, community.id)
            startObservingJoinedStatus(user.userId, community.id)
            getCommunityById(community.id)

            _joinRequestState.value = JoinRequestState.Idle // Reset to idle after successful join
            true
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "Community join failed: ${e.message}")

            // Show a toast to the user
            Toast.makeText(context, "Please check your internet connection and try again.", Toast.LENGTH_LONG).show()

            // Reset state to idle
            _joinRequestState.value = JoinRequestState.Idle
            false
        } finally {
            // Perform these operations regardless of success or failure
            fetchLiveCommunities()
        }
    }

    suspend fun removeUserFromCommunity(
        communityId: String,
        userId: String
    ): Boolean {
        return try {
            _joinRequestState.value = JoinRequestState.Loading
            _leaveRequestState.value = LeaveRequestState.Loading

            // Perform the remove operation
            communityRepository.removeUserFromCommunity(communityId = communityId, userId = userId)

            startObservingJoinedStatus(userId = userId, communityId = communityId)
            getCommunityById(communityId)

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
        } finally {
            // Perform these operations regardless of success or failure
            fetchLiveCommunities()
            userRepository.getCurrentUser()
        }
    }


    /**
     * Demote user to member
     */
    suspend fun demoteMemberInCommunity(
        userId : String,
        communityId: String
    ) : Boolean{
        return try{
            communityRepository.demoteMemberInCommunity(
                userId = userId,
                communityId = communityId
            )
            true
        }catch (e: Exception){
            Log.e(
                "CommunityViewModel",
                "Error demoting member $userId in community $communityId",
                e
            )
            false
        }
    }

    /**
     * Add leaders or editors
      */
    suspend fun promoteMemberInCommunity(
        userId: String,
        communityId: String,
        role: String
    ): Boolean {
        return try {
            communityRepository.promoteMemberInCommunity(
                userId = userId,
                communityId = communityId,
                role = role
            )
            true // Return true if the operation is successful
        } catch (e: Exception) {
            Log.e(
                "CommunityViewModel",
                "Error promoting member $userId in community $communityId to $role",
                e
            )
            false // Return false if an error occurs
        }
    }


    /**
     * update community fields
     */




    /**
     * DELETE
     */



    suspend fun deleteCommunity(
        communityId: String
    ){
        communityRepository.deleteCommunity(
            communityId = communityId
        )
        fetchLiveCommunities()
    }


    /**
     * LEGACY
     */

    /**
     * Fetch community members legacy code
     */
//    private fun fetchCommunityMembers(communityId: String) {
//        viewModelScope.launch {
//            try {
//                val members = communityRepository.getCommunityMembers(communityId)
//                _communityMembers.value = members
//            } catch (e: Exception) {
//                Log.e("CommunityViewModel", "Error fetching community members: ${e.message}")
//            }
//        }
//    }
}





sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}


sealed class RequestStatus {
    data object Idle : RequestStatus()
    data object Loading : RequestStatus()
    data object Success : RequestStatus()
    data class Error(val message: String) : RequestStatus()
}

sealed class CommunityState {
    data object Loading : CommunityState()
    data class Success(val communities: List<Community>) : CommunityState()
    data class Error(val message: String) : CommunityState()
}

sealed class JoinedStatusState {
    data object Loading : JoinedStatusState()
    data class Success(val isJoined: Boolean) : JoinedStatusState()
    data class Error(val message: String) : JoinedStatusState()
}

sealed class JoinRequestState {
    data object Idle : JoinRequestState()
    object Loading : JoinRequestState()
    object Success : JoinRequestState()
    data class Error(val message: String) : JoinRequestState()
}

sealed class LeaveRequestState {
    object Idle : LeaveRequestState()
    object Loading : LeaveRequestState()
    object Success : LeaveRequestState()
    data class Error(val message: String) : LeaveRequestState()
}
