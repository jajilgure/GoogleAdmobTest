/*
 * Copyright (C) 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.example.nativeadvancedexample

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.android.synthetic.main.activity_main.ad_frame
import kotlinx.android.synthetic.main.activity_main.refresh_button
import kotlinx.android.synthetic.main.activity_main.start_muted_checkbox
import kotlinx.android.synthetic.main.activity_main.videostatus_text

const val ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
var currentNativeAd: NativeAd? = null

/**
 * A simple activity class that displays native ad formats.
 */
class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Initialize the Mobile Ads SDK.
    // FIXME:
    //  2 ways that I used to initialize Google Admob.
    //  No matter which option I chose, 'android.os.strictmode.IncorrectContextUseViolation' message logged.
    MobileAds.initialize(this@MainActivity) {} // FIXME way 1
//    MobileAds.initialize(MyApplication.getInstance()) {} // FIXME way 2

    refresh_button.setOnClickListener { refreshAd() }

    refreshAd()
  }

  /**
   * Populates a [UnifiedNativeAdView] object with data from a given
   * [UnifiedNativeAd].
   *
   * @param nativeAd the object containing the ad's assets
   * @param adView the view to be populated
   */
  private fun populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Set the media view.
    adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)

    // Set other ad assets.
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.priceView = adView.findViewById(R.id.ad_price)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    adView.storeView = adView.findViewById(R.id.ad_store)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

    // The headline and media content are guaranteed to be in every UnifiedNativeAd.
    (adView.headlineView as TextView).text = nativeAd.headline
    adView.mediaView?.setMediaContent(nativeAd.mediaContent)

    // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
    // check before trying to display them.
    if (nativeAd.body == null) {
      adView.bodyView?.visibility = View.INVISIBLE
    } else {
      adView.bodyView?.visibility = View.VISIBLE
      (adView.bodyView as TextView).text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
      adView.callToActionView?.visibility = View.INVISIBLE
    } else {
      adView.callToActionView?.visibility = View.VISIBLE
      (adView.callToActionView as Button).text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
      adView.iconView?.visibility = View.GONE
    } else {
      (adView.iconView as ImageView).setImageDrawable(
        nativeAd.icon?.drawable
      )
      adView.iconView?.visibility = View.VISIBLE
    }

    if (nativeAd.price == null) {
      adView.priceView?.visibility = View.INVISIBLE
    } else {
      adView.priceView?.visibility = View.VISIBLE
      (adView.priceView as TextView).text = nativeAd.price
    }

    if (nativeAd.store == null) {
      adView.storeView?.visibility = View.INVISIBLE
    } else {
      adView.storeView?.visibility = View.VISIBLE
      (adView.storeView as TextView).text = nativeAd.store
    }

    if (nativeAd.starRating == null) {
      adView.starRatingView?.visibility = View.INVISIBLE
    } else {
      (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
      adView.starRatingView?.visibility = View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
      adView.advertiserView?.visibility = View.INVISIBLE
    } else {
      (adView.advertiserView as TextView).text = nativeAd.advertiser
      adView.advertiserView?.visibility = View.VISIBLE
    }

    // This method tells the Google Mobile Ads SDK that you have finished populating your
    // native ad view with this native ad.
    adView.setNativeAd(nativeAd)
  }

  /**
   * Creates a request for a new native ad based on the boolean parameters and calls the
   * corresponding "populate" method when one is successfully returned.
   *
   */
  private fun refreshAd() {
    refresh_button.isEnabled = false

    val builder = AdLoader.Builder(this, ADMOB_AD_UNIT_ID)

    builder.forNativeAd { nativeAd ->
      // OnUnifiedNativeAdLoadedListener implementation.
      // If this callback occurs after the activity is destroyed, you must call
      // destroy and return or you may get a memory leak.
      var activityDestroyed = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        activityDestroyed = isDestroyed
      }
      if (activityDestroyed || isFinishing || isChangingConfigurations) {
        nativeAd.destroy()
        return@forNativeAd
      }
      // You must call destroy on old ads when you are done with them,
      // otherwise you will have a memory leak.
      currentNativeAd?.destroy()
      currentNativeAd = nativeAd
      val adView = layoutInflater
        .inflate(R.layout.ad_unified, null) as NativeAdView
      populateUnifiedNativeAdView(nativeAd, adView)
      ad_frame.removeAllViews()
      ad_frame.addView(adView)
    }

    val videoOptions = VideoOptions.Builder()
      .setStartMuted(start_muted_checkbox.isChecked)
      .build()

    val adOptions = NativeAdOptions.Builder()
      .setVideoOptions(videoOptions)
      .build()

    builder.withNativeAdOptions(adOptions)

    val adLoader = builder.withAdListener(object : AdListener() {
      override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        val error =
          """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
        refresh_button.isEnabled = true
        Toast.makeText(
          this@MainActivity, "Failed to load native ad with error $error",
          Toast.LENGTH_SHORT
        ).show()

        Log.e("hans.ryu", error)
      }
    }).build()

    adLoader.loadAd(AdRequest.Builder().build())

    videostatus_text.text = ""
  }

  override fun onDestroy() {
    currentNativeAd?.destroy()
    super.onDestroy()
  }
}
