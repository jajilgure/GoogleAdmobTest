package com.google.android.gms.example.nativeadvancedexample

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.Violation
import android.util.Log
import com.google.android.gms.ads.MobileAds

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        sSingleton = this

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll().build()
        )
    }

    companion object {
        private var sSingleton: MyApplication? = null

        fun getInstance(): MyApplication {
            return sSingleton!!
        }
    }
}