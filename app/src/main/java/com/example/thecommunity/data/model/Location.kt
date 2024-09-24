package com.example.thecommunity.data.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Location(
    val locationName: String = "",
    val coordinates: Coordinates? = null,
    val googleMapsLink: String = ""
)

data class Dropoff(
    val location: Location = Location(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
    var attendees: List<String> = emptyList()
)

data class Pickup(
    val location: Location = Location(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
    var attendees: List<String> = emptyList()
)
