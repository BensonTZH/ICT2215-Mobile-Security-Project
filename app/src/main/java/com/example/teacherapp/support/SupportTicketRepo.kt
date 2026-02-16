package com.example.teacherapp.support

import com.example.teacherapp.models.SupportTicket
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object SupportTicketRepo {

    private val db = FirebaseFirestore.getInstance()

    fun createTicket(
        subject: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError("You must be logged in")
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val now = Timestamp.now()
                val payload = hashMapOf(
                    "creatorUid" to uid,
                    "creatorName" to (doc.getString("name") ?: "User"),
                    "creatorRole" to (doc.getString("role") ?: "student"),
                    "subject" to subject.trim(),
                    "description" to description.trim(),
                    "status" to "open",
                    "createdAt" to now,
                    "updatedAt" to now,
                    "resolvedBy" to ""
                )

                db.collection("supportTickets")
                    .add(payload)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "Failed to submit ticket") }
            }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to read user profile") }
    }

    fun listenAllTickets(
        onUpdate: (List<SupportTicket>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("supportTickets")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to load tickets")
                    return@addSnapshotListener
                }
                onUpdate(snapshot?.toObjects(SupportTicket::class.java) ?: emptyList())
            }
    }

    fun listenMyTickets(
        onUpdate: (List<SupportTicket>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return db.collection("supportTickets")
            .whereEqualTo("creatorUid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to load your tickets")
                    return@addSnapshotListener
                }
                onUpdate(snapshot?.toObjects(SupportTicket::class.java) ?: emptyList())
            }
    }

    fun updateTicketStatus(
        ticketId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to Timestamp.now(),
            "resolvedBy" to if (newStatus == "resolved") uid else ""
        )
        updates["resolvedAt"] = if (newStatus == "resolved") Timestamp.now() else FieldValue.delete()

        db.collection("supportTickets")
            .document(ticketId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to update ticket") }
    }

    fun getTicketById(
        ticketId: String,
        onSuccess: (SupportTicket?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("supportTickets")
            .document(ticketId)
            .get()
            .addOnSuccessListener { doc -> onSuccess(doc.toObject(SupportTicket::class.java)) }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to load ticket") }
    }
}
