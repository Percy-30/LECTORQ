package com.scannerpro.lectorqr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.scannerpro.lectorqr.domain.model.BarcodeResult

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayValue: String?,
    val rawValue: String?,
    val format: Int,
    val type: Int,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val imagePath: String? = null,
    val customName: String? = null
)

fun ScanEntity.toDomain() = BarcodeResult(
    id = id,
    displayValue = displayValue,
    rawValue = rawValue,
    format = format,
    type = type,
    timestamp = timestamp,
    isFavorite = isFavorite,
    imagePath = imagePath,
    customName = customName
)

fun BarcodeResult.toEntity() = ScanEntity(
    id = id,
    displayValue = displayValue,
    rawValue = rawValue,
    format = format,
    type = type,
    timestamp = timestamp,
    isFavorite = isFavorite,
    imagePath = imagePath,
    customName = customName
)
