package com.example.teacherapp.navigation.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teacherapp.models.SupportTicket
import com.example.teacherapp.models.SupportTicketMessage
import com.example.teacherapp.support.SupportTicketRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketDetailScreen(navController: NavController, ticketId: String) {
    val context = LocalContext.current
    var ticket by remember { mutableStateOf<SupportTicket?>(null) }
    var messages by remember { mutableStateOf<List<SupportTicketMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(ticketId) {
        SupportTicketRepo.getTicketById(
            ticketId = ticketId,
            onSuccess = { ticket = it },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
    }
    DisposableEffect(ticketId) {
        val reg = SupportTicketRepo.listenTicketMessages(
            ticketId = ticketId,
            onUpdate = { messages = it },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val item = ticket
        if (item == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Loading...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(item.subject, fontWeight = FontWeight.Bold)
            Text("From: ${item.creatorName} (${item.creatorRole})")
            Text("Status: ${item.status}")
            Text(item.description)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("${m.senderName} (${m.senderRole})", fontWeight = FontWeight.SemiBold)
                        Text(m.text)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Reply to user") },
                    enabled = item.status != "resolved",
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        SupportTicketRepo.sendTicketMessage(
                            ticketId = ticketId,
                            text = input,
                            onSuccess = { input = "" },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    enabled = input.isNotBlank() && item.status != "resolved"
                ) {
                    Text("Send")
                }
            }
            if (item.status == "resolved") {
                Text("Replies are disabled for resolved tickets.")
            }

            Button(
                onClick = {
                    val newStatus = if (item.status == "open") "resolved" else "open"
                    SupportTicketRepo.updateTicketStatus(
                        ticketId = ticketId,
                        newStatus = newStatus,
                        onSuccess = {
                            ticket = item.copy(status = newStatus)
                            Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                        },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (item.status == "open") "Mark Resolved" else "Reopen Ticket")
            }
        }
    }
}
