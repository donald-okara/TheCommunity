package com.example.thecommunity.data.model

import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Article(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("topic")
    @set:PropertyName("topic")
    var topic: String = "",

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("spaceId")
    @set:PropertyName("spaceId")
    var spaceId: String? = "",

    @get:PropertyName("communityId")
    @set:PropertyName("communityId")
    var communityId: String? = "",

    @get:PropertyName("body")
    @set:PropertyName("body")
    var body: String? = "",

    @get:PropertyName("draft")
    @set:PropertyName("draft")
    var draft: Boolean? = true,

    @get:PropertyName("images")
    @set:PropertyName("images")
    var images: List<Map<String, String?>> = emptyList(),

    @get:PropertyName("author")
    @set:PropertyName("author")
    var author: String = "",

    @get:PropertyName("readers")
    @set:PropertyName("readers")
    var readers: List<String> = emptyList(),

    @get:PropertyName("upVotes")
    @set:PropertyName("upVotes")
    var upVotes: List<String> = emptyList(),

    @get:PropertyName("downVotes")
    @set:PropertyName("downVotes")
    var downVotes: List<String> = emptyList(),

    @get:PropertyName("comments")
    @set:PropertyName("comments")
    var comments: List<Comment>? = emptyList(),

    @get:PropertyName("edits")
    @set:PropertyName("edits")
    var edits: List<Edit>? = emptyList(),


    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class ArticleDetails(
    var authorProfile: String? = null,
    var authorUserName : String? = null,
    var communityProfileUri : String? = null,
    var spaceProfileUri : String? = null,
    var communityName: String? = null,
    var spaceName : String? = null,
    var article : Article? = null

)

data class Edit(
    var editorId: String = "",
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class EditDetails(
    var editorProfile: String? = null ,
    var editorName : String? = null,
    var timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)