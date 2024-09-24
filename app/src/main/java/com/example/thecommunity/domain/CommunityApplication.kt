package com.example.thecommunity.domain

import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.thecommunity.data.repositories.ThemeManager

class CommunityApplication : Application() {
    lateinit var themeManager: ThemeManager
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        themeManager = ThemeManager(this)
    }
}
