package com.scannerpro.lectorqr.presentation.ui.scanner

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.scannerpro.lectorqr.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.app.Activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.presentation.navigation.Screen
import com.scannerpro.lectorqr.presentation.ui.create.CreateQrScreen
import com.scannerpro.lectorqr.presentation.ui.create.contact.CreateContactScreen
import com.scannerpro.lectorqr.presentation.ui.create.url.CreateUrlScreen
import com.scannerpro.lectorqr.presentation.ui.create.text.CreateTextScreen
import com.scannerpro.lectorqr.presentation.ui.create.email.CreateEmailScreen
import com.scannerpro.lectorqr.presentation.ui.create.sms.CreateSmsScreen
import com.scannerpro.lectorqr.presentation.ui.create.wifi.CreateWifiScreen
import com.scannerpro.lectorqr.presentation.ui.create.location.CreateLocationScreen
import com.scannerpro.lectorqr.presentation.ui.create.calendar.CreateCalendarScreen
import com.scannerpro.lectorqr.presentation.ui.favorites.FavoritesScreen
import com.scannerpro.lectorqr.presentation.ui.history.HistoryScreen
import com.scannerpro.lectorqr.presentation.ui.qrselection.QrTypeSelectionScreen
import kotlinx.coroutines.launch
import com.scannerpro.lectorqr.util.InterstitialAdManager

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scannerViewModel: ScannerViewModel = hiltViewModel()
    val scannerUiState by scannerViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val billingManager = (context as com.scannerpro.lectorqr.MainActivity).billingManager
    
    // BillingManager is now managed globally in MainActivity

    androidx.compose.runtime.LaunchedEffect(Unit) {
        scannerViewModel.interstitialTrigger.collect {
            (context as? android.app.Activity)?.let { activity ->
                if (!scannerUiState.isPremium) {
                    (activity as? com.scannerpro.lectorqr.MainActivity)?.interstitialAdManager?.showAd(activity)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val isPremium = com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium.current
            ModalDrawerSheet {
                DrawerContent(
                    isPremium = isPremium,
                    onItemClick = { item ->
                    scope.launch {
                        drawerState.close()
                        when(item.route) {
                            "scanner" -> {
                                if (currentRoute != Screen.Scanner.route) {
                                    navController.navigate(Screen.Scanner.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                    }
                                }
                            }
                            "scan_image" -> {
                                if (currentRoute != Screen.Scanner.route) {
                                    navController.navigate(Screen.Scanner.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                    }
                                }
                                scannerViewModel.requestGalleryPicker()
                            }
                            "my_qr" -> navController.navigate(Screen.CreateQr.route)
                            "create_qr" -> navController.navigate(Screen.QrTypeSelection.route)
                            "history" -> navController.navigate(Screen.History.route)
                            "favorites" -> navController.navigate(Screen.Favorites.route)
                            "settings" -> navController.navigate(Screen.Settings.route)
                            "share" -> scannerViewModel.shareApp()
                            "our_apps" -> scannerViewModel.openDeveloperPage()
                            "remove_ads" -> navController.navigate(Screen.Premium.route)
                        }
                    }
                })
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Screen.Scanner.route) {
            composable(Screen.Scanner.route) {
                ScannerScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onScanDetected = { scanId -> 
                        navController.navigate(Screen.ScanResult.createRoute(scanId))
                    },
                    viewModel = scannerViewModel
                )
            }
            composable(Screen.Premium.route) {
                com.scannerpro.lectorqr.presentation.ui.premium.PremiumScreen(
                    billingManager = billingManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.QrTypeSelection.route) {
                QrTypeSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onTypeSelected = { typeId ->
                        when (typeId) {
                            "my_qr" -> navController.navigate(Screen.CreateQr.route)
                            "url" -> navController.navigate(Screen.CreateUrl.route)
                            "text" -> navController.navigate(Screen.CreateText.route)
                            "contact" -> navController.navigate(Screen.CreateContact.route)
                            "email" -> navController.navigate(Screen.CreateEmail.route)
                            "sms" -> navController.navigate(Screen.CreateSms.route)
                            "wifi" -> navController.navigate(Screen.CreateWifi.route)
                            "phone" -> navController.navigate(Screen.CreatePhone.route)
                            "location" -> navController.navigate(Screen.CreateLocation.route)
                            "calendar" -> navController.navigate(Screen.CreateCalendar.route)
                            "whatsapp" -> navController.navigate(Screen.CreateWhatsApp.route)
                            "instagram" -> navController.navigate(Screen.CreateInstagram.route)
                            "facebook" -> navController.navigate(Screen.CreateFacebook.route)
                            "youtube" -> navController.navigate(Screen.CreateYouTube.route)
                            "twitter" -> navController.navigate(Screen.CreateTwitter.route)
                            "linkedin" -> navController.navigate(Screen.CreateLinkedIn.route)
                            "settings" -> navController.navigate(Screen.Settings.route)
                            "tiktok" -> navController.navigate(Screen.CreateTikTok.route)
                            "ean8" -> navController.navigate(Screen.CreateEan8.route)
                            "ean13" -> navController.navigate(Screen.CreateEan13.route)
                            "upce" -> navController.navigate(Screen.CreateUpce.route)
                            "upca" -> navController.navigate(Screen.CreateUpca.route)
                            "code39" -> navController.navigate(Screen.CreateCode39.route)
                            "code93" -> navController.navigate(Screen.CreateCode93.route)
                            "code128" -> navController.navigate(Screen.CreateCode128.route)
                            "itf" -> navController.navigate(Screen.CreateItf.route)
                            "pdf417" -> navController.navigate(Screen.CreatePdf417.route)
                            "codabar" -> navController.navigate(Screen.CreateCodabar.route)
                            "datamatrix" -> navController.navigate(Screen.CreateDataMatrix.route)
                            "aztec" -> navController.navigate(Screen.CreateAztec.route)
                            else -> navController.navigate(Screen.CreateText.route)
                        }
                    }
                )
            }
            composable(Screen.CreateUrl.route) {
                CreateUrlScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateText.route) {
                CreateTextScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateEmail.route) {
                CreateEmailScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateSms.route) {
                CreateSmsScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateWifi.route) {
                CreateWifiScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateLocation.route) {
                CreateLocationScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateCalendar.route) {
                CreateCalendarScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreatePhone.route) {
                com.scannerpro.lectorqr.presentation.ui.create.phone.CreatePhoneScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateQr.route) {
                CreateQrScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateWhatsApp.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "WhatsApp",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateInstagram.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "Instagram",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateFacebook.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "Facebook",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateYouTube.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "YouTube",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateTwitter.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "Twitter",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateLinkedIn.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "LinkedIn",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateTikTok.route) {
                com.scannerpro.lectorqr.presentation.ui.create.social.CreateSocialScreen(
                    type = "TikTok",
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            // Barcode formats
            composable(Screen.CreateEan8.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "EAN_8",
                    format = 64,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateEan13.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "EAN_13",
                    format = 32,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateUpce.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "UPC_E",
                    format = 1024,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateUpca.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "UPC_A",
                    format = 512,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateCode39.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "CODE_39",
                    format = 2,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateCode93.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "CODE_93",
                    format = 4,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateCode128.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "CODE_128",
                    format = 1,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateItf.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "ITF",
                    format = 128,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreatePdf417.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "PDF_417",
                    format = 2048,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateCodabar.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "CODABAR",
                    format = 8,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateDataMatrix.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "DATA_MATRIX",
                    format = 16,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateAztec.route) {
                com.scannerpro.lectorqr.presentation.ui.create.barcode.CreateBarcodeScreen(
                    formatName = "AZTEC",
                    format = 4096,
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.Settings.route) {
                com.scannerpro.lectorqr.presentation.ui.settings.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.CreateContact.route) {
                CreateContactScreen(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onResultSelected = { result -> 
                        if (scannerViewModel.isProfileScan(result.id)) {
                            navController.navigate(Screen.CreateQr.route)
                        } else {
                            navController.navigate(Screen.ScanResult.createRoute(result.id))
                        }
                    }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onResultSelected = { result -> 
                        if (scannerViewModel.isProfileScan(result.id)) {
                            navController.navigate(Screen.CreateQr.route)
                        } else {
                            navController.navigate(Screen.ScanResult.createRoute(result.id))
                        }
                    }
                )
            }
            composable(
                route = Screen.ScanResult.route,
                arguments = listOf(
                    androidx.navigation.navArgument("scanId") { type = androidx.navigation.NavType.LongType }
                )
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getLong("scanId") ?: -1L
                ScanResultScreen(
                    scanId = scanId,
                    onBack = { navController.popBackStack() },
                    onEdit = { route -> navController.navigate(route) }
                )
            }
        }
    }
}

@Composable
fun ScannerScreen(
    onMenuClick: () -> Unit,
    onScanDetected: (Long) -> Unit, // Still useful for history navigation
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanResultUiState by viewModel.scanResultUiState.collectAsState()

    val context = LocalContext.current
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showRationaleDialog = true
            } else {
                showSettingsDialog = true
            }
        }
    }

    // Re-check permission when resuming from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (isGranted && !hasCameraPermission) {
                    hasCameraPermission = true
                    showSettingsDialog = false
                    showRationaleDialog = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        android.util.Log.e("ScannerScreen", "Gallery result received: $uri")
        uri?.let { viewModel.scanFromGallery(it) }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.isGalleryRequested) {
        if (uiState.isGalleryRequested) {
            android.util.Log.e("ScannerScreen", "isGalleryRequested is true, launching gallery")
            galleryLauncher.launch("image/*")
            viewModel.onGalleryPickerLaunched()
        }
    }

    // Back handler to clear local result before closing activity/navigating back
    BackHandler(enabled = scanResultUiState.result != null) {
        viewModel.onResultHandled()
    }

    Scaffold(
        topBar = {
            if (scanResultUiState.result == null) {
                ScannerTopBar(
                    isFlashEnabled = uiState.isFlashEnabled,
                    isBatchScanEnabled = uiState.isBatchScanEnabled,
                    isBatchModeActive = uiState.isBatchModeActive,
                    onMenuClick = onMenuClick,
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onFlashToggle = { viewModel.toggleFlash() },
                    onCameraFlip = { viewModel.flipCamera() },
                    onBatchModeToggle = { viewModel.toggleBatchMode() }
                )
            }
        }
    ) { paddingValues ->
        if (scanResultUiState.result != null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                ScanResultContent(
                    uiState = scanResultUiState,
                    onBack = { viewModel.onResultHandled() },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onOpenRename = { viewModel.openRenameDialog() },
                    onCloseRename = { viewModel.closeRenameDialog() },
                    onSaveName = { viewModel.saveName() },
                    onUpdateRenameInput = { viewModel.updateRenameInput(it) },
                    onDelete = { viewModel.deleteScan() },
                    onExportTxt = { share -> viewModel.exportAsTxt(share) },
                    onExportCsv = { share -> viewModel.exportAsCsv(share) },
                    onGetSearchUrl = { viewModel.getSearchUrl(it) },
                    onShare = { /* shared in ScanResultContent */ },
                    onEdit = { /* No-op for live scans */ },
                    onSaveQr = { viewModel.saveQrToGallery() }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (hasCameraPermission) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isFlashEnabled = uiState.isFlashEnabled,
                        isFrontCamera = uiState.isFrontCamera,
                        zoomRatio = uiState.zoomRatio,
                        isAutofocusEnabled = uiState.isAutofocusEnabled,
                        isTapToFocusEnabled = uiState.isTapToFocusEnabled,
                        cameraSelection = uiState.cameraSelection,
                        onZoomRangeChanged = { min, max ->
                            viewModel.onZoomRangeChanged(min, max)
                        },
                        onBarcodeDetected = { barcode, bitmap ->
                            viewModel.handleBarcode(barcode, bitmap)
                        }
                    )

                    ScannerViewfinder()

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.onZoomChanged((uiState.zoomRatio - 0.5f).coerceAtLeast(uiState.minZoomRatio)) }) {
                                Icon(
                                    Icons.Default.ZoomOut, 
                                    contentDescription = stringResource(R.string.zoom_out), 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                value = uiState.zoomRatio.coerceIn(uiState.minZoomRatio, uiState.maxZoomRatio),
                                onValueChange = { viewModel.onZoomChanged(it) },
                                valueRange = uiState.minZoomRatio..uiState.maxZoomRatio,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                            IconButton(onClick = { viewModel.onZoomChanged((uiState.zoomRatio + 0.5f).coerceAtMost(uiState.maxZoomRatio)) }) {
                                Icon(
                                    Icons.Default.ZoomIn, 
                                    contentDescription = stringResource(R.string.zoom_in), 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Banner Ad at the bottom of the scanner screen
                com.scannerpro.lectorqr.presentation.ui.components.BannerAdView(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // --- Permission Dialogs ---
                if (showRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Essential permission, no dismiss */ },
                        title = { Text(stringResource(R.string.permission_camera_title)) },
                        text = { Text(stringResource(R.string.permission_camera_rationale)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRationaleDialog = false
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.permission_grant))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { (context as? Activity)?.finish() }) {
                                Text("Salir", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }

                if (showSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Essential permission, no dismiss */ },
                        title = { Text(stringResource(R.string.permission_camera_required)) },
                        text = { Text(stringResource(R.string.permission_settings_msg)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.permission_go_to_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { (context as? Activity)?.finish() }) {
                                Text("Salir", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }
}
