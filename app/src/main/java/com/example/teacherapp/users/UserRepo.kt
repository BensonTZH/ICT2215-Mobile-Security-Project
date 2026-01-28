package com.example.teacherapp.users

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

object UserRepo {
    // Fetch the current user data from Firestore
    fun getUserData(uid: String, onSuccess: (UserItem, String) -> Unit, onError: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        Log.d("UserRepo", "Fetching user data for UID: $uid")

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("UserRepo", "User document found: ${document.data}")

                    // Get the user data and cast to the appropriate class (Teacher or Student)
                    val user = document.toObject(UserItem::class.java)
                    val userType = user?.role
                    Log.d("UserRepo", "User type: $userType")

                    when (userType) {
                        "teacher" -> {
                            val teacher = document.toObject(TeacherItem::class.java)
                            Log.d("UserRepo", "Teacher data: ${teacher?.subjects}")
                            teacher?.let {
                                // Pass the teacher data and role to onSuccess
                                onSuccess(it, userType)
                            }
                        }
                        "student" -> {
                            val student = document.toObject(StudentItem::class.java)
                            Log.d("UserRepo", "Student data: ${student?.interests}")
                            student?.let {
                                // Pass the student data and role to onSuccess
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
}