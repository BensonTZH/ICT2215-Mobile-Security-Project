package com.example.teacherapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.models.AppData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.teacherapp.R

val EducationBlue = Color(0xFF0056D2)
val BackgroundGray = Color(0xFFF8F9FA)
val SurfaceVariant = Color(0xFFE9ECEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    var searchQuery by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf("Any")) }
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }
    var teacherList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var currentUserRole by remember { mutableStateOf("student") }

    val subjectOptions = remember { AppData.allSubjects }
    val days = listOf("Any", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LaunchedEffect(Unit) {
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    currentUserRole = doc.getString("role") ?: "student"
                }
        }

        db.collection("users")
            .whereEqualTo("role", "teacher")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    teacherList = snapshot.documents.map { doc ->
                        val data = doc.data ?: emptyMap()
                        data + mapOf("uid" to doc.id)
                    }
                }
            }
    }

    val filteredTeachers = remember(searchQuery, selectedSubjects, selectedDays, teacherList) {
        teacherList.filter { teacher ->
            val name = teacher["name"] as? String ?: ""
            val teacherSubjects = (teacher["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val availability = (teacher["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val matchesSearch = name.contains(searchQuery, ignoreCase = true)
            val matchesSubject = if (selectedSubjects.isEmpty()) true else teacherSubjects.any { it in selectedSubjects }
            val matchesDay = if (selectedDays.contains("Any")) {
                true
            } else {
                // Check if there is any overlap between teacher availability and selected days
                availability.any { it in selectedDays }
            }

            matchesSearch && matchesSubject && matchesDay
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundGray, // Softens the white background
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discover Teachers",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EducationBlue,
                    scrolledContainerColor = EducationBlue, // Changed from Transparent to Blue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { CustomBottomNavigation(navController) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Modern Search Bar
            SearchBarSection(searchQuery) { searchQuery = it }

            // Filter Section
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 16.dp)) {
                Text(
                    "Expertise",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Increased spacing
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    items(subjectOptions) { subject ->
                        val isSelected = selectedSubjects.contains(subject)
                        LargeCustomChip(
                            label = subject,
                            selected = isSelected,
                            // Pass a custom icon based on the subject name
                            iconResId = when(subject.lowercase().trim()) {
                                "maths" -> R.drawable.mathematic // Or your custom drawable
                                "computer science" -> R.drawable.data_science
                                "biology" -> R.drawable.dna
                                "chemistry" -> R.drawable.chemistry
                                "english literature" -> R.drawable.english_book
                                "physics" -> R.drawable.physics_book
                                "history" -> R.drawable.history_book
                                "music" -> R.drawable.musical_note
                                else -> {R.drawable.chemistry}
                            },
                            onClick = {
                                selectedSubjects = if (isSelected) selectedSubjects - subject else selectedSubjects + subject
                            }
                        )
                    }
                }

                Text(
                    "Availability",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween // Spreads icons across the screen
                ) {
                    days.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        DayIconChip(
                            day = day,
                            isSelected = isSelected,
                            onClick = {
                                if (day == "Any") {
                                    selectedDays = setOf("Any") // Reset to only "Any"
                                } else {
                                    val newSelection = selectedDays.toMutableSet()
                                    newSelection.remove("Any") // Remove "Any" if a specific day is picked

                                    if (isSelected) {
                                        newSelection.remove(day)
                                    } else {
                                        newSelection.add(day)
                                    }

                                    // If nothing is selected, default back to "Any"
                                    selectedDays = if (newSelection.isEmpty()) setOf("Any") else newSelection
                                }
                            }
                        )
                    }
                }
            }

            // Results Section
            if (filteredTeachers.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredTeachers) { teacher ->
                        ModernTeacherCard(
                            name = teacher["name"] as? String ?: "Anonymous",
                            subjects = (teacher["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            availability = (teacher["availability"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            onCardClick = {
                                val id = teacher["uid"] as? String ?: ""
                                if (id.isNotBlank()) navController.navigate("${Routes.PUBLIC_PROFILE}/$id")
                            },
                            onMessageClick = {
                                val id = teacher["uid"] as? String ?: ""
                                if (id.isNotBlank()) navController.navigate("${Routes.CHAT}/$id")
                            },
                            messageEnabled = currentUserRole != "administrator"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    // 1. The Surface provides the 3D "Shadow" and elevation
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), // Space around the 3D element
        shape = RoundedCornerShape(20.dp), // Highly rounded for a modern look
        color = Color.White,
        shadowElevation = 10.dp, // High elevation makes it "pop" out
        tonalElevation = 2.dp    // Adds a subtle Material 3 tint
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name...", color = Color.Gray) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = EducationBlue,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            // 2. We set indicators to Transparent to keep the "Floating Box" look
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = EducationBlue
            )
        )
    }
}

@Composable
fun ModernTeacherCard(
    name: String,
    subjects: List<String>,
    availability: List<String>,
    onCardClick: () -> Unit,
    onMessageClick: () -> Unit,
    messageEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial-based Avatar
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = EducationBlue.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = EducationBlue,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    text = subjects.joinToString(", "),
                    color = EducationBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp), // Size goes here!
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = availability.joinToString(", "),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Message Button
            Button(
                onClick = onMessageClick,
                enabled = messageEnabled,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (messageEnabled) "Message" else "Tickets Only")
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
            Text("No matches found.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun LargeCustomChip(
    label: String,
    selected: Boolean,
    iconResId: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(48.dp) // Fixed larger height
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp), // More "rectangular-round"
        color = if (selected) EducationBlue else Color.White,
        border = if (selected) null else BorderStroke(1.dp, SurfaceVariant),
        shadowElevation = if (selected) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) Color.White else Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun DayIconChip(
    day: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // We map the day string to a single character for the icon look
    // e.g., "Mon" -> "M", "Any" -> Icon
    Box(
        modifier = Modifier
            .size(44.dp) // Standard touch target size
            .background(
                color = if (isSelected) EducationBlue else Color.Transparent,
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else SurfaceVariant,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Removes the ripple for a cleaner feel
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day == "Any") {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Any Day",
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Color.White else Color.Gray
            )
        } else {
            Text(
                text = day.take(1), // Takes just the first letter (M, T, W...)
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}
