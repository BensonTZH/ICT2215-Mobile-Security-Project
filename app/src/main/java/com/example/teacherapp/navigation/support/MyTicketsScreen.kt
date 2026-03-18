package com.example.teacherapp.navigation.support

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.teacherapp.models.SupportTicket
import com.example.teacherapp.navigation.EducationBlue
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
fun MyTicketsScreen(navController: NavController) {
    val context = LocalContext.current
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var sortOption by remember { mutableStateOf(TicketSortOption.STATUS_ASC) }
    var sortExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val reg = SupportTicketRepo.listenMyTickets(
            onUpdate = { tickets = it },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
        onDispose { reg?.remove() }
    }
    val sortedTickets = sortTickets(tickets, sortOption)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Support Tickets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EducationBlue,
                    scrolledContainerColor = EducationBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        }
    ) { padding ->
        if (sortedTickets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("No support tickets yet")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
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
            items(sortedTickets, key = { it.id }) { ticket ->
                val isResolved = ticket.status == "resolved"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("ticket_detail/${ticket.id}") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isResolved) Color(0xFFE8F5E9) else Color(0xFFFFF4E5)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ticket.subject, fontWeight = FontWeight.Bold)
                        Text(ticket.description, maxLines = 2)
                        Text("Created: ${formatCreatedAt(ticket)}")
                    }
                }
            }
        }
    }
}
