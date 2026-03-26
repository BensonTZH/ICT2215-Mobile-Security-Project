package com.example.teacherapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ── Data model for a matched contact ─────────────────────────────────────────

data class MatchedContact(
    val uid: String,
    val name: String,
    val phoneNumber: String,
    val role: String
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSyncScreen(navController: NavController) {

    val context       = LocalContext.current
    val db            = FirebaseFirestore.getInstance()
    val currentUid    = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // UI state
    var matchedContacts  by remember { mutableStateOf<List<MatchedContact>>(emptyList()) }
    var isSyncing        by remember { mutableStateOf(false) }
    var hasSynced        by remember { mutableStateOf(false) }
    var errorMessage     by remember { mutableStateOf<String?>(null) }
    var hasPermission    by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            errorMessage = "Contacts permission is required to sync."
        }
    }

    // ── Helper: read phone numbers from device contacts ───────────────────────
    fun readDevicePhoneNumbers(): Set<String> {
        val numbers = mutableSetOf<String>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val raw = it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    ) ?: continue
                    // Normalise: strip spaces, dashes, brackets
                    val cleaned = raw.replace(Regex("[\\s\\-().+]"), "")
                    // Keep last 8 digits for SG numbers (handles +65 prefix)
                    if (cleaned.length >= 8) {
                        numbers.add(cleaned.takeLast(8))
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or cursor error — return empty set
        }
        return numbers
    }

    // ── Helper: query Firebase for matching phone numbers ─────────────────────
    fun syncContacts() {
        isSyncing     = true
        errorMessage  = null
        matchedContacts = emptyList()

        val deviceNumbers = readDevicePhoneNumbers()

        if (deviceNumbers.isEmpty()) {
            isSyncing = false
            hasSynced = true
            return
        }

        // Firestore whereIn supports max 30 items per query — chunk if needed
        val chunks = deviceNumbers.toList().chunked(30)
        val results = mutableListOf<MatchedContact>()
        var completedChunks = 0

        for (chunk in chunks) {
            db.collection("users")
                .whereIn("phoneNumber", chunk.map { it })
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        val uid  = doc.getString("uid") ?: doc.id
                        // Exclude current logged-in user
                        if (uid == currentUid) continue
                        val role   = doc.getString("role")        ?: "student"
                        // Exclude admins from contact sync results
                        if (role == "admin") continue
                        val name   = doc.getString("name")        ?: "Unknown"
                        val phone  = doc.getString("phoneNumber") ?: ""
                        results.add(MatchedContact(uid, name, phone, role))
                    }
                    completedChunks++
                    if (completedChunks == chunks.size) {
                        matchedContacts = results.distinctBy { it.uid }
                            .sortedBy { it.name }
                        isSyncing = false
                        hasSynced = true
                    }
                }
                .addOnFailureListener { e ->
                    completedChunks++
                    errorMessage = "Sync failed: ${e.message}"
                    if (completedChunks == chunks.size) {
                        isSyncing = false
                        hasSynced = true
                    }
                }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF0056D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            // ── Sync Button Area ──────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint               = Color(0xFF0056D2),
                        modifier           = Modifier.size(40.dp)
                    )
                    Text(
                        "Sync Contacts",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1A1A2E)
                    )
                    Text(
                        "Find people you know who are already on TeacherApp.",
                        fontSize  = 13.sp,
                        color     = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            if (hasPermission) {
                                syncContacts()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        enabled  = !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0056D2)
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color    = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing...", color = Color.White)
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (hasSynced) "Sync Again" else "Sync Contacts",
                                color = Color.White
                            )
                        }
                    }

                    // Error message
                    errorMessage?.let {
                        Text(it, color = Color(0xFFDC2626), fontSize = 12.sp)
                    }
                }
            }

            // ── Results ───────────────────────────────────────────────────────
            if (hasSynced) {
                if (matchedContacts.isEmpty()) {
                    // Empty state
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint               = Color(0xFFD1D5DB),
                            modifier           = Modifier.size(64.dp)
                        )
                        Text(
                            "No contacts found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            color      = Color(0xFF6B7280)
                        )
                        Text(
                            "None of your contacts are on TeacherApp yet.",
                            fontSize  = 13.sp,
                            color     = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Results header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${matchedContacts.size} contact${if (matchedContacts.size != 1) "s" else ""} found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                            color      = Color(0xFF374151)
                        )
                    }

                    // Contact list
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matchedContacts) { contact ->
                            ContactCard(
                                contact       = contact,
                                onMessageClick = {
                                    navController.navigate("${Routes.CHAT}/${contact.uid}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Contact Card ──────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    contact: MatchedContact,
    onMessageClick: () -> Unit
) {
    val roleColor = if (contact.role == "teacher") Color(0xFF0056D2) else Color(0xFF059669)
    val roleLabel = contact.role.replaceFirstChar { it.uppercase() }
    val initials  = contact.name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with initials
            Box(
                modifier            = Modifier
                    .size(48.dp)
                    .background(roleColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment    = Alignment.Center
            ) {
                Text(
                    initials,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = roleColor
                )
            }

            // Name + role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = Color(0xFF1A1A2E)
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape  = RoundedCornerShape(4.dp),
                    color  = roleColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        roleLabel,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color      = roleColor,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Message button
            Button(
                onClick = onMessageClick,
                shape   = RoundedCornerShape(8.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0056D2)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Message",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Message", fontSize = 12.sp)
            }
        }
    }
}