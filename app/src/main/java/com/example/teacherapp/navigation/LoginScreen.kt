package com.example.teacherapp.navigation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

@Composable
fun LoginScreen(
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Blah blah",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .width(300.dp)
                    .height(60.dp)
                    .background(Color.White, shape = RoundedCornerShape(50))
                    .border(1.dp, Color.Black, shape = RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .width(300.dp)
                    .height(60.dp)
                    .background(Color.White, shape = RoundedCornerShape(50))
                    .border(1.dp, Color.Black, shape = RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(email.trim(), password.trim())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid

                                if (userId != null) {
                                    db.collection("users").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("main_screen") {
                                                    popUpTo("login_screen") { inclusive = true }
                                                }
                                            } else {
                                                errorMessage = "Profile data not found."
                                            }
                                        }
                                        .addOnFailureListener {
                                            errorMessage = "Database error: ${it.localizedMessage}"
                                        }
                                }
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Login failed"
                            }
                        }
                },
                enabled = email.isNotEmpty() && password.isNotEmpty(),
                modifier = Modifier
                    .width(300.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF505D8A), // Normal button color (blue)
                    disabledContainerColor = Color(0xFF757575) // Darker grey when disabled
                )

            ) {
                Text(
                    "Login", fontSize = 18.sp,
                    color = Color.White
                )
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Don't have an account? Sign Up",
                modifier = Modifier
                    .clickable { navController.navigate("register_screen") }
            )
        }
    }
}