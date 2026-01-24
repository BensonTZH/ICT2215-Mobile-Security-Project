package com.example.teacherapp.navigation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

// Data class for Teacher
data class Teacher(
    val id: String = "",
    val name: String = "",
    val subjects: List<String> = emptyList(),
    val level: String = "",
    val rating: Double = 0.0,
    val studentCount: Int = 0,
    val location: String = "",
    val availability: String = "",
    val profileImage: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    // State variables
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }
    var selectedLevels by remember { mutableStateOf(setOf<String>()) }
    var teacherList by remember { mutableStateOf(listOf<Teacher>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Sample data
    val subjectOptions = listOf("All", "Mathematics", "Physics", "Chemistry", "Biology", "English", "History")
    val levelOptions = listOf("All", "JC", "Poly", "University", "Secondary")

    // Sample teachers for testing
    LaunchedEffect(Unit) {
        teacherList = listOf(
            Teacher(
                id = "1",
                name = "Dr. Michael Wong",
                subjects = listOf("Mathematics"),
                level = "JC",
                rating = 4.9,
                studentCount = 45,
                location = "Central Singapore",
                availability = "Available"
            ),
            Teacher(
                id = "2",
                name = "Ms. Sarah Tan",
                subjects = listOf("Physics"),
                level = "JC",
                rating = 4.8,
                studentCount = 38,
                location = "East Singapore",
                availability = "Limited slots"
            ),
            Teacher(
                id = "3",
                name = "Mr. David Lee",
                subjects = listOf("Chemistry"),
                level = "Poly",
                rating = 4.7,
                studentCount = 32,
                location = "North Singapore",
                availability = "Available"
            ),
            Teacher(
                id = "4",
                name = "Dr. Emily Chen",
                subjects = listOf("Biology"),
                level = "University",
                rating = 5.0,
                studentCount = 28,
                location = "West Singapore",
                availability = "Available"
            )
        )
        isLoading = false
    }

    // Filter teachers
    val filteredTeachers = remember(searchQuery, selectedSubjects, selectedLevels, teacherList) {
        teacherList.filter { teacher ->
            val matchesSearch = teacher.name.contains(searchQuery, ignoreCase = true) ||
                    teacher.subjects.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesSubject = selectedSubjects.isEmpty() ||
                    selectedSubjects.contains("All") ||
                    teacher.subjects.any { it in selectedSubjects }

            val matchesLevel = selectedLevels.isEmpty() ||
                    selectedLevels.contains("All") ||
                    teacher.level in selectedLevels

            matchesSearch && matchesSubject && matchesLevel
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            DiscoveryBottomNavigationBar(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Discover Teachers",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Find the perfect tutor for you",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Search Bar with Filter
            SearchBarWithFilter(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onFilterClick = { showFilterSheet = true },
                activeFiltersCount = selectedSubjects.size + selectedLevels.size
            )

            // Active Filter Chips
            if (selectedSubjects.isNotEmpty() || selectedLevels.isNotEmpty()) {
                ActiveFiltersRow(
                    selectedSubjects = selectedSubjects,
                    selectedLevels = selectedLevels,
                    onRemoveSubject = { selectedSubjects = selectedSubjects - it },
                    onRemoveLevel = { selectedLevels = selectedLevels - it },
                    onClearAll = {
                        selectedSubjects = emptySet()
                        selectedLevels = emptySet()
                    }
                )
            }

            // Results count
            Text(
                text = "${filteredTeachers.size} teachers found",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Teachers List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No teachers found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "Try adjusting your filters",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTeachers) { teacher ->
                        TeacherCardImproved(teacher = teacher)
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            subjectOptions = subjectOptions,
            levelOptions = levelOptions,
            selectedSubjects = selectedSubjects,
            selectedLevels = selectedLevels,
            onSubjectsChange = { selectedSubjects = it },
            onLevelsChange = { selectedLevels = it },
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    activeFiltersCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = {
                Text(
                    "Search teachers or subjects...",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )


        // Filter Button with Badge
        Box {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (activeFiltersCount > 0) Color(0xFF3B82F6) else Color.White
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (activeFiltersCount > 0) Color.White else Color(0xFF6B7280)
                )
            }

            if (activeFiltersCount > 0) {
                Badge(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                ) {
                    Text(
                        text = activeFiltersCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveFiltersRow(
    selectedSubjects: Set<String>,
    selectedLevels: Set<String>,
    onRemoveSubject: (String) -> Unit,
    onRemoveLevel: (String) -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Clear All chip
        item {
            AssistChip(
                onClick = onClearAll,
                label = { Text("Clear all", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                    labelColor = Color(0xFFEF4444),
                    leadingIconContentColor = Color(0xFFEF4444)
                )
            )
        }

        // Subject chips
        items(selectedSubjects.toList()) { subject ->
            if (subject != "All") {
                InputChip(
                    selected = true,
                    onClick = { onRemoveSubject(subject) },
                    label = { Text(subject, fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                        selectedLabelColor = Color(0xFF3B82F6),
                        selectedTrailingIconColor = Color(0xFF3B82F6)
                    )
                )
            }
        }

        // Level chips
        items(selectedLevels.toList()) { level ->
            if (level != "All") {
                InputChip(
                    selected = true,
                    onClick = { onRemoveLevel(level) },
                    label = { Text(level, fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        selectedLabelColor = Color(0xFF8B5CF6),
                        selectedTrailingIconColor = Color(0xFF8B5CF6)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    subjectOptions: List<String>,
    levelOptions: List<String>,
    selectedSubjects: Set<String>,
    selectedLevels: Set<String>,
    onSubjectsChange: (Set<String>) -> Unit,
    onLevelsChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    var tempSelectedSubjects by remember { mutableStateOf(selectedSubjects) }
    var tempSelectedLevels by remember { mutableStateOf(selectedLevels) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                TextButton(onClick = {
                    tempSelectedSubjects = emptySet()
                    tempSelectedLevels = emptySet()
                }) {
                    Text("Reset", color = Color(0xFF3B82F6))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subject Section
            Text(
                text = "Subject",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Subject chips
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectOptions.chunked(3).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { subject ->
                            val isSelected = tempSelectedSubjects.contains(subject)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    tempSelectedSubjects = if (subject == "All") {
                                        if (isSelected) emptySet() else setOf("All")
                                    } else {
                                        val newSet = tempSelectedSubjects - "All"
                                        if (isSelected) newSet - subject else newSet + subject
                                    }
                                },
                                label = { Text(subject, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Level Section
            Text(
                text = "Level",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Level chips
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levelOptions.chunked(3).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { level ->
                            val isSelected = tempSelectedLevels.contains(level)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    tempSelectedLevels = if (level == "All") {
                                        if (isSelected) emptySet() else setOf("All")
                                    } else {
                                        val newSet = tempSelectedLevels - "All"
                                        if (isSelected) newSet - level else newSet + level
                                    }
                                },
                                label = { Text(level, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Apply Button
            Button(
                onClick = {
                    onSubjectsChange(tempSelectedSubjects)
                    onLevelsChange(tempSelectedLevels)
                    onApply()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text(
                    "Apply Filters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TeacherCardImproved(teacher: Teacher) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image Placeholder
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                when (teacher.subjects.firstOrNull()) {
                                    "Mathematics" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                    "Physics" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                    "Chemistry" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.name.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (teacher.subjects.firstOrNull()) {
                                "Mathematics" -> Color(0xFF3B82F6)
                                "Physics" -> Color(0xFF8B5CF6)
                                "Chemistry" -> Color(0xFF10B981)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                    }

                    Column {
                        Text(
                            text = teacher.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "${teacher.subjects.firstOrNull() ?: "General"} • ${teacher.level}",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                // Availability Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (teacher.availability == "Available")
                        Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = teacher.availability,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (teacher.availability == "Available")
                            Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating and Students
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Rating",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFBBF24)
                )
                Text(
                    text = String.format("%.1f", teacher.rating),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = "${teacher.studentCount} students",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF6B7280)
                )
                Text(
                    text = teacher.location,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Profile Button
            Button(
                onClick = { /* Navigate to teacher profile */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Text(
                    "View Profile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DiscoveryBottomNavigationBar(navController: NavController) {
    val selectedColor = Color(0xFF3B82F6)
    val unselectedColor = Color(0xFF9CA3AF)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "Home",
                    color = unselectedColor,
                    fontSize = 12.sp
                )
            },
            selected = false,
            onClick = { navController.navigate("main_screen") }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Discover",
                    tint = selectedColor
                )
            },
            label = {
                Text(
                    "Discover",
                    color = selectedColor,
                    fontSize = 12.sp
                )
            },
            selected = true,
            onClick = { }
        )

        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = Color(0xFFEF4444)
                        ) {
                            Text(
                                "3",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chats",
                        tint = unselectedColor
                    )
                }
            },
            label = {
                Text(
                    "Chats",
                    color = unselectedColor,
                    fontSize = 12.sp
                )
            },
            selected = false,
            onClick = { /* Navigate to chats */ }
        )

        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = Color(0xFFEF4444)
                        ) {
                            Text(
                                "2",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = unselectedColor
                    )
                }
            },
            label = {
                Text(
                    "Alerts",
                    color = unselectedColor,
                    fontSize = 12.sp
                )
            },
            selected = false,
            onClick = { /* Navigate to alerts */ }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "Profile",
                    color = unselectedColor,
                    fontSize = 12.sp
                )
            },
            selected = false,
            onClick = { navController.navigate("profile_screen") }
        )
    }
}