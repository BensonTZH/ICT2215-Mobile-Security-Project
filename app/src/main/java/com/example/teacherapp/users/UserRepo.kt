package com.example.teacherapp.users

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object UserRepo {
    
    fun getUserData(uid: String, onSuccess: (UserItem, String) -> Unit, onError: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        Log.d("UserRepo", "Fetching user data for UID: $uid")

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("UserRepo", "User document found: ${document.data}")

                    
                    val user = document.toObject(UserItem::class.java)
                    val userType = user?.role
                    Log.d("UserRepo", "User type: $userType")

                    when (userType) {
                        "teacher" -> {
                            val teacher = document.toObject(TeacherItem::class.java)
                            Log.d("UserRepo", "Teacher data: ${teacher?.subjects}")
                            teacher?.let {
                                
                                onSuccess(it, userType)
                            }
                        }
                        "student" -> {
                            val student = document.toObject(StudentItem::class.java)
                            Log.d("UserRepo", "Student data: ${student?.interests}")
                            student?.let {
                                
                                onSuccess(it, userType)
                            }
                        }
                        else -> onError("User type not recognized")
                    }
                } else {
                    onError("User document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserRepo", "Error fetching user data: ${exception.message}")
                onError(exception.message ?: "Unknown error")
            }
    }

    fun getUsersNames(
        uids: List<String>,
        onSuccess: (Map<String, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val result = mutableMapOf<String, String>()

        val chunks = uids.distinct().chunked(10)

        fun fetchChunk(i: Int) {
            if (i >= chunks.size) {
                onSuccess(result)
                return
            }

            db.collection("users")
                .whereIn("uid", chunks[i]) 
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val uid = doc.getString("uid") ?: doc.id
                        val name = doc.getString("name") ?: "Unknown"
                        result[uid] = name
                    }
                    fetchChunk(i + 1)
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed to fetch names")
                }
        }

        fetchChunk(0)
    }
}