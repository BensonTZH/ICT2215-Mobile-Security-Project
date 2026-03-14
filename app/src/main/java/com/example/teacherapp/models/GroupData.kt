package com.example.teacherapp.models

data class Group(
    val id: String = "",
    val name: String = "",
    val level: String = "", // JC, Poly, etc.
    val inviteCode: String = "",
    val members: List<String> = emptyList(),
    val teacherId: String = ""
)

data class StudentUser(
    val uid: String = "",
    val name: String = "",
    val level: String = "",
    val isSelected: Boolean = false
)

data class DiscussionThread(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val creatorName: String = "",
    val teacherId: String = "",
    val groupId: String = "" // <--- ADD THIS LINE
)

data class Comment(
    val id: String = "",
    val creatorName: String = "", // Use the same name here for consistency
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)