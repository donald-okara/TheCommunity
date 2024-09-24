package com.example.thecommunity.presentation.communities.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.thecommunity.R
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.communities.CommunityViewModel
import com.example.thecommunity.ui.theme.TheCommunityTheme
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMembers(
    modifier: Modifier = Modifier,
    communityId: String,
    communityViewModel: CommunityViewModel,
    navigateBack: () -> Unit,
    userRepository: UserRepository
) {
    var user by remember { mutableStateOf<UserData?>(null) }
    var community by remember { mutableStateOf<Community?>(null) }
    var members by remember { mutableStateOf<List<UserData?>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        coroutineScope.launch {
            try {
                isLoading = true
                user = userRepository.getCurrentUser()
                community = communityViewModel.getCommunityById(communityId)
                community?.let {
                    communityViewModel.fetchCommunityMembers(communityId)
                    communityViewModel.communityMembers.collect { membersList ->
                        members = membersList
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

    val isLeader =
        community?.members?.any { it.containsKey(user?.userId) && it[user?.userId] == "leader" }

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    community?.let {
                        Text(
                            text = it.name + " members",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },


                )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (isLeader != null) {
                community?.let {
                    user?.let { it1 ->
                        MembersList(
                            modifier = modifier.padding(innerPadding),
                            members = members,
                            community = it,
                            currentUser = it1,
                            curUserIsLeader = isLeader,
                            communityViewModel = communityViewModel,
                            onRefresh = {fetchData()}
                        )
                    }
                }
            }

        }
    }

}

@Composable
fun MembersList(
    modifier: Modifier = Modifier,
    members: List<UserData?> = emptyList(),
    community: Community,
    currentUser: UserData,
    curUserIsLeader: Boolean,
    communityViewModel: CommunityViewModel,
    onRefresh: () -> Unit
) {
    val sortedMembers = members
        .filterNotNull()
        .sortedWith(
            compareByDescending<UserData> { it.userId == currentUser.userId } // Current user first
                .thenByDescending { community.members.any { member -> member[it.userId] == "leader" } } // Leaders next
                .thenByDescending { community.members.any { member -> member[it.userId] == "editor" } } // Editors after leaders
                .thenBy { it.username } // Rest of the members sorted by username
        )

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(sortedMembers) { member ->
            MemberItem(
                curUserIsLeader = curUserIsLeader,
                community = community,
                listUser = member,
                currentUser = currentUser,
                communityViewModel = communityViewModel,
                onRefresh = onRefresh
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberItem(
    curUserIsLeader: Boolean,
    community: Community,
    communityViewModel: CommunityViewModel,
    listUser: UserData,
    currentUser: UserData,
    onRefresh: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showMemberBottomSheet by remember { mutableStateOf(false) }
    var showPromoteBottomSheet by remember { mutableStateOf(false) }

    val isListedUserEditor =
        community.members.any { it.containsKey(listUser.userId) && (it[listUser.userId] == "editor") }
    val isListedUserLeader =
        community.members.any { it.containsKey(listUser.userId) && (it[listUser.userId] == "leader") }
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
        supportingContent = {
            if (listUserIsCurrentUser || isListedUserEditor || isListedUserLeader) {
                Text(
                    text = if (listUserIsCurrentUser) "You" else if (isListedUserEditor) "Editor" else "Leader",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            }
        },
        trailingContent = {
            if (curUserIsLeader && !listUserIsCurrentUser) {
                IconButton(onClick = { showMemberBottomSheet = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }
    )

    if (showMemberBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showMemberBottomSheet = false
            },
            sheetState = sheetState
        ) {
            CommunityMembersBottomSheet(
                onDismissSheet = {
                    showMemberBottomSheet = false
                },
                listUser = listUser,
                isListedUserEditor = isListedUserEditor,
                isListedUserLeader = isListedUserLeader,
                isCurrentUserLeader = curUserIsLeader,
                communityViewModel = communityViewModel,
                community = community,
                onRefresh = {
                    onRefresh()
                },
                onOpenPromoteSheet = {showPromoteBottomSheet = true}
            )
        }
    }

    if (showPromoteBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                showPromoteBottomSheet = false
            },
            sheetState = sheetState
        ){
            PromoteUserSheet(
                onPromoteToLeader = {
                    scope.launch {
                        val result = communityViewModel.promoteMemberInCommunity(
                            communityId = community.id,
                            userId = listUser.userId,
                            role = "leader"
                        )

                        if (result){
                            showPromoteBottomSheet = false
                            onRefresh()
                        }
                    }
                },
                onPromoteToEditor = {
                    scope.launch {
                        val result = communityViewModel.promoteMemberInCommunity(
                            communityId = community.id,
                            userId = listUser.userId,
                            role = "editor"
                        )
                        if (result){
                            showPromoteBottomSheet = false
                            onRefresh()
                        }
                    }

                }

                )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun CommunityMembersBottomSheet(
    modifier: Modifier = Modifier,
    onDismissSheet: () -> Unit,
    listUser: UserData,
    isListedUserEditor: Boolean,
    isListedUserLeader: Boolean,
    isCurrentUserLeader : Boolean,
    communityViewModel: CommunityViewModel,
    community: Community,
    onRefresh: () -> Unit,
    onOpenPromoteSheet : () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val openRemoveDialog = remember { mutableStateOf(false) }
    val openDemoteAlertDialog = remember { mutableStateOf(false) }

    // Leave Alert Dialog
    if (openRemoveDialog.value) {
        CommunityDialog(
            onDismissRequest = { openRemoveDialog.value = false },
            onConfirmation = {
                coroutineScope.launch {
                    val success = communityViewModel.removeUserFromCommunity(
                        communityId = community.id,
                        userId = listUser.userId
                    )
                    if (success) {
                        onRefresh()
                    }
                    openRemoveDialog.value = false
                    onRefresh()
                    onDismissSheet()
                }
            },
            dialogTitle = "Remove User?",
            dialogText = if (isListedUserEditor || isListedUserLeader) {
                "The user is a leader / editor in this community. \n Are you sure you want to remove them?"
            } else {
                "Are you sure you want to remove this user?"
            },
            icon = Icons.AutoMirrored.Filled.Logout
        )
    }

    //Step Down Alert Dialog
    if (openDemoteAlertDialog .value) {
        CommunityDialog(
            onDismissRequest = { openDemoteAlertDialog .value = false },
            onConfirmation = {
                coroutineScope.launch {
                    val result = communityViewModel.demoteMemberInCommunity(
                        communityId = community.id,
                        userId = listUser.userId
                    )
                    if (result){
                        openDemoteAlertDialog.value = false
                        onDismissSheet()
                        onRefresh()
                    }
                }

            },
            dialogTitle = "Demote user?",
            dialogText = "Are you sure you want to demote this user?",
            icon = Icons.Outlined.GppBad
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

        if (isCurrentUserLeader){
            CommunityBottomSheetItem(
                text = "Remove",
                leadingIcon = Icons.Default.PersonRemove,
                onClick = {
                    openRemoveDialog.value = true
                }
            )

            if (isListedUserEditor || isListedUserLeader) {
                CommunityBottomSheetItem(
                    text = "Demote",
                    leadingIcon = Icons.Outlined.GppBad,
                    onClick = {
                        openDemoteAlertDialog.value = true
                    }
                )
            }else{
                CommunityBottomSheetItem(
                    text = "Promote",
                    leadingIcon = Icons.Outlined.GppGood,
                    onClick = {
                        onOpenPromoteSheet()
                    }
                )
            }
        }
    }
}

@Composable
fun PromoteUserSheet(
    modifier: Modifier = Modifier,
    onPromoteToLeader : () -> Unit,
    onPromoteToEditor: () -> Unit
){
    val cardWidth = 150.dp // Set a fixed width for the cards

    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Promote User",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.padding(16.dp))
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = modifier
                    .width(cardWidth)
                    .clickable {
                        onPromoteToLeader()
                    }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Promote to leader",
                            modifier = modifier.align(Alignment.Center),
                        )
                    }
                    Text(
                        text = "Leader",
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Card(
                modifier = modifier
                    .width(cardWidth)
                    .clickable {
                        onPromoteToEditor()
                    }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Promote to editor",
                            modifier = modifier.align(Alignment.Center),
                        )
                    }
                    Text(
                        text = "Editor",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

}

@Preview
@Composable
fun PromoteUserSheetPreview(){
    TheCommunityTheme {
        PromoteUserSheet(
            onPromoteToEditor = {},
            onPromoteToLeader = {}
        )
    }
}

@Composable
@Preview
fun MemberItemPreview() {
    TheCommunityTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.placeholder),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp) // Specify a fixed size to avoid layout recalculations
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = "Unknown",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Editor",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            )

            Spacer(modifier = Modifier.padding(8.dp))

            IconButton(onClick = {  }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
        }

    }
}