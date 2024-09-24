package com.example.thecommunity.presentation.spaces.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thecommunity.R
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.communities.screens.CommunityBottomSheetItem
import com.example.thecommunity.presentation.communities.screens.CommunityDialog
import com.example.thecommunity.presentation.communities.CommunityViewModel
import com.example.thecommunity.presentation.communities.JoinRequestState
import com.example.thecommunity.presentation.communities.JoinedStatusState
import com.example.thecommunity.presentation.communities.screens.EventsList
import com.example.thecommunity.presentation.events.EventsState
import com.example.thecommunity.presentation.events.EventsViewModel
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SpaceDetails(
    spaceId: String,
    navigateBack: () -> Unit,
    communityViewModel: CommunityViewModel,
    spacesViewModel: SpacesViewModel,
    eventsViewModel: EventsViewModel,
    onNavigateToEditSpace : (Space) -> Unit,
    userRepository: UserRepository,
    navigateToEvent: (eventId: String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToSpaceMembers : (Space) -> Unit
) {
    var user by remember { mutableStateOf<UserData?>(null) }
    var space by remember { mutableStateOf<Space?>(null) }
    val joinedStatusState by spacesViewModel.isJoined.collectAsState()
    val joinRequestState by spacesViewModel.joinRequestState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    val snackBarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        coroutineScope.launch {
            isLoading = true // Set loading to true at the beginning of the fetch
            user = userRepository.getCurrentUser()
            space = spacesViewModel.getSpaceById(spaceId)

            if (space != null) {
                spacesViewModel.startObservingSpaceMembers(spaceId)
                spacesViewModel.startObservingJoinedStatus(user?.userId ?: "", spaceId)
                eventsViewModel.getEventsForSpaces(communityId = null, spaceId = spaceId)

                isLoading = false

            }else{
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
    DisposableEffect(Unit) {
        val job = coroutineScope.launch {
            // Trigger observation of live communities
            fetchData()
        }
        onDispose {
            job.cancel()
        }
    }

    val isLeader = space?.members?.any { member ->
        user?.let { member.containsKey(it.userId) } == true && member[user!!.userId]?.role == "leader"
    }

    // Check if the user is an editor
    val isEditor = space?.members?.any { member ->
        user?.let { member.containsKey(it.userId) } == true && member[user!!.userId]?.role == "editor"
    }


    val isJoined = space?.members?.any { it.containsKey(user?.userId) }
    var refreshTrigger by remember { mutableStateOf(false) } // Trigger for refreshing data


    Scaffold(
        topBar = {
            space?.let {
                SpaceTopBar(
                    space = it,
                    navigateBack = { navigateBack() },
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            space?.let { it1 ->
                if (isLeader != null) {
                    if (isEditor != null) {
                        SpaceDetailsContent(
                            space = it1,
                            user = user,
                            communityViewModel = communityViewModel,
                            spacesViewModel = spacesViewModel,
                            onRefresh = { fetchData() },
                            isLeader = isLeader,
                            isEditor = isEditor,
                            joinedStatusState = joinedStatusState,
                            joinRequestState = joinRequestState,
                            isJoined = isJoined,
                            navigateBack = navigateBack,
                            onNavigateToEditSpace = onNavigateToEditSpace,
                            onNavigateToSpaceMembers = onNavigateToSpaceMembers,
                            eventsViewModel = eventsViewModel,
                            navigateToEvent = navigateToEvent,
                            onNavigateToCreateEvent = onNavigateToCreateEvent
                        )
                    }
                }
            }


        }

    }
}

@Composable
fun SpaceDetailsContent(
    modifier: Modifier = Modifier,
    space: Space,
    isLeader: Boolean,
    isEditor: Boolean,
    user: UserData?,
    isJoined : Boolean?,
    eventsViewModel: EventsViewModel,
    navigateToEvent: (eventId: String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    joinedStatusState: JoinedStatusState,
    joinRequestState: JoinRequestState,
    navigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToEditSpace : (Space) -> Unit,
    communityViewModel: CommunityViewModel,
    spacesViewModel: SpacesViewModel,
    onNavigateToSpaceMembers : (Space) -> Unit
){
    SpaceHeader(
        space = space,
        isLeader = isLeader,
        isEditor = isEditor,
        user = user,
        isJoined = isJoined,
        joinedStatusState = joinedStatusState,
        joinRequestState = joinRequestState,
        navigateBack = navigateBack,
        onRefresh = onRefresh,
        onNavigateToEditSpace = onNavigateToEditSpace,
        communityViewModel = communityViewModel,
        spacesViewModel = spacesViewModel,
        onNavigateToSpaceMembers = onNavigateToSpaceMembers,
        eventsViewModel = eventsViewModel,
        navigateToEvent = navigateToEvent,
        onNavigateToCreateEvent = onNavigateToCreateEvent
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceHeader(
    modifier: Modifier = Modifier,
    space: Space,
    isLeader: Boolean,
    isEditor: Boolean,
    user: UserData?,
    navigateToEvent : (eventId : String) -> Unit,
    onNavigateToCreateEvent : () -> Unit,
    eventsViewModel: EventsViewModel,
    isJoined : Boolean?,
    joinedStatusState: JoinedStatusState,
    joinRequestState: JoinRequestState,
    navigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToEditSpace : (Space) -> Unit,
    communityViewModel: CommunityViewModel,
    spacesViewModel: SpacesViewModel,
    onNavigateToSpaceMembers : (Space) -> Unit
) {

    val members = space.members.size
    val events = space.events.size

    //Modal bottom sheet
    val sheetState = rememberModalBottomSheetState()
    var showSpaceActionsBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isButtonEnabled by remember { mutableStateOf(false) }
    var showEventsBottomSheet by remember { mutableStateOf(false) }

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.BottomCenter,

        ) {
        // AsyncImage with placeholder and error handling
        AsyncImage(
            model = space.bannerUri,
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
                if (space.profileUri != null) {
                    AsyncImage(
                        model = space.profileUri,
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
                 * Space size
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
                                text = "$members",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier.clickable {
                                    onNavigateToSpaceMembers(space)
                                }
                            )

                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = modifier
                                    .alpha(0.8f)
                                    .clickable {
                                        onNavigateToSpaceMembers(space)
                                    }
                            )
                        }

                        Column(
                            modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "$events",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier.clickable {
                                    showEventsBottomSheet = true                                }
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
                    }


                    if (space.approvalStatus == "Live") {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            when (joinedStatusState) {
                                is JoinedStatusState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                                is JoinedStatusState.Error -> {
                                    Snackbar(
                                        action = {
                                            TextButton(onClick = { communityViewModel.startObservingJoinedStatus(user?.userId ?: "", space.id) }) {
                                                Text("Retry")
                                            }
                                        }
                                    ) {
                                        Text((joinedStatusState as JoinedStatusState.Error).message)
                                    }
                                }
                                is JoinedStatusState.Success -> {
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
                                                Text((joinRequestState as JoinRequestState.Error).message)
                                            }
                                        }
                                        else -> {
                                            // Success or Idle
                                            if (!isJoined!!) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            if (user != null && joinRequestState == JoinRequestState.Idle) {
                                                                val success = spacesViewModel.onJoinSpace(user, space)
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
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            } else {
                                                OutlinedButton(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onClick = {
                                                        if (joinRequestState == JoinRequestState.Idle) {
                                                            showSpaceActionsBottomSheet = true
                                                        } else {
                                                            showSpaceActionsBottomSheet = true
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
                    else {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showSpaceActionsBottomSheet = true }
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

    if (showSpaceActionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSpaceActionsBottomSheet = false
            },
            sheetState = sheetState
        ) {
            SpaceBottomSheet(
                isLeader = isLeader,
                isEditor = isEditor,
                space = space,
                navigateBack = navigateBack,
                user = user,
                spacesViewModel = spacesViewModel,
                onRefresh = onRefresh,
                onDismissSheet = { showSpaceActionsBottomSheet = false },
                onNavigateToEditSpace = onNavigateToEditSpace
            )
        }
    }

    if (showEventsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showEventsBottomSheet = false
            },
            sheetState = sheetState
        ) {
            SpaceEventsColumn(
                space = space,
                isLeader = isLeader,
                isEditor = isEditor,
                navigateToEvent = navigateToEvent,
                eventsViewModel = eventsViewModel,
                joinedStatusState = joinedStatusState,
                onNavigateToCreateEvent = onNavigateToCreateEvent
            )
        }
    }

}

@SuppressLint("UnrememberedMutableState")
@Composable
fun SpaceBottomSheet(
    modifier: Modifier = Modifier,
    user: UserData?,
    isLeader: Boolean,
    navigateBack: () -> Unit,
    onDismissSheet: () -> Unit,
    onNavigateToEditSpace : (Space) -> Unit,
    isEditor: Boolean,
    spacesViewModel: SpacesViewModel,
    space: Space,
    onRefresh: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val openLeaveAlertDialog = remember { mutableStateOf(false) }
    val openStepDownAlertDialog = remember { mutableStateOf(false) }
    val openDeleteAlertDialog = remember { mutableStateOf(false) }
    val deleteSpaceName = remember { mutableStateOf("") }
    val isDeleteButtonEnabled by derivedStateOf {
        deleteSpaceName.value.trim() == space.name
    }


    // Leave Alert Dialog
    if (openLeaveAlertDialog.value) {
        CommunityDialog(
            onDismissRequest = { openLeaveAlertDialog.value = false },
            onConfirmation = {
                coroutineScope.launch {
                    if (user != null) {
                        val success = spacesViewModel.removeUserFromSpace(
                            space = space,
                            userId = user.userId
                        )
                        if (success) {
                            onRefresh()
                        }
                    }
                    openLeaveAlertDialog.value = false
                    onRefresh()
                    onDismissSheet()
                }
            },
            dialogTitle = "Leave Space?",
            dialogText = if (isLeader || isEditor) {
                "You are a leader / editor in this space. \n Are you sure you want to leave?"
            } else {
                "Are you sure you want to leave"
            },
            icon = Icons.AutoMirrored.Filled.Logout
        )
    }

     //Step Down Alert Dialog
    if (openStepDownAlertDialog.value) {
        CommunityDialog(
            onDismissRequest = { openStepDownAlertDialog.value = false },
            onConfirmation = {
                coroutineScope.launch {
                    if (user != null) {
                        spacesViewModel.demoteMemberInSpaces(
                            spaceId = space.id,
                            userId = user.userId
                        )
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
                Icon(Icons.Default.Delete, contentDescription = "Delete Space")
            },
            title = {
                Text("Delete Space?")
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete this space? You wouldn't be able to recover it or its spaces later. \n \n \n Type \"${space.name}\" to confirm ",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = deleteSpaceName.value,
                        onValueChange = { deleteSpaceName.value = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },            onDismissRequest = {
                openDeleteAlertDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isDeleteButtonEnabled) {
                            coroutineScope.launch {
                                spacesViewModel.deleteSpace(space)
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
            text = space.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        HorizontalDivider()
        if (isLeader) {
            CommunityBottomSheetItem(
                text = "Edit",
                leadingIcon = Icons.Default.Edit,
                onClick = {
                    coroutineScope.launch {
                        Log.d("SpaceDetails", "Attempting to edit space : ${space.name}")
                        onNavigateToEditSpace(space)

                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceTopBar(
    modifier: Modifier = Modifier,
    space: Space,
    navigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = space.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        navigationIcon = {
            IconButton(onClick = { navigateBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(Color.Transparent)
    )
}

@Composable
fun SpaceEventsColumn(
    modifier: Modifier = Modifier,
    space: Space,
    isLeader : Boolean,
    isEditor: Boolean,
    navigateToEvent: (eventId : String) -> Unit,
    eventsViewModel: EventsViewModel,
    onNavigateToCreateEvent: () -> Unit,
    joinedStatusState: JoinedStatusState
) {
    val eventsState by eventsViewModel.spaceEventsState.collectAsState()
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
            space.approvalStatus == "Live" &&
            joinedStatusState is JoinedStatusState.Success &&
            (joinedStatusState.isJoined)
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
                            text = "There is nothing here yet.",
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