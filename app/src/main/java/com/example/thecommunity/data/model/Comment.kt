package com.example.thecommunity.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Comment(
    var commentId: String = "",
    var userId: String = "",
    var text: String = "",
    var isEditable: Boolean = true,
    var infractions: List<Map<String,String>> = emptyList(),
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    var replies: List<Reply> = emptyList()
)

data class CommentWithDetails(
    val commentId: String,
    val userId: String,
    val username: String,
    var infractions: List<Map<String,String>> = emptyList(),
    val profileUri: String?,
    val text: String,
    var isEditable: Boolean = true,
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val replies: List<Reply> = emptyList()
)

data class Reply(
    var commentId: String = "",
    var replyId: String = "",
    var userId: String = "",
    var infractions: List<Map<String,String>> = emptyList(),
    var isEditable: Boolean = true,
    var text: String = "",
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class ReplyWithDetails(
    var commentId: String = "",
    val replyId: String,
    val userId: String,
    val username: String,
    var infractions: List<Map<String,String>> = emptyList(),
    val profileUri: String?,
    var isEditable: Boolean = true,
    val text: String,
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class Rating(
    var ratingId: String = "",
    var userId: String = "",
    var rating: Int = 0,
    var remark: String = "",
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class RatingWithDetails(
    var ratingId: String = "",
    var userId: String = "",
    val profileUri: String?,
    val username: String,
    var rating: Int = 0,
    var remark: String = "",
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)