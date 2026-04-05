package com.example.teacherapp.navigation.groups

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object ResourcesRepo {

    
    fun addGroup(group: GroupItem, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups")
            .add(group)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ResourcesRepo", "Error adding group: ", e)
                onError(e.message ?: "Unknown error")
            }
    }

    
    fun updateGroup(groupId: String, updatedGroup: GroupItem, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups")
            .document(groupId)
            .set(updatedGroup)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ResourcesRepo", "Error updating group: ", e)
                onError(e.message ?: "Unknown error")
            }
    }

    
    fun deleteGroup(groupId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups")
            .document(groupId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ResourcesRepo", "Error deleting group: ", e)
                onError(e.message ?: "Unknown error")
            }
    }

    
    fun addMemberToGroup(groupId: String, memberId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups")
            .document(groupId)
            .update("members", FieldValue.arrayUnion(memberId))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ResourcesRepo", "Error adding member to group: ", e)
                onError(e.message ?: "Unknown error")
            }
    }

    
    fun removeMemberFromGroup(groupId: String, memberId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups")
            .document(groupId)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ResourcesRepo", "Error removing member from group: ", e)
                onError(e.message ?: "Unknown error")
            }
    }
}