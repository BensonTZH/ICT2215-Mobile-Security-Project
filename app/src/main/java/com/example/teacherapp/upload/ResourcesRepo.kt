package com.example.teacherapp.upload

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object ResourcesRepo {
    // Write/Save resource to firestore
    fun saveResourceMetadata(
        fileName: String,
        description: String,
        cloudinaryUrl: String,
        cloudinaryPublicId: String,
        uploaderUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "fileName" to fileName,
            "description" to description,
            "cloudinaryUrl" to cloudinaryUrl,
            "cloudinaryPublicId" to cloudinaryPublicId,
            "uploaderUid" to uploaderUid,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("resources")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to save metadata") }
    }
}