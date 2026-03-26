package com.example.teacherapp.upload

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo

object CloudinaryUploader {

    fun uploadFile(
        uri: Uri,
        mimeType: String?,
        uploadPreset: String,
        onSuccess: (secureUrl: String, publicId: String, originalFilename: String?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val uploadRequest = MediaManager.get().upload(uri)
            .unsigned(uploadPreset)

        when (mimeType) {
            "application/pdf" -> uploadRequest.option("resource_type", "raw")
            else -> uploadRequest.option("resource_type", "auto")
        }

        uploadRequest.callback(object : UploadCallback {
            override fun onStart(requestId: String) {}

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val secureUrl = resultData["secure_url"] as? String ?: ""
                val publicId = resultData["public_id"] as? String ?: ""
                val originalFilename = resultData["original_filename"] as? String
                val format = resultData["format"] as? String

                val finalFileName = if (!originalFilename.isNullOrBlank() && !format.isNullOrBlank()) {
                    "$originalFilename.$format"
                } else {
                    originalFilename
                }

                if (secureUrl.isBlank() || publicId.isBlank()) {
                    onError("Cloudinary returned empty url/publicId")
                    return
                }

                onSuccess(secureUrl, publicId, finalFileName)
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                onError(error.description ?: "Upload failed")
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) {
                onError(error.description ?: "Upload rescheduled")
            }
        }).dispatch()
    }
}