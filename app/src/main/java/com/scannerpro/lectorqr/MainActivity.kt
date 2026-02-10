package com.scannerpro.lectorqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scannerpro.lectorqr.presentation.ui.scanner.AppNavigation
import com.scannerpro.lectorqr.presentation.ui.scanner.ScannerScreen
import com.scannerpro.lectorqr.presentation.ui.theme.LectorQRTheme
import com.scannerpro.lectorqr.presentation.ui.settings.SettingsViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.scannerpro.lectorqr.util.InterstitialAdManager
import com.scannerpro.lectorqr.util.BillingManager
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager
    
    lateinit var billingManager: BillingManager

    val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        interstitialAdManager.loadAd(this)
        billingManager = BillingManager(this, settingsViewModel.settingsRepository, lifecycleScope)
        
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val primaryColorLong by settingsViewModel.primaryColor.collectAsState()
            val isPremium by settingsViewModel.isPremium.collectAsState()
            android.util.Log.d("MainActivity", "isPremium global state: $isPremium")
            
            val isDarkTheme = when(themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            LectorQRTheme(
                darkTheme = isDarkTheme,
                primaryColor = Color(primaryColorLong)
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium provides isPremium
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}