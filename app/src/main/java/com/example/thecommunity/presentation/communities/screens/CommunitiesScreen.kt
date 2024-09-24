package com.example.thecommunity.presentation.communities.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.data.repositories.UserRepository
import com.example.thecommunity.presentation.communities.CommunityState
import com.example.thecommunity.presentation.communities.CommunityViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    communityViewModel: CommunityViewModel,
    navigateBack: () -> Unit,
    userRepository: UserRepository,
    navigateToAddCommunity: () -> Unit,
    navigateToPendingCommunityDetails : (Community) -> Unit,
    navigateToCommunityDetails: (Community) -> Unit // Add this parameter
) {
    val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserData?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Trigger observation of live communities
            user = userRepository.getCurrentUser()

            if (user != null) {
                communityViewModel.fetchPendingRequestsForUser(userId = user!!.userId)
            }
        }
    }

    Scaffold (
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Communities",
                        style = MaterialTheme.typography.titleLarge
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

                actions = {
                    IconButton(onClick = { navigateToAddCommunity() }) {
                        Icon(
                            imageVector = Icons.Filled.GroupWork,
                            contentDescription = "Add group"
                        )
                    }
                }

            )
        },
        modifier = Modifier.fillMaxSize()
    ) {innerPadding->
        CommunitiesScreenContent(
            modifier = Modifier.padding(innerPadding),
            communityViewModel = communityViewModel,
            user = user,
            navigateToCommunityDetails = navigateToCommunityDetails,
            navigateToPendingCommunityDetails = navigateToPendingCommunityDetails,
            navigateToAddCommunity = navigateToAddCommunity
        )
    }
}

@Composable
fun CommunitiesScreenContent(
    modifier: Modifier = Modifier,
    user : UserData?,
    navigateToAddCommunity: () -> Unit,
    navigateToCommunityDetails: (Community) -> Unit,
    navigateToPendingCommunityDetails : (Community) -> Unit, // Add this parameter
    communityViewModel: CommunityViewModel
){
    val communityState by communityViewModel.communityState.collectAsState()
    // Create a SwipeRefreshState to manage the refresh state
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var itemCount by remember { mutableIntStateOf(15) }

    fun refresh() = refreshScope.launch {
        refreshing = true
        delay(1500)
        itemCount += 5
        refreshing = false
    }
    LaunchedEffect(refreshing) {
        if (user != null) {
            communityViewModel.fetchLiveCommunities()
            communityViewModel.fetchPendingRequestsForUser(userId = user.userId)

        }

    }
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when(communityState){
            is CommunityState.Loading ->{
                CircularProgressIndicator()
            }
            is CommunityState.Error ->{
                Column{
                    Text(
                        modifier = modifier.clickable {
                            refresh()
                        },
                        text = "We could not fetch your communities at this time." +"\nTry turning on your internet connection and try again. "

                    )

                }
            }
            is CommunityState.Success ->{
                val userCommunities = (communityState as CommunityState.Success).communities.filter { community ->
                    user?.communities?.any { it.containsKey(community.id)} == true
                }
                val communities = (communityState as CommunityState.Success).communities

                CommunitiesList(
                    user = user,
                    userCommunities = userCommunities,
                    communities = communities,
                    communityViewModel = communityViewModel,
                    navigateToCommunityDetails = navigateToCommunityDetails,
                    refresh = ::refresh,
                    navigateToAddCommunity = navigateToAddCommunity,
                    navigateToPendingCommunityDetails = navigateToPendingCommunityDetails
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommunitiesList(
    modifier: Modifier = Modifier,
    user: UserData?,
    refresh : () -> Unit,
    userCommunities: List<Community>,
    communities: List<Community>,
    navigateToAddCommunity: () -> Unit,
    communityViewModel: CommunityViewModel,
    navigateToCommunityDetails: (Community) -> Unit,
    navigateToPendingCommunityDetails : (Community) -> Unit
) {
    // State to manage refreshing
    val pendingState by communityViewModel.pendingState.collectAsState()

    val refreshScope = rememberCoroutineScope()
    val refreshing by remember { mutableStateOf(false) }
    val itemCount by remember { mutableIntStateOf(15) }
    var showAll by remember { mutableStateOf(false) } // State to manage whether all communities are shown

    val state = rememberPullRefreshState(refreshing, refresh)
    val scrollState = rememberScrollState()
    var isPendingExpanded by remember { mutableStateOf(false) }
    val pendingCommunities = when (pendingState) {
        is CommunityState.Success -> (pendingState as CommunityState.Success).communities
        else -> emptyList() // Handle other states where there are no communities to display
    }

    // List of communities that the user has not joined
    val notJoinedCommunities = communities.filterNot { userCommunities.contains(it) }

    // List of communities to display based on the `showAll` state
    val displayedCommunities = if (showAll) {
        communities.sortedByDescending { community ->
            userCommunities.contains(community)
        }
    } else {
        userCommunities
    }
    if (userCommunities.isEmpty()){
        showAll = true
    }


    Box(
        modifier = Modifier
            .pullRefresh(state)
            .fillMaxSize()
    ) {
        if (communities.isEmpty()){
            Text(
                modifier = modifier
                    .clickable {navigateToAddCommunity()}
                    .align(Alignment.Center),
                text = "No communities yet. \n Would you like to create one?",
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
        ) {
            // Header item
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (showAll || communities.isEmpty()) "All communities" else "Your communities",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (notJoinedCommunities.isNotEmpty()) {
                        Text(
                            text = if (showAll) "Show less" else "View all",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.clickable { showAll = !showAll }
                        )
                    }
                }
            }



            // Display user or all communities
            items(displayedCommunities.take(itemCount)) { community ->
                CommunityItem(
                    navigateToCommunityDetails = navigateToCommunityDetails,
                    community = community,
                    isJoined = userCommunities.contains(community)
                )
            }

            // Handle pending state
            when (pendingState) {
                is CommunityState.Loading -> {
                    item {
                        CircularProgressIndicator()
                    }
                }
                is CommunityState.Error -> {
                    item {
                        Column {
                            Text(
                                modifier = modifier.clickable { refresh() },
                                text = "Could not fetch your pending requests"
                            )
                        }
                    }
                }
                is CommunityState.Success -> {

                    if (pendingCommunities.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isPendingExpanded) "Hide pending requests" else "Your pending requests",
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.clickable { isPendingExpanded = !isPendingExpanded }
                            )
                        }

                        if (isPendingExpanded) {
                            // Display pending communities
                            items(pendingCommunities) { community ->
                                PendingCommunityItem(
                                    navigateToCommunityDetails = navigateToPendingCommunityDetails,
                                    community = community
                                )
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing,
            state,
            Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface
        )
    }
}




@Composable
fun CommunityItem(
    modifier: Modifier = Modifier,
    navigateToCommunityDetails: (Community) -> Unit,
    community: Community,
    isJoined: Boolean // Add this parameter to indicate if the community is in the user's list
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(8.dp)
            .clickable { navigateToCommunityDetails(community) }
    ) {
        Card(
            modifier = modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    // AsyncImage with placeholder and error handling
                    AsyncImage(
                        model = community.communityBannerUrl,
                        //placeholder = rememberAsyncImagePainter(model = community.bannerThumbnailUrl),
                        contentDescription = "Community Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Semi-transparent overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
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
                            AsyncImage(
                                model = community.profileUrl,
                                contentDescription = "Community Profile",
                                contentScale = ContentScale.Crop,
                                //placeholder = rememberAsyncImagePainter(model = community.profileThumbnailUrl),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = community.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    // Add "Joined" text if the user is part of the community
                                    if (isJoined) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            modifier = modifier.alpha(0.5f),
                                            text = "Joined",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = community.type,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }

                        if (community.aboutUs != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = community.aboutUs!!,
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
}


@Composable
fun PendingCommunityItem(
    modifier: Modifier = Modifier,
    navigateToCommunityDetails: (Community) -> Unit, // Add this parameter
    community: Community
) {
    Box(
        modifier = modifier
            .width(256.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(8.dp)
            .clickable { navigateToCommunityDetails(community) },

        ) {
        Card(
            modifier = modifier
                .fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    // AsyncImage with placeholder and error handling
                    AsyncImage(
                        model = community.communityBannerUrl,
                        //placeholder = rememberAsyncImagePainter(model = community.bannerThumbnailUrl), // Replace with your placeholder drawable resource
                        contentDescription = "Community Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )


                    // Semi-transparent overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f))
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
                            AsyncImage(
                                model = community.profileUrl,
                                contentDescription = "Community Profile",
                                contentScale = ContentScale.Crop,
                                //placeholder = rememberAsyncImagePainter(model = community.profileThumbnailUrl), // Replace with your placeholder drawable resource
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                                    .alpha(0.5f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = community.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = community.type,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }

                        if (community.aboutUs != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = community.aboutUs!!,
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
}

