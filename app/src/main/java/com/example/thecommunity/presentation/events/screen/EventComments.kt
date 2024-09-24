package com.example.thecommunity.presentation.events.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.thecommunity.data.model.CommentWithDetails
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.ReplyWithDetails
import com.example.thecommunity.data.model.UserData
import com.example.thecommunity.presentation.events.EventsViewModel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentsBottomSheet(
    currUser: UserData,
    eventsViewModel: EventsViewModel,
    event: Event,
    onRefresh: () -> Unit
) {
    val comments by eventsViewModel.eventComments.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var replyId by remember { mutableStateOf("") }
    var commentId by remember { mutableStateOf("") }
    var replyToComment by remember { mutableStateOf<CommentWithDetails?>(null) }
    var isEditingComment by remember { mutableStateOf(false) } // Track if editing a comment
    var isEditingReply by remember { mutableStateOf(false) } // Track if editing a reply
    val bringIntoViewRequester = BringIntoViewRequester()
    val isUserMuted = event.attendees?.any {
        it.keys.contains(currUser.userId).and(it[currUser.userId]?.muted == true)
    } == true

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Get insets to handle keyboard visibility
    val insets = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = insets.calculateBottomPadding()

    fun resetCommentInput() {
        commentText = ""
        replyToComment = null
        replyId = ""
        commentId = ""
        isEditingComment = false
        isEditingReply = false
    }

    // Handle comment input focus
    LaunchedEffect(replyToComment) {
        if (replyToComment != null) {
            focusRequester.requestFocus()

        }

    }
    DisposableEffect(Unit) {
        // Ensure focusRequester is properly set before the view gains focus
        onDispose { }
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .padding(bottom = bottomPadding) // Apply bottom padding to account for the keyboard
            .imePadding(),  // Add padding to adjust for the keyboard
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .padding(bottom = bottomPadding) // Ensure padding is applied here as well
                .fillMaxHeight() // Make sure Column takes the full available height
            ,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Comments",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider()

            if (comments.isEmpty()) {
                Text(text = "No comments yet")
            }
            CommentsList(
                currUser = currUser,
                eventsViewModel = eventsViewModel,
                comments = comments,
                onCommentClicked = { comment ->
                    replyToComment = comment
                    commentText = ""
                    isEditingComment = false
                    isEditingReply = false
                },
                event = event,
                onRefresh = onRefresh,
                onEditComment = { comment ->
                    commentText = comment.text
                    commentId = comment.commentId
                    isEditingComment = true // Set editing flag for comment
                    isEditingReply = false
                },
                onEditReply = { reply ->
                    commentText = reply.text
                    replyId = reply.replyId
                    isEditingComment = false
                    isEditingReply = true // Set editing flag for reply
                    replyToComment = comments.find { it.commentId == reply.commentId } // Update replyToComment for proper reply editing
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .height(100.dp)
            ) {

                // Text field for adding comments or replies
                if (!isUserMuted){
                    OutlinedTextField(
                        value = commentText,
                        readOnly = isUserMuted,
                        onValueChange = {
                            if (!isUserMuted) {
                                commentText = it

                            }
                        },
                        label = {
                            if (isUserMuted) {
                                Text(text = "You are muted from adding a comment/reply")
                            } else {
                                Text(text = if (replyToComment != null) "Reply to ${replyToComment?.username}" else "Add a comment")

                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                coroutineScope.launch {
                                    when {
                                        replyToComment != null && isEditingReply -> {
                                            // Edit reply
                                            val success = eventsViewModel.editReply(
                                                eventId = event.id,
                                                replyId = replyId,
                                                commentId = replyToComment!!.commentId,
                                                newText = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        replyToComment != null -> {
                                            // Add reply
                                            val success = eventsViewModel.addReplyToComment(
                                                eventId = event.id,
                                                commentId = replyToComment!!.commentId,
                                                userId = currUser.userId,
                                                text = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        isEditingComment -> {
                                            // Edit comment
                                            val success = eventsViewModel.editComment(
                                                eventId = event.id,
                                                commentId = commentId,
                                                newText = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        else -> {
                                            // Add comment
                                            val success = eventsViewModel.addCommentToEvent(
                                                eventId = event.id,
                                                userId = currUser.userId,
                                                text = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }
                                    }
                                }
                            }
                        ),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .weight(0.8f)
                            .onFocusEvent {
                                if (it.isFocused || it.hasFocus) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }

                    )

                    Column(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (isEditingComment || isEditingReply) {
                            IconButton(
                                onClick = { resetCommentInput() },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .weight(0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel"
                                )
                            }
                        }

                        IconButton(
                            enabled = commentText.isNotBlank(),
                            onClick = {
                                coroutineScope.launch {
                                    when {
                                        replyToComment != null && isEditingReply -> {
                                            // Edit reply
                                            val success = eventsViewModel.editReply(
                                                eventId = event.id,
                                                replyId = replyId,
                                                commentId = replyToComment!!.commentId,
                                                newText = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        replyToComment != null -> {
                                            // Add reply
                                            val success = eventsViewModel.addReplyToComment(
                                                eventId = event.id,
                                                commentId = replyToComment!!.commentId,
                                                userId = currUser.userId,
                                                text = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        isEditingComment -> {
                                            // Edit comment
                                            val success = eventsViewModel.editComment(
                                                eventId = event.id,
                                                commentId = commentId,
                                                newText = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }

                                        else -> {
                                            // Add comment
                                            val success = eventsViewModel.addCommentToEvent(
                                                eventId = event.id,
                                                userId = currUser.userId,
                                                text = commentText
                                            )
                                            if (success) {
                                                resetCommentInput()
                                                onRefresh()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .weight(0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Reply,
                                contentDescription = "Send comment"
                            )
                        }
                    }
                }else{
                    Text(text = "You are muted from adding a comment/reply")
                }
            }
        }
    }
}


@Composable
fun CommentsList(
    currUser: UserData,
    onRefresh: () -> Unit,
    eventsViewModel: EventsViewModel,
    comments: List<CommentWithDetails?>,
    onEditComment: (CommentWithDetails) -> Unit,
    onEditReply: (ReplyWithDetails) -> Unit,
    onCommentClicked: (CommentWithDetails) -> Unit,
    event: Event
) {
    LazyColumn {
        items(comments.size) { index ->
            CommentItem(
                comment = comments[index]!!,
                currUser = currUser,
                eventsViewModel = eventsViewModel,
                onCommentClicked = onCommentClicked,
                event = event,
                onRefresh = onRefresh,
                onEditComment = onEditComment,
                onEditReply = onEditReply
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentWithDetails,
    currUser: UserData,
    eventsViewModel: EventsViewModel,
    onEditComment: (CommentWithDetails) -> Unit,
    onEditReply: (ReplyWithDetails) -> Unit,
    onRefresh: () -> Unit,
    event: Event,
    onCommentClicked: (CommentWithDetails) -> Unit
) {
    val timeAgo = getTimeAgo(comment.timestamp)
    val isCurrentUser = currUser.userId == comment.userId
    val eventReplies by eventsViewModel.eventReplies.collectAsState()
    val isCurrUserOrganiser = currUser.userId == event.organizer
    val isCommenterOrganizer = comment.userId == event.organizer
    // Filter replies for the current comment
    val filteredReplies = eventReplies.filter { it.commentId == comment.commentId }

    var isCommentMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isRepliesExpanded by remember { mutableStateOf(false) }

    // Skip rendering the comment if it's not editable and the current user is neither the commenter nor the organizer
    if (!comment.isEditable && !isCurrentUser && !isCurrUserOrganiser) {
        return
    }

    val isCommentEditable = comment.isEditable

    var openReportDialog by remember { mutableStateOf(false) }

    var openInfractionsDialog by remember { mutableStateOf(false) }

    val numberOfInfractions = comment.infractions.size

    var reasonToReport by remember { mutableStateOf("") }

    val infractions = comment.infractions

    if (openInfractionsDialog) {
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Flag, contentDescription = "Infractions")
            },
            title = {
                Text(
                    text = "Infractions"
                )
            },
            text = {
                Column {
                    Text(
                        text = "This comment has the following infractions. Would you like to clear it and mute the author?",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Group infractions by value and count occurrences
                    val groupedInfractions = infractions
                        .groupingBy { it.values }
                        .eachCount()
                        .toList()
                        .sortedByDescending { (_, count) -> count }
                        .toMap()

                    // Display grouped infractions with their count
                    groupedInfractions.forEach { (infraction, count) ->
                        Text(
                            text = "$infraction: $count occurrences",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            },
            onDismissRequest = {
                openInfractionsDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.toggleMute(
                                eventId = event.id,
                                userId = comment.userId
                            ) && eventsViewModel.clearCommentAndDeleteReplies(
                                eventId = event.id,
                                commentId = comment.commentId
                            )
                            if (result) {
                                openInfractionsDialog = false
                                onRefresh()
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openInfractionsDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (openReportDialog) {
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Flag, contentDescription = "Report comment")
            },
            title = {
                Text(
                    text = "Report comment?"
                )
            },
            text = {
                Column {
                    Text(
                        text = "Could you tell us more on why you are reporting this comment?",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Boilerplate reasons
                    val reasons = listOf(
                        "Inappropriate language",
                        "Harassment or bullying",
                        "Hate speech",
                        "Spam",
                        "Misinformation",
                        "Other"
                    )

                    // Display clickable reasons
                    reasons.forEach { reason ->
                        Text(
                            text = reason,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reasonToReport = reason // Set the selected reason
                                }
                                .padding(8.dp),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = reasonToReport,
                        onValueChange = { reasonToReport = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        label = { Text("Additional comments (optional)") }
                    )
                }
            },
            onDismissRequest = {
                openReportDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.addInfractionToComment(
                                eventId = event.id,
                                reporterId = currUser.userId,
                                commentId = comment.commentId,
                                reason = reasonToReport
                            )
                            if (result) {
                                openReportDialog = false
                                onRefresh()
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openReportDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }


    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCurrentUser) "You" else if (isCommenterOrganizer)comment.username + ": Organizer" else comment.username,
                        fontSize = 18.sp, // Increased font size
                        fontWeight = FontWeight.Bold, // Bold for the headline
                        fontStyle = if(isCommenterOrganizer) FontStyle.Italic else FontStyle.Normal,
                        color = if (isCommenterOrganizer) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface

                    )

                    if (numberOfInfractions > 0 && isCurrUserOrganiser){
                        IconButton(
                            onClick = {
                                openInfractionsDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "infractions",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                    }

                }


            },
            supportingContent = {
                Text(
                    text = comment.text,
                    fontSize = 16.sp // Larger text size for comment content
                )
            },
            leadingContent = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comment.profileUri)
                        .size(200, 200)
                        .scale(Scale.FILL)
                        .build(),
                    contentDescription = "profile picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp) // Set a consistent size for the profile picture
                )
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeAgo,
                        fontSize = 14.sp // Adjust the font size for the timestamp
                    )

                    // Dropdown trigger for current user
                    IconButton(onClick = { isCommentMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    // Dropdown Menu
                    DropdownMenu(
                        expanded = isCommentMenuExpanded,
                        onDismissRequest = { isCommentMenuExpanded = false }
                    ) {

                        if (isCurrentUser){
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    coroutineScope.launch {
                                        val result =  eventsViewModel.deleteComment(
                                            eventId = event.id,
                                            commentId = comment.commentId
                                        )
                                        if (result) {
                                            isCommentMenuExpanded = false
                                            onRefresh()
                                        }

                                    }
                                }

                            )

                            if (isCommentEditable && comment.isEditable){
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEditComment(comment)
                                        isCommentMenuExpanded = false
                                    }
                                )
                            }

                        }else{
                            DropdownMenuItem(
                                onClick = {
                                    openReportDialog = true
                                    isCommentMenuExpanded = !isCommentMenuExpanded
                                },
                                text = { Text("Report") },
                            )
                        }

                        if (isCurrUserOrganiser && comment.isEditable){
                            DropdownMenuItem(
                                onClick = {
                                    coroutineScope.launch {
                                        val result =  eventsViewModel.clearCommentAndDeleteReplies(
                                            eventId = event.id,
                                            commentId = comment.commentId
                                        )

                                        if (result){
                                            isCommentMenuExpanded = false
                                            onRefresh()
                                        }
                                    }
                                },
                                text = { Text("Clear comment") },
                            )
                            DropdownMenuItem(
                                onClick = {
                                    coroutineScope.launch {
                                        val result =  eventsViewModel.toggleMute(
                                            eventId = event.id,
                                            userId = comment.userId
                                        )

                                        if (result){
                                            isCommentMenuExpanded = false
                                            onRefresh()
                                        }
                                    }
                                },
                                text = { Text("Mute attendee") },
                            )


                        }

                    }

                }

            },
            modifier = Modifier
                .clickable {
                    onCommentClicked(comment)
                }
        )

        if (comment.replies.isNotEmpty()) {
            TextButton(
                onClick = {
                    isRepliesExpanded = !isRepliesExpanded
                }
            ) {
                Text(text = if (isRepliesExpanded) "Hide replies" else "View replies")
            }
        }

        if (isRepliesExpanded) {
            if (filteredReplies.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .padding(start = 16.dp) // Indent the replies
                        .heightIn(max = 200.dp) // Limit the height for replies
                ) {
                    items(filteredReplies) { reply ->
                        ReplyItem(
                            reply = reply,
                            currUser = currUser,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp), // Additional indentation for replies,
                            event = event,
                            eventsViewModel = eventsViewModel,
                            onRefresh = onRefresh,
                            onEditReply = onEditReply
                        )
                    }
                }
            } else {
                Text(text = "No replies yet")
            }
        }

        HorizontalDivider()

    }
}

@Composable
fun ReplyItem(
    reply: ReplyWithDetails,
    currUser: UserData,
    event: Event,
    eventsViewModel: EventsViewModel,
    onRefresh: () -> Unit,
    onEditReply: (ReplyWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeAgo = getTimeAgo(reply.timestamp)
    val isCurrentUser = currUser.userId == reply.userId
    val isCurrUserOrganiser = currUser.userId == event.organizer
    val isReplierOrganizer = reply.userId == event.organizer
    var isReplyMenuExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Skip rendering the comment if it's not editable and the current user is neither the commenter nor the organizer
    if (!reply.isEditable && !isCurrentUser && !isCurrUserOrganiser) {
        return
    }


    var openReportDialog by remember { mutableStateOf(false) }

    var openInfractionsDialog by remember { mutableStateOf(false) }

    val numberOfInfractions = reply.infractions.size

    var reasonToReport by remember { mutableStateOf("") }

    val infractions = reply.infractions


    if (openReportDialog) {
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Flag, contentDescription = "Report reply")
            },
            title = {
                Text(
                    text = "Report reply?"
                )
            },
            text = {
                Column {
                    Text(
                        text = "Could you tell us more on why you are reporting this reply?",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Boilerplate reasons
                    val reasons = listOf(
                        "Inappropriate language",
                        "Harassment or bullying",
                        "Hate speech",
                        "Spam",
                        "Misinformation",
                        "Other"
                    )

                    // Display clickable reasons
                    reasons.forEach { reason ->
                        Text(
                            text = reason,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reasonToReport = reason // Set the selected reason
                                }
                                .padding(8.dp),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = reasonToReport,
                        onValueChange = { reasonToReport = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        label = { Text("Additional comments (optional)") }
                    )
                }
            },
            onDismissRequest = {
                openReportDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.addInfractionToReply(
                                eventId = event.id,
                                replyId = reply.replyId,
                                reporterId = currUser.userId,
                                commentId = reply.commentId,
                                reason = reasonToReport
                            )
                            if (result) {
                                openReportDialog = false
                                onRefresh()
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openReportDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (openInfractionsDialog) {
        AlertDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.Flag, contentDescription = "Infractions")
            },
            title = {
                Text(
                    text = "Infractions"
                )
            },
            text = {
                Column {
                    Text(
                        text = "This reply has the following infractions. Would you like to clear it and mute the author?",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Group infractions by value and count occurrences
                    val groupedInfractions = infractions.groupingBy { it.values }
                        .eachCount()
                        .toList()
                        .sortedByDescending { (_, count) -> count }
                        .toMap()

                    // Display grouped infractions with their count
                    groupedInfractions.forEach { (infraction, count) ->
                        Text(
                            text = "$infraction: $count occurrences",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            },
            onDismissRequest = {
                openInfractionsDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = eventsViewModel.toggleMute(
                                eventId = event.id,
                                userId = reply.userId
                            ) && eventsViewModel.clearReplyContent(
                                eventId = event.id,
                                commentId = reply.commentId,
                                replyId = reply.replyId
                            )
                            if (result) {
                                openInfractionsDialog = false
                                onRefresh()
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openInfractionsDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(
        modifier = modifier.padding(8.dp), // Apply padding around reply item
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = if (isCurrentUser) "You" else if (isReplierOrganizer) reply.username + ": Organizer" else reply.username,
                        fontSize = 16.sp, // Increased font size for replies
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (isReplierOrganizer) FontStyle.Italic else FontStyle.Normal,
                        color = if (isReplierOrganizer) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (numberOfInfractions > 0 && isCurrUserOrganiser){
                        IconButton(
                            onClick = {
                                openInfractionsDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "infractions",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                    }

                }
            },
            supportingContent = {
                Text(
                    text = reply.text,
                    fontSize = 14.sp // Larger text size for reply content
                )
            },
            leadingContent = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(reply.profileUri)
                        .size(150, 150) // Set the size of the reply profile picture
                        .scale(Scale.FILL)
                        .build(),
                    contentDescription = "profile picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp) // Smaller profile picture for replies
                )
            },
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeAgo,
                        fontSize = 12.sp // Smaller font size for reply timestamp
                    )
                    // Dropdown trigger for current user
                    IconButton(onClick = { isReplyMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    // Dropdown Menu
                    DropdownMenu(
                        expanded = isReplyMenuExpanded,
                        onDismissRequest = { isReplyMenuExpanded = false }
                    ) {
                        if (isCurrentUser){
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    coroutineScope.launch {
                                        val result =  eventsViewModel.deleteReply(
                                            eventId = event.id,
                                            replyId = reply.replyId,
                                            commentId = reply.commentId
                                        )

                                        if (result) {
                                            isReplyMenuExpanded = false
                                            onRefresh()
                                        }

                                    }
                                }

                            )

                            if (reply.isEditable){
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEditReply(reply)
                                        isReplyMenuExpanded = false
                                    }
                                )
                            }


                        }else{
                            DropdownMenuItem(
                                onClick = {
                                    openReportDialog = true
                                    isReplyMenuExpanded = !isReplyMenuExpanded
                                },
                                text = { Text("Report") },
                            )
                        }

                        if (isCurrUserOrganiser){
                            DropdownMenuItem(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = eventsViewModel.clearReplyContent(
                                            eventId = event.id,
                                            replyId = reply.replyId,
                                            commentId = reply.commentId
                                        )

                                        if (result){
                                            isReplyMenuExpanded = false
                                            onRefresh()
                                        }
                                    }
                                },
                                text = { Text("Clear comment") },
                            )

                            DropdownMenuItem(
                                onClick = {
                                    coroutineScope.launch {
                                        val result =  eventsViewModel.toggleMute(
                                            eventId = event.id,
                                            userId = reply.userId
                                        )

                                        if (result){
                                            isReplyMenuExpanded = false
                                            onRefresh()
                                        }
                                    }
                                },
                                text = { Text("Mute attendee") },
                            )
                        }

                    }
                }

            }
        )
    }
}





fun getTimeAgo(timestamp: String): String {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val commentTime = LocalDateTime.parse(timestamp, formatter)
    val now = LocalDateTime.now()

    val duration = Duration.between(commentTime, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1 -> "${duration.toMinutes()} minutes ago"
        duration.toDays() < 1 -> "${duration.toHours()} hours ago"
        else -> "${duration.toDays()} days ago"
    }
}

