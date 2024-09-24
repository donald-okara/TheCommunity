package com.example.thecommunity.presentation.spaces.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.thecommunity.R
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.communities.screens.EditBannerImageBottomSheet
import com.example.thecommunity.presentation.communities.screens.EditProfileImageBottomSheet
import com.example.thecommunity.presentation.communities.RequestStatus
import com.example.thecommunity.presentation.communities.screens.UserInputChip
import com.example.thecommunity.presentation.communities.screens.UserItem
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSpaceScreen(
    viewModel: SpacesViewModel,
    navigateBack: () -> Unit,
    communityId: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Trigger observation of live communities
            viewModel.fetchUsers()
        }
    }

    var selectedLeaders by remember { mutableStateOf(emptyList<UserData>()) }
    var selectedEditors by remember { mutableStateOf(emptyList<UserData>()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    rememberCoroutineScope()
    var roleToAdd by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Space request") },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        RequestSpaceForm(
            modifier = Modifier.padding(innerPadding),
            spacesViewModel = viewModel,
            parentCommunityId = communityId,
            navigateBack = navigateBack,
            selectedLeaders = selectedLeaders,
            selectedEditors = selectedEditors,
            onLeadersChanged = { updatedLeaders -> selectedLeaders = updatedLeaders },
            onEditorsChanged = { updatedEditors -> selectedEditors = updatedEditors },
            onAddLeader = {
                roleToAdd = "Leader"
                showBottomSheet = true
            },
            onAddEditor = {
                roleToAdd = "Editor"
                showBottomSheet = true
            },
            space = null
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            UserSelectionBottomSheet(
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false },
                selectedLeaders = selectedLeaders,
                selectedEditors = selectedEditors,
                communityId = communityId,
                onUserSelected = { user ->
                    when (roleToAdd) {
                        "Leader" -> {
                            selectedLeaders = selectedLeaders + user
                            Log.d("RequestCommunityScreen", "Selected Leader: ${user.username}")
                        }

                        "Editor" -> {
                            selectedEditors = selectedEditors + user
                            Log.d("RequestCommunityScreen", "Selected Editor: ${user.username}")
                        }
                    }
                    showBottomSheet = false
                    Log.d("RequestSpaceScreen", "Selected Leader: ${user.username}")
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RequestSpaceForm(
    modifier: Modifier = Modifier,
    isEdit : Boolean = false,
    spacesViewModel: SpacesViewModel,
    navigateBack: () -> Unit,
    selectedLeaders: List<UserData>,
    selectedEditors: List<UserData>,
    parentCommunityId: String,
    onLeadersChanged: (List<UserData>) -> Unit,
    onEditorsChanged: (List<UserData>) -> Unit,
    onAddLeader: () -> Unit,
    onAddEditor: () -> Unit,
    space : Space?
) {
    val scrollState = rememberScrollState()
    val requestStatus by spacesViewModel.requestStatus.collectAsState()
    var spaceName by remember { mutableStateOf(space?.name?:"") }
    var description by remember { mutableStateOf(space?.description?:"") }
    var profilePictureUri by remember { mutableStateOf(space?.profileUri) }
    var bannerUri by remember { mutableStateOf(space?.bannerUri) }
    var membersRequireApproval by remember { mutableStateOf(space?.membersRequireApproval?: false) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val launcherBanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { bannerUri = it.toString() }
        }
    )
    val launcherProfile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { profilePictureUri = it.toString() }
        }

    )

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBannerBottomSheet by remember { mutableStateOf(false) }
    var showProfileBottomSheet by remember { mutableStateOf(false) }
    val confirmDialogState = remember { mutableStateOf(false) }
    // Confirmation Dialog
    if (confirmDialogState.value) {
        AlertDialog(
            onDismissRequest = { confirmDialogState.value = false },
            title = { Text("Confirm Edit") },
            text = { Text("Are you sure you want to update this space?") },
            confirmButton = {
                Button(
                    onClick = {
                        if(isEdit){
                            coroutineScope.launch {
                                Log.d("EditSpace","Editing Space")
                                Toast.makeText(context, "Editing Space", Toast.LENGTH_SHORT).show()

                                spacesViewModel.editSpace(
                                    spaceId = space?.id ?: "",
                                    newSpaceName = spaceName,
                                    parentCommunityId = space?.parentCommunity ?: "",
                                    newProfileUri = profilePictureUri?.toUri(),
                                    newBannerUri = bannerUri?.toUri(),
                                    newAboutUs = description,
                                    newRequiresApproval = membersRequireApproval
                                )

                                navigateBack()
                            }
                        }
                    }
                ){
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {confirmDialogState.value = false}
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp), // Adds space between elements
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        // Banner and Profile Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (bannerUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(bannerUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showBannerBottomSheet = true }
                )
            } else {
                Image(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { launcherBanner.launch("image/*") },
                    contentScale = ContentScale.Crop,
                    painter = painterResource(id = R.drawable.placeholder),
                    contentDescription = "Select community banner"
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            )
            // Semi-transparent overlay
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape),
            ){
                // Profile Image
                if (profilePictureUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePictureUri),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(128.dp)
                            .clickable { showProfileBottomSheet = true }
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.placeholder),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .clickable { launcherProfile.launch("image/*") }
                            .size(128.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = spaceName,
            onValueChange = { spaceName = it },
            label = { Text("Space Name") },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1
        )

        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = { description = it },
            maxLines = 10,
            label = { Text("Space description") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
            )
        )

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = membersRequireApproval,
                onCheckedChange = { membersRequireApproval = it }
            )
            Text(text = "Members require approval to join")
        }

        HorizontalDivider()

        // Chips for Leaders
        if(!isEdit){
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedLeaders.forEach { user ->
                    UserInputChip(
                        user = user,
                        onRemove = { onLeadersChanged(selectedLeaders.filter { it != user }) })
                }
                Button(
                    onClick = { onAddLeader() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Add Leader"
                        )
                        Text("Add Leader")
                    }
                }
            }

            HorizontalDivider()

            // Chips for Editors
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedEditors.forEach { user ->
                    UserInputChip(
                        user = user,
                        onRemove = { onEditorsChanged(selectedEditors.filter { it != user }) })

                }
                Button(
                    onClick = { onAddEditor() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Add Editor"
                        )
                        Text("Add Editor")
                    }
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = {
                coroutineScope.launch {
                    if (isEdit){
                        confirmDialogState.value = true
                    }else{
                        Log.d("CreateSpace","Creating Space")
                        Toast.makeText(context, "Creating Space", Toast.LENGTH_SHORT).show()

                        spacesViewModel.requestNewSpace(
                            parentCommunityId = parentCommunityId,
                            spaceName = spaceName,
                            profilePictureUri = profilePictureUri?.toUri(),
                            bannerUri = bannerUri?.toUri(),
                            description = description,
                            membersRequireApproval = membersRequireApproval,
                            selectedLeaders = selectedLeaders,
                            selectedEditors = selectedEditors
                        )
                        navigateBack()
                    }
                    
                }

            },
            modifier = Modifier.fillMaxWidth(),
            enabled = spaceName.isNotEmpty()
        ) {
            Text(if (isEdit)"Update Space" else "Request Space")
        }
        if(showBannerBottomSheet){
            ModalBottomSheet(
                onDismissRequest = { showBannerBottomSheet = false},
                sheetState = sheetState
            ){
                EditBannerImageBottomSheet(
                    onSelectImage = { launcherBanner.launch("image/*") },
                    onRemoveImage = { bannerUri = null },
                    onDismiss = { showBannerBottomSheet = false }
                )
            }
        }

        if (showProfileBottomSheet){
            ModalBottomSheet(
                onDismissRequest = { showProfileBottomSheet = true },
                sheetState = sheetState
            ) {
                EditProfileImageBottomSheet(
                    onSelectImage = { launcherProfile.launch("image/*") },
                    onRemoveImage = { profilePictureUri = null },
                    onDismiss = {showProfileBottomSheet = false}
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (requestStatus) {
        RequestStatus.Success -> {
            Text(text = "Space requested successfully", color = MaterialTheme.colorScheme.primary)
            spacesViewModel.clearRequestStatus()
        }

        is RequestStatus.Error -> {
            Text(
                text = (requestStatus as RequestStatus.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> { /* Do nothing */
        }
    }

}

@Composable
fun UserSelectionBottomSheet(
    viewModel: SpacesViewModel,
    communityId: String,
    onDismiss: () -> Unit,
    selectedLeaders: List<UserData>,
    selectedEditors: List<UserData>,
    onUserSelected: (UserData) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Fetch users when the bottom sheet is displayed
    LaunchedEffect(Unit) {
        viewModel.fetchCommunityUsers(communityId)
    }

    val users by viewModel.users.collectAsState(emptyList())
    val filteredUsers = users.filterNot { user ->
        selectedLeaders.contains(user)
    }

    LazyColumn {
        items(filteredUsers) { user ->
            UserItem(user = user) {
                onUserSelected(user)
            }
        }
    }
}
