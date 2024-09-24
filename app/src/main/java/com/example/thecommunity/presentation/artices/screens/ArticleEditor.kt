package com.example.thecommunity.presentation.artices.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.thecommunity.data.model.Article
import com.example.thecommunity.presentation.artices.ArticleViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleEditor(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (articleId: String) -> Unit,
    isEdit: Boolean,
    communityId: String?,
    spaceId: String?,
    articleViewModel: ArticleViewModel,
    initialArticleId: String? = null
) {
    var initialArticle by remember { mutableStateOf<Article?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf(listOf<Uri>()) }
    var showSaveDraftDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    var isDraftSaved by remember { mutableStateOf(false) } // Flag to track draft save status
    var isSaving by remember { mutableStateOf(false) }

    // Function to fetch data
    fun fetchData() {
        coroutineScope.launch {
            isLoading = true
            initialArticle = initialArticleId?.let { articleViewModel.getArticleById(it) }
            initialArticle?.let {
                title = it.title
                topic = it.topic
                body = it.body ?: ""
                imageUris = it.images.mapNotNull { image ->
                    image["url"]?.let { url -> Uri.parse(url) }
                }
            }
            isLoading = false
        }
    }

    // Fetch event data on first composition
    LaunchedEffect(Unit) {
        fetchData()
    }
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.coroutineContext.cancelChildren()
        }
    }

    if (showSaveDraftDialog){
        // Show save draft dialog
        AlertDialog(
            title = {
                Text(
                    text = "Save to draft"
                )
            },
            text = {
                Text(
                    text = "You and the other editors can come back to this article later to edit it."
                )
            },
            onDismissRequest = {
                showSaveDraftDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val (articleId, success) = articleViewModel.publishArticleToDraft(
                                title = title,
                                communityId = communityId,
                                spaceId = spaceId,
                                body = body,
                                imageUris = imageUris,
                                topic = topic
                            )
                            if (success) {
                                snackBarHostState.showSnackbar("Article saved to draft")
                                if (articleId != null) {
                                    onNavigateToPreview(
                                        articleId
                                    )
                                }
                            } else {
                                snackBarHostState.showSnackbar("Failed to save to draft")
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDraftDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Don't Save")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (title.isNotEmpty()) {
                        Text(
                            text = title
                        )
                    } else {
                        Text(if (!isEdit) "Write article" else "Edit article")

                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isDraftSaved){
                            onNavigateBack()
                        } else {
                            showSaveDraftDialog = true
                        }
                    }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            val (articleId,result) = articleViewModel.publishArticleToDraft(
                                title = title,
                                communityId = communityId,
                                spaceId = spaceId,
                                body = body,
                                imageUris = imageUris,
                                topic = topic
                            )
                            if (result) {
                                isDraftSaved = true
                                isSaving = false
                                snackBarHostState.showSnackbar("Article saved to draft")
                                if (articleId != null) {
                                    onNavigateToPreview(
                                        articleId
                                    )
                                }
                            } else {
                                snackBarHostState.showSnackbar("Failed to save to draft")
                            }
                        }
                    }) {
                        Text(
                            text = "Preview"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )) {
            if ((isEdit && isLoading) || isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                ArticleEditorContent(
                    title = title,
                    onTitleChange = { title = it },
                    body = body,
                    onBodyChange = { body = it },
                    imageUris = imageUris,
                    onImageUrisChange = { imageUris = it },
                    topic = topic,
                    onTopicChange = { topic = it }

                )
            }
        }
    }
}


@Composable
fun ArticleEditorContent(
    modifier: Modifier = Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    topic: String,
    onTopicChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    imageUris: List<Uri>,
    onImageUrisChange: (List<Uri>) -> Unit
){
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            onImageUrisChange(imageUris + uris)
        }
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Title...",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField() // The actual text field content
                }
            }
        )

         BasicTextField(
            value = topic,
            onValueChange = onTopicChange,
            textStyle = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    if (topic.isEmpty()) {
                        Text(
                            text = "Topic...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField() // The actual text field content
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (imageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(imageUris.size) { index ->
                        val uri = imageUris[index]
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { /* Preview or other actions */ },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    // Update imageUris by removing the clicked item
                                    onImageUrisChange(imageUris.toMutableList().apply {
                                        removeAt(index)
                                    })
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        CircleShape
                                    )
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    // Button to add more images
                    item {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Add Images"
                            )
                        }
                    }
                }
            } else {
                // Show button to add images if none are selected
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Add Images"
                    )
                }
            }
        }

        BasicTextField(
            value = body,
            onValueChange = onBodyChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    if (body.isEmpty()) {
                        Text(
                            text = "Body...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField() // The actual text field content
                }
            }

        )
    }
}
