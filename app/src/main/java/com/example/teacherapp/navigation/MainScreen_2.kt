package com.example.teacherapp.navigation

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teacherapp.users.UserViewModel
import java.text.SimpleDateFormat
import java.util.*


// Data classes for sessions and resources
data class Session(
    val subject: String,
    val teacher: String,
    val dateTime: String,
    val location: String,
    val status: String
)

data class Resource(
    val title: String,
    val teacher: String,
    val size: String
)

@Composable
fun MainScreen_2(navController: NavController) {
    var userRole by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    val userViewModel: UserViewModel = viewModel()

    // Fetch user data from Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            userViewModel.loadUserData(userId)
        }
    }
    userRole = userViewModel.role

    // TODO: Replace with real recent resources
    val recentResources = remember {
        listOf(
            Resource("Calculus Revision Notes", "Dr. Michael Wong", "2.4 MB"),
            Resource("Physics Formula Sheet", "Ms. Sarah Tan", "1.2 MB")
        )
    }

    // Use shared scaffold (bottom bar + divider)
    BotNavBar(navController = navController, showBottomBar = true) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(15.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (userRole == "teacher") {
                TeacherDashboard(navController, userViewModel)
            } else {
                StudentDashboard(navController, userViewModel, paddingValues = innerPadding,recentResources)
            }
        }
    }
}

@Composable
fun TeacherDashboard(navController: NavController, userViewModel: UserViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Upload\nResource", Icons.Default.CloudUpload, Modifier.weight(1f)) {
                navController.navigate("upload_resources")
            }
            SquareActionButton("Manage\nGroups", Icons.Default.Groups, Modifier.weight(1f)) {
                navController.navigate("manage_groups")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SquareActionButton("Messages", Icons.Default.QuestionAnswer, Modifier.weight(1f)) {
                navController.navigate(Routes.INBOX)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StudentDashboard(
    navController: NavController,
    userViewModel: UserViewModel,
    paddingValues: PaddingValues,
    recentResources: List<Resource>
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gradient Header Card
        item {
            GradientHeaderCard(
                userName = userViewModel.name,
                activeGroups = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stats Cards
        item {
            StatsRow(modifier = Modifier.fillMaxWidth())
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareActionButton(
                        "Find a\nTeacher",
                        Icons.Default.Search,
                        Modifier.weight(1f)
                    ) {
                        navController.navigate(Routes.DISCOVERY)
                    }
                    SquareActionButton(
                        "My Study\nGroups",
                        Icons.Default.Class,
                        Modifier.weight(1f)
                    ) {
                        // navController.navigate("student_groups")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareActionButton(
                        "Study\nResources",
                        Icons.Default.LibraryBooks,
                        Modifier.weight(1f)
                    ) {
                        navController.navigate("view_resources")
                    }
                    SquareActionButton("Inbox", Icons.Default.QuestionAnswer, Modifier.weight(1f)) {
                        navController.navigate(Routes.INBOX)
                    }
                }
            }
        }

        // Recent Resources Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Recent Resources", actionText = "View All") {
                // Navigate to all resources
            }
        }

        items(recentResources) { resource ->
            ResourceCard(resource)
        }
    }
}

@Composable
fun GradientHeaderCard(
    userName: String,
    activeGroups: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Welcome back, $userName!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Ready to learn today?",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoBadge(label = "Groups", value = "$activeGroups Active")
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun StatsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.CalendarToday,
            count = "3",
            label = "Sessions",
            iconColor = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.People,
            count = "5",
            label = "Teachers",
            iconColor = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    count: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = count,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onActionClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionText,
                color = Color(0xFF3B82F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SessionCard(session: Session) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.subject,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDEEBFF)
                    ) {
                        Text(
                            text = session.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = session.teacher,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.dateTime,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEC4899)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.location,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceCard(resource: Resource) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Resource",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${resource.teacher} • ${resource.size}",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }

            TextButton(onClick = { /* View resource */ }) {
                Text(
                    text = "View",
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
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
                    tint = selectedColor
                )
            },
            label = {
                Text(
                    "Home",
                    color = selectedColor,
                    fontSize = 12.sp
                )
            },
            selected = true,
            onClick = { }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Discover",
                    tint = unselectedColor
                )
            },
            label = {
                Text(
                    "Discover",
                    color = unselectedColor,
                    fontSize = 12.sp
                )
            },
            selected = false,
            onClick = { navController.navigate("discovery_screen") }
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

@Composable
fun SquareActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(36.dp),
                tint = Color(0xFF505D8A)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
        }
    }
}