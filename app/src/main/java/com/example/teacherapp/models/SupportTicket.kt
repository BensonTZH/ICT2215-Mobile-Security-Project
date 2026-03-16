package com.example.teacherapp.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SupportTicket(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val creatorRole: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "open", // open | resolved
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val resolvedBy: String = "",
    val resolvedAt: Timestamp? = null
)
