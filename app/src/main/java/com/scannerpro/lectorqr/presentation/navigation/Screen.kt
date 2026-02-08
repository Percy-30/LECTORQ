package com.scannerpro.lectorqr.presentation.navigation

sealed class Screen(val route: String) {
    object Scanner : Screen("scanner")
    object CreateQr : Screen("create_qr")
    object History : Screen("history")
    object Favorites : Screen("favorites")
    object ScanResult : Screen("scan_result/{scanId}") {
        fun createRoute(scanId: Long) = "scan_result/$scanId"
    }
}
