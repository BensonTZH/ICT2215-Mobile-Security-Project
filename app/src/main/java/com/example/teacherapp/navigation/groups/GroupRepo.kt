package com.example.teacherapp.navigation.groups

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object ResourcesRepo {


    // Add a new group
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

    // Update an existing group
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

    // Delete a group
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

    // Add a new member to a group
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

    // Remove a member from a group
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