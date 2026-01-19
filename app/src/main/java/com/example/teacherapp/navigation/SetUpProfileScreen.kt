package com.example.teacherapp.navigation
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


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
    var subject by remember { mutableStateOf("") }
    var name by remember{mutableStateOf("")}

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject Area (e.g. Math)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                // 1. Check if the form is NOT empty
                if (subject.trim().isNotBlank()) {
                    // 2. Pass the data back to SetupProfileScreen
                    onComplete(mapOf(
                        "name" to name,
                        "subject" to subject.trim(),
                        "role" to "teacher"
                    ))
                } else {
                    // 3. Show a warning if it is empty
                    Toast.makeText(context, "Please enter a subject", Toast.LENGTH_SHORT).show()
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