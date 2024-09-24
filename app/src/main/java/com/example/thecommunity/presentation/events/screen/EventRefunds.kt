package com.example.thecommunity.presentation.events.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.events.EventsViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRefunds(
    eventId: String,
    userRepository : UserRepository,
    eventsViewModel: EventsViewModel,
    navigateBack: () -> Unit
){
    var user by remember { mutableStateOf<UserData?>(null) }
    var event by remember { mutableStateOf<Event?>(null) }
    var refunds by remember { mutableStateOf<List<UserData?>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }


    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        coroutineScope.launch {
            try {
                isLoading = true
                user = userRepository.getCurrentUser()
                event = eventsViewModel.getEventById(eventId)
                event?.let {
                    eventsViewModel.fetchEventRefunds(eventId)
                    eventsViewModel.eventRefunds.collect { eventUnattendees ->
                        refunds = eventUnattendees
                        // Once data is received, stop the loading indicator
                        isLoading = false
                    }
                } ?: run {
                    snackBarHostState.showSnackbar(
                        message = "Failed to fetch community members. Please try again.",
                        actionLabel = "Retry"
                    ).also {
                        if (it == SnackbarResult.ActionPerformed) fetchData()
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                snackBarHostState.showSnackbar("An error occurred: ${e.message}")
            }
        }
    }

    val isOrganizer = event?.organizer == user?.userId

    LaunchedEffect(Unit) {
        fetchData()
        // After 10 seconds, check if the community is still null
        delay(10000)
        if (event == null && isLoading) {
            coroutineScope.coroutineContext.cancelChildren()
            val result = snackBarHostState.showSnackbar(
                message = "Failed to fetch community. Please try again.",
                actionLabel = "Retry"
            )
            if (result == SnackbarResult.ActionPerformed) {
                fetchData()
            }
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.coroutineContext.cancelChildren()
        }
    }

    Scaffold(topBar = {
        event?.let {
            CenterAlignedTopAppBar(title = {
                Text(
                    text = it.name + " refunds",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }, navigationIcon = {
                IconButton(onClick = {
                    navigateBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(Color.Transparent)
            )
        }
    }, snackbarHost = { SnackbarHost(hostState = snackBarHostState) },

        modifier = Modifier.fillMaxSize()
    ) {innerPadding->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            event?.let {
                user?.let { it1 ->
                    RefundList(
                        curUserIsOrganizer = isOrganizer,
                        event = it,
                        eventViewModel = eventsViewModel,
                        currentUser = it1,
                        refunds = refunds,
                        onRefresh = { fetchData() },
                        modifier = Modifier
                            .align(
                                Alignment.TopStart
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundList(
    modifier: Modifier = Modifier,
    curUserIsOrganizer: Boolean,
    event: Event,
    eventViewModel: EventsViewModel,
    currentUser: UserData,
    refunds: List<UserData?>,  // New attendees parameter
    onRefresh: () -> Unit
) {
    if(refunds.isEmpty()){
        Text(
            text = "There is no one here yet"
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(refunds){ refund->
            if (refund != null) {
                RefundItem(
                    curUserIsOrganizer = curUserIsOrganizer,
                    event = event,
                    eventViewModel = eventViewModel,
                    listUser = refund,
                    currentUser = currentUser,
                    onRefresh = onRefresh
                )
            }

        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundItem(
    curUserIsOrganizer: Boolean,
    event: Event,
    eventViewModel: EventsViewModel,
    listUser: UserData,
    currentUser: UserData,
    onRefresh: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showAttendeeBottomSheet by remember { mutableStateOf(false) }
    val tertiaryColor = MaterialTheme.colorScheme.onTertiaryContainer // Access tertiary color
    //val reasonToLeave = event.unattendees?.find { it.containsKey(listUser.userId) }?.get(listUser.userId) ?: "No reason provided"


    ListItem(
        headlineContent = {
            Text(text = listUser.username ?: "Unknown")
        },
        leadingContent = {
            if (listUser.profilePictureUrl != null) {
                AsyncImage(
                    model = listUser.profilePictureUrl,
                    contentDescription = listUser.username,
                    modifier = Modifier
                        .size(40.dp) // Specify a fixed size to avoid layout recalculations
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

        },

    )

}
