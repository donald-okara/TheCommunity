package com.example.thecommunity.data.model

import com.google.firebase.firestore.PropertyName


data class Community(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "",

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "Pending",

    @get:PropertyName("aboutUs")
    @set:PropertyName("aboutUs")
    var aboutUs: String? = null,

    @get:PropertyName("communityBannerUrl")
    @set:PropertyName("communityBannerUrl")
    var communityBannerUrl: String? = null,

    @get:PropertyName("profileUrl")
    @set:PropertyName("profileUrl")
    var profileUrl: String? = null,

    @get:PropertyName("location")
    @set:PropertyName("location")
    var location: Location?,

    @get:PropertyName("bannerThumbnailUrl")
    @set:PropertyName("bannerThumbnailUrl")
    var bannerThumbnailUrl: String? = null,  // New field for banner thumbnail

    @get:PropertyName("profileThumbnailUrl")
    @set:PropertyName("profileThumbnailUrl")
    var profileThumbnailUrl: String? = null,

    @get:PropertyName("members")
    @set:PropertyName("members")
    var members: List<Map<String /*userId*/, String /*role*/>> = emptyList(),

    @get:PropertyName("spaces")
    @set:PropertyName("spaces")
    var spaces: List<Map<String/*space name*/, String/*Approval status*/>> = emptyList(),

    @get:PropertyName("events")
    @set:PropertyName("events")
    var events: List<String> = emptyList(),

    @get:PropertyName("articles")
    @set:PropertyName("articles")
    var articles: List<String> = emptyList()
) {
    constructor() : this(
        "",
        "",
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )
}

