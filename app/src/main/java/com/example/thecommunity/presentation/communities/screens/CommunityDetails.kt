@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.thecommunity.presentation.communities.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.thecommunity.R
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.communities.CommunityViewModel
import com.example.thecommunity.presentation.communities.JoinRequestState
import com.example.thecommunity.presentation.communities.JoinedStatusState
import com.example.thecommunity.presentation.events.EventsState
import com.example.thecommunity.presentation.events.EventsViewModel
import com.example.thecommunity.presentation.spaces.SpaceState
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import com.example.thecommunity.ui.theme.TheCommunityTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CommunityDetails(
    communityId: String,
    navigateBack: () -> Unit,
    onNavigateToAddSpace: (Community) -> Unit,
    onNavigateToSpace: (Space) -> Unit,
    navigateToEvent: (eventId : String) -> Unit,
    communityViewModel: CommunityViewModel,
    eventsViewModel: EventsViewModel,
    spacesViewModel: SpacesViewModel,
    userRepository: UserRepository,
    onNavigateToApproveSpaces: () -> Unit,
    onNavigateToEditCommunity: (Community) -> Unit,
    onNavigateToCreateEvent : () -> Unit,
    onNavigateToWriteArticle : () -> Unit,
    onNavigateToCommunityMembers : (Community) -> Unit
) {
    var user by remember { mutableStateOf<UserData?>(null) }
    var community by remember { mutableStateOf<Community?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    val snackBarHostState = remember { SnackbarHostState() }

    // Function to fetch data
    fun fetchData() {
        coroutineScope.launch {
            isLoading = true // Set loading to true at the beginning of the fetch
            user = userRepository.getCurrentUser()
            community = communityViewModel.getCommunityById(communityId)
            if (community != null) {
                eventsViewModel.getEventsForCommunities(communityId = communityId, spaceId = null)
                spacesViewModel.fetchLiveSpacesByCommunity(communityId)
                spacesViewModel.fetchPendingSpacesByCommunity(communityId)
                spacesViewModel.fetchRejectedSpacesByCommunity(communityId)
                communityViewModel.startObservingJoinedStatus(user?.userId ?: "", communityId)
                isLoading = false // Set loading to false once the community is successfully fetched
            } else {
                // Handle failure case by showing the snack bar with a retry option
                isLoading = false
                val result = snackBarHostState.showSnackbar(
                    message = "Failed to fetch community. Please try again.",
                    actionLabel = "Retry"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    fetchData()
                }
            }
        }
    }

    // Call fetchData initially
    LaunchedEffect(Unit) {
        fetchData()

        // After 10 seconds, check if the community is still null
        delay(10000)
        if (community == null && isLoading) {
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

    val isLeader = community?.members?.any {
        it.containsKey(user?.userId) && it[user?.userId] == "leader"
    } == true
    val isEditor = community?.members?.any {
        it.containsKey(user?.userId) && it[user?.userId] == "editor"
    } == true

    Scaffold(
        topBar = {
            community?.let {
                CommunityTopBar(
                    community = it,
                    navigateBack = { navigateBack() },
                )
            }
        },
        floatingActionButton = {
            community?.let {
                if(isEditor){
                    Button(
                        onClick = {
                            onNavigateToWriteArticle()
                        }
                    ){
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Newspaper,
                                contentDescription = "Write article"
                            )

                            Text(text = "Write Article")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            community?.let { it1 ->
                if (isLeader != null && isEditor != null) {
                    CommunityDetailsContent(
                        community = it1,
                        user = user,
                        communityViewModel = communityViewModel,
                        onNavigateToAddSpace = onNavigateToAddSpace,
                        spacesViewModel = spacesViewModel,
                        onNavigateToSpace = onNavigateToSpace,
                        onNavigateToApproveSpaces = onNavigateToApproveSpaces,
                        onRefresh = { fetchData() },  // Pass fetchData as the onRefresh function
                        isLeader = isLeader,
                        isEditor = isEditor,
                        navigateBack = navigateBack,
                        eventsViewModel = eventsViewModel,
                        onNavigateToEditCommunity = onNavigateToEditCommunity,
                        navigateToEvent = navigateToEvent,
                        onNavigateToCommunityMembers = onNavigateToCommunityMembers,
                        onNavigateToCreateEvent = onNavigateToCreateEvent
                    )
                }
            }

        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommunityDetailsContent(
    modifier: Modifier = Modifier,
    community: Community,
    isLeader : Boolean,
    navigateBack: () -> Unit,
    navigateToEvent: (eventId : String) -> Unit,
    isEditor : Boolean,
    onNavigateToAddSpace: (Community) -> Unit,
    onNavigateToSpace: (Space) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    user: UserData?,
    onRefresh: () -> Unit,
    communityViewModel: CommunityViewModel,
    eventsViewModel: EventsViewModel,
    spacesViewModel: SpacesViewModel,
    onNavigateToEditCommunity: (Community) -> Unit,
    onNavigateToApproveSpaces : () -> Unit,
    onNavigateToCommunityMembers: (Community) -> Unit
) {
    val joinedStatusState by communityViewModel.isJoined.collectAsState()
    val joinRequestState by communityViewModel.joinRequestState.collectAsState()
    // Create a SwipeRefreshState to manage the refresh state
    val refreshScope = rememberCoroutineScope()
    val refreshing by remember { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(15) }

    val isJoined = community.members.any { it.containsKey(user?.userId) }


    LaunchedEffect(refreshing) {
        if (refreshing){
            onRefresh()
        }
    }
    val snackBarHostState = remember { SnackbarHostState() }
    val state = rememberPullRefreshState(refreshing, onRefresh)
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .pullRefresh(state)
            .fillMaxSize()
    ){
        Column(
            modifier = modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .pullRefresh(state),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            CommunityHeader(
                community = community,
                user = user,
                isLeader = isLeader,
                isEditor = isEditor,
                onRefresh = onRefresh,
                communityViewModel = communityViewModel,
                navigateBack = navigateBack,
                joinedStatusState = joinedStatusState,
                joinRequestState = joinRequestState,
                isJoined = isJoined,
                onNavigateToEditCommunity = onNavigateToEditCommunity,
                onNavigateToCommunityMembers = onNavigateToCommunityMembers,
                navigateToEvent = navigateToEvent,
                eventsViewModel = eventsViewModel,
                onNavigateToCreateEvent = onNavigateToCreateEvent,
                spacesViewModel = spacesViewModel,
                onNavigateToAddSpace = onNavigateToAddSpace,
                onNavigateToSpace = onNavigateToSpace
            )

            Spacer(modifier = modifier.height(16.dp))


            if (isLeader) {
                SpacesDashboardContent(
                    community = community,
                    onNavigateToApproveSpaces = { onNavigateToApproveSpaces() }
                )
            }


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

    SnackbarHost(
        hostState = snackBarHostState,
        snackbar = { snackBarData ->
            Snackbar(
                snackbarData = snackBarData,
                modifier = modifier.padding(16.dp)
            )
        }
    )
}
/**
 * Community Leader dashboard
 */

@Composable
fun SpacesDashboardContent(
    modifier: Modifier = Modifier,
    community: Community,
    onNavigateToApproveSpaces: () -> Unit,
){
    val liveSpaces = community.spaces.filter { it.values.firstOrNull() == "Live" }
    val pendingSpaces = community.spaces.filter { it.values.firstOrNull() == "Pending" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.Start
    ){
        Text(
            modifier = modifier.fillMaxWidth(),
            text = "Community Leader Dashboard",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
        )
        Card(
            modifier = modifier
                .padding(16.dp)
                .clickable { onNavigateToApproveSpaces() }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Spaces Overview",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = modifier.padding(8.dp))

                Row {
                    Text(
                        text = "${liveSpaces.size}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = modifier.weight(1f))

                    Text(
                        text = "Live",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = modifier.padding(16.dp))

                Row {
                    Text(
                        text = "${pendingSpaces.size}",
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(modifier = modifier.weight(1f))

                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = modifier.padding(16.dp))

            }
        }
    }

}


/**
 * Spaces segment
 */

@Composable
fun SpacesColumn(
    modifier: Modifier = Modifier,
    community: Community,
    onNavigateToAddSpace: (Community) -> Unit,
    navigateToSpace: (Space) -> Unit,
    spacesViewModel: SpacesViewModel,
    joinedStatusState: JoinedStatusState
) {
    val spacesState by spacesViewModel.liveSpacesState.collectAsState()
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = modifier.fillMaxWidth(),
            text = "Spaces",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        if (
            community.status == "Live" &&
            joinedStatusState is JoinedStatusState.Success &&
            (joinedStatusState.isJoined || community.spaces.isNotEmpty())
        ) {
            IconButton(
                onClick = { onNavigateToAddSpace(community) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Space"
                )
            }
        }

        HorizontalDivider()
        Column {
            // Handle the different states of spacesState
            when (spacesState) {
                is SpaceState.Loading -> {
                    CircularProgressIndicator()
                }

                is SpaceState.Error -> {
                    val errorMessage =
                        (spacesState as? SpaceState.Error)?.message ?: "Failed to load spaces"
                    Text(text = errorMessage)
                }

                is SpaceState.Success -> {
                    val spaces = (spacesState as? SpaceState.Success)?.spaces.orEmpty()
                    SpaceList(
                        spaces = spaces,
                        navigateToSpace = navigateToSpace
                    )
                }
            }

        }
    }
}

@Composable
fun SpaceList(
    spaces: List<Space>,
    navigateToSpace: (Space) -> Unit
){
    LazyColumn {
        items(spaces){space->
            SpaceCard(
                navigateToSpace = { navigateToSpace(space) },
                space = space
            )

        }
    }
}

@Composable
fun SpaceCard(
    navigateToSpace: (Space) -> Unit,
    space: Space
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                navigateToSpace(space)
                Log.d("CommunityDetails", "Space clicked: ${space.name}")
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // AsyncImage with placeholder and error handling
                if(space.bannerUri != null){
                    AsyncImage(
                        model = space.bannerUri,
                        contentDescription = "Space Banner",
                        contentScale = ContentScale.Crop,
                        //error = painterResource(R.drawable.error), // Replace with your error drawable resource
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Semi-transparent overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Image with placeholder and error handling

                        if(space.profileUri != null){
                            AsyncImage(
                                model = space.profileUri,
                                contentDescription = "Community Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = space.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }

                    if (space.description != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = space.description!!,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventsColumn(
    modifier: Modifier = Modifier,
    community: Community,
    isLeader : Boolean,
    isEditor: Boolean,
    navigateToEvent: (eventId : String) -> Unit,
    eventsViewModel: EventsViewModel,
    onNavigateToCreateEvent: () -> Unit,
    joinedStatusState: JoinedStatusState
) {
    val eventsState by eventsViewModel.eventsState.collectAsState()
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = modifier.fillMaxWidth(),
            text = "Events",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        if (
            community.status == "Live" &&
            joinedStatusState is JoinedStatusState.Success &&
            (joinedStatusState.isJoined || community.spaces.isNotEmpty())
        ) {
            if (isLeader || isEditor) {
                IconButton(
                    onClick = { onNavigateToCreateEvent() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add event"
                    )
                }
            }

        }
        HorizontalDivider()


        Column {
            // Handle the different states of spacesState
            when (eventsState) {
                is EventsState.Loading -> {
                    CircularProgressIndicator()
                }

                is EventsState.Error -> {
                    val errorMessage =
                        (eventsState as? EventsState.Error)?.message ?: "Failed to load events"
                    Text(text = errorMessage)
                }

                is EventsState.Success -> {
                    val events = (eventsState as? EventsState.Success)?.events.orEmpty()

                    if (events.isEmpty()){
                        Text(
                            text = "There is nothing here yet",
                            textAlign = TextAlign.Center
                        )
                    }
                    EventsList(
                        events = events,
                        navigateToEvent = navigateToEvent
                    )
                }
            }
        }
    }
}

@Composable
fun DotIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = Color.Gray,
    dotSize: Dp = 8.dp,
    spacing: Dp = 4.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
    ) {
        for (i in 0 until totalDots) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (i == selectedIndex) selectedColor else unselectedColor)
            )
        }
    }
}

@Composable
fun EventsList(
    events: List<Event>,
    navigateToEvent: (eventId: String) -> Unit,
) {
    // Get the current date
    val currentDate = LocalDate.now()

    // Filter events into upcoming and past events
    val (upcomingEvents, pastEvents) = events.partition { event ->
        val eventEndDate = LocalDate.parse(event.endDate) // Assuming endDate is in ISO format
        eventEndDate.isAfter(currentDate) || eventEndDate.isEqual(currentDate)
    }

    Column {
        // Upcoming Events
        if (upcomingEvents.isNotEmpty()) {
            Text(
                text = "Upcoming Events",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )
            LazyColumn {
                items(upcomingEvents) { event ->
                    EventCard(
                        navigateToEvent = { navigateToEvent(event.id) },
                        event = event
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Past Events
        if (pastEvents.isNotEmpty()) {
            Text(
                text = "Past Events",
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
            LazyColumn {
                items(pastEvents) { event ->
                    EventCard(
                        navigateToEvent = { navigateToEvent(event.id) },
                        event = event
                    )
                }
            }
        }
    }
}


@Composable
fun EventCard(
    navigateToEvent: (eventId : String) -> Unit,
    event: Event
) {
    //val cardWidth = 200.dp // Set a fixed width for the cards
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navigateToEvent(event.id)
                Log.d("CommunityDetails", "Event clicked: ${event.name}")
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            if (event.images.isNotEmpty()) {
                ImageCarousel(images = event.images)
            }

            // Semi-transparent overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                    )
                )
            }

        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImageCarousel(images: List<Map<String, String?>>) {
    val pagerState = rememberPagerState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // Adjust the delay time as needed (3 seconds for smooth auto-scroll)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % images.size)
        }
    }

    HorizontalPager(
        count = images.size,
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val imageUrl = images[page]["url"]
        ImageItem(imageUrl = imageUrl)
    }
}


@Composable
fun ImageItem(imageUrl: String?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        imageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            // Placeholder if image URL is null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Image not available", color = Color.White)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHeader(
    modifier: Modifier = Modifier,
    community: Community,
    isLeader: Boolean,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToAddSpace: (Community) -> Unit,
    spacesViewModel : SpacesViewModel,
    onNavigateToSpace : (Space) -> Unit,
    isEditor: Boolean,
    navigateToEvent : (eventId : String) -> Unit,
    eventsViewModel: EventsViewModel,
    isJoined : Boolean,
    user: UserData?,
    navigateBack: () -> Unit,
    onRefresh: () -> Unit,
    communityViewModel: CommunityViewModel,
    joinedStatusState: JoinedStatusState,
    joinRequestState: JoinRequestState,
    onNavigateToEditCommunity: (Community) -> Unit,
    onNavigateToCommunityMembers : (Community) -> Unit
) {
    val spaces = community.spaces.size
    val events = community.events.size
    val members = community.members.size
    var isButtonEnabled by remember { mutableStateOf(false) }

    // Handle button enabling after a delay
    LaunchedEffect(joinRequestState) {
        if (joinRequestState == JoinRequestState.Idle) {
            isButtonEnabled = false // Disable the button initially
            delay(3000) // 1 seconds delay
            isButtonEnabled = true // Enable the button after delay
        } else {
            isButtonEnabled = false // Disable the button if not idle
        }
    }
    //Modal bottom sheet

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    var showCommunityActionBottomSheet by remember { mutableStateOf(false) }
    var showEventsBottomSheet by remember { mutableStateOf(false) }
    var showSpacesBottomSheet by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // AsyncImage with placeholder and error handling
        AsyncImage(
            model = community.communityBannerUrl,
            placeholder = rememberAsyncImagePainter(model = community.bannerThumbnailUrl), // Replace with your placeholder drawable resource
            contentDescription = "Community Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
        )
        Column(
            modifier = modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Image
                if (community.profileUrl != null) {
                    AsyncImage(
                        model = community.profileUrl,
                        contentDescription = "Profile picture",
                        modifier = modifier
                            .size(128.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.placeholder),
                        contentDescription ="Profile image",
                        modifier = modifier
                            .size(128.dp)
                            .clip(CircleShape)
                    )

                }

                Spacer(modifier = modifier.weight(1f))

                /**
                 * Community size
                 */

                Column(
                    modifier = modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ){
                    Row(
                        modifier = modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "$spaces",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier.clickable {
                                    showSpacesBottomSheet = true
                                }
                            )
                            Text(
                                text = "Spaces",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = modifier
                                    .alpha(0.8f)
                                    .clickable {
                                        showSpacesBottomSheet = true
                                    }
                            )
                        }

                        Column(
                            modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "$events",
                                modifier = modifier
                                    .clickable {
                                    showEventsBottomSheet = true
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Events",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = modifier
                                    .alpha(0.8f)
                                    .clickable {
                                        showEventsBottomSheet = true
                                    }
                            )
                        }

                        Column(
                            modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "$members",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier.clickable {
                                    onNavigateToCommunityMembers(community)
                                }
                            )

                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = modifier
                                    .alpha(0.8f)
                                    .clickable {
                                        onNavigateToCommunityMembers(community)
                                    }
                            )
                        }
                    }


                    if (community.status == "Live") {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            when (joinedStatusState) {
                                is JoinedStatusState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                                is JoinedStatusState.Error -> {
                                    Snackbar(
                                        action = {
                                            TextButton(onClick = { communityViewModel.startObservingJoinedStatus(user?.userId ?: "", community.id) }) {
                                                Text("Retry")
                                            }
                                        }
                                    ) {
                                        Text("Failed to fetch Joined Status.")
                                    }
                                }
                                is JoinedStatusState.Success -> {
                                    //val isJoined = user?.communities?.any { it["communityId"] == community.id } == true
                                    when (joinRequestState) {
                                        JoinRequestState.Loading -> {
                                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                        }
                                        is JoinRequestState.Error -> {
                                            Snackbar(
                                                action = {
                                                    TextButton(onClick = { /* Retry action */ }) {
                                                        Text("Please check your internet and try again")
                                                    }
                                                }
                                            ) {
                                                Text("Failed to fetch join status")
                                            }
                                        }
                                        else -> {
                                            // Success or Idle
                                            when (isJoined) {
                                                false -> {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                if (user != null && joinRequestState == JoinRequestState.Idle) {
                                                                    val success = communityViewModel.onJoinCommunity(user, community)
                                                                    if(success){
                                                                        onRefresh()
                                                                    }
                                                                }
                                                            }

                                                        },
                                                        modifier = Modifier
                                                            .padding(8.dp)
                                                            .fillMaxWidth(),
                                                        enabled = joinRequestState == JoinRequestState.Idle
                                                    ) {
                                                        Text(
                                                            text = "Join",
                                                            //color = MaterialTheme.colorScheme.onPrimary,
                                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }
                                                true -> {
                                                    OutlinedButton(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        onClick = {
                                                            showCommunityActionBottomSheet = if (joinRequestState == JoinRequestState.Idle) {
                                                                true
                                                            } else {
                                                                true
                                                            }
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = "Joined",
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                                            )

                                                            Spacer(modifier = Modifier.width(16.dp))

                                                            Icon(
                                                                imageVector = Icons.Default.ArrowDropDown,
                                                                contentDescription = "More",
                                                                tint = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showCommunityActionBottomSheet = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Pending Approval",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }


                }
            }

            Spacer(modifier = modifier.height(8.dp))
        }
    }

    if (showCommunityActionBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showCommunityActionBottomSheet = false
            },
            sheetState = sheetState
        ) {
            CommunityBottomSheet(
                isLeader = isLeader,
                isEditor = isEditor,
                community = community,
                navigateBack = navigateBack,
                user = user,
                communityViewModel = communityViewModel,
                onRefresh = onRefresh,
                onDismissSheet = { showCommunityActionBottomSheet = false },
                onNavigateToEditCommunity = onNavigateToEditCommunity
            )
        }
    }

    if (showEventsBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showEventsBottomSheet = false
            },
            sheetState = sheetState
        ) {
            EventsColumn(
                community = community,
                isLeader = isLeader,
                isEditor = isEditor,
                navigateToEvent = navigateToEvent,
                eventsViewModel = eventsViewModel,
                joinedStatusState = joinedStatusState,
                onNavigateToCreateEvent = onNavigateToCreateEvent
            )
        }
    }

    if (showSpacesBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showEventsBottomSheet = false
            },
            sheetState = sheetState
        ) {
            SpacesColumn(
                onNavigateToAddSpace = { onNavigateToAddSpace(community) },
                navigateToSpace = onNavigateToSpace,
                community = community,
                spacesViewModel = spacesViewModel,
                joinedStatusState = joinedStatusState
            )
        }
    }

}


@SuppressLint("UnrememberedMutableState")
@Composable
fun CommunityBottomSheet(
    modifier: Modifier = Modifier,
    user: UserData?,
    isLeader: Boolean,
    navigateBack: () -> Unit,
    onDismissSheet: () -> Unit,
    isEditor: Boolean,
    communityViewModel: CommunityViewModel,
    community: Community,
    onRefresh: () -> Unit,
    onNavigateToEditCommunity : (Community) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val openLeaveAlertDialog = remember { mutableStateOf(false) }
    val openStepDownAlertDialog = remember { mutableStateOf(false) }
    val openDeleteAlertDialog = remember { mutableStateOf(false) }
    val deleteCommunityName = remember { mutableStateOf("") }
    val isDeleteButtonEnabled by derivedStateOf {
        deleteCommunityName.value.trim() == community.name
    }

    // Leave Alert Dialog
    if (openLeaveAlertDialog.value) {
        CommunityDialog(
            onDismissRequest = { openLeaveAlertDialog.value = false },
            onConfirmation = {
                coroutineScope.launch {
                    if (user != null) {
                        openLeaveAlertDialog.value = false
                        val success = communityViewModel.removeUserFromCommunity(
                            communityId = community.id,
                            userId = user.userId
                        )
                        if (success){
                            onRefresh()
                        }
                    }
                    onDismissSheet()
                }
            },
            dialogTitle = "Leave Community?",
            dialogText = if (isLeader || isEditor) {
                "You are a leader / editor in this community. \n Are you sure you want to leave?"
            } else {
                "Are you sure you want to leave"
            },
            icon = Icons.AutoMirrored.Filled.Logout
        )
    }

    // Step Down Alert Dialog
    if (openStepDownAlertDialog.value) {
        CommunityDialog(
            onDismissRequest = { openStepDownAlertDialog.value = false },
            onConfirmation = {
                coroutineScope.launch {
                    if (user != null) {
                        val success = communityViewModel.demoteMemberInCommunity(
                            communityId = community.id,
                            userId = user.userId
                        )
                        if (success){
                            onRefresh()
                        }
                    }

                    openStepDownAlertDialog.value = false
                    onDismissSheet()
                }
            },
            dialogTitle = "Step down from role?",
            dialogText = "Are you sure you want to step down from this role? You will be demoted to a regular member if you proceed",
            icon = Icons.Outlined.GppBad
        )
    }

    // Delete Alert Dialog
    if (openDeleteAlertDialog.value) {
        AlertDialog(
            icon = {
                Icon(Icons.Default.Delete, contentDescription = "Delete Community")
            },
            title = {
                Text("Delete community?")
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete this community? You wouldn't be able to recover it or its spaces later. \n \n \n Type \"${community.name}\" to confirm ",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = deleteCommunityName.value,
                        onValueChange = { deleteCommunityName.value = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            onDismissRequest = {
                openDeleteAlertDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isDeleteButtonEnabled) {
                            coroutineScope.launch {
                                communityViewModel.deleteCommunity(community.id)
                                navigateBack()
                                openDeleteAlertDialog.value = false
                                onDismissSheet()
                            }
                        }
                    },
                    enabled = isDeleteButtonEnabled
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
            text = community.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

            HorizontalDivider()
        if (isLeader) {
            CommunityBottomSheetItem(
                text = "Edit",
                leadingIcon = Icons.Default.Edit,
                onClick = {
                    onNavigateToEditCommunity(community)
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

        if (isLeader || isEditor) {
            CommunityBottomSheetItem(
                text = "Step down from role",
                leadingIcon = Icons.Outlined.GppBad,
                onClick = {
                    openStepDownAlertDialog.value = true
                }
            )
        }

        CommunityBottomSheetItem(
            text = "Leave",
            leadingIcon = Icons.AutoMirrored.Filled.Logout,
            onClick = {
                openLeaveAlertDialog.value = true
            }
        )
        if (isLeader) {
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


@Composable
fun CommunityBottomSheetItem(
    modifier: Modifier = Modifier,
    text: String,
    isDangerZone: Boolean = false,
    leadingIcon: ImageVector,
    onClick: () -> Unit
){
    val contentColor = if (isDangerZone) {
        MaterialTheme.colorScheme.onSurface.copy(
            red = (MaterialTheme.colorScheme.onSurface.red + MaterialTheme.colorScheme.onError.red) / 2,
            green = (MaterialTheme.colorScheme.onSurface.green + MaterialTheme.colorScheme.onError.green) / 2,
            blue = (MaterialTheme.colorScheme.onSurface.blue + MaterialTheme.colorScheme.onError.blue) / 2
        )
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = modifier.alpha(0.8f),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
        )
        Spacer(modifier = modifier.weight(1f))

        Icon(
            modifier = modifier.alpha(0.8f),
            imageVector = leadingIcon,
            contentDescription = text,
            tint = contentColor

        )

    }
}

@Composable
fun CommunityDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector?,
) {
    AlertDialog(
        icon = {
            if (icon != null) {
                Icon(icon, contentDescription = dialogText)
            }
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(
                text = dialogText,
                textAlign = TextAlign.Center
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Preview
@Composable
fun CommunityBottomSheetItemPreview(){
    TheCommunityTheme {
        Column (
            modifier = Modifier
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            CommunityBottomSheetItem(
                text = "Edit",
                leadingIcon = Icons.Default.Edit,
                onClick = {}
            )
            CommunityBottomSheetItem(
                text = "Report",
                leadingIcon = Icons.Default.Flag,
                onClick = {}
            )

            CommunityBottomSheetItem(
                text = "Leave",
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = {}
            )
            CommunityBottomSheetItem(
                text = "Delete",
                leadingIcon = Icons.Default.Delete,
                onClick = {}
            )

            CommunityBottomSheetItem(
                text = "Step down from role",
                leadingIcon = Icons.Outlined.GppBad,
                onClick = {}
            )
        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTopBar(
    community: Community,
    navigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = community.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                navigateBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(Color.Transparent)
    )
}
