package com.example.thecommunity.presentation.events.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.events.EventsViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAttendees(
    eventId: String,
    userRepository : UserRepository,
    eventsViewModel: EventsViewModel,
    navigateBack: () -> Unit
){
    var user by remember { mutableStateOf<UserData?>(null) }
    var event by remember { mutableStateOf<Event?>(null) }
    var attendees by remember { mutableStateOf<List<UserData?>>(emptyList()) }
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
                    eventsViewModel.fetchEventAttendees(eventId)
                    eventsViewModel.eventAttendees.collect { eventAttendees ->
                        attendees = eventAttendees
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
                    text = it.name + " attendees",
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
                    AttendeeList(
                        curUserIsOrganizer = isOrganizer,
                        event = it,
                        eventViewModel = eventsViewModel,
                        currentUser = it1,
                        attendees = attendees,
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
fun AttendeeList(
    modifier: Modifier = Modifier,
    curUserIsOrganizer: Boolean,
    event: Event,
    eventViewModel: EventsViewModel,
    currentUser: UserData,
    attendees: List<UserData?>,  // New attendees parameter
    onRefresh: () -> Unit
) {
    val sortedAttendees = event.attendees?.filter { attendeeMap ->
        val attendee = attendeeMap.values.firstOrNull()
        if (attendee != null) {
            // If the current user is the organizer, show all attendees (approved or not).
            if (currentUser.userId == event.organizer) {
                true
            } else {
                // Otherwise, show only approved attendees.
                attendee.approved
            }
        } else {
            false
        }
    }?.sortedByDescending { attendeeMap ->
        val attendee = attendeeMap.values.firstOrNull()
        // Sort by whether the attendance is confirmed and arrival is confirmed
        attendee?.approved == true && attendee.arrived == true
    } ?: emptyList()

    if(attendees.isEmpty()){
        Text(
            text = "There is no one here yet"
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortedAttendees.forEach { attendeeMap ->
            attendeeMap.keys.firstOrNull()?.let { listUserId ->
                // Find the corresponding UserData object from the attendees list
                val listUser = attendees.find { it!!.userId == listUserId }

                attendeeMap.values.firstOrNull()?.let { eventAttendee ->
                    listUser?.let {
                        item {  // `item` should be used instead of `items` for single composable
                            AttendeeItem(
                                curUserIsOrganizer = curUserIsOrganizer,
                                event = event,
                                eventViewModel = eventViewModel,
                                listUser = it,
                                currentUser = currentUser,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendeeItem(
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

    val isAttendanceConfirmed = event.attendees?.any {
        it.keys.contains(listUser.userId) && it[listUser.userId]?.approved == true
    } == true

    val isArrivalConfirmed = event.attendees?.any {
        it.keys.contains(listUser.userId) && it[listUser.userId]?.arrived == true
    } == true

    val isCurrentUserOrganizer = currentUser.userId == event.organizer

    val listUserIsCurrentUser = listUser.userId == currentUser.userId

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
        trailingContent = {
            if (curUserIsOrganizer && !listUserIsCurrentUser) {
                IconButton(onClick = { showAttendeeBottomSheet = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        },
        supportingContent = {
            if (isCurrentUserOrganizer){
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isAttendanceConfirmed) "Attendance confirmed" else "Attendance pending",
                        textAlign = TextAlign.Start,
                    )

                    Text(
                        text = if (isArrivalConfirmed) "Arrival confirmed" else "Arrival pending",
                        textAlign = TextAlign.Start,
                        )
                }
            }

        }
    )

    if (showAttendeeBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showAttendeeBottomSheet = false
            },
            sheetState = sheetState
        ){
            AttendeeBottomSheet(
                onDismissSheet = { showAttendeeBottomSheet = false },
                listUser = listUser,
                onRefresh = onRefresh,
                eventViewModel = eventViewModel,
                event = event,
                isOrganizer = isCurrentUserOrganizer
            )
        }

    }
}

@Composable
fun AttendeeBottomSheet(
    onDismissSheet: () -> Unit,
    listUser: UserData,
    onRefresh: () -> Unit,
    eventViewModel: EventsViewModel,
    event: Event,
    isOrganizer: Boolean
){
    var showApproveDialog by remember { mutableStateOf(false) }
    var showArrivedDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val eventStartDate = LocalDate.parse(event.startDate, DateTimeFormatter.ISO_DATE) // Parse the start date
    val currentDate = LocalDate.now()

    val isSwitchEnabled = currentDate.isEqual(eventStartDate) || currentDate.isAfter(eventStartDate) // Check if current date is same or after the event start date

    val isListUserApproved = event.attendees?.any {
        it.keys.contains(listUser.userId) && it[listUser.userId]?.approved == true
    } == true

    val isListUserArrived = event.attendees?.any {
      it.keys.contains(listUser.userId) && it [listUser.userId]?.arrived == true
    } == true

    val isListUserMuted =  event.attendees?.any {
        it.keys.contains(listUser.userId).and(it[listUser.userId]?.muted == true)
    } == true

    if (showApproveDialog){
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Inventory, contentDescription = "Toggle attendance")
            },
            title = {
                Text(
                    text = if (isListUserApproved) "Unapprove this attendee?" else "Approve this attendee"
                )
            },
            text = {
                Text(
                    text = if (isListUserApproved) "This user will no longer be able to attend this event" else "This user will have access to the event",
                    textAlign = TextAlign.Center
                )
            },
            onDismissRequest = {
                showArrivedDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventViewModel.toggleAttendanceApproval(
                                eventId = event.id,
                                userId = listUser.userId
                            )
                            if (result){
                                showApproveDialog = false
                                onDismissSheet()
                                onRefresh()
                            }
                        }

                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showArrivedDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (showArrivedDialog) {
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Inventory, contentDescription = "Toggle attendance")
            },
            title = {
                Text(
                    text = if (isListUserApproved) "Mark this attendee as not arrived?" else "Mark this attendee as arrived?"
                )
            },
            text = {
                Text(
                    text = if (isListUserApproved) "This user will be marked absent from the event might be entitled to a refund." else "This user will be marked present at the event.",
                    textAlign = TextAlign.Center
                )
            },
            onDismissRequest = {
                showArrivedDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventViewModel.toggleAttendanceArrival(
                                eventId = event.id,
                                userId = listUser.userId
                            )
                            if (result){
                                showApproveDialog = false
                                onDismissSheet()
                                onRefresh()
                            }
                        }

                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showArrivedDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isOrganizer){
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Approve"
                )

                Switch(
                    checked = isListUserApproved,
                    onCheckedChange = {
                        showApproveDialog = true
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mark arrived"
                )

                Switch(
                    checked = isListUserArrived,
                    onCheckedChange = {
                        showArrivedDialog = true
                    },
                    enabled = isSwitchEnabled
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mute attendee"
                )

                Switch(
                    checked = isListUserMuted,
                    onCheckedChange = {
                        coroutineScope.launch {
                            val result = eventViewModel.toggleMute(
                                eventId = event.id,
                                userId = listUser.userId
                            )

                            if (result){
                                onRefresh()
                            }
                        }
                    },
                )
            }
        }else{
            Text(
                text = "This is a restricted area",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

    }
}