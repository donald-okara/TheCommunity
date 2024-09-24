package com.example.thecommunity.presentation.communities.screens

import android.util.Log
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
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.communities.CommunityViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommunityScreen(
    viewModel: CommunityViewModel,
    communityId: String,
    navigateBack: () -> Unit
) {
    // Convert community members to UserData
    var selectedLeaders by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var selectedEditors by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var community by remember { mutableStateOf<Community?>(null) }


    var showBottomSheet by remember { mutableStateOf(false) }
    var roleToAdd by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            community = viewModel.getCommunityById(communityId)
        }

        Log.d("EditCommunity", "Editing community: ${community?.name}")

    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if (community != null)"Edit ${community?.name}" else "Loading") },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        community?.let{
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
                community = community,
                isEdit = true

            )
        }
    }
}



