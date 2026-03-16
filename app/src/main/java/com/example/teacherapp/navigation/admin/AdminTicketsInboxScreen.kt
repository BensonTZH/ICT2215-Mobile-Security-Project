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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.teacherapp.models.SupportTicket
import com.example.teacherapp.support.SupportTicketRepo
import java.text.SimpleDateFormat
import java.util.Locale

private enum class TicketSortOption(val label: String) {
    CREATED_NEWEST("Creation Time (Newest)"),
    CREATED_OLDEST("Creation Time (Oldest)"),
    SUBJECT_AZ("Alphabetical (A-Z)"),
    SUBJECT_ZA("Alphabetical (Z-A)"),
    STATUS_ASC("Status (Open -> Resolved)"),
    STATUS_DESC("Status (Resolved -> Open)")
}

private fun formatCreatedAt(ticket: SupportTicket): String {
    val ts = ticket.createdAt ?: return "Unknown"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(ts.toDate())
}

private fun sortTickets(tickets: List<SupportTicket>, option: TicketSortOption): List<SupportTicket> {
    return when (option) {
        TicketSortOption.CREATED_NEWEST ->
            tickets.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        TicketSortOption.CREATED_OLDEST ->
            tickets.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        TicketSortOption.SUBJECT_AZ ->
            tickets.sortedBy { it.subject.lowercase() }
        TicketSortOption.SUBJECT_ZA ->
            tickets.sortedByDescending { it.subject.lowercase() }
        TicketSortOption.STATUS_ASC ->
            tickets.sortedWith(
                compareBy<SupportTicket> { it.status == "resolved" }
                    .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            )
        TicketSortOption.STATUS_DESC ->
            tickets.sortedWith(
                compareByDescending<SupportTicket> { it.status == "resolved" }
                    .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketsInboxScreen(navController: NavController) {
    val context = LocalContext.current
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deletingTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var sortOption by remember { mutableStateOf(TicketSortOption.STATUS_ASC) }
    var sortExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val reg = SupportTicketRepo.listenAllTickets(
            onUpdate = {
                tickets = it
                isLoading = false
                errorMessage = null
            },
            onError = {
                isLoading = false
                errorMessage = it
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        )
        onDispose { reg.remove() }
    }
    val filteredTickets = tickets.filter { ticket ->
        ticket.subject.contains(searchQuery, ignoreCase = true)
    }
    val sortedTickets = sortTickets(filteredTickets, sortOption)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Tickets Inbox") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Loading tickets...")
            }
            return@Scaffold
        }
        if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Failed to load tickets: ${errorMessage.orEmpty()}")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }
                ) {
                    OutlinedTextField(
                        value = sortOption.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        TicketSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    sortOption = option
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by ticket name") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (sortedTickets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (tickets.isEmpty()) "No support tickets yet"
                            else "No tickets match your search/filter"
                        )
                    }
                }
            } else {
                items(sortedTickets, key = { it.id }) { ticket ->
                    val isResolved = ticket.status == "resolved"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isResolved) Color(0xFFE8F5E9) else Color(0xFFFFF4E5)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { navController.navigate("admin_ticket_detail/${ticket.id}") },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    ticket.subject,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("From: ${ticket.creatorName} (${ticket.creatorRole})")
                                Text("Status: ${if (isResolved) "Resolved" else "Open"}")
                                Text("Created: ${formatCreatedAt(ticket)}")
                            }
                            IconButton(onClick = { deletingTicket = ticket }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ticket")
                            }
                        }
                    }
                }
            }
        }
    }

    deletingTicket?.let { ticket ->
        AlertDialog(
            onDismissRequest = { deletingTicket = null },
            title = { Text("Delete support ticket?") },
            text = { Text("This will permanently remove the ticket and its conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        SupportTicketRepo.deleteTicket(
                            ticketId = ticket.id,
                            onSuccess = {
                                deletingTicket = null
                                Toast.makeText(context, "Ticket deleted", Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingTicket = null }) { Text("Cancel") }
            }
        )
    }
}
