package com.apkhistory.downloader.ui.downloads

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.DownloadRecord
import com.apkhistory.downloader.data.model.DownloadStatus
import com.apkhistory.downloader.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadsUiState(
    val downloads: List<DownloadRecord> = emptyList(),
    val isLoading: Boolean = true
)

class DownloadsViewModel : ViewModel() {
    private val repository: AppRepository = App.getInstance().repository
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeDownloads = ConcurrentHashMap<Long, Job>()

    private val _state = MutableStateFlow(DownloadsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllDownloads().collect { downloads ->
                _state.value = DownloadsUiState(downloads = downloads, isLoading = false)
            }
        }
    }

    fun startDownload(
        context: Context,
        appId: String,
        appName: String,
        packageName: String,
        vid: String,
        versionName: String,
        iconUrl: String,
        apkSize: String = ""
    ) {
        val running = _state.value.downloads.any {
            it.appId == appId && it.vid == vid && it.status == DownloadStatus.DOWNLOADING
                && activeDownloads.containsKey(it.id)
        }
        if (running) return

        viewModelScope.launch {
            val existing = repository.getDownload(appId, vid)
            if (existing?.status == DownloadStatus.COMPLETED && existing.filePath.isNotEmpty()) {
                installApk(context, existing.filePath, appName)
                return@launch
            }
            if (existing?.status == DownloadStatus.DOWNLOADING) return@launch

            val downloadUrl = repository.getDownloadUrl(appId, vid)
            val recordId = existing?.id ?: repository.insertDownload(
                DownloadRecord(
                    appId = appId, appName = appName, packageName = packageName,
                    vid = vid, versionName = versionName, iconUrl = iconUrl,
                    downloadUrl = downloadUrl, apkSize = apkSize
                )
            )

            repository.updateDownloadStatus(recordId, DownloadStatus.DOWNLOADING)

            val job = viewModelScope.launch {
                doDownload(context, recordId, downloadUrl, appName, versionName)
            }
            activeDownloads[recordId] = job
            job.invokeOnCompletion { activeDownloads.remove(recordId) }
        }
    }

    private suspend fun doDownload(
        context: Context, recordId: Long, url: String,
        appName: String, versionName: String
    ) = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            val fileName = "${appName}_${versionName}.apk"
                .replace(" ", "_").replace("/", "_").replace(":", "_")

            val (outputUri, displayPath) = createOutput(context, fileName)
            ensureActive()

            val response = downloadClient.newCall(
                Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            ).execute()
            val body = response.body ?: throw Exception("Empty response")
            val totalBytes = body.contentLength()

            ensureActive()

            body.byteStream().use { input ->
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n == -1) break
                        output.write(buffer, 0, n)
                        downloaded += n
                        if (totalBytes > 0) {
                            val progress = ((downloaded * 100) / totalBytes).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                repository.updateProgress(recordId, progress, downloaded, DownloadStatus.DOWNLOADING)
                            }
                        }
                    }
                } ?: throw Exception("Can't open output stream")
            }

            // 标记 MediaStore 文件完成（API 29+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(outputUri, ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }, null, null)
            }

            ensureActive()
            repository.completeDownload(recordId, DownloadStatus.COMPLETED, displayPath, System.currentTimeMillis())

        } catch (e: CancellationException) {
            try { deleteOutputFile(context, appName, versionName) } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            repository.updateDownloadStatus(recordId, DownloadStatus.FAILED)
        }
    }

    /** 创建输出目标：API 29+ 用 MediaStore，否则用公共 Downloads */
    private fun createOutput(context: Context, fileName: String): Pair<Uri, String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.Downloads.IS_PENDING, 1)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/APKHistory")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            Pair(uri, uri.toString())
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "APKHistory")
            dir.mkdirs()
            val file = File(dir, fileName)
            Pair(
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
                file.absolutePath
            )
        }
    }

    private fun deleteOutputFile(context: Context, appName: String, versionName: String) {
        val fileName = "${appName}_${versionName}.apk"
            .replace(" ", "_").replace("/", "_").replace(":", "_")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val where = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            context.contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, where, arrayOf(fileName))
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "APKHistory/$fileName").delete()
        }
    }

    fun installApk(context: Context, filePath: String, appName: String) {
        try {
            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (!file.exists()) {
                    viewModelScope.launch {
                        repository.getDownloadById(
                            _state.value.downloads.find { it.filePath == filePath }?.id ?: return@launch
                        )?.let { repository.updateDownloadStatus(it.id, DownloadStatus.FAILED) }
                    }
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun deleteDownload(record: DownloadRecord) {
        activeDownloads[record.id]?.cancel(CancellationException("用户取消下载"))
        activeDownloads.remove(record.id)
        val ctx = App.getInstance()
        viewModelScope.launch {
            val path = record.filePath
            if (path.startsWith("content://")) {
                try { ctx.contentResolver.delete(Uri.parse(path), null, null) } catch (_: Exception) {}
            } else if (path.isNotEmpty()) {
                File(path).delete()
            }
            deleteOutputFile(ctx, record.appName, record.versionName)
            repository.deleteDownload(record)
        }
    }

    fun retryDownload(context: Context, record: DownloadRecord) {
        startDownload(context, record.appId, record.appName, record.packageName,
            record.vid, record.versionName, record.iconUrl, record.apkSize)
    }
}
