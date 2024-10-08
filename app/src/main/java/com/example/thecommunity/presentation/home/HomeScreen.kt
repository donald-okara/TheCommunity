package com.example.thecommunity.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.thecommunity.R
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.domain.CommunityApplication
import com.example.thecommunity.ui.theme.TheCommunityTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    userData: UserData?,
    onDarkModeToggle: (Boolean) -> Unit,
    navigateToCommunities: () -> Unit,
) {
    //var userData by remember { mutableStateOf<UserData?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Fetch the initial theme mode from ThemeManager
    val context = LocalContext.current
    val themeManager = (context.applicationContext as CommunityApplication).themeManager
    var isDarkMode by remember { mutableStateOf(themeManager.getInitialThemeMode()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HomeDrawerContent(
                    modifier = Modifier,
                    onNavigateToProfile = { onNavigateToProfile() },
                    navigateToCommunities = navigateToCommunities,
                    drawerState = drawerState,
                    onDarkModeToggle = { newMode ->
                        isDarkMode = newMode
                        onDarkModeToggle(newMode)
                    },
                    userData = userData,
                    isDarkMode = isDarkMode
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),

            topBar = {
                HomeTopBar(
                    scrollBehavior = scrollBehavior,
                    onShowDrawer = { scope.launch { drawerState.open() } },
                    isDarkMode = isDarkMode
                )
            }
        ) {innerPadding->
            PageInDevelopment(modifier = modifier
                .padding(innerPadding))
        }
    }
}

@Composable
fun HomeDrawerContent(
    userData: UserData?,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    navigateToCommunities: () -> Unit,
    isDarkMode: Boolean,
    drawerState: DrawerState,
){
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val themeManager = (context.applicationContext as CommunityApplication).themeManager

    val icon = if (isDarkMode) Icons.Filled.DarkMode else Icons.Outlined.LightMode
    val iconDescription = if (isDarkMode) "Dark Mode" else "Light Mode"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        DrawerHeader(
            userData = userData,
            onNavigateToProfile = onNavigateToProfile
            )

        Spacer(modifier = modifier.height(16.dp))

        HorizontalDivider(
            modifier = modifier.fillMaxWidth(),
            thickness = 2.dp
        )

        Spacer(modifier = modifier.height(16.dp))

        DrawerItem(
            itemIcon = Icons.Outlined.Workspaces,
            label = R.string.communities,
            modifier = modifier.clickable {
                coroutineScope.launch {
                    navigateToCommunities()
                    drawerState.close()
                }
            }
        )

        Spacer(modifier = modifier.height(16.dp))

        HorizontalDivider(
            modifier = modifier.fillMaxWidth(),
            thickness = 2.dp
        )

        Spacer(modifier = modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription
            )
            Spacer(modifier = Modifier.width(16.dp)) // Adjust space between the icon and text

            Switch(
                checked = isDarkMode,
                onCheckedChange = {
                    onDarkModeToggle(it)
                }
            )
        }

    }
}

@Composable
fun DrawerHeader(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit,
    userData: UserData?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onNavigateToProfile()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (userData?.profilePictureUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userData.profilePictureUrl)
                    .size(200, 200) // Set the size of the image
                    .scale(Scale.FILL)
                    .build(),
                contentDescription = "Profile picture",
                modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile picture",
                modifier
                    .size(64.dp)
                    .clip(CircleShape),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (userData?.username != null) {
            Text(
                text = userData.username!!,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

}


@Composable
fun DrawerItem(
    modifier: Modifier = Modifier,
    itemIcon: ImageVector,
    label: Int,
    badgeCount: Int? = null,

) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = itemIcon,
            contentDescription = stringResource(id = label),
            modifier.size(24.dp) // Size the icon appropriately
        )

        Spacer(modifier = Modifier.width(16.dp)) // Adjust space between the icon and text

        // Box for text and badge
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.weight(1f))
            // Add badge only if badgeCount is not null and greater than 0
            badgeCount?.takeIf { it > 0 }?.let {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp) // Padding to avoid overlap
                ) {
                    Badge(
                        content = { Text(it.toString()) },
                        modifier = Modifier.size(24.dp) // Fixed badge size
                    )
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior : TopAppBarScrollBehavior,
    onShowDrawer : () -> Unit,
    isDarkMode: Boolean
){
    CenterAlignedTopAppBar(
        title = {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier
                    .fillMaxHeight()
                    .size(100.dp)
                )
        },
        navigationIcon = {
            IconButton(onClick = { onShowDrawer() }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open drawer"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search"
                )
            }
        },
        scrollBehavior = scrollBehavior,

        )
}

@Composable
fun PageInDevelopment(modifier: Modifier = Modifier){
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Page is still in development",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
@Preview
fun PageInDevelopmentPreview(){
    TheCommunityTheme {
        PageInDevelopment()
    }
}

