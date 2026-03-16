package com.example.teacherapp.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// ✅ FIXED: Mock location for emulator, real GPS for phone
@SuppressLint("MissingPermission")
private suspend fun getCurrentLatLng(activity: Activity?): Pair<Double, Double>? {
    return try {
        // Check if running on emulator
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
                android.os.Build.FINGERPRINT.contains("unknown") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK") ||
                android.os.Build.BRAND.contains("generic")

        if (isEmulator) {
            // Return mock location for emulator (Singapore - SIT @ Punggol)
            Log.d("MessageScreen", "🌍 Using mock location for emulator (Singapore)")
            return Pair(1.3521, 103.8198)
        }

        // Real device - get actual location
        Log.d("MessageScreen", "📍 Getting real GPS location...")
        val fused = activity?.let { LocationServices.getFusedLocationProviderClient(it) }
        val loc = fused?.lastLocation?.await()

        if (loc != null) {
            Log.d("MessageScreen", "✅ Got location: ${loc.latitude}, ${loc.longitude}")
            Pair(loc.latitude, loc.longitude)
        } else {
            Log.w("MessageScreen", "⚠️ Location is null, using default (Singapore)")
            Pair(1.3521, 103.8198) // Fallback to Singapore
        }
    } catch (e: Exception) {
        Log.e("MessageScreen", "❌ Location error: ${e.message}, using default", e)
        // Fallback to Singapore coordinates
        Pair(1.3521, 103.8198)
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun chatIdFor(uid1: String, uid2: String): String =
    listOf(uid1, uid2).sorted().joinToString("_")

fun openMap(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    label: String?
) {
    val encodedLabel = Uri.encode(label ?: "Location")
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(geoIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // fallback
    }

    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
    val webIntent = Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(webIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open maps", Toast.LENGTH_SHORT).show()
    }
}

data class Message(
    val type: String = "text",           // "text" | "location"
    val message: String = "",            // for text
    val senderId: String = "",
    val recipientId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val latitude: Double? = null,        // for location
    val longitude: Double? = null,
    val label: String? = null
)

private suspend fun fetchUserRole(db: FirebaseFirestore, uid: String): String {
    return try {
        val doc = db.collection("users").document(uid).get().await()
        doc.getString("role") ?: "student"
    } catch (_: Exception) {
        "student"
    }
}

private fun resolveTeacherStudentIds(
    senderId: String,
    senderRole: String,
    recipientId: String,
    recipientRole: String
): Pair<String?, String?> {
    val teacherId: String?
    val studentId: String?

    if (senderRole == "teacher" && recipientRole == "student") {
        teacherId = senderId
        studentId = recipientId
    } else if (senderRole == "student" && recipientRole == "teacher") {
        teacherId = recipientId
        studentId = senderId
    } else {
        teacherId = null
        studentId = null
    }
    return Pair(teacherId, studentId)
}

private fun computeNeedsResponse(
    senderRole: String,
    recipientRole: String
): Boolean {
    // needsResponse is true when a STUDENT sends to a TEACHER (teacher should respond).
    return senderRole == "student" && recipientRole == "teacher"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen_3(navController: NavController, otherUserId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser ?: return

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var otherUserName by remember { mutableStateOf("Unknown") }
    val chatId = chatIdFor(currentUser.uid, otherUserId)
    var currentRole by remember { mutableStateOf("") }
    var roleLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.uid) {
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                currentRole = doc.getString("role") ?: ""
                roleLoaded = true
            }
            .addOnFailureListener {
                roleLoaded = true
            }
    }

    if (roleLoaded && currentRole == "administrator") {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Direct Chat Disabled") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Administrators should communicate through support tickets.")
            }
        }
        return
    }

    // Location Setup
    val context = LocalContext.current
    val activity = LocalActivity.current
    var pendingSendLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("MessageScreen", "Permission result: $granted")
        if (granted) pendingSendLocation = true
        else Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    // Fetch roles once (used for needsResponse + teacherId/studentId)
    LaunchedEffect(currentUser.uid, otherUserId) {
        currentUserRole = fetchUserRole(db, currentUser.uid)
        otherUserRole = fetchUserRole(db, otherUserId)
    }

    fun requestAndSendLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) pendingSendLocation = true
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Fetch the other user's display name
    LaunchedEffect(otherUserId) {
        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { doc ->
                otherUserName = doc.getString("name") ?: "Unknown"
            }
            .addOnFailureListener { e ->
                Log.e("MessageScreen", "Failed to fetch other user name", e)
            }
    }

    // ✅ FIXED: Real-time messages listener (removed .orderBy, sort in Kotlin)
    DisposableEffect(chatId) {
        Log.d("MessageScreen", "=".repeat(60))
        Log.d("MessageScreen", "🔵 SETTING UP LISTENER for chatId: $chatId")
        Log.d("MessageScreen", "=".repeat(60))

        val reg: ListenerRegistration = db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, e ->
                Log.d("MessageScreen", "🔔 LISTENER TRIGGERED!")

                if (e != null) {
                    Log.e("MessageScreen", "❌ Snapshot error: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("MessageScreen", "⚠️ Snapshot is null")
                    return@addSnapshotListener
                }

                val docCount = snapshot.documents.size
                Log.d("MessageScreen", "📊 Received $docCount documents from Firestore")

                snapshot.documents.forEachIndexed { index, doc ->
                    Log.d("MessageScreen", "  Doc $index: ${doc.id} - ${doc.getString("message")?.take(20)}")
                }

                // Parse messages
                val parsedMessages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val senderId = doc.getString("senderId")
                        val recipientId = doc.getString("recipientId")

                        if (senderId == null || recipientId == null) {
                            Log.w("MessageScreen", "⚠️ Missing senderId or recipientId in doc ${doc.id}")
                            return@mapNotNull null
                        }

                        Message(
                            type = doc.getString("type") ?: "text",
                            message = doc.getString("message") ?: "",
                            senderId = senderId,
                            recipientId = recipientId,
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            label = doc.getString("label")
                        )
                    } catch (ex: Exception) {
                        Log.e("MessageScreen", "❌ Error parsing doc ${doc.id}: ${ex.message}", ex)
                        null
                    }
                }

                Log.d("MessageScreen", "✅ Parsed ${parsedMessages.size} valid messages")

                // Sort by timestamp in Kotlin (not Firestore)
                messages = parsedMessages.sortedBy { it.timestamp.toDate() }

                Log.d("MessageScreen", "✅ UPDATED UI with ${messages.size} messages")
                Log.d("MessageScreen", "-".repeat(60))
            }

        onDispose {
            Log.d("MessageScreen", "🔴 REMOVING LISTENER for chatId: $chatId")
            reg.remove()
        }
    }

    // ✅ FIXED: Send location with try-catch
    LaunchedEffect(pendingSendLocation) {
        if (!pendingSendLocation) return@LaunchedEffect
        pendingSendLocation = false

        try {
            val latLng = getCurrentLatLng(activity)

            if (latLng == null) {
                Log.e("MessageScreen", "Location unavailable")
                Toast.makeText(context, "Location unavailable", Toast.LENGTH_LONG).show()
                return@LaunchedEffect
            }

            val (lat, lng) = latLng
            val now = Timestamp.now()

            val (teacherId, studentId) = resolveTeacherStudentIds(
                senderId = currentUser.uid,
                senderRole = currentUserRole,
                recipientId = otherUserId,
                recipientRole = otherUserRole
            )
            val needsResponse = computeNeedsResponse(currentUserRole, otherUserRole)

            val msgRef = db.collection("messages").document()
            val chatRef = db.collection("chats").document(chatId)

            val msgData = mapOf(
                "chatId" to chatId,
                "type" to "location",
                "message" to "",
                "label" to "My location",
                "latitude" to lat,
                "longitude" to lng,
                "senderId" to currentUser.uid,
                "recipientId" to otherUserId,
                "participants" to listOf(currentUser.uid, otherUserId),
                "timestamp" to now
            )

            val chatData = hashMapOf(
                "participants" to listOf(currentUser.uid, otherUserId),
                "lastMessage" to "📍 Location shared",
                "lastTimestamp" to now,
                "lastSenderId" to currentUser.uid,
                "lastSenderRole" to currentUserRole,
                "needsResponse" to needsResponse,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (teacherId != null) chatData["teacherId"] = teacherId
            if (studentId != null) chatData["studentId"] = studentId

            Log.d("MessageScreen", "Sending location...")

            db.runBatch { batch ->
                batch.set(msgRef, msgData)
                batch.set(chatRef, chatData, SetOptions.merge())
            }.addOnSuccessListener {
                Log.d("MessageScreen", "✅ Location sent successfully")
                Toast.makeText(context, "Location shared!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e("MessageScreen", "❌ Send location failed: ${e.message}", e)
                Toast.makeText(context, "Failed to send location", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MessageScreen", "Location send error: ${e.message}", e)
            Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Chat with $otherUserName") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Divider()
            }
        }
    ) { paddingValues ->
        MessageScreenUI(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            messages = messages,
            currentUserId = currentUser.uid,
            onSend = { text ->
                val now = Timestamp.now()

                val (teacherId, studentId) = resolveTeacherStudentIds(
                    senderId = currentUser.uid,
                    senderRole = currentUserRole,
                    recipientId = otherUserId,
                    recipientRole = otherUserRole
                )
                val needsResponse = computeNeedsResponse(currentUserRole, otherUserRole)

                val msgRef = db.collection("messages").document()
                val chatRef = db.collection("chats").document(chatId)

                val msgData = mapOf(
                    "chatId" to chatId,
                    "type" to "text",
                    "message" to text,
                    "senderId" to currentUser.uid,
                    "recipientId" to otherUserId,
                    "participants" to listOf(currentUser.uid, otherUserId),
                    "timestamp" to now
                )

                val chatData = hashMapOf(
                    "participants" to listOf(currentUser.uid, otherUserId),
                    "lastMessage" to text,
                    "lastTimestamp" to now,
                    "lastSenderId" to currentUser.uid,
                    "lastSenderRole" to currentUserRole,
                    "needsResponse" to needsResponse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (teacherId != null) chatData["teacherId"] = teacherId
                if (studentId != null) chatData["studentId"] = studentId

                Log.d("MessageScreen", "Sending text message...")

                db.runBatch { batch ->
                    batch.set(msgRef, msgData)
                    batch.set(chatRef, chatData, SetOptions.merge())
                }.addOnSuccessListener {
                    Log.d("MessageScreen", "✅ Message sent successfully!")
                }.addOnFailureListener { e ->
                    Log.e("MessageScreen", "❌ Send failed: ${e.message}", e)
                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            },
            onSendLocation = { requestAndSendLocation() }
        )
    }
}

// message bubble
@Composable
fun MessageItem(message: Message, isCurrentUser: Boolean) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isCurrentUser) Color(0xFF1A73E8) else Color.White
    val textColor = if (isCurrentUser) Color.White else Color(0xFF111827)
    val timeColor = Color(0xFF6B7280)
    val context = LocalContext.current

    if (message.type == "location" && message.latitude != null && message.longitude != null) {
        val lat = message.latitude
        val lng = message.longitude

        val bubbleShape = if (isCurrentUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
        else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Surface(shape = bubbleShape, shadowElevation = 2.dp, color = bubbleColor) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(text = message.label ?: "Location", color = textColor, fontSize = 16.sp)
                    Text(
                        text = "Open in Maps",
                        color = textColor,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable {
                                openMap(
                                    context = context,
                                    lat = lat,
                                    lng = lng,
                                    label = message.label
                                )
                            }
                    )
                }
            }

            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 12.sp,
                color = timeColor,
                modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp)
            )
        }
        return
    }

    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 6.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 6.dp,
            bottomEnd = 20.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            shadowElevation = 2.dp,
            color = bubbleColor
        ) {
            Text(
                text = message.message,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 280.dp)
            )
        }

        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 12.sp,
            color = timeColor,
            modifier = Modifier
                .padding(top = 4.dp, start = 6.dp, end = 6.dp)
        )
    }
}

@Composable
fun MessageScreenUI(
    modifier: Modifier = Modifier,
    messages: List<Message>,
    currentUserId: String,
    onSend: (String) -> Unit,
    onSendLocation: () -> Unit
) {
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { msg ->
                MessageItem(
                    message = msg,
                    isCurrentUser = msg.senderId == currentUserId
                )
            }
        }

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.LightGray, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    if (messageText.text.isEmpty()) {
                        Text(
                            text = "Message",
                            color = Color.Gray
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = onSendLocation) { Text("📍") }

            Button(onClick = {
                val text = messageText.text.trim()
                if (text.isNotEmpty()) {
                    onSend(text)
                    messageText = TextFieldValue("")
                }
            }) { Text("Send") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun MessageScreenWithTopBarPreview() {
    val mockMessages = listOf(
        Message("text", "Hi, are you free for consultation?", "student", "teacher", timestamp = Timestamp.now()),
        Message("text", "Yes, I am available at 4pm.", "teacher", "student", timestamp = Timestamp.now()),
        Message("text", "Great, see you then!", "student", "teacher", timestamp = Timestamp.now())
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat with Alice") },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            MessageScreenUI(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                messages = mockMessages,
                currentUserId = "student",
                onSend = {},
                onSendLocation = {}
            )
        }
    }
}
