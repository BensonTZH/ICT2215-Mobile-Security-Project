package com.example.teacherapp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SupportTicketMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
