package com.example.teacherapp.navigation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.example.teacherapp.models.AppData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var selectedRole by remember { mutableStateOf("Student") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Subject selection state
    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Availability for teachers
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    // Modern gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF6366F1)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "EduConnect",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Create your account",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // White Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign Up",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Role Selection Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3F4F6)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedRole == "Student") Color.White else Color.Transparent)
                                .clickable { selectedRole = "Student" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Student",
                                fontWeight = if (selectedRole == "Student") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedRole == "Student") Color(0xFF6366F1) else Color(0xFF6B7280)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedRole == "Teacher") Color.White else Color.Transparent)
                                .clickable { selectedRole = "Teacher" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Teacher",
                                fontWeight = if (selectedRole == "Teacher") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedRole == "Teacher") Color(0xFF6366F1) else Color(0xFF6B7280)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("John Doe") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, "Name", tint = Color(0xFF6B7280))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("your.email@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, "Email", tint = Color(0xFF6B7280))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Education Level Dropdown (Students only)
                    if (selectedRole == "Student") {
                        var educationExpanded by remember { mutableStateOf(false) }
                        val educationLevels = listOf(
                            "Primary School",
                            "Middle School",
                            "High School",
                            "University/College",
                            "Graduate School",
                            "Other"
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = educationLevel,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Education Level") },
                                placeholder = { Text("Select your level") },
                                trailingIcon = {
                                    IconButton(onClick = { educationExpanded = !educationExpanded }) {
                                        Icon(
                                            imageVector = if (educationExpanded) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = "Dropdown",
                                            tint = Color(0xFF6B7280)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { educationExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    disabledBorderColor = Color(0xFFE5E7EB),
                                    disabledTextColor = Color(0xFF1F2937)
                                ),
                                enabled = false
                            )

                            DropdownMenu(
                                expanded = educationExpanded,
                                onDismissRequest = { educationExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                educationLevels.forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(level) },
                                        onClick = {
                                            educationLevel = level
                                            educationExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Subject Selection
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (selectedRole == "Student") "Subjects I want to learn:" else "Subjects I teach:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chips
                        if (selectedSubjects.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedSubjects.forEach { subject ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedSubjects = selectedSubjects - subject },
                                        label = { Text(subject) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Dropdown for subjects
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedSubjects.isEmpty()) "" else "${selectedSubjects.size} selected",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Add subjects") },
                                placeholder = { Text("Tap to select") },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                        Icon(
                                            imageVector = if (dropdownExpanded) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = "Dropdown",
                                            tint = Color(0xFF6B7280)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    disabledBorderColor = Color(0xFFE5E7EB),
                                    disabledTextColor = Color(0xFF1F2937)
                                ),
                                enabled = false
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                AppData.allSubjects.forEach { subject ->
                                    if (!selectedSubjects.contains(subject)) {
                                        DropdownMenuItem(
                                            text = { Text(subject) },
                                            onClick = {
                                                selectedSubjects = selectedSubjects + subject
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Availability Days (Teachers only)
                    if (selectedRole == "Teacher") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Select Availability:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AppData.daysOfWeek.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedDays = if (isSelected) {
                                                selectedDays - day
                                            } else {
                                                selectedDays + day
                                            }
                                        },
                                        label = { Text(day) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, "Password", tint = Color(0xFF6B7280))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    if (passwordVisible) "Hide" else "Show",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, "Confirm", tint = Color(0xFF6B7280))
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    if (confirmPasswordVisible) "Hide" else "Show",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Account Button
                    Button(
                        onClick = {
                            // Validation
                            if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (selectedSubjects.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (selectedRole == "Student") "Please select at least one subject" else "Please select at least one subject you teach",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            if (selectedRole == "Student" && educationLevel.isBlank()) {
                                Toast.makeText(context, "Please enter your education level", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (password.length < 6) {
                                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Create account
                            auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                                .addOnSuccessListener { result ->
                                    val userId = result.user?.uid
                                    if (userId != null) {
                                        // Build profile based on role
                                        val userProfile: Map<String, Any> = if (selectedRole == "Student") {
                                            mapOf(
                                                "email" to email.trim(),
                                                "uid" to userId,
                                                "name" to fullName.trim(),
                                                "role" to "student",
                                                "grade" to educationLevel.trim(),
                                                "interests" to ArrayList(selectedSubjects),
                                                "isSetupComplete" to true
                                            )
                                        } else {
                                            mapOf(
                                                "email" to email.trim(),
                                                "uid" to userId,
                                                "name" to fullName.trim(),
                                                "role" to "teacher",
                                                "subjects" to ArrayList(selectedSubjects),
                                                "availability" to ArrayList(selectedDays.toList().sortedBy { AppData.daysOfWeek.indexOf(it) }),
                                                "isSetupComplete" to true
                                            )
                                        }

                                        db.collection("users").document(userId)
                                            .set(userProfile)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("main_screen") {
                                                    popUpTo("register_screen") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    val errorMessage = when {
                                        e.message?.contains("email address is already in use") == true -> "This email is already registered"
                                        e.message?.contains("email address is badly formatted") == true -> "Please enter a valid email"
                                        else -> "Registration failed: ${e.message}"
                                    }
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Already have an account? ", color = Color(0xFF6B7280), fontSize = 14.sp)
                        Text(
                            text = "Login",
                            color = Color(0xFF3B82F6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { navController.navigate("login_screen") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}