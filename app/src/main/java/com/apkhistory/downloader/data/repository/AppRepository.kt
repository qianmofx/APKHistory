package com.apkhistory.downloader.data.repository

import com.apkhistory.downloader.data.local.AppDatabase
import com.apkhistory.downloader.data.local.DownloadDao
import com.apkhistory.downloader.data.local.FavoriteDao
import com.apkhistory.downloader.data.model.*
import com.apkhistory.downloader.data.network.WandoujiaClient
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val client: WandoujiaClient,
    private val database: AppDatabase
) {
    private val downloadDao: DownloadDao = database.downloadDao()
    private val favoriteDao: FavoriteDao = database.favoriteDao()

    // ========== 网络 ==========

    suspend fun searchApps(keyword: String, page: Int = 1) = client.searchApps(keyword, page)
    suspend fun getAppDetail(appId: String) = client.getAppDetail(appId)
    suspend fun getVersions(appId: String) = client.getVersions(appId)
    suspend fun getVersionDetail(appId: String, vcode: String) = client.getVersionDetail(appId, vcode)
    fun getDownloadUrl(appId: String, vid: String) = client.getDownloadUrl(appId, vid)

    // ========== 下载记录 ==========

    fun getAllDownloads(): Flow<List<DownloadRecord>> = downloadDao.getAllDownloads()
    suspend fun getDownload(appId: String, vid: String) = downloadDao.getDownload(appId, vid)
    suspend fun getDownloadById(id: Long) = downloadDao.getDownloadById(id)
    suspend fun insertDownload(download: DownloadRecord) = downloadDao.insertDownload(download)
    suspend fun updateDownload(download: DownloadRecord) = downloadDao.updateDownload(download)
    suspend fun deleteDownload(download: DownloadRecord) = downloadDao.deleteDownload(download)
    suspend fun deleteDownloadById(id: Long) = downloadDao.deleteDownloadById(id)
    suspend fun updateProgress(id: Long, progress: Int, downloadedBytes: Long, status: Int) =
        downloadDao.updateProgress(id, progress, downloadedBytes, status)
    suspend fun completeDownload(id: Long, status: Int, filePath: String, completedTime: Long) =
        downloadDao.completeDownload(id, status, filePath, completedTime)
    suspend fun updateDownloadStatus(id: Long, status: Int) = downloadDao.updateStatus(id, status)

    // ========== 收藏 ==========

    fun getAllFavorites(): Flow<List<FavoriteApp>> = favoriteDao.getAllFavorites()
    suspend fun getFavorite(appId: String) = favoriteDao.getFavorite(appId)
    suspend fun addFavorite(favorite: FavoriteApp) = favoriteDao.insertFavorite(favorite)
    suspend fun removeFavorite(appId: String) = favoriteDao.deleteFavoriteByAppId(appId)
    suspend fun isFavorite(appId: String) = favoriteDao.isFavorite(appId) > 0
}
