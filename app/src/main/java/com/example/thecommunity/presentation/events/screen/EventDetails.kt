package com.example.thecommunity.presentation.events.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.outlined.AirlineSeatReclineExtra
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material.icons.outlined.Hail
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.RatingWithDetails
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.communities.screens.CommunityBottomSheetItem
import com.example.thecommunity.presentation.communities.screens.ImageCarousel
import com.example.thecommunity.presentation.events.EventsViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetails(
    eventsViewModel: EventsViewModel,
    currentUser: UserData,
    onNavigateToAttendees: (eventId: String) -> Unit,
    navigateBack: () -> Unit,
    onNavigateToUnattendees : (eventId: String) -> Unit,
    onNavigateToRefunds : (eventId: String) -> Unit,
    onNavigateToEditEvent: (eventId: String) -> Unit,
    eventId: String
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showBottomSheet by remember { mutableStateOf(false) }
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }



    // Function to fetch data
    fun fetchData() {
        coroutineScope.launch {
            isLoading = true // Set loading to true at the beginning of the fetch
            event = eventsViewModel.getEventById(eventId)
            eventsViewModel.fetchEventComments(eventId)
            eventsViewModel.getEventRatings(eventId)
            eventsViewModel.fetchEventReplies(eventId)
            Log.d("EventDetails", "Event details are: $event")

            isLoading = false // Set loading to false once the community is successfully fetched
        }
    }

    // Call fetchData initially
    LaunchedEffect(Unit) {
        fetchData()

        // After 10 seconds, check if the community is still null
        delay(10000)
        if (event == null && isLoading) {
            coroutineScope.coroutineContext.cancelChildren()
            val result = snackBarHostState.showSnackbar(
                message = "Failed to fetch event. Please try again.", actionLabel = "Retry"
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


    Scaffold(
        topBar = {
            event?.let {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = it.name,
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
    },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        floatingActionButton = {
            if (event != null){
                val isOrganizer = event!!.organizer == currentUser.userId
                val isAttending = event?.attendees?.any {
                    it.keys.contains(currentUser.userId)
                } == true

                if (isOrganizer || isAttending){
                    Button(
                        onClick = {
                            showBottomSheet = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (event!!.comments?.isEmpty() == true){
                                Icon(
                                    imageVector = Icons.Outlined.ModeComment,
                                    contentDescription = "Comments"
                                )
                            }else{
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                                    contentDescription = "Comments"
                                )
                            }

                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

            }

        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            event?.let { it1 ->
                val isAttending = event?.attendees?.any {
                    it.keys.contains(currentUser.userId)
                } == true
                EventsDetailsContent(
                    currentUser = currentUser,
                    event = it1,
                    onNavigateBack = navigateBack,
                    onNavigateToEditEvent = onNavigateToEditEvent,
                    onRefresh = {fetchData()},
                    isAttending = isAttending,
                    onNavigateToAttendees= onNavigateToAttendees,
                    onNavigateToUnattendees = onNavigateToUnattendees,
                    onNavigateToRefunds = onNavigateToRefunds,
                    eventsViewModel = eventsViewModel
                )
            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            event?.let {
                CommentsBottomSheet(
                    currUser = currentUser,
                    eventsViewModel = eventsViewModel,
                    event = it,
                    onRefresh = {fetchData()}
                )
            }

        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventsDetailsContent(
    event: Event,
    onNavigateToAttendees: (eventId: String) -> Unit,
    currentUser: UserData,
    isAttending: Boolean,
    onNavigateToUnattendees : (eventId: String) -> Unit,
    onNavigateToRefunds: (eventId: String) -> Unit,
    onRefresh: () -> Unit,
    eventsViewModel: EventsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditEvent: (eventId: String) -> Unit,
) {

    val refreshing by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val isPast = event.startDate.let { LocalDate.parse(it, dateFormatter) }.isBefore(LocalDate.now())
    LaunchedEffect(refreshing) {
        if (refreshing){
            onRefresh()
        }
    }
    val scrollState = rememberScrollState()
    val state = rememberPullRefreshState(refreshing, onRefresh)
    Box(
        modifier = Modifier
            .pullRefresh(state)
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            val isOrganizer = event.organizer == currentUser.userId


            val isPending = event.attendees?.any {
                it.keys.contains(currentUser.userId) && it[currentUser.userId]?.approved == false
            } == true

            EventHeader(
                currentUser = currentUser,
                isOrganizer = isOrganizer,
                isPending = isPending,
                isAttending = isAttending,
                isPast = isPast,
                onRefresh = onRefresh,
                event = event,
                onNavigateToUnattendees = onNavigateToUnattendees,
                onNavigateToAttendees = onNavigateToAttendees,
                onNavigateBack = onNavigateBack,
                onNavigateToEditEvent = onNavigateToEditEvent,
                eventsViewModel = eventsViewModel,
                onNavigateToRefunds = onNavigateToRefunds
            )

            event.description?.let {
                Text(
                    text = it
                )
            }

            LocationDetails(
                event = event
            )

            DateDetails(
                event = event
            )

            PickUpList(
                event = event,
                isAttending = isAttending,
                user = currentUser,
                onRefresh = onRefresh,
                eventsViewModel = eventsViewModel
            )

            DropOffList(
                event = event,
                onRefresh = onRefresh,
                isAttending = isAttending,
                user = currentUser,
                eventsViewModel = eventsViewModel
            )
            /**
             * TODO:  comments[Done], ratings[Add, Edit, Delete], Delete Event
             */
        }

        PullRefreshIndicator(
            refreshing,
            state,
            Modifier
                .align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface

        )
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventHeader(
    modifier: Modifier = Modifier,
    isOrganizer: Boolean,
    isAttending: Boolean,
    isPending: Boolean,
    isPast : Boolean,
    onNavigateToAttendees: (eventId: String) -> Unit,
    onNavigateToUnattendees : (eventId: String) -> Unit,
    onNavigateToRefunds: (eventId: String) -> Unit,
    onRefresh : () -> Unit,
    currentUser: UserData,
    event: Event,
    eventsViewModel: EventsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditEvent: (eventId: String) -> Unit,
) {

    val ratings = eventsViewModel.eventRatings.collectAsState().value

    val attendees = event.attendees?.size
    val unattendees = event.unattendees?.size
    val refundRequests = event.refundRequests?.size
    val averageRatingFlow = eventsViewModel.averageRatingFlow.collectAsState(0)
    val averageRating = averageRatingFlow.value

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var openRatingsDialog by remember { mutableStateOf(false) }

    var ratingsRemark by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }

    fun resetRatings() {
        ratingsRemark = ""
        selectedRating = 0
    }
    var isAttendButtonEnabled by remember { mutableStateOf(isAttending) }
    LaunchedEffect(isAttendButtonEnabled) {
        if (!isAttendButtonEnabled) {
            showBottomSheet = false
        }
    }

    if (openRatingsDialog) {
        AlertDialog(
            icon = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val isFilled = index < averageRating
                        Icon(
                            imageVector =  if (isFilled) Icons.Default.StarRate else Icons.Outlined.StarRate,
                            contentDescription = "Star",
                            modifier = Modifier.padding(2.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Rate Event"
                )
            },
            text = {
                Column (
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){

                    Text(
                        text = "What would you rate this event?",
                        textAlign = TextAlign.Center
                    )

                    // Display rating input
                    Row(
                        modifier = Modifier.clickable {
                            // Handle rating click
                            selectedRating = if (selectedRating == 0) 1 else selectedRating
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val isFilled = index < selectedRating
                            Icon(
                                imageVector =  if (isFilled) Icons.Default.StarRate else Icons.Outlined.StarRate,
                                contentDescription = "Star",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .clickable {
                                        selectedRating = index + 1
                                    }
                                    .padding(2.dp),
                            )
                        }
                    }


                }
            },
            onDismissRequest = {
                openRatingsDialog = false
                resetRatings()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.addRatingToEvent(
                                eventId = event.id,
                                rating = selectedRating,
                                remark = ratingsRemark,
                                userId = currentUser.userId
                            )
                            if (result) {
                                onRefresh()
                                resetRatings()
                                openRatingsDialog = false

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
                        openRatingsDialog = false
                        resetRatings()
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // AsyncImage with placeholder and error handling
        if (event.images.isNotEmpty()) {
            ImageCarousel(images = event.images)
        }

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f))
        )
        Column(
            modifier = modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)

                    ) {
                        Column(
                            modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "$attendees",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier
                                    .clickable {
                                    onNavigateToAttendees(event.id)
                                }
                            )

                            Text(
                                text = "Attendees",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = modifier
                                    .alpha(0.8f)
                                    .clickable {
                                        onNavigateToAttendees(event.id)
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        if (isOrganizer){
                            Column(
                                modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "$unattendees",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = modifier
                                        .clickable {
                                            onNavigateToUnattendees(event.id)
                                        }
                                )

                                Text(
                                    text = "Leavers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = modifier
                                        .alpha(0.8f)
                                        .clickable {
                                            onNavigateToUnattendees(event.id)
                                        }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))


                            Column(
                                modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "$refundRequests",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = modifier
                                        .clickable {
                                            onNavigateToRefunds(event.id)
                                        }
                                )

                                Text(
                                    text = "Refunds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = modifier
                                        .alpha(0.8f)
                                        .clickable {
                                            onNavigateToRefunds(event.id)
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if(!isPast){
                            if (isOrganizer) {
                                OutlinedButton(
                                    onClick = {
                                        showBottomSheet = true
                                    }
                                ) {
                                    Text(
                                        text = "Organizing"
                                    )
                                }
                            } else if (isAttending) {
                                if (isPending) {
                                    OutlinedButton(
                                        onClick = {
                                            showBottomSheet = true
                                        }
                                    ) {
                                        Text(
                                            text = "Pending"
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            showBottomSheet = true
                                        }
                                    ) {
                                        Text(
                                            text = "Attending"
                                        )
                                    }
                                }


                            } else {
                                Button(
                                    onClick = {
                                        isAttendButtonEnabled = false

                                        coroutineScope.launch {
                                            val result = eventsViewModel.attendEvent(
                                                eventId = event.id,
                                                userId = currentUser.userId
                                            )
                                            if (result) {
                                                onRefresh()
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Attend"
                                    )
                                }
                            }
                        }else{
                            // Display the star rating
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        openRatingsDialog = true
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { index ->
                                    val isFilled = index < averageRating
                                    Icon(
                                        imageVector =  if (isFilled) Icons.Default.StarRate else Icons.Outlined.StarRate,
                                        contentDescription = "Star",
                                        modifier = Modifier.padding(2.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            EventBottomSheet(
                navigateBack = onNavigateBack,
                user = currentUser,
                onDismissSheet = { showBottomSheet = false },
                onNavigateToEditEvent = onNavigateToEditEvent,
                event = event,
                isOrganizer = isOrganizer,
                eventsViewModel = eventsViewModel,
                isPending = isPending,
                onRefresh = onRefresh
            )
        }
    }

}

@Composable
fun PickUpList(
    event: Event,
    onRefresh: () -> Unit,
    user: UserData?,
    eventsViewModel: EventsViewModel,
    isAttending: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val tertiaryColor = MaterialTheme.colorScheme.onTertiaryContainer

    // Check if the user is in any pick-up
    val userInPickUp = user?.let { u ->
        event.pickUp?.any { pickup ->
            pickup.attendees.contains(u.userId) // Assuming `users` is a list of user IDs
        } ?: false
    } ?: false

    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize()
            .animateContentSize()
    ) {
        if (event.pickUp?.isNotEmpty() == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AirportShuttle, contentDescription = "PickUp"
                )

                Text(
                    text = "PickUp",
                    style = TextStyle(fontSize = MaterialTheme.typography.titleLarge.fontSize).copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
        }

        if (isExpanded) {
            event.pickUp?.forEachIndexed { index, pickup ->
                ListItem(
                    headlineContent = { Text(pickup.location.locationName) },
                    supportingContent = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Google Maps Link: " + pickup.location.googleMapsLink,
                                color = tertiaryColor,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri(pickup.location.googleMapsLink)
                                }
                            )
                            if (pickup.location.coordinates != null) {
                                val coordinates = "Coordinates: ${pickup.location.coordinates.latitude}, ${pickup.location.coordinates.longitude}"
                                Text(
                                    text = coordinates,
                                    color = tertiaryColor,
                                    modifier = Modifier.clickable {
                                        val uri = "geo:${pickup.location.coordinates.latitude},${pickup.location.coordinates.longitude}"
                                        uriHandler.openUri(uri)
                                    }
                                )
                            }
                            if (pickup.time.isNotEmpty()) {
                                Text("At: ${pickup.time}")
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Pick up."
                        )
                    },
                    trailingContent = {
                        if (userInPickUp) {
                            if ( pickup.attendees.contains(user?.userId)){
                                IconButton(
                                    enabled = isAttending,
                                    onClick = {
                                        coroutineScope.launch {
                                            val result =  user?.let { u ->
                                                eventsViewModel.removeUserFromPickup(
                                                    event.id,
                                                    u.userId,
                                                    index
                                                )
                                            } == true

                                            if (result){
                                                onRefresh()
                                            }

                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Hail,
                                        contentDescription = "Pick up",
                                        tint = tertiaryColor
                                    )
                                }
                            }
                        }else{
                            IconButton(
                                enabled = isAttending,
                                onClick = {
                                    coroutineScope.launch {

                                        val result = user?.let { u ->
                                            eventsViewModel.addUserToPickup(event.id, u.userId, index)
                                        } == true
                                        if (result){
                                            onRefresh()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Hail,
                                    contentDescription = "Pick up"
                                )
                            }
                        }


                    }
                )
            }
        }
    }
}

@Composable
fun DropOffList(
    event: Event,
    isAttending: Boolean,
    user: UserData?,
    onRefresh: () -> Unit,
    eventsViewModel: EventsViewModel,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val tertiaryColor = MaterialTheme.colorScheme.onTertiaryContainer

    // Check if the user is in any drop-off
    val userInDropOff = user?.let { u ->
        event.dropOff?.any { dropoff ->
            dropoff.attendees.contains(u.userId) // Assuming `users` is a list of user IDs
        } ?: false
    } ?: false

    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize()
            .animateContentSize()
    ) {
        if (event.dropOff?.isNotEmpty() == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AirportShuttle, contentDescription = "Drop off"
                )

                Text(
                    text = "Drop off",
                    style = TextStyle(fontSize = MaterialTheme.typography.titleLarge.fontSize).copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
        }

        if (isExpanded) {
            event.dropOff?.forEachIndexed { index, dropoff ->
                ListItem(
                    headlineContent = { Text(dropoff.location.locationName) },
                    supportingContent = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Google Maps Link: " + dropoff.location.googleMapsLink,
                                color = tertiaryColor,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri(dropoff.location.googleMapsLink)
                                }
                            )
                            if (dropoff.location.coordinates != null) {
                                Text(
                                    text = "Coordinates: ${dropoff.location.coordinates.latitude}, ${dropoff.location.coordinates.longitude}",
                                    color = tertiaryColor,
                                    modifier = Modifier.clickable {
                                        val uri = "geo:${dropoff.location.coordinates.latitude},${dropoff.location.coordinates.longitude}"
                                        uriHandler.openUri(uri)
                                    }
                                )
                            }
                            if (dropoff.time.isNotEmpty()) {
                                Text("At: ${dropoff.time}")
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Drop off."
                        )
                    },
                    trailingContent = {
                        if (userInDropOff) {
                            if (dropoff.attendees.contains(user?.userId)){
                                IconButton(
                                    enabled = isAttending,
                                    onClick = {
                                        coroutineScope.launch {
                                            val result =  user?.let { u ->
                                                eventsViewModel.removeUserFromDropoff(
                                                    event.id,
                                                    u.userId,
                                                    index
                                                )
                                            } == true

                                            if (result){
                                                onRefresh()
                                            }

                                        }                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AirlineSeatReclineExtra,
                                        contentDescription = "Drop off",
                                        tint = tertiaryColor
                                    )
                                }
                            }

                        }else{
                            IconButton(
                                enabled = isAttending,
                                onClick = {
                                     coroutineScope.launch {
                                        val result =  user?.let { u ->
                                            eventsViewModel.addUserToDropOff(
                                                event.id,
                                                u.userId,
                                                index
                                            )
                                        } == true

                                        if (result) {
                                            onRefresh()
                                        }
                                    }


                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AirlineSeatReclineExtra,
                                    contentDescription = "Drop off"
                                )
                            }
                        }

                    },
                )
            }
        }
    }
}


@Composable
fun LocationDetails(event: Event) {
    val uriHandler = LocalUriHandler.current // For opening URLs
    val tertiaryColor = MaterialTheme.colorScheme.onTertiaryContainer // Access tertiary color
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .padding(16.dp)
            .animateContentSize() // Animate the content size change
    ) {
        event.location?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn, contentDescription = "Location"
                )

                Text(
                    text = "Destination",
                    style = TextStyle(fontSize = MaterialTheme.typography.titleLarge.fontSize).copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            if (it.locationName != ""){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = it.locationName,
                        style = TextStyle(fontSize = MaterialTheme.typography.titleMedium.fontSize).copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if((it.googleMapsLink.isNotEmpty() || (it.coordinates?.latitude?.toInt() != 0 || it.coordinates.longitude.toInt() != 0))){
                        IconButton(
                            onClick = {
                                isExpanded = !isExpanded
                            }
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                }
            }else{
                Text(
                    text = "Destination to be confirmed"
                )
            }

            if (isExpanded) {
                if (it.googleMapsLink.isNotEmpty()) {
                    val googleMapsLinkText = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = tertiaryColor)) {
                            append("Google Maps Link:   " + it.googleMapsLink)
                        }
                    }
                    ClickableText(text = googleMapsLinkText, onClick = {
                        uriHandler.openUri(event.location!!.googleMapsLink)
                    })
                }

                if (it.coordinates != null && (it.coordinates.latitude != 0.0 || it.coordinates.longitude != 0.0)) {
                    val coordinatesText = "${it.coordinates.latitude}, ${it.coordinates.longitude}"
                    val googleMapsCoordinatesLink =
                        "https://www.google.com/maps/search/?api=1&query=${it.coordinates.latitude},${it.coordinates.longitude}"

                    val coordinatesAnnotatedText = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = tertiaryColor)) {
                            append("Coordinates: $coordinatesText")
                        }
                    }

                    ClickableText(text = coordinatesAnnotatedText, onClick = {
                        uriHandler.openUri(googleMapsCoordinatesLink)
                    })
                }
            }

        }
    }
}

@Composable
fun DateDetails(
    event: Event
) {
    val uriHandler = LocalUriHandler.current // For opening URLs
    val tertiaryColor = MaterialTheme.colorScheme.onTertiaryContainer // Access tertiary color
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday, contentDescription = "Date"
            )

            Text(
                text = "Date",
                style = TextStyle(fontSize = MaterialTheme.typography.titleLarge.fontSize).copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if(event.startDate.isNotEmpty() || event.startTime.isNotEmpty() || event.endTime.isNotEmpty() || event.endDate.isNotEmpty()){
            Text(
                text = "Starts on:  " + event.startDate + "    at: " + event.startTime,
                style = TextStyle(fontSize = MaterialTheme.typography.titleMedium.fontSize).copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = "Ends on:    " + event.endDate + "    at:  " + event.endTime,
                style = TextStyle(fontSize = MaterialTheme.typography.titleMedium.fontSize).copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            if (event.numberOfDays > 1){
                Text(
                    text = event.numberOfDays.toString() + " days",
                )
            }
        }else{
            Text(
                text = "Date to be confirmed"
            )
        }

    }

}

@SuppressLint("UnrememberedMutableState")
@Composable
fun EventBottomSheet(
    modifier: Modifier = Modifier,
    user: UserData?,
    isPending: Boolean,
    isOrganizer: Boolean,
    eventsViewModel: EventsViewModel,
    onRefresh: () -> Unit,
    event: Event,
    navigateBack: () -> Unit,
    onDismissSheet: () -> Unit,
    onNavigateToEditEvent: (eventId: String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val openLeaveAlertDialog = remember { mutableStateOf(false) }
    val openDeleteAlertDialog = remember { mutableStateOf(false) }
    var reasonToLeave by remember { mutableStateOf("") }
    var refundRequested by remember { mutableStateOf(false) }

    val deleteEventName = remember { mutableStateOf("") }
    val isDeleteButtonEnabled by derivedStateOf {
        deleteEventName.value.trim() == event.name
    }
    if (openLeaveAlertDialog.value){
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Inventory, contentDescription = "Toggle attendance")
            },
            title = {
                Text(
                    text = "Leave event?"
                )
            },
            text = {
                Column {
                    Text(
                        text = "It's sad to see you leave. Would you care to tell us why?",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = reasonToLeave,
                        onValueChange = { reasonToLeave = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (event.price!!>0){
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                text= "Request refund"
                            )

                            Checkbox(
                                checked = refundRequested,
                                onCheckedChange = { refundRequested = !refundRequested }
                            )
                        }
                    }
                }


            },
            onDismissRequest = {
                openLeaveAlertDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.leaveEvent(
                                eventId = event.id,
                                userId = user!!.userId,
                                reasonToLeave = reasonToLeave,
                                refundRequested = refundRequested
                            )
                            if (result){
                                openLeaveAlertDialog.value = false
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
                        openLeaveAlertDialog.value = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (openDeleteAlertDialog.value){
        AlertDialog(
            icon = {
                Icon(Icons.Default.Delete, contentDescription = "Delete Event")
            },
            title = {
                Text("Delete Space?")
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete this event? You wouldn't be able to recover it or its spaces later.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
//                    TextField(
//                        value = deleteEventName.value,
//                        onValueChange = { deleteEventName.value = it },
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        textStyle = TextStyle(
//                            color = MaterialTheme.colorScheme.onSurface,
//                            fontWeight = FontWeight.Bold
//                        )
//                    )
                }
            },            onDismissRequest = {
                openDeleteAlertDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                            coroutineScope.launch {
                                val result = eventsViewModel.deleteEvent(event)

                                if (result){
                                    navigateBack()
                                }

                            }

                    },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDeleteAlertDialog.value = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }


    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = event.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        if (isPending){
            Text(
                text = "Please contact the organizer to confirm payment",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        HorizontalDivider()

        if (!isOrganizer){
            CommunityBottomSheetItem(
                text = "Leave",
                leadingIcon = Icons.Default.Logout,
                onClick = {
                    openLeaveAlertDialog.value = true
                }
            )
        }
        if (isOrganizer){
            CommunityBottomSheetItem(
                text = "Edit",
                leadingIcon = Icons.Default.Edit,
                onClick = {
                    onNavigateToEditEvent(event.id)
                }
            )
        }

        CommunityBottomSheetItem(
            text = "Report",
            leadingIcon = Icons.Default.Flag,
            onClick = {
                TODO()
            }
        )


        if (isOrganizer) {
            CommunityBottomSheetItem(
                text = "Delete",
                leadingIcon = Icons.Default.Delete,
                onClick = {
                    openDeleteAlertDialog.value = true
                }
            )
        }
    }
}

fun calculateAverageRating(ratings: List<RatingWithDetails>?): Int {
    val validRatings = ratings?.filter { it.rating in 1..5 } ?: emptyList()
    return if (validRatings.isNotEmpty()) {
        (validRatings.sumOf { it.rating } / validRatings.size).toInt()
    } else {
        0
    }
}
