package com.example.teacherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.teacherapp.navigation.NavGraph
import androidx.navigation.compose.rememberNavController
import com.example.teacherapp.ui.theme.TeacherappTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeacherappTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    // Column fills the screen and arranges items vertically
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Title Section (Spaced out from the top)
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Welcome to MyApp",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 2. Push buttons to the bottom using another Spacer with weight
        Spacer(modifier = Modifier.weight(2f))

        // 3. Button Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Gap between buttons
        ) {
            Button(
                onClick = {navController.navigate("register_screen")},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Create New Account")
            }

            OutlinedButton(
                onClick = {navController.navigate("login_screen")},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Use Existing Account")
            }
        }

        // Final padding at the very bottom
        Spacer(modifier = Modifier.height(16.dp))
    }
}