package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val platformName: String,
    val formatName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudio: Boolean = false
)
