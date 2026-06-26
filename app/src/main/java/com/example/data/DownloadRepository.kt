package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun insert(item: DownloadItem) {
        downloadDao.insertDownload(item)
    }

    suspend fun deleteById(id: Int) {
        downloadDao.deleteDownloadById(id)
    }
}
