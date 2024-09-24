package com.example.thecommunity.presentation.communities.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.communities.CommunityViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCommunityScreen(
    viewModel: CommunityViewModel,
    navigateBack: () -> Unit
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
    val scope = rememberCoroutineScope()
    var roleToAdd by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Community request") },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        CommunityRequestForm(
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel,
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
            community = null,

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
                roleToAdd = roleToAdd, // Pass the role to add
                selectedLeaders = selectedLeaders,
                selectedEditors = selectedEditors,
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
                }
            )
        }
    }
}




@OptIn( ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityRequestForm(
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel,
    isEdit: Boolean = false,
    navigateBack: () -> Unit,
    selectedLeaders: List<UserData>,
    selectedEditors: List<UserData>,
    onLeadersChanged: (List<UserData>) -> Unit,
    onEditorsChanged: (List<UserData>) -> Unit,
    onAddLeader: () -> Unit,
    community: Community?,
    onAddEditor: () -> Unit,
){
    var name by remember { mutableStateOf(community?.name ?: "") }
    var type by remember { mutableStateOf(community?.type ?: "Campus") }
    var bannerImageUri by remember { mutableStateOf(community?.communityBannerUrl) }
    var profileImageUri by remember { mutableStateOf(community?.profileUrl) }
    var aboutUs by remember { mutableStateOf(community?.aboutUs ?: "") }

    val coroutineScope = rememberCoroutineScope() // Define the CoroutineScope
    val scrollState = rememberScrollState()

    val confirmDialogState = remember { mutableStateOf(false) }
    // Confirmation Dialog
    val context = LocalContext.current
    if (confirmDialogState.value) {
        AlertDialog(
            onDismissRequest = { confirmDialogState.value = false },
            title = { Text("Confirm Edit") },
            text = { Text("Are you sure you want to update this community?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            coroutineScope.launch {
                                navigateBack() // Navigate back on success

                                try {
                                    if (isEdit) {
                                        viewModel.editCommunity(
                                            communityId = community?.id ?: "",
                                            communityName = name,
                                            communityType = type,
                                            bannerUri = bannerImageUri?.toUri(),
                                            profileUri = profileImageUri?.toUri(),
                                            aboutUs = aboutUs
                                        )
                                    } else {
                                        viewModel.requestNewCommunity(
                                            communityName = name,
                                            communityType = type,
                                            bannerUri = bannerImageUri?.toUri(),
                                            profileUri = profileImageUri?.toUri(),
                                            selectedLeaders = selectedLeaders,
                                            selectedEditors = selectedEditors,
                                            aboutUs = aboutUs
                                        )
                                    }
                                    navigateBack() // Navigate back on success
                                } catch (e: Exception) {
                                    // Handle the error, e.g., show a toast or snack bar

                                }
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { confirmDialogState.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val launcherBanner = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { bannerImageUri = it.toString() }
        }
    )
    val launcherProfile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { profileImageUri = it.toString() }
        }

    )
    //Modal bottom sheet
    val sheetState = rememberModalBottomSheetState()
    var showBannerBottomSheet by remember { mutableStateOf(false) }
    var showProfileBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween, // Adjust spacing as needed
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image and Banner Section
        Box(
            modifier = Modifier
                .fillMaxWidth()

                .height(256.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (bannerImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(bannerImageUri),
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
            // Semi-transparent overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            )
            // Profile Image
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape),
            ){
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .clickable {
                                showProfileBottomSheet = true
                            }
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.placeholder),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .clickable {
                                launcherProfile.launch("image/*")
                            }
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }


        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChange = { name = it },
            label = { Text("Community Name") },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1
        )

        HorizontalDivider()

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = aboutUs,
            maxLines = 10,
            onValueChange = { aboutUs = it },
            label = { Text("Community description") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
            )
        )

        HorizontalDivider()

        Column {
            Text(
                "Community Type",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                RadioButton(
                    selected = type == "Campus",
                    onClick = { type = "Campus" }
                )
                Text(text = "Campus", modifier = Modifier.padding(start = 8.dp))

                Spacer(modifier = Modifier.weight(1f))

                RadioButton(
                    selected = type == "Church",
                    onClick = { type = "Church" }
                )
                Text(text = "Church", modifier = Modifier.padding(start = 8.dp))
            }
        }

        HorizontalDivider()

        if (!isEdit) { // Chips for Leaders
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedLeaders.forEach { user ->
                    UserInputChip(
                        user = user,
                        onRemove = { onLeadersChanged(selectedLeaders.filter { it != user }) }
                    )
                }
                Button(onClick = { onAddLeader() }) {
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
                        onRemove = { onEditorsChanged(selectedEditors.filter { it != user }) }
                    )
                }
                Button(onClick = { onAddEditor() }) {
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
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (name.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            if (isEdit) {
                                confirmDialogState.value = true
                            } else {
                                viewModel.requestNewCommunity(
                                    communityName = name,
                                    communityType = type,
                                    bannerUri = bannerImageUri?.toUri(),
                                    profileUri = profileImageUri?.toUri(),
                                    selectedLeaders = selectedLeaders,
                                    selectedEditors = selectedEditors,
                                    aboutUs = aboutUs
                                )
                                navigateBack() // Navigate back on success

                            }
                        } catch (e: Exception) {
                            // Handle the error, e.g., show a toast or snack bar
                            Toast.makeText(
                                context,
                                "Request failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Handle validation error (e.g., show a toast or snackbar)
                    Toast.makeText(context, "Please enter a community name", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            enabled = name.isNotEmpty()
        ) {
            Text(if (isEdit) "Update Community" else "Request Community")
        }
    }

    if (showBannerBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBannerBottomSheet = false },
            sheetState = sheetState
        ) {
            EditBannerImageBottomSheet(
                onSelectImage = { launcherBanner.launch("image/*") },
                onRemoveImage = { bannerImageUri = null },
                onDismiss = { showBannerBottomSheet = false }
            )
        }
    }

    if (showProfileBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileBottomSheet = false },
            sheetState = sheetState
        ) {
            EditProfileImageBottomSheet(
                onSelectImage = { launcherProfile.launch("image/*") },
                onRemoveImage = { profileImageUri = null },
                onDismiss = { showProfileBottomSheet = false }
            )
        }
    }

}


@Composable
fun UserSelectionBottomSheet(
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit,
    selectedLeaders: List<UserData>,
    selectedEditors: List<UserData>,
    roleToAdd: String, // Pass the role to add
    onUserSelected: (UserData) -> Unit
) {
    val users by viewModel.users.collectAsState(emptyList())
    // Filter out already selected users
    val filteredUsers = users.filterNot { user ->
        selectedLeaders.contains(user) || selectedEditors.contains(user)
    }
    // No need for role selection UI in the bottom sheet
    LazyColumn {
        items(filteredUsers) { user ->
            UserItem(user = user) {
                onUserSelected(user) // Pass the user and role
            }
        }
    }
}



@Composable
fun UserItem(user: UserData, onUserSelected: (UserData) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .clickable { onUserSelected(user) }
            .padding(16.dp)
    ) {
        if (user.profilePictureUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(user.profilePictureUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp) // Specify a fixed size to avoid layout recalculations
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = user.username ?: "Unknown",
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


@Composable
fun UserInputChip(
    modifier: Modifier = Modifier,
    user: UserData,
    onRemove: (UserData) -> Unit
) {
    val displayName = user.username?.take(8) ?: "Unknown"

    InputChip(
        avatar =
        {
            Image(
                painter = rememberAsyncImagePainter(model = user.profilePictureUrl),
                contentDescription = null,
                modifier = modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        ,
        selected = false,
        onClick = { onRemove(user) },
        label = {
            Text(
                text = displayName,
            )
        },
        trailingIcon = {
            IconButton(onClick = { onRemove(user) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun EditImageBottomSheet(
    modifier: Modifier = Modifier,
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = {
            onSelectImage()
            onDismiss()
        }
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Image")
        }

        Spacer(modifier = modifier.weight(1f))

        IconButton(onClick = {
            onRemoveImage()
            onDismiss()
        }
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = "Remove Image")
        }
    }
}

@Composable
fun EditProfileImageBottomSheet(
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit
) {
    EditImageBottomSheet(
        onSelectImage = onSelectImage,
        onRemoveImage = onRemoveImage,
        onDismiss = onDismiss
    )
}

@Composable
fun EditBannerImageBottomSheet(
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDismiss: () -> Unit
) {
    EditImageBottomSheet(
        onSelectImage = onSelectImage,
        onRemoveImage = onRemoveImage,
        onDismiss = onDismiss
    )
}