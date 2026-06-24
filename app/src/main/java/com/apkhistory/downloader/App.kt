package com.apkhistory.downloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.apkhistory.downloader.data.local.AppDatabase
import com.apkhistory.downloader.data.network.WandoujiaClient
import com.apkhistory.downloader.data.repository.AppRepository

class App : Application() {

    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val client = WandoujiaClient()
        val database = AppDatabase.getInstance(this)
        repository = AppRepository(client, database)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "APK 下载进度通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOAD = "download"

        @Volatile
        private var instance: App? = null

        fun getInstance(): App = instance ?: throw IllegalStateException("App not initialized")
    }
}
