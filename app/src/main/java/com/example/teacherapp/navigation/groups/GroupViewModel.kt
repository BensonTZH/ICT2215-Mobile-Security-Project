package com.example.teacherapp.navigation.groups

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore

data class GroupItem(
    @DocumentId val docId: String = "",
    val id: String = "",
    val inviteCode: String = "",
    val level: String = "",
    val members: List<String> = emptyList(),
    val name: String = "",
    val teacherId: String = "",
)

class GroupViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    
    private val _groups = MutableLiveData<List<GroupItem>>()
    val groups: LiveData<List<GroupItem>> get() = _groups

    
    fun fetchGroupsByTeacherId(teacherId: String) {
        firestore.collection("groups")
            .whereEqualTo("teacherId", teacherId) 
            .get()
            .addOnSuccessListener { documents ->
                val groupList = mutableListOf<GroupItem>()
                for (document in documents) {
                    val group = document.toObject(GroupItem::class.java)
                    groupList.add(group)
                }
                _groups.value = groupList
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error fetching groups: ", e)
            }
    }

    fun fetchGroupsForStudent(studentUid: String) {
        firestore.collection("groups")
            .whereArrayContains("members", studentUid)
            .get()
            .addOnSuccessListener { documents ->
                val groupList = documents.map { it.toObject(GroupItem::class.java) }
                _groups.value = groupList
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error fetching student groups: ", e)
                _groups.value = emptyList()
            }
    }

    
    fun fetchGroupById(groupId: String, onSuccess: (GroupItem) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val group = document.toObject(GroupItem::class.java)
                    group?.let { onSuccess(it) }
                } else {
                    onFailure(Exception("Group not found"))
                }
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error fetching group: ", e)
                onFailure(e)
            }
    }

    
    fun addGroup(group: GroupItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("groups")
            .add(group)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error adding group: ", e)
                onFailure(e)
            }
    }

    
    fun updateGroup(groupId: String, updatedGroup: GroupItem, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("groups")
            .document(groupId)
            .set(updatedGroup)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error updating group: ", e)
                onFailure(e)
            }
    }

    
    fun deleteGroup(groupId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("groups")
            .document(groupId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("GroupViewModel", "Error deleting group: ", e)
                onFailure(e)
            }
    }

}