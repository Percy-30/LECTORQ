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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import com.scannerpro.lectorqr.presentation.navigation.Screen
import com.scannerpro.lectorqr.presentation.ui.create.CreateQrScreen
import com.scannerpro.lectorqr.presentation.ui.favorites.FavoritesScreen
import com.scannerpro.lectorqr.presentation.ui.history.HistoryScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(onItemClick = { item ->
                    scope.launch {
                        drawerState.close()
                        when(item.route) {
                            "scanner" -> navController.navigate(Screen.Scanner.route) {
                                popUpTo(Screen.Scanner.route) { inclusive = true }
                            }
                            "create_qr" -> navController.navigate(Screen.CreateQr.route)
                            "history" -> navController.navigate(Screen.History.route)
                            "favorites" -> navController.navigate(Screen.Favorites.route)
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
                    }
                )
            }
            composable(Screen.CreateQr.route) {
                CreateQrScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onResultSelected = { result -> 
                        navController.navigate(Screen.ScanResult.createRoute(result.id))
                    }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onResultSelected = { result -> 
                        navController.navigate(Screen.ScanResult.createRoute(result.id))
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
                    onBack = { navController.popBackStack() }
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

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.scanFromGallery(it) }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
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
                    onMenuClick = onMenuClick,
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onFlashToggle = { viewModel.toggleFlash() },
                    onCameraFlip = { viewModel.flipCamera() }
                )
            }
        }
    ) { paddingValues ->
        if (scanResultUiState.result != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                ScanResultContent(
                    uiState = scanResultUiState,
                    onBack = { viewModel.onResultHandled() },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onOpenRename = { viewModel.openRenameDialog() },
                    onCloseRename = { viewModel.closeRenameDialog() },
                    onSaveName = { viewModel.saveName() },
                    onRenameInputChange = { viewModel.updateRenameInput(it) }
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
                        onBarcodeDetected = { barcode, bitmap ->
                            viewModel.handleBarcode(barcode, bitmap)
                        }
                    )

                    ScannerViewfinder()

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.onZoomChanged((uiState.zoomRatio - 0.5f).coerceAtLeast(1.0f)) }) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                            }
                            Slider(
                                value = uiState.zoomRatio,
                                onValueChange = { viewModel.onZoomChanged(it) },
                                valueRange = 1.0f..10.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            IconButton(onClick = { viewModel.onZoomChanged((uiState.zoomRatio + 0.5f).coerceAtMost(10.0f)) }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                            }
                        }
                    }
                }
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}
