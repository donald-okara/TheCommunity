package com.example.thecommunity.presentation.spaces.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.thecommunity.data.model.Space
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.spaces.SpacesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSpaceScreen(
    viewModel: SpacesViewModel,
    spaceId: String,
    navigateBack: () -> Unit
) {

    // Convert community members to UserData
    var selectedLeaders by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var selectedEditors by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var space by remember { mutableStateOf<Space?>(null) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var roleToAdd by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            space = viewModel.getSpaceById(spaceId)
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if (space != null) "Edit"+ " ${space?.name}" else "Loading") },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        space?.let {
            RequestSpaceForm(
                modifier = Modifier.padding(innerPadding),
                spacesViewModel = viewModel,
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
                parentCommunityId = it.parentCommunity,
                space = space,
                isEdit = true,


            )
        }
    }

}