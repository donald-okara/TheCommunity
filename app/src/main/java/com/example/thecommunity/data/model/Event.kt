package com.example.thecommunity.data.model

import com.google.firebase.firestore.PropertyName
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Event(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("spaceId")
    @set:PropertyName("spaceId")
    var spaceId: String? = "",

    @get:PropertyName("communityId")
    @set:PropertyName("communityId")
    var communityId: String? = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String? = "",

    @get:PropertyName("images")
    @set:PropertyName("images")
    var images: List<Map<String, String?>> = emptyList(),

    @get:PropertyName("location")
    @set:PropertyName("location")
    var location: Location? = null,

    @get:PropertyName("startDate")
    @set:PropertyName("startDate")
    var startDate: String = LocalDate.now().toString(),

    @get:PropertyName("numberOfDays")
    @set:PropertyName("numberOfDays")
    var numberOfDays: Int = 1,

    @get:PropertyName("startTime")
    @set:PropertyName("startTime")
    var startTime: String = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),

    @get:PropertyName("endDate")
    @set:PropertyName("endDate")
    var endDate: String = LocalDate.now().toString(),

    @get:PropertyName("endTime")
    @set:PropertyName("endTime")
    var endTime: String = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),

    @get:PropertyName("attendees")
    @set:PropertyName("attendees")
    var attendees: List<Map<String, EventAttendee>>? = emptyList(),

    @get:PropertyName("unattendees")
    @set:PropertyName("unattendees")
    var unattendees: List<Map<String, String>>? = emptyList(),

    @get:PropertyName("refundRequests")
    @set:PropertyName("refundRequests")
    var refundRequests: List<Map<String, Boolean>>? = emptyList(),


    @get:PropertyName("organizer")
    @set:PropertyName("organizer")
    var organizer: String? = "",

    @get:PropertyName("pickUp")
    @set:PropertyName("pickUp")
    var pickUp: List<Pickup>? = emptyList(),

    @get:PropertyName("dropOff")
    @set:PropertyName("dropOff")
    var dropOff: List<Dropoff>? = emptyList(),

    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Int? = null,

    @get:PropertyName("paymentDetails")
    @set:PropertyName("paymentDetails")
    var paymentDetails: String? = null,

    @get:PropertyName("comments")
    @set:PropertyName("comments")
    var comments: List<Comment>? = emptyList(),

    @get:PropertyName("ratings")
    @set:PropertyName("ratings")
    var ratings: List<Rating>? = emptyList(),
    )


data class EventAttendee(
    val approved : Boolean = false,
    val arrived : Boolean = false,
    val muted : Boolean = false
)