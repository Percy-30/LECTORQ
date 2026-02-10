package com.scannerpro.lectorqr.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.scannerpro.lectorqr.util.AdConfig

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.BANNER_AD_ID
) {
    val isPremium = com.scannerpro.lectorqr.presentation.ui.theme.LocalIsPremium.current
    
    // Debug log
    android.util.Log.d("BannerAdView", "isPremium: $isPremium")
    
    if (isPremium) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // no-op
        }
    )
}
