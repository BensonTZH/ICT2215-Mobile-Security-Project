package com.example.teacherapp.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.teacherapp.MainActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.*

data class TuitionCentre(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val subjects: List<String>,
    val address: String,
    val postalCode: String,
    val rating: Double
)

val fakeTuitionCentres = listOf(
    TuitionCentre(
        "Math Excellence Centre",
        1.3048, 103.8318,
        listOf("Math", "Physics", "Chemistry"),
        "123 Orchard Road, #05-01",
        "238858",
        4.5
    ),
    TuitionCentre(
        "Science Tuition Hub",
        1.2897, 103.8501,
        listOf("Biology", "Chemistry", "Physics"),
        "456 Marina Bay Ave, #03-12",
        "018956",
        4.7
    ),
    TuitionCentre(
        "English Language Academy",
        1.3521, 103.8198,
        listOf("English", "Literature"),
        "789 Somerset Road, #02-05",
        "238164",
        4.3
    ),
    TuitionCentre(
        "Brilliant Minds Learning",
        1.3329, 103.7436,
        listOf("Math", "Science", "English"),
        "321 Jurong East St, #04-20",
        "600321",
        4.6
    ),
    TuitionCentre(
        "Future Scholars Centre",
        1.3699, 103.8491,
        listOf("All Subjects"),
        "555 Serangoon Road, #01-15",
        "218182",
        4.4
    ),
    TuitionCentre(
        "Academic Achievers",
        1.2966, 103.7764,
        listOf("Math", "Physics", "Chemistry"),
        "888 Clementi Ave, #03-08",
        "129809",
        4.8
    ),
    TuitionCentre(
        "Smart Kids Tuition",
        1.3138, 103.8624,
        listOf("Primary All Subjects"),
        "142 Paya Lebar Road, #02-12",
        "409015",
        4.2
    ),
    TuitionCentre(
        "Elite Learning Hub",
        1.3775, 103.8491,
        listOf("Secondary All Subjects"),
        "251 Hougang St, #05-03",
        "530251",
        4.5
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionCentreMapScreen(navController: NavController) {
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    val focusManager = LocalFocusManager.current

    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var permissionDenialCount by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(1.3521, 103.8198), 
            12f
        )
    }

    
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 14f)
        }
    }

    
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            
            hasLocationPermission = true
            permissionDenialCount = 0

            
            getUserLocation(context) { location ->
                userLocation = location
            }

            
            mainActivity?.let {
                android.util.Log.d("TuitionMap", "Location permission granted — starting tracking")
                com.example.teacherapp.services.GeoContextService.startTracking(it)
            }

            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            
            permissionDenialCount++

            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val shouldShowRationale = (context as? android.app.Activity)?.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ?: false

                if (!shouldShowRationale && permissionDenialCount > 1) {
                    
                    showSettingsDialog = true
                } else {
                    
                    showPermissionDeniedDialog = true
                }
            } else {
                showPermissionDeniedDialog = true
            }
        }
    }

    
    fun handleSearchClick() {
        if (!hasLocationPermission) {
            
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            
            isSearching = true
        }
    }

    
    fun searchByPostalCode(postalCode: String) {
        isSearching = true
        focusManager.clearFocus()

        
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName("Singapore $postalCode", 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val location = LatLng(address.latitude, address.longitude)
                userLocation = location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
            }
        } catch (e: Exception) {
            android.util.Log.e("TuitionMap", "Error geocoding postal code", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Tuition Centres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EducationBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (hasLocationPermission) "Search by postal code..."
                            else "Tap to enable location or enter postal code..."
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotEmpty()) {
                                searchByPostalCode(searchQuery)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Press) {
                                        handleSearchClick()
                                    }
                                }
                            }
                        }
                )
            }

            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = hasLocationPermission
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission
                    )
                ) {
                    
                    fakeTuitionCentres.forEach { centre ->
                        Marker(
                            state = MarkerState(position = LatLng(centre.latitude, centre.longitude)),
                            title = centre.name,
                            snippet = "${centre.subjects.joinToString(", ")} - ${centre.rating}⭐"
                        )
                    }
                }

                
                if (!hasLocationPermission) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Find Centres Near You",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Enable location to see which tuition centres are closest to you",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enable Location")
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "or enter postal code above to search",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            
            if (hasLocationPermission && userLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        item {
                            Text(
                                "Nearest Centres",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(
                            fakeTuitionCentres
                                .map { centre ->
                                    centre to calculateDistance(
                                        userLocation!!,
                                        LatLng(centre.latitude, centre.longitude)
                                    )
                                }
                                .sortedBy { it.second }
                                .take(3)
                        ) { (centre, distance) ->
                            TuitionCentreListItem(centre, distance)
                        }
                    }
                }
            }
        }
    }

    
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Access Needed") },
            text = {
                Text("To find tuition centres near you, EduConnect needs access to your location.\n\nYou can still search by postal code if you prefer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDeniedDialog = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Use Postal Code")
                }
            }
        )
    }

    
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Enable Location in Settings") },
            text = {
                Text("To find centres near you, please enable location access in Settings.\n\nGo to:\nSettings → Apps → EduConnect → Permissions → Location → Allow")
            },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TuitionCentreListItem(centre: TuitionCentre, distance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(centre.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(centre.address, fontSize = 13.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Subjects: ${centre.subjects.take(2).joinToString(", ")}",
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${centre.rating}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("⭐", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "📍 ${String.format("%.1f", distance)} km",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun getUserLocation(
    context: android.content.Context,
    onLocationReceived: (LatLng) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        location?.let {
            onLocationReceived(LatLng(it.latitude, it.longitude))
        }
    }
}

fun calculateDistance(from: LatLng, to: LatLng): Double {
    val earthRadius = 6371.0 

    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}