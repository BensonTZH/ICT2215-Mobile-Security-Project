package com.example.teacherapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RequirePrivileged(
    userRole: String?,
    onDenied: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    when (userRole) {
        null -> {
            // Loading state — do not deny yet
            Box(modifier = Modifier, contentAlignment = Alignment.Center) { }
        }
        "teacher", "administrator" -> content()
        else -> onDenied()
    }
}