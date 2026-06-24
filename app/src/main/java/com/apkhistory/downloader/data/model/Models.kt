package com.apkhistory.downloader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 搜索结果中的应用信息 */
data class SearchResult(
    val appId: String,
    val name: String,
    val packageName: String,
    val vid: String,
    val versionName: String,
    val versionCode: String,
    val iconUrl: String,
    val installCount: String,
    val description: String,
    val detailUrl: String
)

/** 应用详情 */
data class AppDetail(
    val appId: String,
    val name: String,
    val packageName: String,
    val currentVid: String,
    val currentVersionName: String,
    val currentVersionCode: String,
    val iconUrl: String,
    val size: String,
    val updateDate: String,
    val systemRequirement: String,
    val category: String,
    val subCategory: String = "",
    val developer: String,
    val installCount: String,
    val rating: String,
    val description: String,
    val changelog: String,
    val screenshotUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val contentRating: String = "",
    val privacyPolicyUrl: String = ""
)

/** 单个版本信息 */
data class AppVersion(
    val appId: String,
    val vid: String,
    val versionName: String,
    val versionCode: String,
    val iconUrl: String,
    val apkSize: String = "",
    val updateDate: String = "",
    val changelog: String = ""
)

/** 版本详情（从 /history_v{vcode} 页面解析） */
data class VersionDetail(
    val vid: String,
    val vcode: String,
    val versionName: String,
    val appName: String = "",
    val apkSize: String,
    val updateDate: String,
    val systemRequirement: String,
    val permissions: List<String> = emptyList(),
    val changelog: String,
    val developer: String,
    val category: String
)

/** 下载记录 */
@Entity(tableName = "downloads")
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: String,
    val appName: String,
    val packageName: String,
    val vid: String,
    val versionName: String,
    val iconUrl: String,
    val downloadUrl: String,
    val filePath: String = "",
    val status: Int = DownloadStatus.PENDING,
    val progress: Int = 0,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val addedTime: Long = System.currentTimeMillis(),
    val completedTime: Long = 0,
    val apkSize: String = ""
)

object DownloadStatus {
    const val PENDING = 0
    const val DOWNLOADING = 1
    const val COMPLETED = 2
    const val FAILED = 3
    const val INSTALLING = 4
    const val PAUSED = 5
}

/** 收藏的应用 */
@Entity(tableName = "favorites")
data class FavoriteApp(
    @PrimaryKey
    val appId: String,
    val name: String,
    val packageName: String,
    val iconUrl: String,
    val currentVersion: String,
    val addedTime: Long = System.currentTimeMillis()
)
