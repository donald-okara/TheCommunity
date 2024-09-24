package com.example.thecommunity

import WelcomeScreen
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.DataStoreRepository
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.domain.AppViewModelFactory
import com.example.thecommunity.domain.CommunityApplication
import com.example.thecommunity.presentation.artices.ArticleViewModel
import com.example.thecommunity.presentation.artices.screens.ArticleEditor
import com.example.thecommunity.presentation.artices.screens.ArticlePreview
import com.example.thecommunity.presentation.communities.CommunityViewModel
import com.example.thecommunity.presentation.communities.screens.CommunitiesScreen
import com.example.thecommunity.presentation.communities.screens.CommunityDetails
import com.example.thecommunity.presentation.communities.screens.CommunityMembers
import com.example.thecommunity.presentation.communities.screens.EditCommunityScreen
import com.example.thecommunity.presentation.communities.screens.RequestCommunityScreen
import com.example.thecommunity.presentation.events.EventsViewModel
import com.example.thecommunity.presentation.events.screen.EventAttendees
import com.example.thecommunity.presentation.events.screen.EventDetails
import com.example.thecommunity.presentation.events.screen.EventForm
import com.example.thecommunity.presentation.events.screen.EventRefunds
import com.example.thecommunity.presentation.events.screen.EventUnattendees
import com.example.thecommunity.presentation.home.HomeScreen
import com.example.thecommunity.presentation.profile.ProfileScreen
import com.example.thecommunity.presentation.sign_in.EmailSignInScreen
import com.example.thecommunity.presentation.sign_in.EmailSignUpScreen
import com.example.thecommunity.presentation.sign_in.GoogleAuthUiClient
import com.example.thecommunity.presentation.sign_in.SignInViewModel
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import com.example.thecommunity.presentation.spaces.screens.ApproveSpacesScreen
import com.example.thecommunity.presentation.spaces.screens.EditSpaceScreen
import com.example.thecommunity.presentation.spaces.screens.RequestSpaceScreen
import com.example.thecommunity.presentation.spaces.screens.SpaceDetails
import com.example.thecommunity.presentation.spaces.screens.SpaceMembers
import com.example.thecommunity.ui.theme.TheCommunityTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val isDarkModeState = mutableStateOf(false) // State for dark mode

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)){ view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = bottom)
            insets
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()

        // Configure Firebase Firestore settings
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(200 * 1024 * 1024) // 200 MB
            .build()

        val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = settings

        val googleAuthUiClient by lazy {
            GoogleAuthUiClient(
                context = applicationContext,
            )
        }
        val coroutineScope = CoroutineScope(Dispatchers.IO) // Define CoroutineScope
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val firebaseService = FirebaseService(db, applicationContext)
        val userRepository = UserRepository(
            db = db,
            auth = auth,
            coroutineScope = coroutineScope,
            context = applicationContext
        )



        val datastoreRepository = DataStoreRepository(applicationContext)

        // Create viewmodel instances
        val viewModelFactory by lazy {
            AppViewModelFactory(
                applicationContext = applicationContext,
                db = FirebaseFirestore.getInstance(),
                auth = FirebaseAuth.getInstance(),
                storage = FirebaseStorage.getInstance(),
                coroutineScope = CoroutineScope(Dispatchers.Main),
                firestore = firestore,
                firebaseService = firebaseService
            )
        }

        val themeManager = (applicationContext as CommunityApplication).themeManager

        // Fetch the initial theme mode
        var isDarkMode = themeManager.getInitialThemeMode()
        isDarkModeState.value = isDarkMode

        val signInViewModel: SignInViewModel by viewModels { viewModelFactory }
        val communityViewModel: CommunityViewModel by viewModels { viewModelFactory }
        val spacesViewModel: SpacesViewModel by viewModels { viewModelFactory }
        val eventsViewModel: EventsViewModel by viewModels { viewModelFactory }
        val articleViewModel: ArticleViewModel by viewModels { viewModelFactory }

        // Determine the start destination
        val startDestination = if (signInViewModel.getSignedInUser() != null) "home" else "welcome"

        setContent {

            TheCommunityTheme(
                darkTheme = isDarkModeState.value
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()
                    var currentUser by remember { mutableStateOf<UserData?>(null) }

                    LaunchedEffect(Unit) {
                        currentUser = userRepository.getCurrentUser()

                    }
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("welcome") {
                            val state by signInViewModel.state.collectAsStateWithLifecycle()

                            // This launcher handles the Google Sign-In result
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult(),
                                onResult = { result ->
                                    Log.d("AuthResult", "Result received: ${result.resultCode}, Intent: ${result.data}")
                                    signInViewModel.setLoading(false)

                                    // Only proceed if the result is successful
                                    if (result.resultCode == RESULT_OK) {
                                        result.data?.let { intent ->
                                            lifecycleScope.launch {
                                                try {
                                                    // Get the result from the GoogleAuthUiClient
                                                    val signInResult = googleAuthUiClient.getSignInWithIntent(intent)
                                                    Log.d("AuthResult", "SignInResult: $signInResult")

                                                    // Update the ViewModel with the sign-in result
                                                    signInViewModel.onSignInResult(signInResult)
                                                } catch (e: Exception) {
                                                    Log.e("AuthResult", "Error processing sign-in result: ${e.message}", e)
                                                }
                                            }
                                        }
                                    } else {
                                        Log.d("AuthResult", "Sign-in result not OK, code: ${result.resultCode}")
                                    }
                                }
                            )


                            // Handle the successful sign-in state and navigate to home
                            LaunchedEffect(state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign-in successful",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Ensure currentUser is set correctly
                                    currentUser = userRepository.getCurrentUser()

                                    // Navigate to the home screen and pop the welcome screen from the backstack
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }

                                    // Reset the state in the ViewModel to avoid repeat navigations
                                    signInViewModel.resetState()
                                }
                            }

                            // UI for WelcomeScreen
                            WelcomeScreen(
                                state = state,
                                onSignInWithGoogle = {
                                    lifecycleScope.launch {
                                        Log.d("MainActivity", "Intent launch")
                                        signInViewModel.setLoading(true) // Start loading state
                                        val signInIntent = googleAuthUiClient.signInIntent() // Get the sign-in intent
                                        launcher.launch(signInIntent) // Launch the sign-in intent
                                    }
                                },
                                onNavigateToSignIn = {
                                    navController.navigate("emailSignIn")
                                },
                                onNavigateToSignUp = {
                                    navController.navigate("emailSignUp")
                                }
                            )
                        }

                        composable("emailSignIn") {
                            val state by signInViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Signin successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    currentUser = userRepository.getCurrentUser()

                                    navController.navigate("home") {
                                        popUpTo("emailSignIn") { inclusive = true }
                                    }
                                    signInViewModel.resetState()
                                }

                            }

                            EmailSignInScreen(
                                state = state,
                                onSignInWithEmail = { email, password ->
                                    signInViewModel.signInWithEmail(email, password)
                                },
                                onNavigateToSignUp = { navController.navigate("emailSignUp") }
                            )
                        }

                        composable("emailSignUp") {
                            val state by signInViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Signin successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    currentUser = userRepository.getCurrentUser()
                                    navController.navigate("home") {
                                        popUpTo("emailSignUp") { inclusive = true }
                                    }
                                    signInViewModel.resetState()
                                }

                            }

                            EmailSignUpScreen(
                                state = state,
                                onSignUpWithEmail = { email, password ->
                                    signInViewModel.signUpWithEmail(email, password)
                                },
                                onNavigateToSignIn = { navController.navigate("emailSignIn") }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                userData = currentUser,
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        currentUser = null

                                        navController.navigate("welcome") {
                                            popUpTo("profile") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                userData = currentUser,
                                onDarkModeToggle = { enabled ->
                                    lifecycleScope.launch {
                                        datastoreRepository.saveDarkMode(enabled)
                                    }
                                    isDarkModeState.value = enabled
                                },
                                onNavigateToProfile = { navController.navigate("profile") },
                                navigateToCommunities = { navController.navigate("community_list") },
                            )
                        }

                        composable("community_list"){
                            CommunitiesScreen(
                                navigateBack = {navController.popBackStack()},
                                navigateToAddCommunity = {navController.navigate("request_community")},
                                communityViewModel = communityViewModel,
                                userRepository = userRepository,
                                navigateToPendingCommunityDetails = { community ->
                                    navController.navigate("pending_community_details/${community.id}")
                                },
                                navigateToCommunityDetails = { community ->
                                    navController.navigate("community_details/${community.id}")
                                }
                            )
                        }

                        composable("request_community"){
                            RequestCommunityScreen(
                                viewModel = communityViewModel,
                                navigateBack = {navController.popBackStack()},
                            )
                        }

                        composable("pending_community_details/{communityId}") { backStackEntry ->
                            val communityId = backStackEntry.arguments?.getString("communityId")

                            if (communityId != null) {
                                CommunityDetails(
                                    communityId = communityId,
                                    userRepository = userRepository,
                                    communityViewModel = communityViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    spacesViewModel = spacesViewModel,
                                    onNavigateToSpace = {space -> navController.navigate("space_details/${space.id}")},
                                    onNavigateToAddSpace = {navController.navigate("request_space/${communityId}")},
                                    onNavigateToApproveSpaces = {navController.navigate("approve_spaces_screen")},
                                    onNavigateToEditCommunity = {navController.navigate("edit_community/${communityId}")},
                                    onNavigateToCommunityMembers = {navController.navigate("community_members/${communityId}")},
                                    onNavigateToCreateEvent = { navController.navigate("create_event/${communityId}") },
                                    eventsViewModel = eventsViewModel,
                                    navigateToEvent = {eventId ->
                                        navController.navigate("event_details/${eventId}")
                                    },
                                    onNavigateToWriteArticle = {
                                        navController.navigate("write_article/${communityId}")
                                    }
                                )
                            }
                        }


                        composable("community_details/{communityId}") { backStackEntry ->
                            val communityId = backStackEntry.arguments?.getString("communityId")


                            if (communityId != null) {
                                CommunityDetails(
                                    communityId = communityId,
                                    userRepository = userRepository,
                                    communityViewModel = communityViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    spacesViewModel = spacesViewModel,
                                    onNavigateToSpace = {
                                        space -> navController.navigate("space_details/${space.id}")
                                                        },
                                    onNavigateToAddSpace = {navController.navigate("request_space/${communityId}")},
                                    onNavigateToApproveSpaces = {navController.navigate("approve_spaces_screen")},
                                    onNavigateToEditCommunity = {navController.navigate("edit_community/${communityId}")},
                                    onNavigateToCommunityMembers = {navController.navigate("community_members/${communityId}")},
                                    onNavigateToCreateEvent = {navController.navigate("create_event/${communityId}")},
                                    eventsViewModel = eventsViewModel,
                                    navigateToEvent = {eventId ->
                                        navController.navigate("event_details/${eventId}")
                                    },
                                    onNavigateToWriteArticle = {
                                        navController.navigate("write_article/${communityId}")
                                    }
                                )
                            }
                        }

                        composable("create_event/{communityId}") { backStackEntry ->
                           val communityId = backStackEntry.arguments?.getString("communityId")

                            EventForm(
                                communityId = communityId,
                                spaceId = null,
                                isEdit = false,
                                eventsViewModel = eventsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("write_article/{communityId}") { backStackEntry ->
                           val communityId = backStackEntry.arguments?.getString("communityId")

                            ArticleEditor(
                                communityId = communityId,
                                spaceId = null,
                                isEdit = false,
                                articleViewModel = articleViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                initialArticleId = null,
                                onNavigateToPreview = {
                                    navController.navigate("article_preview/${it}")
                                }
                            )
                        }

                        composable("article_preview/{articleId}") { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getString("articleId")

                            if (articleId != null) {
                                ArticlePreview(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    articleId = articleId,
                                    articleViewModel = articleViewModel
                                )
                            }
                        }
                        composable("create_event/{spaceId}") { backStackEntry ->
                           val spaceId = backStackEntry.arguments?.getString("spaceId")

                            EventForm(
                                communityId = null,
                                spaceId = spaceId,
                                isEdit = false,
                                eventsViewModel = eventsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("edit_event/{eventId}"){backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId")

                            LaunchedEffect(eventId) {
                                if (eventId != null) {
                                    Log.d("EditEvent", "Event is: $eventId")
                                }
                            }

                            EventForm(
                                communityId = null,
                                spaceId = null,
                                isEdit = true,
                                eventsViewModel = eventsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                initialEventId = eventId
                            )
                        }

                        composable("event_details/{eventId}"){ backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId")

                            LaunchedEffect(eventId) {
                                if (eventId != null) {
                                    Log.d("EventDetails", "Event is: $eventId")
                                }
                            }
                            if (eventId != null) {
                                EventDetails(
                                    eventId = eventId,
                                    eventsViewModel = eventsViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    currentUser = currentUser!!,
                                    onNavigateToEditEvent = {
                                        navController.navigate("edit_event/${eventId}")
                                    },
                                    onNavigateToAttendees = {
                                        navController.navigate("event_attendees/${eventId}")
                                    },
                                    onNavigateToUnattendees = {
                                        navController.navigate("event_unattendees/${eventId}")
                                    },
                                    onNavigateToRefunds = {
                                        navController.navigate("event_refunds/${eventId}")
                                    }
                                )
                            }
                        }

                        composable("event_attendees/{eventId}"){ backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId")

                            LaunchedEffect(eventId) {
                                if (eventId != null) {
                                    Log.d("EventDetails", "Event is: $eventId")
                                }
                            }
                            if (eventId != null) {
                                EventAttendees(
                                    eventId = eventId,
                                    eventsViewModel = eventsViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    userRepository = userRepository
                                )
                            }
                        }

                        composable("event_unattendees/{eventId}"){ backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId")

                            LaunchedEffect(eventId) {
                                if (eventId != null) {
                                    Log.d("EventDetails", "Event is: $eventId")
                                }
                            }
                            if (eventId != null) {
                                EventUnattendees(
                                    eventId = eventId,
                                    eventsViewModel = eventsViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    userRepository = userRepository
                                )
                            }
                        }

                        composable("event_refunds/{eventId}"){ backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId")

                            LaunchedEffect(eventId) {
                                if (eventId != null) {
                                    Log.d("EventDetails", "Event is: $eventId")
                                }
                            }
                            if (eventId != null) {
                                EventRefunds(
                                    eventId = eventId,
                                    eventsViewModel = eventsViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    userRepository = userRepository
                                )
                            }
                        }


                        composable("community_members/{communityId}"){backStackEntry->
                            val communityId = backStackEntry.arguments?.getString("communityId")

                            if (communityId != null) {
                                CommunityMembers(
                                    communityId = communityId,
                                    communityViewModel = communityViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    userRepository = userRepository
                                )
                            }
                        }

                        composable("request_space/{communityId}") { backStackEntry ->
                            val communityId = backStackEntry.arguments?.getString("communityId")
                            if (communityId != null) {
                                RequestSpaceScreen(
                                    viewModel = spacesViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    communityId = communityId
                                )
                            }else {
                                Log.e("CommunityDetails", "Community id is null")
                            }
                        }

                        composable("edit_community/{communityId}"){ backStackEntry ->
                            val communityId = backStackEntry.arguments?.getString("communityId")

                            if (communityId != null) {
                                EditCommunityScreen(
                                    viewModel = communityViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    communityId = communityId,
                                )
                            }
                        }

                        composable("space_details/{spaceId}") { backStackEntry ->
                            val spaceId = backStackEntry.arguments?.getString("spaceId")

                            if (spaceId != null) {
                                SpaceDetails(
                                    spaceId = spaceId,
                                    eventsViewModel = eventsViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    communityViewModel = communityViewModel,
                                    spacesViewModel = spacesViewModel,
                                    userRepository = userRepository,
                                    onNavigateToEditSpace = {
                                        navController.navigate("edit_space/${spaceId}")
                                    },
                                    onNavigateToSpaceMembers = {navController.navigate("space_members/${spaceId}")},
                                    onNavigateToCreateEvent = {navController.navigate("create_event/${spaceId}")},
                                    navigateToEvent = {eventId ->
                                        navController.navigate("event_details/${eventId}")
                                    }

                                )
                            }
                        }

                        composable("space_members/{spaceId}"){backStackEntry->
                            val spaceId = backStackEntry.arguments?.getString("spaceId")

                            if (spaceId != null) {
                                SpaceMembers(
                                    spaceId = spaceId,
                                    spacesViewModel = spacesViewModel,
                                    navigateBack = { navController.popBackStack() },
                                    userRepository = userRepository
                                )
                            }
                        }

                        composable("edit_space/{spaceId}"){backStackEntry ->
                            val spaceId = backStackEntry.arguments?.getString("spaceId")

                            if (spaceId != null) {
                                EditSpaceScreen(
                                    viewModel = spacesViewModel,
                                    spaceId = spaceId,
                                    navigateBack = { navController.popBackStack() }
                                )
                            }else{
                                Log.d("MainActivity", "Space is null")
                            }

                        }

                        composable("approve_spaces_screen"){
                            val pendingCount by spacesViewModel.pendingCount

                            ApproveSpacesScreen(
                                viewModel = spacesViewModel,
                                pendingCount = pendingCount,
                                navigateBack = {navController.popBackStack()}
                            )
                        }
                    }
                }
            }
        }
    }
}

