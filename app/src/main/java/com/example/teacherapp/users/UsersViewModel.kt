package com.example.teacherapp.users

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

open class UserItem(
    @DocumentId val id: String = "",
    val email: String = "",
    val name: String = "",
    val isSetupComplete: Boolean = false,
    val role: String = "",
    val uid: String = ""
)

data class TeacherItem(
    val availability: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
) : UserItem()

data class StudentItem(
    val grade: String = "",
    val interests: List<String> = emptyList()
) : UserItem() 

class UserViewModel : ViewModel() {

    
    val subjects = mutableStateListOf<String>()
    var role by mutableStateOf("student")
    var name by mutableStateOf("User")
    var email by mutableStateOf("User@gmail.com")
    val uidToName = mutableStateMapOf<String, String>()

    
    fun loadUserData(uid: String) {
        
        subjects.clear()

        
        UserRepo.getUserData(uid, onSuccess = { user, userRole ->
            role = userRole 
            name = user.name
            email = user.email
            Log.d("UserViewModel", "User role: $role")

            
            when (user) {
                is TeacherItem -> {
                    
                    subjects.addAll(user.subjects)
                    Log.d("UserViewModel", "Teacher subjects: ${user.subjects}")
                }
                is StudentItem -> {
                    
                    Log.d("UserViewModel", "Student interests: ${user.interests}")
                }
            }
        }, onError = { errorMessage ->
            Log.d("UserViewModel", "Cannot collect subject or interests list: $errorMessage")
        })
    }

    fun loadNamesForUids(uids: List<String>) {
        val missing = uids.distinct().filter { it.isNotBlank() && !uidToName.containsKey(it) }
        if (missing.isEmpty()) return

        UserRepo.getUsersNames(
            uids = missing,
            onSuccess = { map -> uidToName.putAll(map) },
            onError = { err -> Log.e("UserViewModel", err) }
        )
    }
}
