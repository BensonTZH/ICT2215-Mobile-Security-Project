package com.example.teacherapp

import android.app.Application
import com.cloudinary.android.MediaManager

class CloudinaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = hashMapOf(
            "cloud_name" to getString(R.string.cloudinary_cloud_name)
        )
        MediaManager.init(this, config)
    }
}
