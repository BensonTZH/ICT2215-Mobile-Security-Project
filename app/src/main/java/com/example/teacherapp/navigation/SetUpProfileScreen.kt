package com.example.teacherapp.navigation
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.PopupProperties
import com.example.teacherapp.models.AppData


@Composable
fun SetupProfileScreen(navController: NavController) {
    // Start with "student" as default or keep it empty to force a choice
    var selectedRole by remember { mutableStateOf("student") }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Complete Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // --- RADIO BUTTON SELECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedRole == "student",
                onClick = { selectedRole = "student" }
            )
            Text("Student", modifier = Modifier.clickable { selectedRole = "student" })

            Spacer(modifier = Modifier.width(24.dp))

            RadioButton(
                selected = selectedRole == "teacher",
                onClick = { selectedRole = "teacher" }
            )
            Text("Teacher", modifier = Modifier.clickable { selectedRole = "teacher" })
        }

        if (selectedRole == "student") {
            StudentForm(onComplete = { data ->
                saveToFirestore(data, uid, db, navController)
            })
        } else {
            TeacherForm(onComplete = { data ->
                saveToFirestore(data, uid, db, navController)
            })
        }
    }
}

@Composable
fun TeacherForm(onComplete: (Map<String, Any>) -> Unit) {
    val context = LocalContext.current

    // 1. Data States
    var name by remember { mutableStateOf("") }
    var subjectInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    val filteredSubjects = AppData.allSubjects.filter {
        it.contains(subjectInput, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = subjectInput,
                onValueChange = {
                    subjectInput = it
                    expanded = it.isNotEmpty() // Show dropdown when user types
                },
                label = { Text("Subject Area") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (filteredSubjects.isNotEmpty()) {
                            subjectInput = filteredSubjects.first()
                            expanded = false
                        }
                    }
                )
            )

            DropdownMenu(
                expanded = expanded && filteredSubjects.isNotEmpty(),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                filteredSubjects.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            subjectInput = selection
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Select Availability",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            daysOfWeek.forEach { day ->
                val isSelected = selectedDays.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedDays = if (isSelected) {
                            selectedDays - day // Remove if already there
                        } else {
                            selectedDays + day // Add if not there
                        }
                    },
                    label = { Text(day) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (subjectInput.trim().isNotBlank() && name.isNotBlank()) {
                    onComplete(mapOf(
                        "name" to name,
                        "subject" to subjectInput.trim(),
                        "role" to "teacher",
                        "availability" to selectedDays.toList().sortedBy { daysOfWeek.indexOf(it) }
                    ))
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Teacher Profile")
        }
    }
}

@Composable
fun StudentForm(onComplete: (Map<String, Any>) -> Unit) {
    val context = LocalContext.current
    var gradeLevel by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var name by remember{mutableStateOf("")}

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = gradeLevel,
            onValueChange = { gradeLevel = it },
            label = { Text("Grade/Year Level (e.g. Year 10)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = interest,
            onValueChange = { interest = it },
            label = { Text("What do you want to learn? (e.g. Algebra)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                // 1. Check if BOTH fields are not empty
                if (gradeLevel.trim().isNotBlank() && interest.trim().isNotBlank()) {
                    // 2. Pass the map back to the parent
                    onComplete(mapOf(
                        "name" to name,
                        "grade" to gradeLevel.trim(),
                        "interest" to interest.trim(),
                        "role" to "student"
                    ))
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Student Profile")
        }
    }
}

fun saveToFirestore(data: Map<String, Any>, uid: String?, db: FirebaseFirestore, navController: NavController) {
    if (uid == null) return

    val finalData = data.toMutableMap()
    finalData["isSetupComplete"] = true

    db.collection("users").document(uid)
        .update(finalData)
        .addOnSuccessListener {
            navController.navigate("main_screen") {
                popUpTo("setup_screen") { inclusive = true }
            }
        }
}