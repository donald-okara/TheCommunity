package com.example.thecommunity.domain

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.repositories.ArticleRepository
import com.example.thecommunity.data.repositories.CommunityRepository
import com.example.thecommunity.data.repositories.EventRepository
import com.example.thecommunity.data.repositories.SpaceRepository
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.artices.ArticleViewModel
import com.example.thecommunity.presentation.communities.CommunityViewModel
import com.example.thecommunity.presentation.events.EventsViewModel
import com.example.thecommunity.presentation.sign_in.SignInViewModel
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope

class AppViewModelFactory(
    private val applicationContext: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseService: FirebaseService,
    private val storage: FirebaseStorage,
    private val coroutineScope: CoroutineScope
) : ViewModelProvider.Factory {

    private val userRepository by lazy {
        UserRepository(
            db = db,
            auth = auth,
            coroutineScope = coroutineScope,
            context = applicationContext
        )
    }

    private val communityRepository by lazy {
        CommunityRepository(
            context = applicationContext,
            db = db,
            userRepository = userRepository,
            firestore = firestore,
            storage = storage,
            coroutineScope = coroutineScope,
            firebaseService = firebaseService

        )
    }

    private val eventRepository by lazy {
        EventRepository(
            db = db,
            firebaseService = firebaseService,
            context = applicationContext,
        )
    }

    private val articleRepository by lazy {
        ArticleRepository(
            db = db,
            firebaseService = firebaseService,
            context = applicationContext,
        )
    }

    private val spaceRepository by lazy {
        SpaceRepository(
            context = applicationContext,
            userRepository = userRepository,
            coroutineScope = coroutineScope,
            storage = storage,
            firebaseService = firebaseService,
            db = db,
            firestore = firestore
        )
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SignInViewModel::class.java) -> {
                SignInViewModel() as T
            }
            modelClass.isAssignableFrom(CommunityViewModel::class.java) -> {
                CommunityViewModel(
                    context = applicationContext,
                    communityRepository = communityRepository,
                    userRepository = userRepository,
                    eventsRepository = eventRepository
                ) as T
            }
            modelClass.isAssignableFrom(SpacesViewModel::class.java) -> {
                SpacesViewModel(
                    context = applicationContext,
                    userRepository = userRepository,
                    spaceRepository = spaceRepository
                ) as T
            }
            modelClass.isAssignableFrom(EventsViewModel::class.java) -> {
                EventsViewModel(
                    eventRepository = eventRepository,
                    userRepository = userRepository,
                ) as T
            }
            modelClass.isAssignableFrom(ArticleViewModel::class.java) -> {
                ArticleViewModel(
                    articleRepository = articleRepository,
                    userRepository = userRepository,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
