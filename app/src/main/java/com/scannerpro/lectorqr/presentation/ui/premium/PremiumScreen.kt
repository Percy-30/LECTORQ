package com.scannerpro.lectorqr.presentation.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scannerpro.lectorqr.util.BillingManager
import com.scannerpro.lectorqr.util.AdConfig
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Restore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import com.scannerpro.lectorqr.R
import com.scannerpro.lectorqr.domain.model.PremiumPlan
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    billingManager: BillingManager,
    onBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isPremium = com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium.current

    LaunchedEffect(billingManager) {
        viewModel.setBillingManager(billingManager)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_premium)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E1E),
                            Color(0xFF121212)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium Icon
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).fillMaxSize(),
                        tint = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isPremium) stringResource(R.string.premium_active_msg) else stringResource(R.string.premium_unlock_msg),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.premium_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Benefit List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BenefitItem(stringResource(R.string.benefit_no_ads))
                    BenefitItem(stringResource(R.string.benefit_unlimited_history))
                    BenefitItem(stringResource(R.string.benefit_create_all))
                    BenefitItem(stringResource(R.string.benefit_biometrics))
                    BenefitItem(stringResource(R.string.benefit_custom_qr))
                    BenefitItem(stringResource(R.string.benefit_app_icons))
                    BenefitItem(stringResource(R.string.benefit_speed))
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isPremium) {
                    Text(
                        text = stringResource(R.string.select_plan),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Plans
                    PremiumPlan.values().forEach { plan ->
                        val displayedPrice = uiState.dynamicPrices[plan.id] ?: plan.price
                        PlanCard(
                            title = stringResource(plan.titleResId),
                            price = displayedPrice,
                            period = stringResource(plan.periodResId),
                            isSelected = uiState.selectedPlan == plan,
                            isHighlighted = plan == PremiumPlan.YEARLY,
                            onClick = { viewModel.selectPlan(plan) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Green
                    )
                    Text(
                        stringResource(R.string.subscription_active),
                        color = Color.Green,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Button(
                    onClick = {
                        viewModel.purchaseSelectedPlan { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "¡PREMIUM ACTIVADO! ✅", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (uiState.selectedPlan == PremiumPlan.LIFETIME) 
                            stringResource(R.string.get_lifetime) 
                        else 
                            stringResource(R.string.start_free_trial),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { billingManager.queryPurchases() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.restore_purchase), color = Color.Gray, fontSize = 12.sp)
                    }
                }


            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color(0xFF252525)
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else if (isHighlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(period, color = Color.Gray, fontSize = 12.sp)
            }
            Text(price, color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}



@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}