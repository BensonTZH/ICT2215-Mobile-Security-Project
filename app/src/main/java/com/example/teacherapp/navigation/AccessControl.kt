package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RequireTeacher(
    userRole: String?,
    onDenied: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (userRole == "teacher") {
        content()
    } else {
        onDenied()
    }
}

@Composable
fun UnauthorizedScreen(
    message: String = "Access restricted.",
    onGoHome: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.titleMedium)
        if (onGoHome != null) {
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = onGoHome
            ) {
                Text("Go Home")
            }
        }
    }
}