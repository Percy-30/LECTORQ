package com.scannerpro.lectorqr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LectorQRApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.google.android.gms.ads.MobileAds.initialize(this) {}
    }
}
