package com.example.teacherapp.admin

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions

object AdminRepo {

    private val db = FirebaseFirestore.getInstance()

    fun listenManagedUsers(
        onUpdate: (List<Map<String, Any?>>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to load users")
                    return@addSnapshotListener
                }

                val users = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val role = doc.getString("role") ?: return@mapNotNull null
                        if (role != "student" && role != "teacher") return@mapNotNull null

                        mapOf(
                            "uid" to (doc.getString("uid") ?: doc.id),
                            "name" to (doc.getString("name") ?: ""),
                            "email" to (doc.getString("email") ?: ""),
                            "role" to role,
                            "isSetupComplete" to (doc.getBoolean("isSetupComplete") ?: true)
                        )
                    }
                    ?: emptyList()

                onUpdate(users)
            }
    }

    fun upsertManagedUser(
        uid: String,
        name: String,
        email: String,
        role: String,
        isSetupComplete: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (role != "student" && role != "teacher") {
            onError("Only student/teacher roles are allowed")
            return
        }

        if (uid.isBlank()) {
            onError("UID is required")
            return
        }

        val data = hashMapOf(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim(),
            "role" to role,
            "isSetupComplete" to isSetupComplete,
            "updatedAt" to Timestamp.now()
        )

        db.collection("users")
            .document(uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to save user") }
    }

    fun createManagedUserWithAuth(
        name: String,
        email: String,
        password: String,
        role: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (role != "student" && role != "teacher") {
            onError("Only student/teacher roles are allowed")
            return
        }

        FirebaseFunctions.getInstance()
            .getHttpsCallable("adminCreateUser")
            .call(
                mapOf(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "password" to password,
                    "role" to role
                )
            )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val uid = data?.get("uid") as? String
                if (uid.isNullOrBlank()) {
                    onError("Cloud Function did not return UID")
                    return@addOnSuccessListener
                }
                onSuccess(uid)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Failed to create user via Cloud Function")
            }
    }

    fun deleteManagedUser(
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == currentUid) {
            onError("Administrators cannot delete their own account")
            return
        }

        db.collection("users")
            .document(uid)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to delete user") }
    }
}
