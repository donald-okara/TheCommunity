package com.example.thecommunity.data.model

import com.google.firebase.firestore.PropertyName

data class SignInResult(
    val data : UserData?,
    val errorMessage : String?
)

data class UserData(
    @get:PropertyName("userId")
    var userId: String,

    @get:PropertyName("username")
    @set:PropertyName("username")
    var username: String?,

    @get:PropertyName("alias")
    @set:PropertyName("alias")
    var alias : String?,

    @get:PropertyName("admin")
    @set:PropertyName("admin")
    var admin : Boolean = false,

    @get:PropertyName("campus")
    @set:PropertyName("campus")
    var campus : String? = null,

    @get:PropertyName("yearOfStudy")
    @set:PropertyName("yearOfStudy")
    var yearOfStudy : Int? = null,

    @get:PropertyName("profilePictureUrl")
    @set:PropertyName("profilePictureUrl")
    var profilePictureUrl: String?,

    @get:PropertyName("headerImageUrl")
    @set:PropertyName("headerImageUrl")
    var headerImageUrl: String?,

    @get:PropertyName("dateOfBirth")
    @set:PropertyName("dateOfBirth")
    var dateOfBirth: String?,

    @get:PropertyName("biography")
    @set:PropertyName("biography")
    var biography: String?,

    @get:PropertyName("biographyBackgroundImageUrl")
    @set:PropertyName("biographyBackgroundImageUrl")
    var biographyBackgroundImageUrl: String?,

    @get:PropertyName("communities")
    @set:PropertyName("communities")
    var communities: List<Map<String /*community id*/, String? /*role*/>>,

    @get:PropertyName("spaces")
    @set:PropertyName("spaces")
    var spaces: List<Map<String /*space id*/, UserSpaces? /*role*/ >>,

    @get:PropertyName("events")
    @set:PropertyName("events")
    var events: List<String> = emptyList(),

   ){
    constructor() : this(
        "",
        null,
        null,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        emptyList(),
        emptyList(),
        emptyList()
    )
}

data class UserSpaces(
    val role: String,
    val approvalStatus: String
){
    constructor() : this("", "")

}


