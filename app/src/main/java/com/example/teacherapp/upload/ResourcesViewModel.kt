package com.example.teacherapp.upload

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.teacherapp.users.UserViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObjects

data class ResourceItem(
    @DocumentId val id: String = "",
    val fileName: String = "",
    val group: String = "",
    val description: String = "",
    val cloudinaryUrl: String = "",
    val cloudinaryPublicId: String = "",
    val uploaderUid: String = "",
    val timestamp: Timestamp? = null
)

class ResourcesViewModel : ViewModel() {

    val resources = mutableStateListOf<ResourceItem>()

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    
    fun startListeningTeacher(onError: (String) -> Unit) {
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError("User not logged in.")
            return
        }

        
        if (reg != null) return

        reg = db.collection("resources")
            .whereEqualTo("uploaderUid", uid)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Failed to load resources")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { doc ->
                    Log.d("RESOURCES_DEBUG", doc.data.toString())
                }

                val list = snapshot?.toObjects<ResourceItem>() ?: emptyList()
                resources.clear()
                resources.addAll(list)
            }
    }

    fun startListeningStudent(onError: (String) -> Unit) {
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError("User not logged in.")
            return
        }

        
        if (reg != null) return

        reg = db.collection("resources")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e.message ?: "Failed to load resources")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { doc ->
                    Log.d("RESOURCES_DEBUG", doc.data.toString())
                }

                val list = snapshot?.toObjects<ResourceItem>() ?: emptyList()
                resources.clear()
                resources.addAll(list)
            }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}