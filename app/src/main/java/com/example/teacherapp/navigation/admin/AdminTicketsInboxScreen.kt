package com.example.teacherapp.navigation.admin

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.teacherapp.support.SupportTicketRepo

@Composable
fun AdminTicketsInboxScreen(navController: NavController) {
    val context = LocalContext.current
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }

    DisposableEffect(Unit) {
        val reg = SupportTicketRepo.listenAllTickets(
            onUpdate = { tickets = it },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
        onDispose { reg.remove() }
    }

    if (tickets.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("No support tickets yet")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tickets, key = { it.id }) { ticket ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("admin_ticket_detail/${ticket.id}") },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("From: ${ticket.creatorName} (${ticket.creatorRole})")
                    Text("Status: ${ticket.status}")
                }
            }
        }
    }
}
