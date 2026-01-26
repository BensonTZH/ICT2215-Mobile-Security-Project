package com.example.teacherapp.upload

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri

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

object FileDownloader {
    fun downloadToDownloads(context: Context, url: String, fileName: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Missing file URL", Toast.LENGTH_SHORT).show()
            return
        }

        val safeName = fileName.ifBlank { "download" }

        val ext = safeName.substringAfterLast('.', "")
            .lowercase()
        val mime = if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else null

        val request = DownloadManager.Request(url.toUri())
            .setTitle(safeName)
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)

        if (!mime.isNullOrBlank()) {
            request.setMimeType(mime)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }

//    fun downloadToDownloads(
//        context: Context,
//        url: String,
//        fileName: String
//    ) {
//        if (url.isBlank()) {
//            Toast.makeText(context, "Missing file URL", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val safeName = fileName.ifBlank { "download" }
//
//        val request = DownloadManager.Request(url.toUri())
//            .setTitle(safeName)
//            .setDescription("Downloading...")
//            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            .setAllowedOverMetered(true)
//            .setAllowedOverRoaming(true)
//            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
//
//        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        dm.enqueue(request)
//
//        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
//    }
}