package com.example.thecommunity.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.model.Comment
import com.example.thecommunity.data.model.CommentWithDetails
import com.example.thecommunity.data.model.Dropoff
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.EventAttendee
import com.example.thecommunity.data.model.Location
import com.example.thecommunity.data.model.Pickup
import com.example.thecommunity.data.model.Rating
import com.example.thecommunity.data.model.RatingWithDetails
import com.example.thecommunity.data.model.Reply
import com.example.thecommunity.data.model.ReplyWithDetails
import com.example.thecommunity.data.model.UserData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class EventRepository(
    private val db : FirebaseFirestore,
    private val firebaseService: FirebaseService,
    private val context: Context,
) {
    private val Tag = "EventRepository"
    /**
     * CREATE
     */
    suspend fun createEvent(
        eventName: String,
        communityId: String?,
        spaceId: String?,
        description: String?,
        location: Location,
        numberOfDays: Int,
        startDate: LocalDate,
        startTime: LocalTime,
        endDate: LocalDate,
        endTime: LocalTime,
        organizerId: String,
        pickUpList: List<Pickup>,
        dropOffList: List<Dropoff>,
        imageUris: List<Uri>?,
        price : Int?,
        paymentDetails : String?
    ) {
        // Generate a new ID for the event
        val eventId = db.collection("events").document().id

        // Upload images and get their URLs with metadata
        val imageMetadataList = imageUris?.map { uri ->
            val imageName = "${UUID.randomUUID()}.jpg" // Generate a unique name
            val imageUrl = firebaseService.uploadImageToStorage(
                uri,
                "events/images/${eventId}/$imageName"
            )
            mapOf("name" to imageName, "url" to imageUrl)
        } ?: emptyList()

        // Create the Event object
        val event = Event(
            id = eventId,
            name = eventName,
            communityId = communityId,
            spaceId = spaceId,
            description = description,
            location = location,
            numberOfDays = numberOfDays,
            startDate = startDate.toString(),
            startTime = startTime.toString(),
            endDate = endDate.toString(),
            endTime = endTime.toString(),
            organizer = organizerId,
            pickUp = pickUpList,
            dropOff = dropOffList,
            images = imageMetadataList,
            price = price,
            paymentDetails = paymentDetails
        )

        try {
            // Start a batch operation
            val batch = db.batch()

            // Add the new event to the events collection
            val eventRef = db.collection("events").document(eventId)
            batch.set(eventRef, event)

            // Optionally, update the community or space with the new event reference
            communityId?.let {
                val communityRef = db.collection("communities").document(it)
                val communityDoc = communityRef.get().await()
                val currentEvents = communityDoc.get("events") as? List<String> ?: emptyList()
                val updatedEvents = currentEvents.toMutableList().apply { add(eventId) }
                batch.update(communityRef, "events", updatedEvents)
            }

            spaceId?.let {
                val spaceRef = db.collection("spaces").document(it)
                val spaceDoc = spaceRef.get().await()
                val currentEvents = spaceDoc.get("events") as? List<String> ?: emptyList()
                val updatedEvents = currentEvents.toMutableList().apply { add(eventId) }
                batch.update(spaceRef, "events", updatedEvents)
            }

            // Commit the batch
            batch.commit().await()

            Log.d(Tag, "Event created successfully: $eventId")
            Toast.makeText(context, "Event created successfully", Toast.LENGTH_SHORT).show()


        } catch (e: Exception) {
            Log.e(Tag, "Error creating event: $eventId", e)
        }
    }

    suspend fun editEvent(
        eventId: String,
        eventName: String? = null,
        description: String? = null,
        location: Location? = null,
        numberOfDays: Int? = null,
        startDate: LocalDate? = null,
        startTime: LocalTime? = null,
        imagesToDelete: List<Uri>? = null,
        endDate: LocalDate? = null,
        endTime: LocalTime? = null,
        pickUpList: List<Pickup>? = null,
        dropOffList: List<Dropoff>? = null,
        imageUris: List<Uri>? = null,
        price: Int? = null,
        paymentDetails: String? = null
    ) {
        try {
            // Step 1: Upload new images asynchronously before the transaction
            val newImageMetadataList = imageUris?.filterNot { uri ->
                uri.toString().startsWith("https")
            }?.map { uri ->
                val imageName = "${UUID.randomUUID()}.jpg"
                val imageUrl = firebaseService.uploadImageToStorage(uri, "events/images/$eventId/$imageName")
                mapOf("name" to imageName, "url" to imageUrl)
            } ?: emptyList()

            // Step 2: Run the Firestore transaction
            db.runTransaction { transaction ->

                val eventRef = db.collection("events").document(eventId)
                val eventDoc = transaction.get(eventRef)

                if (!eventDoc.exists()) {
                    throw Exception("Event not found: $eventId")
                }

                // Get the current images metadata from the event document
                val currentImages = eventDoc.get("images") as? List<Map<String, String?>> ?: emptyList()

                // Prepare URLs to delete
                val urlsToDelete = imagesToDelete?.map { it.toString() } ?: emptyList()

                // Combine new images with existing images that are not deleted
                val cleanedCurrentImages = currentImages.filter { imageMetadata ->
                    val imageUrl = imageMetadata["url"]
                    imageUrl != null && !urlsToDelete.contains(imageUrl)
                }
                val finalImageMetadataList = newImageMetadataList + cleanedCurrentImages

                // Prepare updated fields for the event
                val updatedEvent = mutableMapOf<String, Any>()
                eventName?.let { updatedEvent["name"] = it }
                description?.let { updatedEvent["description"] = it }
                location?.let { updatedEvent["location"] = it }
                numberOfDays?.let { updatedEvent["numberOfDays"] = it }
                startDate?.let { updatedEvent["startDate"] = it.toString() }
                startTime?.let { updatedEvent["startTime"] = it.toString() }
                endDate?.let { updatedEvent["endDate"] = it.toString() }
                endTime?.let { updatedEvent["endTime"] = it.toString() }
                pickUpList?.let { updatedEvent["pickUp"] = it }
                dropOffList?.let { updatedEvent["dropOff"] = it }
                updatedEvent["images"] = finalImageMetadataList
                price?.let { updatedEvent["price"] = it }
                paymentDetails?.let { updatedEvent["paymentDetails"] = it }

                // Update the event document in the transaction
                transaction.update(eventRef, updatedEvent)
            }.await()

            // Step 3: Delete images from Firebase Storage after the transaction
            imagesToDelete?.forEach { uri ->
                val imageUrl = uri.toString()
                firebaseService.deleteImageFromStorage(imageUrl)
            }

            Log.d(Tag, "Event updated successfully: $eventId")
            Toast.makeText(context, "Event updated successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(Tag, "Error updating event: $eventId", e)
            Toast.makeText(context, "Error updating event", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * READ
     */
    fun observeEventsByCommunityOrSpace(
        communityId: String?,
        spaceId: String?
    ) : Flow<List<Event>> {
        return firebaseService.observeEventsByCommunityOrSpace(
            communityId = communityId,
            spaceId = spaceId
        )
    }
    // Function to get ratings for a specific event
    suspend fun getEventRatings(eventId: String): Flow<List<RatingWithDetails>> = flow {
        try {
            Log.d("getEventRating", "Fetching event: $eventId")

            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Fetch the event document from Firestore
            val eventSnapshot = eventRef.get().await()

            if (eventSnapshot.exists()) {
                Log.d("getEventRating", "Event document exists")

                // Convert the event document to the Event data model
                val event = eventSnapshot.toObject(Event::class.java)

                // Extract comments
                val ratings = event?.ratings ?: emptyList()
                Log.d("getEventRating", "Number of ratings: ${ratings.size}")

                // Fetch user details for each commenter
                val userRatings = ratings.map { rating ->
                    Log.d("getEventRating", "Fetching user for ratingId: ${rating.ratingId}, userId: ${rating.userId}")

                    val userRef = db.collection("users").document(rating.userId)

                    // Fetch user document
                    val userSnapshot = userRef.get().await()
                    val user = userSnapshot.toObject(UserData::class.java)

                    Log.d("getEventRating", "User found: ${user?.username ?: "Unknown"}")

                    RatingWithDetails(
                        ratingId = rating.ratingId,
                        userId = rating.userId,
                        username = user?.username ?: "Unknown",
                        rating = rating.rating,
                        remark = rating.remark,
                        profileUri = user?.profilePictureUrl
                    )
                }

                // Emit the list of comments with details
                emit(userRatings)
            } else {
                Log.w("getEventComments", "Event document does not exist")
                // If the event document doesn't exist, emit an empty list
                emit(emptyList())
            }
        } catch (e: Exception) {
            // Handle exceptions (e.g., log error, rethrow, etc.)
            Log.e(Tag, "Error fetching event refunds", e)
            emit(emptyList())
        }
    }

    suspend fun getEventById(eventId: String): Event? {
        return try {
            val documentSnapshot = db.collection("events")
                .document(eventId)
                .get(Source.DEFAULT)
                .await()

            // Convert the document snapshot to a Community object
            documentSnapshot.toObject(Event::class.java)
        } catch (e: Exception) {
            Log.e(Tag, "Error fetching event by ID", e)
            null
        }
    }

    fun getEventAttendeesFlow(
        eventId: String,
        pageSize: Int = 15 // Number of documents to fetch per page
    ): Flow<List<UserData>> = flow {
        try {
            val eventRef = db.collection("events").document(eventId)
            val snapshot = eventRef.get().await()

            // Retrieve the list of member IDs from the community document
            val attendeesId = snapshot.get("attendees") as? List<Map<String, EventAttendee>> ?: emptyList()

            Log.d(Tag, "Attendees ids are $attendeesId")
            if (attendeesId.isNotEmpty()) {
                // Extract user IDs from member entries
                val userIds = attendeesId.mapNotNull { it.keys.firstOrNull() }

                Log.d(Tag, "User Ids are: $userIds")

                // Check if userIds is not empty before querying
                if (userIds.isNotEmpty()) {
                    // Query the users collection with pagination
                    val userCollectionRef = db.collection("users")

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


    fun getEventUnattendeesFlow(
        eventId: String,
        pageSize: Int = 15 // Number of documents to fetch per page
    ): Flow<List<UserData>> = flow {
        try {
            val eventRef = db.collection("events").document(eventId)
            val snapshot = eventRef.get().await()

            // Retrieve the list of member IDs from the community document
            val unAttendeesId = snapshot.get("unattendees") as? List<Map<String, String>> ?: emptyList()

            Log.d(Tag, "Unattendees ids are $unAttendeesId")
            if (unAttendeesId.isNotEmpty()) {
                // Extract user IDs from member entries
                val userIds = unAttendeesId.mapNotNull { it.keys.firstOrNull() }

                Log.d(Tag, "User Ids are: $userIds")

                // Check if userIds is not empty before querying
                if (userIds.isNotEmpty()) {
                    // Query the users collection with pagination
                    val userCollectionRef = db.collection("users")

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


    suspend fun getEventComments(
        eventId: String
    ): Flow<List<CommentWithDetails>> = flow {
        try {
            Log.d("getEventComments", "Fetching event: $eventId")

            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Fetch the event document from Firestore
            val eventSnapshot = eventRef.get().await()

            if (eventSnapshot.exists()) {
                Log.d("getEventComments", "Event document exists")

                // Convert the event document to the Event data model
                val event = eventSnapshot.toObject(Event::class.java)

                // Extract comments
                val comments = event?.comments ?: emptyList()
                Log.d("getEventComments", "Number of comments: ${comments.size}")

                // Fetch user details for each commenter
                val userComments = comments.map { comment ->
                    Log.d("getEventComments", "Fetching user for commentId: ${comment.commentId}, userId: ${comment.userId}")

                    val userRef = db.collection("users").document(comment.userId)

                    // Fetch user document
                    val userSnapshot = userRef.get().await()
                    val user = userSnapshot.toObject(UserData::class.java)

                    Log.d("getEventComments", "User found: ${user?.username ?: "Unknown"}")

                    CommentWithDetails(
                        commentId = comment.commentId,
                        userId = comment.userId,
                        infractions = comment.infractions,
                        username = user?.username ?: "Unknown",
                        isEditable = comment.isEditable,
                        profileUri = user?.profilePictureUrl,
                        text = comment.text,
                        timestamp = comment.timestamp,
                        replies = comment.replies
                    )
                }

                // Emit the list of comments with details
                emit(userComments)
            } else {
                Log.w("getEventComments", "Event document does not exist")
                // If the event document doesn't exist, emit an empty list
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("getEventComments", "Error fetching comments: ${e.message}")
            // Emit an empty list in case of an error
            emit(emptyList())
        }
    }


    suspend fun getAllReplies(
        eventId: String
    ): Flow<List<ReplyWithDetails>> = flow {
        try {
            Log.d("getAllReplies", "Fetching event: $eventId")

            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Fetch the event document from Firestore
            val eventSnapshot = eventRef.get().await()

            if (eventSnapshot.exists()) {
                Log.d("getAllReplies", "Event document exists")

                // Convert the event document to the Event data model
                val event = eventSnapshot.toObject(Event::class.java)

                // Fetch all replies from the event's comments
                val allReplies = event?.comments?.flatMap { comment ->
                    Log.d("getAllReplies", "Fetching replies for commentId: ${comment.commentId}")

                    comment.replies.map { reply ->
                        Log.d("getAllReplies", "Fetching user for replyId: ${reply.replyId}, userId: ${reply.userId}")

                        val userRef = db.collection("users").document(reply.userId)

                        // Fetch user document
                        val userSnapshot = userRef.get().await()
                        val user = userSnapshot.toObject(UserData::class.java)

                        Log.d("getAllReplies", "User found: ${user?.username ?: "Unknown"}")

                        ReplyWithDetails(
                            commentId = comment.commentId, // Include the commentId from the original comment
                            replyId = reply.replyId,
                            infractions = reply.infractions,
                            userId = reply.userId,
                            isEditable = reply.isEditable,
                            username = user?.username ?: "Unknown",
                            profileUri = user?.profilePictureUrl,
                            text = reply.text,
                            timestamp = reply.timestamp
                        )
                    }
                } ?: emptyList()

                // Emit the list of replies with details
                emit(allReplies)
            } else {
                Log.w("getAllReplies", "Event document does not exist")
                // If the event document doesn't exist, emit an empty list
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("getAllReplies", "Error fetching replies: ${e.message}")
            // Emit an empty list in case of an error
            emit(emptyList())
        }
    }

    fun getEventRefundsFlow(
        eventId: String,
        pageSize: Int = 15 // Number of documents to fetch per page
    ): Flow<List<UserData>> = flow {
        try {
            val eventRef = db.collection("events").document(eventId)
            val snapshot = eventRef.get().await()

            // Retrieve the list of member IDs from the community document
            val refundId = snapshot.get("refundRequests") as? List<Map<String, Boolean>> ?: emptyList()

            Log.d(Tag, "Refunds ids are $refundId")
            if (refundId.isNotEmpty()) {
                // Extract user IDs from member entries
                val userIds = refundId.mapNotNull { it.keys.firstOrNull() }

                Log.d(Tag, "User Ids are: $userIds")

                // Check if userIds is not empty before querying
                if (userIds.isNotEmpty()) {
                    // Query the users collection with pagination
                    val userCollectionRef = db.collection("users")

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
            Log.e(Tag, "Error fetching event refunds", e)
            emit(emptyList()) // Emit an empty list in case of an error
        }
    }.flowOn(Dispatchers.IO) // Ensure this runs on a background thread

    /**
     * UPDATE
     */
    suspend fun addRatingToEvent(
        eventId: String,
        userId: String,
        rating: Int,
        remark: String
    ): Boolean {
        val ratingId = db.collection("events").document().id

        return try {
            db.runTransaction { transaction ->
                val eventRef = db.collection("events").document(eventId)
                val snapshot = transaction.get(eventRef)

                // Retrieve the current ratings from the event
                val event = snapshot.toObject(Event::class.java)
                val currentRatings = event?.ratings?.toMutableList() ?: mutableListOf()

                // Create a new rating object
                val newRating = Rating(
                    ratingId = ratingId,
                    userId = userId,
                    rating = rating,
                    remark = remark
                )

                // Add the new rating to the list
                currentRatings.add(newRating)

                // Update the ratings in the document
                transaction.update(eventRef, "ratings", currentRatings)
            }.await()

            // Log success and return true
            Log.d("AddRating", "Rating added successfully: $ratingId")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Log error and return false
            Log.e("AddRating", "Failed to add rating: $ratingId", e)
            false
        }
    }


    suspend fun attendEvent(
        eventId: String,
        userId: String
    ) {
        val eventRef = db.collection("events").document(eventId)
        val userRef = db.collection("users").document(userId)

        try {
            db.runTransaction { transaction ->
                // Step 1: Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Step 2: Fetch the user's document
                val userSnapshot = transaction.get(userRef)
                if (!userSnapshot.exists()) {
                    throw Exception("User does not exist")
                }

                // Fetch the user object
                val currentUser = userSnapshot.toObject(UserData::class.java) ?: throw Exception("Failed to parse user data")

                // Step 3: Perform necessary checks after all reads
                // Check if user is already an attendee
                val isAlreadyAttendee = event.attendees?.any { attendeeMap ->
                    attendeeMap.containsKey(userId)
                } ?: false

                // Check if the event is already in the user's event list
                val isEventInUserList = currentUser.events.contains(eventId)

                // Step 4: Prepare the write operations after all reads
                if (!isAlreadyAttendee) {
                    // Add the user to the event's attendees list
                    val updatedAttendees = event.attendees?.toMutableList() ?: mutableListOf()

                    // Check if the event is free or paid
                    val newAttendee = if (event.price != null && event.price!! > 0) {
                        mapOf(userId to EventAttendee(approved = false, arrived = false))
                    } else {
                        mapOf(userId to EventAttendee(approved = true, arrived = false))
                    }

                    updatedAttendees.add(newAttendee)

                    // Update the event's attendees list in the transaction
                    transaction.update(eventRef, "attendees", updatedAttendees)
                }

                if (!isEventInUserList) {
                    // Add the event to the user's event list
                    val updatedEvents = currentUser.events.toMutableList()
                    updatedEvents.add(eventId)

                    // Update the user's event list in the transaction
                    transaction.update(userRef, "events", updatedEvents)
                }
            }.await()  // Suspend until transaction completes

            // Transaction completed successfully
            Log.d(Tag, "User attendance updated successfully")
            Toast.makeText(context, "Welcome to the event!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Handle any exceptions, such as Firestore failures
            e.printStackTrace()
            Log.d(Tag, "Failed to update event attendance")
            Toast.makeText(context, "Failed to update event attendance", Toast.LENGTH_SHORT).show()
        }
    }


    suspend fun leaveEvent(
        eventId: String,
        userId: String,
        reasonToLeave: String,
        refundRequested: Boolean
    ) {
        val eventRef = db.collection("events").document(eventId)
        val userRef = db.collection("users").document(userId)

        try {
            db.runTransaction { transaction ->
                // Step 1: Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Step 2: Fetch the user's document
                val userSnapshot = transaction.get(userRef)
                if (!userSnapshot.exists()) {
                    throw Exception("User does not exist")
                }

                // Fetch the user object
                val currentUser = userSnapshot.toObject(UserData::class.java) ?: throw Exception("Failed to parse user data")

                // Step 3: Check if the user is already in the event's attendees list
                val isAttendee = event.attendees?.any { attendeeMap ->
                    attendeeMap.containsKey(userId)
                } ?: false

                if (isAttendee) {
                    // Remove the user from the event's attendees list
                    val updatedAttendees = event.attendees?.filterNot { it.containsKey(userId) }?.toMutableList() ?: mutableListOf()

                    // Update the event's attendees list in the transaction
                    transaction.update(eventRef, "attendees", updatedAttendees)

                    // Step 4: Check if the user is already in the unattendees list
                    val alreadyUnattendee = event.unattendees?.any { unattendeeMap ->
                        unattendeeMap.containsKey(userId)
                    } ?: false

                    if (!alreadyUnattendee) {
                        // Add the user to the event's unattendees list with the reason
                        val updatedUnattendees = event.unattendees?.toMutableList() ?: mutableListOf()
                        updatedUnattendees.add(mapOf(userId to if (reasonToLeave.isNotEmpty()) reasonToLeave else "No reason provided"))

                        // Update the event's unattendees list in the transaction
                        transaction.update(eventRef, "unattendees", updatedUnattendees)
                    }

                    // Handle refund request if requested
                    if (refundRequested) {
                        val updatedRefundRequests = event.refundRequests?.toMutableList() ?: mutableListOf()

                        // Check if the user is already in the refund requests
                        val isRefundRequested = updatedRefundRequests.any { refundRequestMap ->
                            refundRequestMap.containsKey(userId)
                        }

                        // Only add the refund request if it doesn't already exist
                        if (!isRefundRequested) {
                            updatedRefundRequests.add(mapOf(userId to true))

                            // Update the refundRequests list in the transaction
                            transaction.update(eventRef, "refundRequests", updatedRefundRequests)
                        }
                    }

                } else {
                    throw Exception("User is not an attendee of this event")
                }

                // Step 5: Check if the event is in the user's event list and remove it
                if (currentUser.events.contains(eventId)) {
                    val updatedEvents = currentUser.events.toMutableList()
                    updatedEvents.remove(eventId)

                    // Update the user's event list in the transaction
                    transaction.update(userRef, "events", updatedEvents)
                } else {
                    throw Exception("Event not found in user's event list")
                }
            }.await()  // Suspend until transaction completes

            // Transaction completed successfully
            Log.d(Tag, "User successfully left the event")
            Toast.makeText(context, "You have left the event", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Handle any exceptions, such as Firestore failures
            e.printStackTrace()
            Log.d(Tag, "Failed to leave event")
            Toast.makeText(context, "Failed to leave event", Toast.LENGTH_SHORT).show()
        }
    }


    suspend fun toggleAttendanceApproval(
        eventId: String,
        userId: String
    ) {
        val eventRef = FirebaseFirestore.getInstance().collection("events").document(eventId)

        try {
            db.runTransaction { transaction ->
                // Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Update the attendee's approval status
                val updatedAttendees = event.attendees?.map { attendeeMap ->
                    attendeeMap.mapValues { (attendeeUserId, attendee) ->
                        if (attendeeUserId == userId) {
                            attendee.copy(approved = !attendee.approved) // Toggle approval
                        } else {
                            attendee
                        }
                    }
                }

                // Update the event document in the transaction
                transaction.update(eventRef, "attendees", updatedAttendees)
            }.await() // Suspend until transaction completes

            // Transaction completed successfully
            Log.d(Tag, "Toggled attendance approval successfully")
            Toast.makeText(context, "Toggled attendance approval successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Handle exceptions such as Firestore failures
            e.printStackTrace()
            Log.e(Tag, "Error toggling attendance approval")
            Toast.makeText(context, "Error toggling attendance approval", Toast.LENGTH_SHORT).show()
        }
    }


    suspend fun toggleAttendanceArrival(
        eventId: String,
        userId: String
    ) {
        val eventRef = FirebaseFirestore.getInstance().collection("events").document(eventId)

        try {
            db.runTransaction { transaction ->
                // Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Update the attendee's arrival status
                val updatedAttendees = event.attendees?.map { attendeeMap ->
                    attendeeMap.mapValues { (attendeeUserId, attendee) ->
                        if (attendeeUserId == userId) {
                            attendee.copy(arrived = !attendee.arrived) // Toggle arrival
                        } else {
                            attendee
                        }
                    }
                }

                // Update the event document in the transaction
                transaction.update(eventRef, "attendees", updatedAttendees)
            }.await() // Suspend until transaction completes

            // Transaction completed successfully
            Log.d(Tag, "Toggled attendance arrival successfully")
            Toast.makeText(context, "Toggled attendance arrival successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Handle exceptions such as Firestore failures
            e.printStackTrace()
            Log.e(Tag, "Error toggling attendance arrival")
            Toast.makeText(context, "Error toggling attendance arrival", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun toggleMute(
        eventId: String,
        userId: String
    ) {
        val eventRef = FirebaseFirestore.getInstance().collection("events").document(eventId)

        try {
            db.runTransaction { transaction ->
                // Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Update the attendee's arrival status
                val updatedAttendees = event.attendees?.map { attendeeMap ->
                    attendeeMap.mapValues { (attendeeUserId, attendee) ->
                        if (attendeeUserId == userId) {
                            attendee.copy(muted = !attendee.muted) // Toggle arrival
                        } else {
                            attendee
                        }
                    }
                }

                // Update the event document in the transaction
                transaction.update(eventRef, "attendees", updatedAttendees)
            }.await() // Suspend until transaction completes

            // Transaction completed successfully
            Log.d(Tag, "Toggled mute successfully")
            Toast.makeText(context, "Toggled attendance arrival successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Handle exceptions such as Firestore failures
            e.printStackTrace()
            Log.e(Tag, "Error toggling mute")
            Toast.makeText(context, "Error toggling attendance arrival", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun addUserToPickup(eventId: String, userId: String, pickupIndex: Int) {
        val eventRef = db.collection("events").document(eventId)

        try {
            db.runTransaction { transaction ->
                // Step 1: Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Step 2: Check if the pickup exists and if the user is already in the pickup list
                val updatedPickupList = event.pickUp?.toMutableList() ?: throw Exception("Pickups not defined for this event")

                // Check the selected pickup
                val selectedPickup = updatedPickupList.getOrNull(pickupIndex) ?: throw Exception("Invalid pickup index")
                if (selectedPickup.attendees.contains(userId)) {
                    throw Exception("User is already in the pickup list")
                }

                // Step 3: Add the user to the pickup's users list
                val updatedAttendees = selectedPickup.attendees.toMutableList()
                updatedAttendees.add(userId)

                // Step 4: Update the selected pickup's user list
                updatedPickupList[pickupIndex] = selectedPickup.copy(attendees = updatedAttendees)

                // Step 5: Commit the updated pickup list
                transaction.update(eventRef, "pickUp", updatedPickupList)
            }.await()

            Log.d(Tag, "User added to pickup successfully")
            Toast.makeText(context, "User added to pickup successfully", Toast.LENGTH_SHORT).show()


        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(Tag, "Failed to add user to pickup")
        }
    }

    suspend fun addUserToDropOff(eventId: String, userId: String, dropoffIndex: Int) {
        val eventRef = db.collection("events").document(eventId)

        try {
            db.runTransaction { transaction ->
                // Step 1: Fetch the event document
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event does not exist")
                }

                // Fetch the event object
                val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")

                // Step 2: Check if the dropoff exists and if the user is already in the dropoff list
                val updatedDropoffList = event.dropOff?.toMutableList() ?: throw Exception("Dropoffs not defined for this event")

                // Check the selected dropoff
                val selectedDropoff = updatedDropoffList.getOrNull(dropoffIndex) ?: throw Exception("Invalid dropoff index")
                if (selectedDropoff.attendees.contains(userId)) {
                    throw Exception("User is already in the dropoff list")
                }

                // Step 3: Add the user to the dropoff's users list
                val updatedAttendees = selectedDropoff.attendees.toMutableList()
                updatedAttendees.add(userId)

                // Step 4: Update the selected dropoff's user list
                updatedDropoffList[dropoffIndex] = selectedDropoff.copy(attendees = updatedAttendees)

                // Step 5: Commit the updated dropoff list
                transaction.update(eventRef, "dropOff", updatedDropoffList)
            }.await()
            Toast.makeText(context, "User added to dropoff successfully", Toast.LENGTH_SHORT).show()
            Log.d(Tag, "User added to dropoff successfully")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(Tag, "Failed to add user to dropoff")
        }
    }

    suspend fun removeUserFromPickup(
        eventId: String,
        userId: String,
        pickupIndex: Int,
    ) {
        try {
            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Transaction to remove the user from the specific pickup list
            db.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)
                val event = eventSnapshot.toObject(Event::class.java)

                if (event?.pickUp != null) {
                    val pickupList = event.pickUp!![pickupIndex].attendees.toMutableList()

                    // Remove the user if they exist in the list
                    if (pickupList.contains(userId)) {
                        pickupList.remove(userId)
                        event.pickUp!![pickupIndex].attendees = pickupList

                        // Update the pickup attendees list
                        transaction.update(eventRef, "pickUp", event.pickUp)
                    }
                }
            }.await()
            Toast.makeText(context, "User removed from pickup successfully", Toast.LENGTH_SHORT).show()
            Log.d(Tag, "User removed from pickup successfully")
        } catch (e: Exception) {
            // Handle failure
            Log.e("Pickup", "Error removing user from pickup", e)
        }
    }

    suspend fun removeUserFromDropOff(
        eventId: String,
        userId: String,
        dropOffIndex: Int,
    ) {
        try {
            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Transaction to remove the user from the specific dropoff list
            db.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)
                val event = eventSnapshot.toObject(Event::class.java)

                if (event?.dropOff != null) {
                    val dropOffList = event.dropOff!![dropOffIndex].attendees.toMutableList()

                    // Remove the user if they exist in the list
                    if (dropOffList.contains(userId)) {
                        dropOffList.remove(userId)
                        event.dropOff!![dropOffIndex].attendees = dropOffList

                        // Update the dropoff attendees list
                        transaction.update(eventRef, "dropOff", event.dropOff)
                    }
                }
            }.await()
            Toast.makeText(context, "User removed from dropoff successfully", Toast.LENGTH_SHORT).show()
            Log.d(Tag, "User removed from dropoff successfully")
        } catch (e: Exception) {
            // Handle failure
            Log.e("DropOff", "Error removing user from dropoff", e)
        }
    }

    suspend fun addCommentToEvent(
        eventId: String,
        userId: String,
        text: String
    ) {
        try {
            val uniqueCommentId = db.collection("events").document().id
            val newComment = Comment(
                commentId = uniqueCommentId,
                userId = userId,
                text = text,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // Use LocalDateTime
            )

            val eventRef = db.collection("events").document(eventId)

            db.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)

                if (eventSnapshot.exists()) {
                    val existingComments = eventSnapshot.toObject(Event::class.java)?.comments ?: emptyList()
                    val updatedComments = existingComments.toMutableList().apply { add(newComment) }
                    transaction.update(eventRef, "comments", updatedComments)
                    Log.d(Tag, "Comment added successfully")
                } else {
                    throw Exception("Event not found")
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addCommentReply(
        eventId: String,
        commentId: String,
        userId: String,
        text: String
    ) {
        val eventRef = db.collection("events").document(eventId)
        val replyId = UUID.randomUUID().toString()

        val reply = Reply(
            commentId = commentId,
            replyId = replyId,
            userId = userId,
            text = text,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // Use LocalDateTime
        )

        try {
            db.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)

                if (eventSnapshot.exists()) {
                    val event = eventSnapshot.toObject(Event::class.java) ?: throw Exception("Failed to parse event data")
                    val updatedComments = event.comments?.map { comment ->
                        if (comment.commentId == commentId) {
                            comment.copy(replies = comment.replies + reply)
                        } else {
                            comment
                        }
                    } ?: listOf(reply)

                    transaction.update(eventRef, "comments", updatedComments)
                } else {
                    throw Exception("Event not found")
                }
            }.await()

            Log.d(Tag, "Reply added successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(Tag, "Failed to add reply")
        }
    }

    suspend fun editComment(
        eventId: String,
        commentId: String,
        newText: String
    ){
        try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Find the comment to edit and update its text
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        comment.copy(text = newText) // Update the comment text
                    } else {
                        comment
                    }
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("EditComment", "Comment edited successfully: $commentId")
            Toast.makeText(context, "Comment edited successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("EditComment", "Failed to edit comment: $commentId", e)
            Toast.makeText(context, "Failed to edit comment", Toast.LENGTH_SHORT).show()

        }
    }

    suspend fun editReply(
        eventId: String,
        commentId: String,
        replyId: String,
        newText: String
    ){
        try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Find the comment that contains the reply and update the reply's text
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        val updatedReplies = comment.replies.map { reply ->
                            if (reply.replyId == replyId) {
                                reply.copy(text = newText) // Update the reply text
                            } else {
                                reply
                            }
                        }
                        comment.copy(replies = updatedReplies)
                    } else {
                        comment
                    }
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("EditReply", "Reply edited successfully: $replyId")
            Toast.makeText(context, "Reply edited successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("EditReply", "Failed to edit reply: $replyId", e)
            Toast.makeText(context, "Failed to edit reply", Toast.LENGTH_SHORT).show()

        }
    }

    private suspend fun addInfraction(
        eventId: String,
        commentId: String,
        infraction: Pair<String, String>,
        isReply: Boolean = false,
        replyId: String = ""
    ) {
        try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Update the infractions in either comment or reply
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        if (isReply) {
                            val updatedReplies = comment.replies.map { reply ->
                                if (reply.replyId == replyId) {
                                    reply.copy(infractions = reply.infractions + mapOf(infraction))
                                } else {
                                    reply
                                }
                            }
                            comment.copy(replies = updatedReplies)
                        } else {
                            comment.copy(infractions = comment.infractions + mapOf(infraction))
                        }
                    } else {
                        comment
                    }
                }

                // Update the document
                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            Log.d("AddInfraction", "Infraction added successfully")
            Toast.makeText(context, "Infraction added successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AddInfraction", "Failed to add infraction", e)
            Toast.makeText(context, "Failed to add infraction", Toast.LENGTH_SHORT).show()
        }
    }

    // Add infraction to comment
    suspend fun addInfractionToComment(
        eventId: String,
        commentId: String,
        reporterId: String,
        reason: String
    ) {
        addInfraction(eventId, commentId, reporterId to reason, isReply = false)
    }

    // Add infraction to reply
    suspend fun addInfractionToReply(
        eventId: String,
        commentId: String,
        replyId: String,
        reporterId: String,
        reason: String
    ) {
        addInfraction(eventId, commentId, reporterId to reason, isReply = true, replyId = replyId)
    }


    /**
     *DELETE
     */
    suspend fun deleteComment(
        eventId: String,
        commentId: String
    ): Boolean {
        return try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Filter out the comment to delete
                val updatedComments = comments.filterNot { comment ->
                    comment.commentId == commentId
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("DeleteComment", "Comment deleted successfully: $commentId")
            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()

            true // Successfully deleted the comment
        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("DeleteComment", "Failed to delete comment: $commentId", e)
            Toast.makeText(context, "Failed to delete comment", Toast.LENGTH_SHORT).show()

            false // Failed to delete the comment
        }
    }

    suspend fun deleteReply(
        eventId: String,
        commentId: String,
        replyId: String
        ): Boolean {
        return try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Find the comment that contains the reply to delete
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        val updatedReplies = comment.replies.filterNot { reply ->
                            reply.replyId == replyId
                        }
                        comment.copy(replies = updatedReplies)
                    } else {
                        comment
                    }
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("DeleteReply", "Reply deleted successfully: $replyId")
            Toast.makeText(context, "Reply deleted", Toast.LENGTH_SHORT).show()

            true // Successfully deleted the reply
        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("DeleteReply", "Failed to delete reply: $replyId", e)
            Toast.makeText(context, "Failed to delete reply", Toast.LENGTH_SHORT).show()

            false // Failed to delete the reply
        }
    }

    suspend fun clearCommentAndDeleteReplies(
        eventId: String,
        commentId: String
    ) {
        try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Find the comment and replace its content, remove its replies
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        // Update the comment text and remove replies
                        comment.copy(
                            text = "Comment deleted by organizer",
                            replies = emptyList(),
                            isEditable = false
                        )
                    } else {
                        comment
                    }
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("ClearComment", "Comment cleared and replies deleted successfully: $commentId")
            Toast.makeText(context, "Comment cleared and replies deleted", Toast.LENGTH_SHORT).show()


        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("ClearComment", "Failed to clear comment: $commentId", e)
            Toast.makeText(context, "Failed to clear comment", Toast.LENGTH_SHORT).show()

        }
    }

    suspend fun clearReplyContent(
        eventId: String,
        commentId: String,
        replyId: String
    ) {
        try {
            val eventRef = db.collection("events").document(eventId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val comments = snapshot.toObject(Event::class.java)?.comments ?: emptyList()

                // Find the comment that contains the reply to be cleared
                val updatedComments = comments.map { comment ->
                    if (comment.commentId == commentId) {
                        val updatedReplies = comment.replies.map { reply ->
                            if (reply.replyId == replyId) {
                                // Update the reply text
                                reply.copy(
                                    text = "Reply deleted by organizer",
                                    isEditable = false
                                )
                            } else {
                                reply
                            }
                        }
                        comment.copy(replies = updatedReplies)
                    } else {
                        comment
                    }
                }

                transaction.update(eventRef, "comments", updatedComments)
            }.await()

            // Log success and show a toast
            Log.d("ClearReply", "Reply cleared successfully: $replyId")
            Toast.makeText(context, "Reply cleared successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()

            // Log error and show a toast
            Log.e("ClearReply", "Failed to clear reply: $replyId", e)
            Toast.makeText(context, "Failed to clear reply", Toast.LENGTH_SHORT).show()

        }
    }

    suspend fun deleteEvent(
        event: Event,
    ) {
        val eventId = event.id
        val communityId = event.communityId
        val spaceId = event.spaceId
        val imageUris = event.images
        val attendees = event.attendees  // List<Map<String, EventAttendee>>

        try {
            var batch = db.batch()  // Initialize a Firestore batch
            var operationCount = 0  // Counter for operations in a batch

            // Reference to the event document
            val eventRef = db.collection("events").document(eventId)

            // Update the community document if needed
            communityId?.let {
                val communityRef = db.collection("communities").document(it)
                val communityDoc = communityRef.get().await()  // Get the document
                val currentEvents = communityDoc.get("events") as? List<String> ?: emptyList()
                val updatedEvents = currentEvents.toMutableList().apply { remove(eventId) }

                batch.update(communityRef, "events", updatedEvents)
                operationCount++

                if (operationCount == 500) {
                    batch.commit().await()  // Commit the batch when it reaches 500 operations
                    batch = db.batch()  // Start a new batch
                    operationCount = 0  // Reset operation count
                }
            }

            // Update the space document if needed
            spaceId?.let {
                val spaceRef = db.collection("spaces").document(it)
                val spaceDoc = spaceRef.get().await()  // Get the document
                val currentEvents = spaceDoc.get("events") as? List<String> ?: emptyList()
                val updatedEvents = currentEvents.toMutableList().apply { remove(eventId) }

                batch.update(spaceRef, "events", updatedEvents)
                operationCount++

                if (operationCount == 500) {
                    batch.commit().await()  // Commit the batch
                    batch = db.batch()  // Start a new batch
                    operationCount = 0  // Reset operation count
                }
            }

            // Remove the event from the attendees' user documents
            attendees?.forEach { attendeeMap ->
                val userId = attendeeMap.keys.first()  // Extract the user ID from the map
                val userRef = db.collection("users").document(userId)
                batch.update(userRef, "events", FieldValue.arrayRemove(eventId))
                operationCount++

                if (operationCount == 500) {
                    batch.commit().await()  // Commit the batch when it reaches 500 operations
                    batch = db.batch()  // Start a new batch
                    operationCount = 0  // Reset operation count
                }
            }

            // Delete the event document
            batch.delete(eventRef)
            operationCount++

            if (operationCount == 500) {
                batch.commit().await()  // Commit the batch
                batch = db.batch()  // Start a new batch
                operationCount = 0  // Reset operation count
            }

            // Commit the final batch
            if (operationCount > 0) {
                batch.commit().await()
            }

            // Perform Firebase Storage operations after the Firestore batch operations are completed
            // Delete images from Firebase Storage
            imageUris.forEach { imageMap ->
                val imageUrl = imageMap["url"]  // Extract the image URL from the map
                imageUrl?.let {
                    firebaseService.deleteImageFromStorage(it)
                }
            }

            Log.d(Tag, "Event deleted successfully: $eventId")
            Toast.makeText(context, "Event deleted successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(Tag, "Error deleting event: $eventId", e)
            Toast.makeText(context, "Error deleting event", Toast.LENGTH_SHORT).show()
        }
    }




}