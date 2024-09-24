package com.example.thecommunity.presentation.artices.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.thecommunity.data.model.Article
import com.example.thecommunity.data.model.ArticleDetails
import com.example.thecommunity.presentation.artices.ArticleViewModel
import com.example.thecommunity.presentation.communities.screens.ImageCarousel
import com.example.thecommunity.presentation.events.screen.getTimeAgo
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlePreview(
    articleViewModel: ArticleViewModel,
    articleId: String,
    onNavigateBack: () -> Unit,
){
    val coroutineScope = rememberCoroutineScope()
    var article by remember { mutableStateOf<Article?>(null) }
    var articleDetails by remember { mutableStateOf<ArticleDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val snackBarHostState = remember { SnackbarHostState() }

    fun fetchData() {
        coroutineScope.launch {
            isLoading = true // Set loading to true at the beginning of the fetch
            article = articleViewModel.getArticleById(articleId)
            if (article != null){
                articleDetails = articleViewModel.getArticleDetails(article!!)

            }
            Log.d("ArticleDetails", "Article details details are: $article")

            isLoading = false // Set loading to false once the community is successfully fetched
        }
    }

    // Call fetchData initially
    LaunchedEffect(Unit) {
        fetchData()

        // After 10 seconds, check if the community is still null
        delay(10000)
        if (article == null && isLoading) {
            coroutineScope.coroutineContext.cancelChildren()
            val result = snackBarHostState.showSnackbar(
                message = "Failed to fetch event. Please try again.", actionLabel = "Retry"
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
                    Text(
                        text = "Article Preview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }, navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        modifier = Modifier.fillMaxSize()
    ){innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ){
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            ArticlePreviewContent(
                articleDetails = articleDetails!!,
                articleViewModel = articleViewModel,
                onNavigateBack = onNavigateBack
            )

        }

    }


}

@Composable
fun ArticlePreviewContent(
    articleDetails: ArticleDetails,
    articleViewModel: ArticleViewModel,
    onNavigateBack: () -> Unit
){
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.background(
                color = Color.LightGray.copy(alpha = 0.5f)
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Article Preview"
            )
            Text(
                text = "This is a preview of the article and how it will be displayed on the app."
            )


        }


        ArticleCard(articleDetails = articleDetails)

        Button(
            onClick = {
                coroutineScope.launch {
                    val result = articleViewModel.publishArticleToPublic(
                        articleId = articleDetails.article!!.id
                    )
                    if (result){
                        onNavigateBack()
                    }
                }
            }
        ) {
            Text(text = "Publish article")
        }
    }
}

@Composable
fun ArticleCard(
    articleDetails: ArticleDetails
){
    val timeAgo = articleDetails.article?.timestamp?.let { getTimeAgo(it) }

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // AsyncImage with placeholder and error handling
            if (articleDetails.article?.images?.isNotEmpty() == true) {
                ImageCarousel(images = articleDetails.article!!.images)
            }
        }

        Text(
            text = articleDetails.article!!.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),

        )
        articleDetails.article?.body?.let {
            Text(
                text = it,
                maxLines = 3,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                overflow = TextOverflow.Ellipsis
            )
        }

        if (articleDetails.article?.communityId != null){
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(articleDetails.communityProfileUri)
                        .size(150, 150) // Set the size of the reply profile picture
                        .scale(Scale.FILL)
                        .build(),
                    contentDescription = "profile picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(20.dp) // Smaller profile picture for replies
                )
                Text(
                    text = articleDetails.communityName!!
                )
            }


        }else if (articleDetails.article!!.spaceId != null){
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(articleDetails.spaceProfileUri)
                        .size(150, 150) // Set the size of the reply profile picture
                        .scale(Scale.FILL)
                        .build(),
                    contentDescription = "profile picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(20.dp) // Smaller profile picture for replies
                )
                Text(
                    text = articleDetails.spaceName!!
                )
            }
        }
    }
}