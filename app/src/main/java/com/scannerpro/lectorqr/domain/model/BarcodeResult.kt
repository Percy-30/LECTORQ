package com.scannerpro.lectorqr.domain.model

data class BarcodeResult(
    val id: Long = 0,
    val displayValue: String?,
    val rawValue: String?,
    val format: Int,
    val type: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val imagePath: String? = null,
    val customName: String? = null,
    val foregroundColor: Int? = null,
    val backgroundColor: Int? = null
)
