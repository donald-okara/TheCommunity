package com.example.thecommunity.data.model

import com.google.firebase.firestore.PropertyName

data class Space (
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String,

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name : String,

    @get:PropertyName("parentCommunity")
    @set:PropertyName("parentCommunity")
    var parentCommunity : String,

    @get:PropertyName("profileUri")
    @set:PropertyName("profileUri")
    var profileUri : String?,

    @get:PropertyName("bannerUri")
    @set:PropertyName("bannerUri")
    var bannerUri : String?,

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description : String?,

    @get:PropertyName("location")
    @set:PropertyName("location")
    var location: Location? = null,

    @get:PropertyName("approvalStatus")
    @set:PropertyName("approvalStatus")
    var approvalStatus : String,

    @get:PropertyName("membersRequireApproval")
    @set:PropertyName("membersRequireApproval")
    var membersRequireApproval : Boolean,

    @get:PropertyName("members")
    @set:PropertyName("members")
    var members: List<Map<String /*userId*/, SpaceMember /*role*/>>,

    @get:PropertyName("events")
    @set:PropertyName("events")
    var events : List<String> = emptyList(),

    @get:PropertyName("articles")
    @set:PropertyName("articles")
    var articles: List<String> = emptyList()
){
    constructor() : this(
        "",
        "",
        "",
        "",
        null,
        null,
        null,
        "pending",
        false,
        emptyList(),
        emptyList()
    )
}

data class SpaceMember(
    val role: String,
    val approvalStatus: String
){
    constructor() : this("", "")

}