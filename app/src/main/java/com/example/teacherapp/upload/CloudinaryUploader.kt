package com.example.teacherapp.upload

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo

object CloudinaryUploader {

    fun uploadFile(
        uri: Uri,
        uploadPreset: String,
        onSuccess: (secureUrl: String, publicId: String, originalFilename: String?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        MediaManager.get().upload(uri)
            .unsigned(uploadPreset)
            .option("resource_type", "raw") // for PDF/docs/zip/etc
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    val publicId = resultData["public_id"] as? String ?: ""
                    val originalFilename = resultData["original_filename"] as? String
                    if (secureUrl.isBlank() || publicId.isBlank()) {
                        onError("Cloudinary returned empty url/publicId")
                        return
                    }
                    onSuccess(secureUrl, publicId, originalFilename)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description ?: "Upload failed")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    onError(error.description ?: "Upload rescheduled")
                }
            })
            .dispatch()
    }
}