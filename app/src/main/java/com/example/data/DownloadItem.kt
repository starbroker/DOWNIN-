package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val format: String,
    val quality: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)
