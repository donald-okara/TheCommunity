package com.example.thecommunity.presentation.events.screen

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.thecommunity.data.model.Coordinates
import com.example.thecommunity.data.model.Dropoff
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.Location
import com.example.thecommunity.data.model.Pickup
import com.example.thecommunity.presentation.events.EventsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventForm(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    isEdit: Boolean,
    communityId: String?,
    spaceId: String?,
    eventsViewModel: EventsViewModel,
    initialEventId: String? = null
) {
    var initialEvent by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    // Function to fetch data
    fun fetchData() {
        coroutineScope.launch {
            isLoading = true
            initialEvent = initialEventId?.let { eventsViewModel.getEventById(it) }
            isLoading = false
        }
    }

    // Fetch event data on first composition
    LaunchedEffect(Unit) {
        fetchData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (!isEdit) "Create event" else "Edit event") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            if (isEdit && isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EventFormContent(
                    modifier = modifier.fillMaxSize(),
                    initialEvent = initialEvent,
                    communityId = communityId,
                    spaceId = spaceId,
                    isEdit = isEdit,
                    eventsViewModel = eventsViewModel,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormContent(
    modifier: Modifier = Modifier,
    communityId: String?,
    spaceId: String?,
    isEdit: Boolean,
    eventsViewModel: EventsViewModel,
    onNavigateBack: () -> Unit,
    initialEvent: Event? = null
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    fun convertMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var eventName by remember { mutableStateOf(initialEvent?.name ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }

    var locationName by remember { mutableStateOf(initialEvent?.location?.locationName ?: "") }
    var googleMapsLink by remember { mutableStateOf(initialEvent?.location?.googleMapsLink ?: "") }
    val isLinkValid = remember(googleMapsLink) {
        // Regular expression for the format https://maps.app.goo.gl/XXXXXXX
        googleMapsLink.isEmpty() || Regex("https://maps\\.app\\.goo\\.gl/[A-Za-z0-9_-]+").matches(
            googleMapsLink
        )
    }

    var latitude by remember {
        mutableStateOf(
            initialEvent?.location?.coordinates?.latitude?.toString() ?: ""
        )
    }
    var longitude by remember {
        mutableStateOf(
            initialEvent?.location?.coordinates?.longitude?.toString() ?: ""
        )
    }

    val latitudeDouble = latitude.toDoubleOrNull() ?: 0.0
    val longitudeDouble = longitude.toDoubleOrNull() ?: 0.0
    val location =
        Location(locationName, Coordinates(latitudeDouble, longitudeDouble), googleMapsLink)

    var pickUpList by remember { mutableStateOf(initialEvent?.pickUp ?: listOf<Pickup>()) }
    var dropOffList by remember { mutableStateOf(initialEvent?.dropOff ?: listOf<Dropoff>()) }
    var imageUris by remember {
        mutableStateOf(
            initialEvent?.images?.mapNotNull {
                it["url"]?.let { url -> Uri.parse(url) }
            } ?: listOf<Uri>()
        )
    }

    var imagesToDelete by remember { mutableStateOf(listOf<Uri>()) }

    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    // Default value of startDate from either initialEvent or datePickerState
    // Initial state values
    val today = LocalDate.now()

    var startDate by remember {
        mutableStateOf(
            initialEvent?.startDate?.let { LocalDate.parse(it, dateFormatter) } ?: LocalDate.now()
        )
    }
    var numberOfDays by remember { mutableStateOf(initialEvent?.numberOfDays ?: 1) }

    // Default end date calculation
    var endDate by remember {
        mutableStateOf(startDate.plusDays((initialEvent?.numberOfDays?.toLong() ?: 1L) - 1))
    }

    val isEnabled = eventName != "" && locationName != "" && isLinkValid
    LaunchedEffect(startDate, numberOfDays) {
        endDate = startDate.plusDays((numberOfDays - 1).toLong())
    }
    val keyboardController = LocalSoftwareKeyboardController.current


    // When endDate changes, update numberOfDays
    LaunchedEffect(endDate) {
        numberOfDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    }
    // Handle potential null start time and end time
    var startTime by remember {
        mutableStateOf(
            initialEvent?.startTime?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
        )
    }
    var endTime by remember {
        mutableStateOf(
            initialEvent?.endTime?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
        )
    }
    //var googleLocation by remember { mutableStateOf(GoogleLocation(locationName, googleMapsLink)) }
    var price by remember { mutableStateOf(initialEvent?.price?.toString() ?: "") }
    var paymentDetails by remember { mutableStateOf(initialEvent?.paymentDetails ?: "") }


    LaunchedEffect(initialEvent) {
        if (initialEvent != null) {
            eventName = initialEvent.name
            description = initialEvent.description.toString()
            locationName = initialEvent.location?.locationName.toString()
            googleMapsLink = initialEvent.location!!.googleMapsLink.toString()
            latitude = initialEvent.location!!.coordinates?.latitude.toString()
            longitude = initialEvent.location!!.coordinates?.longitude.toString()
            pickUpList = initialEvent.pickUp!!
            dropOffList = initialEvent.dropOff!!
            imageUris = initialEvent.images.mapNotNull {
                it["url"]?.let { url -> Uri.parse(url) }
            }
            startDate = initialEvent.startDate.let { LocalDate.parse(it, dateFormatter) } ?: LocalDate.now()
            startTime = initialEvent.startTime.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
            numberOfDays = initialEvent.numberOfDays
            endDate = startDate.plusDays((numberOfDays - 1).toLong())
            endTime = initialEvent.endTime.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
            price = initialEvent.price.toString()
            paymentDetails = initialEvent.paymentDetails.toString()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            imageUris += uris
        }
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
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
                                    imageUris = imageUris.toMutableList().apply {
                                        removeAt(index)
                                    }

                                    if (isEdit){
                                        imagesToDelete = imagesToDelete.toMutableList().apply {
                                            add(uri)
                                        }
                                    }
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

        TextField(
            label = { Text("Event Name *") },
            value = eventName,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { eventName = it },
            modifier = modifier.fillMaxWidth()
        )

        TextField(
            label = { Text("Description") },
            value = description,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { description = it },
            modifier = modifier.fillMaxWidth()
        )

        /**
         * Prompt user to add Location or GoogleLocation
         */

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Location information")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Location Information: ")
        }
        //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

        TextField(
            label = { Text("Location Name *") },
            value = locationName,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { locationName = it },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            label = { Text("Google Maps Link") },
            value = googleMapsLink,
            onValueChange = {
                if (isLinkValid) {
                    googleMapsLink = it

                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            isError = !isLinkValid,
            placeholder = { Text("https://maps.app.goo.gl/XXXXXXX") },
            modifier = modifier.fillMaxWidth(),
        )

        //-4.272027, 39.402288
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                label = { Text("Latitude") },
                value = latitude,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, // Restrict input to numbers
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                onValueChange = { newValue ->
                    // Optionally, ensure only valid numeral input
                    latitude = newValue.filter { it.isDigit() || it == '.' }
                },
                modifier = Modifier.weight(1f)
            )

            TextField(
                label = { Text("Longitude") },
                value = longitude,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, // Restrict input to numbers
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                onValueChange = { newValue ->
                    // Optionally, ensure only valid numeral input
                    longitude = newValue.filter { it.isDigit() || it == '.' }
                },
                modifier = Modifier.weight(1f)
            )
        }

        /**
         * Start date and time
         */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Event start information"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Event starts: ")
        }


        // Start Date TextField

        TextField(
            value = startDate.format(dateFormatter),
            onValueChange = { },
            label = { Text("Date event starts") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select start date"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDatePicker) {
            Column {
                DatePicker(
                    title = { Text("Select start date for event") },
                    state = datePickerState,
                    showModeToggle = false
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Dismiss picker")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = datePickerState.selectedDateMillis?.let {
                            val selectedDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                            selectedDate.isAfter(today)
                        } ?: false,
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                if (selectedDate.isAfter(today)) {
                                    startDate = selectedDate
                                    endDate = startDate.plusDays(numberOfDays.toLong())
                                }
                                showDatePicker = false
                            }
                        }
                    ) {
                        Text("Confirm selection")
                    }
                }
            }
        }

        // Number of Days TextField

        TextField(
            value = startTime.format(timeFormatter),
            onValueChange = { /* No need to change value manually */ },
            label = { Text("Time event starts") },
            trailingIcon = {
                IconButton(onClick = { showTimePicker = !showTimePicker }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true },
            readOnly = true
        )

        // TimePicker Dialog
        if (showTimePicker) {
            DialUseState(
                onConfirm = { timePickerState ->
                    // Format the selected time and set it to selectedTime
                    startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                },
                onDismiss = {
                    showTimePicker = false
                }
            )
        }


        TextField(
            label = { Text("Days") },
            value = numberOfDays.toString(),
            trailingIcon = {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = {
                        if (numberOfDays > 1) {
                            numberOfDays--
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease"
                        )
                    }
                    IconButton(onClick = {
                        numberOfDays++
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            },
            onValueChange = { newValue ->
                numberOfDays = newValue.toIntOrNull() ?: 1
            },
            modifier = Modifier.width(100.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            readOnly = true
        )



        /**
         * End date and time
         */

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Event ends information"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Event ends: ")
        }
        TextField(
            value = endDate.format(dateFormatter),
            onValueChange = { },
            label = { Text("Date event ends") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showEndDatePicker = !showEndDatePicker }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select end date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        if (showEndDatePicker) {
            Column {
                DatePicker(
                    title = { Text("Select end date for event") },
                    state = datePickerState,
                    showModeToggle = false
                )


                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                showEndDatePicker = false
                            }
                        ) {
                            Text("Dismiss picker")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                selectedDate.isEqual(startDate) || selectedDate.isAfter(startDate)
                            } ?: false,
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                                    if (selectedDate.isEqual(startDate) || selectedDate.isAfter(startDate)) {
                                        endDate = selectedDate
                                    }
                                    showEndDatePicker = false
                                }
                            }
                        ) {
                            Text("Confirm selection")
                        }
                    }
                }
            }

        }


        TextField(
            value = endTime.format(timeFormatter),
            onValueChange = { /* No need to change value manually */ },
            label = { Text("Time event ends") },
            trailingIcon = {
                IconButton(onClick = { showEndTimePicker = !showEndTimePicker }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true },
            readOnly = true
        )

        // TimePicker Dialog
        if (showEndTimePicker) {
            DialUseState(
                onConfirm = { timePickerState ->
                    // Format the selected time and set it to selectedTime
                    val formattedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showEndTimePicker = false
                },
                onDismiss = {
                    showEndTimePicker = false
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(imageVector = Icons.Filled.Payments, contentDescription = "Payment information")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Payment: ")
        }

        TextField(
            label = { Text("Price") },
            value = price,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, // Restrict input to numbers
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { newValue ->
                // Optionally, ensure only valid numeral input
                price = newValue.filter { it.isDigit() || it == '.' }
            },
            modifier = modifier.fillMaxWidth()
        )

        TextField(
            label = { Text("Payment Details") },
            value = paymentDetails,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { paymentDetails = it },
            modifier = modifier.fillMaxWidth()
        )

        PickupList(
            pickupList = pickUpList,
            onPickupListChange = { updatedList ->
                pickUpList = updatedList
            }
        )

        DropOffList(
            dropOffList = dropOffList,
            onDropOffListChange = { updatedList ->
                dropOffList = updatedList
            }
        )


        var isUploading by remember { mutableStateOf(false) }

        if(showEditDialog){
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Confirm Edit") },
                text = { Text("Are you sure you want to update this event?") },
                confirmButton = {
                    Button(
                        onClick = {
                            if(isEdit){
                                coroutineScope.launch {
                                    val parsedPrice = price.toIntOrNull() ?: 0 // Default to 0 if price is empty
                                    Log.d("EditEvent","Editing event")
                                    Toast.makeText(context, "Editing event", Toast.LENGTH_SHORT).show()
                                    isUploading = true
                                    if (initialEvent != null) {
                                        eventsViewModel.editEvent(
                                            eventId = initialEvent.id,
                                            eventName = eventName,
                                            description = description,
                                            location = location,
                                            startDate = startDate,
                                            startTime = startTime,
                                            endDate = endDate,
                                            imagesToDelete = imagesToDelete,
                                            endTime = endTime,
                                            pickUpList = pickUpList,
                                            dropOffList = dropOffList,
                                            imageUris = imageUris,
                                            price = parsedPrice,
                                            paymentDetails = paymentDetails,
                                            numberOfDays = numberOfDays
                                        )
                                        onNavigateBack()

                                    }

                                }
                            }
                        }
                    ){
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {showEditDialog = false}
                    ) {
                        Text(text = "Cancel")
                    }
                }
            )
        }
        if (!isUploading){
            Button(
                enabled = isEnabled,
                onClick = {
                    if (!isEdit){
                        val parsedPrice = price.toIntOrNull() ?: 0 // Default to 0 if price is empty
                        Log.d("CreateEvent","Uploading event")
                        Toast.makeText(context, "Uploading event", Toast.LENGTH_SHORT).show()
                        isUploading = true
                        coroutineScope.launch {
                            eventsViewModel.createEvent(
                                eventName = eventName,
                                communityId = communityId,
                                spaceId = spaceId,
                                description = description,
                                location = location,
                                startDate = startDate,
                                startTime = startTime,
                                endDate = endDate,
                                endTime = endTime,
                                pickUpList = pickUpList,
                                dropOffList = dropOffList,
                                imageUris = imageUris,
                                price = parsedPrice,
                                paymentDetails = paymentDetails,
                                numberOfDays = numberOfDays
                            )
                            onNavigateBack()
                        }
                    }else{
                        showEditDialog = true
                    }


                },
                modifier = modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                Text( if(!isEdit) "Submit Event" else "Edit Event")
            }
        }else{
            CircularProgressIndicator(
                modifier = modifier
                    .padding(top = 16.dp)
            )
        }

    }
}


@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropOffItem(
    dropOff: Dropoff,
    onValueChange: (Dropoff) -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isRemovable: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val keyboardController = LocalSoftwareKeyboardController.current
    var showTimePicker by remember { mutableStateOf(false) }
    val latitude =
        remember { mutableStateOf(dropOff.location.coordinates?.latitude?.toString() ?: "") }
    val longitude =
        remember { mutableStateOf(dropOff.location.coordinates?.longitude?.toString() ?: "") }
    var googleMapsLink by remember { mutableStateOf(dropOff.location.googleMapsLink) }
    val isLinkValid = remember(googleMapsLink) {
        googleMapsLink.isEmpty() || Regex("https://maps\\.app\\.goo\\.gl/[A-Za-z0-9_-]+").matches(
            googleMapsLink
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(imageVector = Icons.Filled.AirportShuttle, contentDescription = "Drop off")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Dropoff: ")
        }

        TextField(
            label = { Text("Location Name") },
            value = dropOff.location.locationName,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { newName ->
                onValueChange(dropOff.copy(location = dropOff.location.copy(locationName = newName)))
            },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            label = { Text("Google Maps Link") },
            value = googleMapsLink,
            onValueChange = { newLink ->
                googleMapsLink = newLink
                if (isLinkValid) {
                    onValueChange(dropOff.copy(location = dropOff.location.copy(googleMapsLink = newLink)))
                }
            },
            isError = !isLinkValid,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            placeholder = { Text("https://maps.app.goo.gl/XXXXXXX") },
            modifier = Modifier.fillMaxWidth()
        )

        if (!isLinkValid) {
            Text(
                text = "Invalid link format. Should be https://maps.app.goo.gl/XXXXXXX",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = latitude.value,
                onValueChange = {
                    // Filter input to allow only numbers and dots
                    val filteredValue = it.filter { char -> char.isDigit() || char == '.' }
                    latitude.value = filteredValue
                    val newCoordinates = Coordinates(
                        latitude.value.toDoubleOrNull() ?: 0.0,
                        longitude.value.toDoubleOrNull() ?: 0.0
                    )
                    onValueChange(dropOff.copy(location = dropOff.location.copy(coordinates = newCoordinates)))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f)
            )

            TextField(
                value = longitude.value,
                onValueChange = {
                    // Filter input to allow only numbers and dots
                    val filteredValue = it.filter { char -> char.isDigit() || char == '.' }
                    longitude.value = filteredValue
                    val newCoordinates = Coordinates(
                        latitude.value.toDoubleOrNull() ?: 0.0,
                        longitude.value.toDoubleOrNull() ?: 0.0
                    )
                    onValueChange(dropOff.copy(location = dropOff.location.copy(coordinates = newCoordinates)))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f)
            )
        }

        TextField(
            label = { Text("Dropoff Time") },
            value = dropOff.time.format(timeFormatter),
            onValueChange = {},
            trailingIcon = {
                IconButton(onClick = { showTimePicker = !showTimePicker }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select date"
                    )
                }
            },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true }
        )

        if (showTimePicker) {
            DialUseState(
                onConfirm = { timePickerState ->
                    val formattedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onValueChange(
                        dropOff.copy(
                            time = formattedTime
                        )
                    )
                    showTimePicker = false
                },
                onDismiss = {
                    showTimePicker = false
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isRemovable) {
                IconButton(onClick = onRemoveClick) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Remove Dropoff")
                }
            }
            IconButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Dropoff")
            }
        }
    }
}

@Composable
fun DropOffList(
    dropOffList: List<Dropoff>,
    onDropOffListChange: (List<Dropoff>) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (dropOffList.isEmpty()) {
            // Show the first blank dropoff if the list is empty
            DropOffItem(
                dropOff = Dropoff(location = Location("", Coordinates(0.0, 0.0)),  LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)),
                onValueChange = { newDropoff ->
                    onDropOffListChange(listOf(newDropoff)) // Add the first item to the list
                },
                onAddClick = {
                    onDropOffListChange(
                        dropOffList + Dropoff(
                            location = Location(
                                "",
                                Coordinates(0.0, 0.0)
                            ),  LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                        )
                    )
                },
                onRemoveClick = {},
                isRemovable = false
            )
        }

        dropOffList.forEachIndexed { index, dropoff ->
            DropOffItem(
                dropOff = dropoff,
                onValueChange = { newDropoff ->
                    val updatedList = dropOffList.toMutableList().apply {
                        set(index, newDropoff)
                    }
                    onDropOffListChange(updatedList)
                },
                onAddClick = {
                    onDropOffListChange(
                        dropOffList + Dropoff(
                            location = Location(
                                "",
                                Coordinates(0.0, 0.0)
                            ),  LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                        )
                    )
                },
                onRemoveClick = {
                    val updatedList = dropOffList.toMutableList().apply {
                        removeAt(index)
                    }
                    onDropOffListChange(updatedList)
                },
                isRemovable = dropOffList.size > 1 // Allow removal only if more than one dropoff exists
            )
        }
    }
}


@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupItem(
    pickup: Pickup,
    onValueChange: (Pickup) -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isRemovable: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val keyboardController = LocalSoftwareKeyboardController.current
    var showTimePicker by remember { mutableStateOf(false) }
    val latitude =
        remember { mutableStateOf(pickup.location.coordinates?.latitude?.toString() ?: "") }
    val longitude =
        remember { mutableStateOf(pickup.location.coordinates?.longitude?.toString() ?: "") }
    var googleMapsLink by remember { mutableStateOf(pickup.location.googleMapsLink) }
    val isLinkValid = remember(googleMapsLink) {
        // Regular expression for the format https://maps.app.goo.gl/XXXXXXX
        googleMapsLink.isEmpty() || Regex("https://maps\\.app\\.goo\\.gl/[A-Za-z0-9_-]+").matches(
            googleMapsLink
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(imageVector = Icons.Outlined.AirportShuttle, contentDescription = "Pick up")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Pick up: ")
        }

        TextField(
            label = { Text("Location Name") },
            value = pickup.location.locationName,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { newName ->
                onValueChange(pickup.copy(location = pickup.location.copy(locationName = newName)))
            },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            label = { Text("Google Maps Link") },
            value = googleMapsLink,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done // Set the action to 'Done'
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide() // Dismiss the keyboard
                }
            ),
            onValueChange = { newLink ->
                googleMapsLink = newLink
                if (isLinkValid) {
                    onValueChange(pickup.copy(location = pickup.location.copy(googleMapsLink = newLink)))
                }
            },
            isError = !isLinkValid,
            placeholder = { Text("https://maps.app.goo.gl/XXXXXXX") },
            modifier = Modifier.fillMaxWidth()
        )
        if (!isLinkValid) {
            Text(
                text = "Invalid link format. Should be https://maps.app.goo.gl/XXXXXXX",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = latitude.value,
                onValueChange = {
                    // Filter input to allow only numbers and dots
                    val filteredValue = it.filter { char -> char.isDigit() || char == '.' }
                    latitude.value = filteredValue
                    val newCoordinates = Coordinates(
                        latitude.value.toDoubleOrNull() ?: 0.0,
                        longitude.value.toDoubleOrNull() ?: 0.0
                    )
                    onValueChange(pickup.copy(location = pickup.location.copy(coordinates = newCoordinates)))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f)
            )

            TextField(
                value = longitude.value,
                onValueChange = {
                    // Filter input to allow only numbers and dots
                    val filteredValue = it.filter { char -> char.isDigit() || char == '.' }
                    longitude.value = filteredValue
                    val newCoordinates = Coordinates(
                        latitude.value.toDoubleOrNull() ?: 0.0,
                        longitude.value.toDoubleOrNull() ?: 0.0
                    )
                    onValueChange(pickup.copy(location = pickup.location.copy(coordinates = newCoordinates)))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done // Set the action to 'Done'
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide() // Dismiss the keyboard
                    }
                ),
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f)
            )
        }

        TextField(
            label = { Text("Pickup Time") },
            value = pickup.time.format(timeFormatter),
            onValueChange = {},
            trailingIcon = {
                IconButton(onClick = { showTimePicker = !showTimePicker }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select date"
                    )
                }
            },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTimePicker = true }
        )

        if (showTimePicker) {
            DialUseState(
                onConfirm = { timePickerState ->
                    // Format the selected time and set it to selectedTime
                    val formattedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onValueChange(
                        pickup.copy(
                            time = formattedTime
                        )
                    )
                    showTimePicker = false
                },
                onDismiss = {
                    showTimePicker = false
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isRemovable) {
                IconButton(onClick = onRemoveClick) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Remove Dropoff")
                }
            }
            IconButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Dropoff")
            }
        }
    }
}

@Composable
fun PickupList(
    pickupList: List<Pickup>,
    onPickupListChange: (List<Pickup>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (pickupList.isEmpty()) {
            PickupItem(
                pickup = Pickup(location = Location("", Coordinates(0.0, 0.0)),  LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)),
                onValueChange = { newPickup ->
                    onPickupListChange(listOf(newPickup)) // Add the first item to the list
                },
                onAddClick = {
                    onPickupListChange(
                        pickupList + Pickup(
                            location = Location(
                                "",
                                Coordinates(0.0, 0.0)
                            ), LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                        )
                    )
                },
                onRemoveClick = {},
                isRemovable = false
            )
        } else {
            pickupList.forEachIndexed { index, pickup ->
                PickupItem(
                    pickup = pickup,
                    onValueChange = { newPickup ->
                        val updatedList = pickupList.toMutableList().apply {
                            set(index, newPickup)
                        }
                        onPickupListChange(updatedList)
                    },
                    onAddClick = {
                        onPickupListChange(
                            pickupList + Pickup(
                                location = Location(
                                    "",
                                    Coordinates(0.0, 0.0)
                                ),  LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
                            )
                        )
                    },
                    onRemoveClick = {
                        val updatedList = pickupList.toMutableList().apply {
                            removeAt(index)
                        }
                        onPickupListChange(updatedList)
                    },
                    isRemovable = pickupList.size > 1 // Allow removal only if more than one dropoff exists
                )
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialUseState(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    Column {
        TimePicker(
            state = timePickerState,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            ) {
                Text("Dismiss picker")
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                ,
                onClick = { onConfirm(timePickerState) }
            ) {
                Text("Confirm selection")
            }
        }
    }
}


