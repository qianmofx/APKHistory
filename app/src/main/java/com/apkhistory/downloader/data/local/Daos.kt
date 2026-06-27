package com.apkhistory.downloader.data.local

import androidx.room.*
import com.apkhistory.downloader.data.model.DownloadRecord
import com.apkhistory.downloader.data.model.FavoriteApp
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY addedTime DESC")
    fun getAllDownloads(): Flow<List<DownloadRecord>>

    @Query("SELECT * FROM downloads WHERE appId = :appId AND vid = :vid LIMIT 1")
    suspend fun getDownload(appId: String, vid: String): DownloadRecord?

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadRecord): Long

    @Update
    suspend fun updateDownload(download: DownloadRecord)

    @Delete
    suspend fun deleteDownload(download: DownloadRecord)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, downloadedBytes: Long, status: Int)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, completedTime = :completedTime WHERE id = :id")
    suspend fun completeDownload(id: Long, status: Int, filePath: String, completedTime: Long)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedTime DESC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Query("SELECT * FROM favorites WHERE appId = :appId LIMIT 1")
    suspend fun getFavorite(appId: String): FavoriteApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteApp)

    @Query("DELETE FROM favorites WHERE appId = :appId")
    suspend fun deleteFavoriteByAppId(appId: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE appId = :appId")
    suspend fun isFavorite(appId: String): Int
}
