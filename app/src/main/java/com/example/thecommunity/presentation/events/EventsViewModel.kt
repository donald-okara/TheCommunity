package com.example.thecommunity.presentation.events

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thecommunity.data.model.CommentWithDetails
import com.example.thecommunity.data.model.Dropoff
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.Location
import com.example.thecommunity.data.model.Pickup
import com.example.thecommunity.data.model.RatingWithDetails
import com.example.thecommunity.data.model.ReplyWithDetails
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.EventRepository
import com.example.thecommunity.data.repositories.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
): ViewModel(){
    val Tag = "EventsViewModel"

    private val _eventAttendees = MutableStateFlow<List<UserData>>(emptyList())
    val eventAttendees: StateFlow<List<UserData>> get() = _eventAttendees

    private val _eventUnattendees = MutableStateFlow<List<UserData>>(emptyList())
    val eventUnattendees: StateFlow<List<UserData>> get() = _eventUnattendees

    private val _eventRefunds = MutableStateFlow<List<UserData>>(emptyList())
    val eventRefunds: StateFlow<List<UserData>> get() = _eventRefunds

    private val _eventComments = MutableStateFlow<List<CommentWithDetails>>(emptyList())
    val eventComments: StateFlow<List<CommentWithDetails>> get() = _eventComments

    private val _eventRatings = MutableStateFlow<List<RatingWithDetails>>(emptyList())
    val eventRatings: StateFlow<List<RatingWithDetails>> get() = _eventRatings

    private val _eventReplies = MutableStateFlow<List<ReplyWithDetails>>(emptyList())
    val eventReplies: StateFlow<List<ReplyWithDetails>> get() = _eventReplies

    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Loading)
    val eventsState: StateFlow<EventsState> = _eventsState

    private val _spaceEventsState = MutableStateFlow<EventsState>(EventsState.Loading)
    val spaceEventsState: StateFlow<EventsState> = _spaceEventsState

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    fun clearState(){
        _event.value = null
        _eventsState.value = EventsState.Loading
        _spaceEventsState.value = EventsState.Loading
    }

    /**
     * CREATE
     */
    suspend fun createEvent(
        eventName: String,
        communityId: String?,
        spaceId: String?,
        description: String?,
        location: Location,
        startDate: LocalDate,
        startTime: LocalTime,
        numberOfDays : Int,
        endDate: LocalDate,
        endTime: LocalTime,
        pickUpList: List<Pickup>,
        dropOffList: List<Dropoff>,
        imageUris: List<Uri>?,
        price : Int?,
        paymentDetails : String?
    ){
        val currentUser = userRepository.getCurrentUser()

        eventRepository.createEvent(
            eventName = eventName,
            communityId = communityId,
            spaceId = spaceId,
            description = description,
            location = location,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            endTime = endTime,
            organizerId = currentUser?.userId ?: "",
            pickUpList = pickUpList,
            dropOffList = dropOffList,
            imageUris = imageUris,
            price = price,
            numberOfDays = numberOfDays,
            paymentDetails = paymentDetails
        )
    }

    suspend fun editEvent(
        eventId: String,
        eventName: String? = null,
        description: String? = null,
        location: Location? = null,
        numberOfDays: Int? = null,
        startDate: LocalDate? = null,
        startTime: LocalTime? = null,
        endDate: LocalDate? = null,
        endTime: LocalTime? = null,
        pickUpList: List<Pickup>? = null,
        dropOffList: List<Dropoff>? = null,
        imageUris: List<Uri>? = null,
        imagesToDelete : List<Uri>? = null,
        price: Int? = null,
        paymentDetails: String? = null
    ){
        eventRepository.editEvent(
            eventId = eventId,
            eventName = eventName,
            description = description,
            location = location,
            numberOfDays = numberOfDays,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            endTime = endTime,
            imagesToDelete = imagesToDelete,
            pickUpList = pickUpList,
            dropOffList = dropOffList,
            imageUris = imageUris,
            price = price,
            paymentDetails = paymentDetails
        )
    }


    /**
     * READ
     */
    fun getEventsForCommunities(
        communityId: String?,
        spaceId: String?
    ) {
        viewModelScope.launch {
            try {
                val currentState = _eventsState.value
                if (currentState is EventsState.Success && currentState.events.isEmpty()) {
                    _eventsState.value = EventsState.Loading
                }

                eventRepository.observeEventsByCommunityOrSpace(
                    communityId = communityId,
                    spaceId = spaceId
                ).collect { events ->
                    if (_eventsState.value !is EventsState.Success ||
                        (_eventsState.value as EventsState.Success).events != events) {
                        _eventsState.value = EventsState.Success(events)
                    }
                }
            } catch (e: Exception) {
                _eventsState.value = EventsState.Error("Error fetching events: ${e.message}")
            }
        }
    }

    fun getEventsForSpaces(
        communityId: String?,
        spaceId: String?
    ) {
        viewModelScope.launch {
            try {
                val currentState = _spaceEventsState.value
                if (currentState is EventsState.Success && currentState.events.isEmpty()) {
                    _spaceEventsState.value = EventsState.Loading
                }

                eventRepository.observeEventsByCommunityOrSpace(
                    communityId = communityId,
                    spaceId = spaceId
                ).collect { events ->
                    if (_spaceEventsState.value !is EventsState.Success ||
                        (_spaceEventsState.value as EventsState.Success).events != events) {
                        _spaceEventsState.value = EventsState.Success(events)
                    }
                }
            } catch (e: Exception) {
                _spaceEventsState.value = EventsState.Error("Error fetching events: ${e.message}")
            }
        }
    }


    // Calculate the average rating based on the ratingsFlow
    val averageRatingFlow: Flow<Int> = eventRatings.map { ratings ->
        val validRatings = ratings.filter { it.rating in 1..5 }
        if (validRatings.isNotEmpty()) {
            (validRatings.sumOf { it.rating } / validRatings.size).toInt()
        } else {
            0
        }
    }

    suspend fun getEventById(
        eventId: String
    ): Event? {
        return eventRepository.getEventById(eventId)
    }

    suspend fun getEventRatings(
        eventId: String
    ){
        try {
            eventRepository.getEventRatings(eventId= eventId).collect { rating ->
                _eventRatings.value = rating
            }
        }catch (e : Exception){
            _eventRatings.value = emptyList()
            Log.e(Tag, "Failed to fetch ratings. $e")
        }

    }

    fun fetchEventAttendees(
        eventId: String
    ){
        viewModelScope.launch {
            eventRepository.getEventAttendeesFlow(eventId).collect { attendees ->
                _eventAttendees.value = attendees
            }
        }

        Log.d(Tag, "UserData : ${_eventAttendees.value}")
    }

    fun fetchEventComments(
        eventId: String
    ){
        viewModelScope.launch {
            eventRepository.getEventComments(eventId).collect { comments ->
                _eventComments.value = comments
            }
        }

        Log.d(Tag, "UserData : ${_eventComments.value}")
    }

    fun fetchEventReplies(
        eventId: String,
    ){
        viewModelScope.launch {
            eventRepository.getAllReplies(eventId= eventId).collect { replies ->
                _eventReplies.value = replies
            }
        }

        Log.d(Tag, "UserData : ${_eventReplies.value}")
    }

    fun fetchEventUnattendees(
        eventId: String
    ) {
        viewModelScope.launch {
            eventRepository.getEventUnattendeesFlow(eventId).collect { unattendees ->
                _eventUnattendees.value = unattendees
            }
        }

        Log.d(Tag, "UserData : ${_eventUnattendees.value}")
    }

    fun fetchEventRefunds(
        eventId: String
    ) {
        viewModelScope.launch {
            eventRepository.getEventRefundsFlow(eventId).collect { refunds ->
                _eventRefunds.value = refunds
            }
        }

        Log.d(Tag, "UserData : ${_eventUnattendees.value}")
    }

    /**
     * UPDATE
     */

    suspend fun attendEvent(
        eventId: String,
        userId: String
    ):Boolean{
        return try {
            eventRepository.attendEvent(
                userId = userId,
                eventId = eventId
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error attending event")
            false
        }
    }

    suspend fun addRatingToEvent(
        eventId: String,
        userId: String,
        rating: Int,
        remark: String
    ): Boolean{
        return eventRepository.addRatingToEvent(
            eventId = eventId,
            userId = userId,
            rating = rating,
            remark = remark
        )
    }
    suspend fun leaveEvent(
        eventId: String,
        userId: String,
        reasonToLeave: String,
        refundRequested: Boolean = false
    ):Boolean{
        return try {
            eventRepository.leaveEvent(
                eventId = eventId,
                userId = userId,
                reasonToLeave = reasonToLeave,
                refundRequested = refundRequested
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error leaving event")
            false
        }
    }

    suspend fun toggleAttendanceApproval(
        eventId: String,
        userId: String
    ):Boolean{
        return try{
            eventRepository.toggleAttendanceApproval(
                eventId = eventId,
                userId = userId
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error toggling attendance approval")
            false
        }
    }

    suspend fun toggleAttendanceArrival(
        eventId: String,
        userId: String
    ):Boolean{
        return try{
            eventRepository.toggleAttendanceArrival(
                eventId = eventId,
                userId = userId
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error toggling attendance approval")
            false
        }
    }

    suspend fun toggleMute(
        eventId: String,
        userId: String
    ):Boolean{
        return try{
            eventRepository.toggleMute(
                eventId = eventId,
                userId = userId
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error toggling attendance approval")
            false
        }
    }

    suspend fun addUserToPickup(
        eventId: String,
        userId: String,
        pickupIndex: Int
    ):Boolean{
        return try {
            eventRepository.addUserToPickup(
                eventId = eventId,
                userId = userId,
                pickupIndex = pickupIndex
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error adding user to pickup")
            false
        }


    }

    suspend fun addUserToDropOff(
        eventId: String,
        userId: String,
        dropOffIndex: Int):Boolean{
        return try {
            eventRepository.addUserToDropOff(
                eventId = eventId,
                userId = userId,
                dropoffIndex = dropOffIndex
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error adding user to drop off")
            false
        }

    }

    suspend fun removeUserFromPickup(
        eventId: String,
        userId: String,
        pickupIndex: Int,
    ):Boolean{
        return try {
            eventRepository.removeUserFromPickup(
                eventId = eventId,
                userId = userId,
                pickupIndex = pickupIndex
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error removing user from pickup")
            false
        }
    }

    suspend fun removeUserFromDropoff(
        eventId: String,
        userId: String,
        dropOffIndex: Int,
    ):Boolean{
        return try {
            eventRepository.removeUserFromDropOff(
                eventId = eventId,
                userId = userId,
                dropOffIndex = dropOffIndex
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error removing user from dropOff")
            false
        }
    }

    suspend fun addCommentToEvent(
        eventId: String,
        userId: String,
        text: String
    ):Boolean{
        return try{
            eventRepository.addCommentToEvent(
                eventId = eventId,
                userId = userId,
                text = text
            )
            true
        }catch (e: Exception){
            Log.e(Tag, "Error adding comment to event")
            false
        }
    }

    suspend fun addReplyToComment(
        eventId: String,
        commentId: String,
        userId: String,
        text: String
    ):Boolean{
        return try{
            eventRepository.addCommentReply(
                eventId = eventId,
                commentId = commentId,
                userId = userId,
                text = text
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error adding reply to comment")
            false

        }
    }

    suspend fun editComment(
        eventId: String,
        commentId: String,
        newText: String
    ): Boolean{
        return try{
            eventRepository.editComment(
                eventId = eventId,
                commentId = commentId,
                newText = newText
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error editing comment")
            false
        }

    }

    suspend fun editReply(
        eventId: String,
        commentId: String,
        replyId: String,
        newText: String
    ): Boolean{
       return try {
           eventRepository.editReply(
               eventId = eventId,
               commentId = commentId,
               replyId = replyId,
               newText = newText
           )
           true
       }catch (e : Exception){
           Log.e(Tag, "Error editing reply")
           false
       }
    }

    suspend fun addInfractionToComment(
    eventId: String,
    commentId: String,
    reporterId: String,
    reason: String
    ): Boolean{
        return try {
            eventRepository.addInfractionToComment(
                eventId = eventId,
                commentId = commentId,
                reporterId = reporterId,
                reason = reason
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error adding infraction to comment")
            false
        }

    }

    suspend fun addInfractionToReply(
        eventId: String,
        commentId: String,
        replyId: String,
        reporterId: String,
        reason: String
    ): Boolean{
        return try {
            eventRepository.addInfractionToReply(
                eventId = eventId,
                commentId = commentId,
                replyId = replyId,
                reporterId = reporterId,
                reason = reason
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error adding infraction to reply")
            false
        }
    }

    /**
     * DELETE
     */
    suspend fun deleteComment(
        eventId: String,
        commentId: String
    ):Boolean{
        return try {
            eventRepository.deleteComment(
                eventId = eventId,
                commentId = commentId
            )
        }catch (e : Exception){
            Log.e(Tag, "Error deleting comment")
            false
        }
    }

    suspend fun deleteReply(
        eventId: String,
        commentId: String,
        replyId: String
    ):Boolean{
        return try {
            eventRepository.deleteReply(
                eventId = eventId,
                commentId = commentId,
                replyId = replyId

            )
        }catch (e : Exception){
            Log.e(Tag, "Error deleting reply")
            false
        }
    }

    suspend fun clearCommentAndDeleteReplies(
        eventId: String,
        commentId: String
    ): Boolean{
        return try {
            eventRepository.clearCommentAndDeleteReplies(
                eventId = eventId,
                commentId = commentId
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error clearing comment and deleting replies")
            false
        }
    }

    suspend fun clearReplyContent(
        eventId: String,
        commentId: String,
        replyId: String
    ): Boolean {
        return try {
            eventRepository.clearReplyContent(
                eventId = eventId,
                commentId = commentId,
                replyId = replyId
            )
            true
        } catch (e: Exception) {
            Log.e(Tag, "Error clearing reply content")
            false
        }
    }


    suspend fun deleteEvent(
        event: Event
    ): Boolean{
        return try {
            eventRepository.deleteEvent(
                event = event
            )
            true
        }catch (e : Exception){
            Log.e(Tag, "Error deleting event, $e")
            false
        }
    }

}

sealed class EventsState {
    data object Loading : EventsState()
    data class Success(val events: List<Event>) : EventsState()
    data class Error(val message: String) : EventsState()
}