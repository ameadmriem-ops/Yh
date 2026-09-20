package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير إعلانات المكافأة (Rewarded Ads Manager)
 * - في نسخة الإنتاج Release: يستخدم حصراً المعرف الحقيقي ca-app-pub-8410578267301371/4181816334
 * - يحافظ على: RewardedAd.load, RewardedAd.show, RewardedAdLoadCallback, OnUserEarnedRewardListener
 * - لا يتم احتساب أي مكافأة إلا بعد استلام Reward Callback الفعلي من Google AdMob
 */
class AdRewardManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    companion object {
        private const val TAG = "AdRewardManager"
    }

    init {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
        preloadRewardedAd()
    }

    /**
     * تحميل إعلان المكافأة مسبقاً (RewardedAd.load)
     */
    fun preloadRewardedAd() {
        if (isAdLoading || rewardedAd != null) return

        isAdLoading = true
        val adUnitId = AdConfig.REWARDED_AD_UNIT_ID
        Log.d(TAG, "Loading Rewarded Ad with Unit ID: $adUnitId (DEBUG = ${BuildConfig.DEBUG})")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isAdLoading = false
                    _isAdLoaded.value = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad successfully loaded!")
                    rewardedAd = ad
                    isAdLoading = false
                    _isAdLoaded.value = true
                }
            }
        )
    }

    /**
     * عرض إعلان المكافأة (RewardedAd.show)
     * @param activity النشاط الحالي
     * @param onRewardEarned استدعاء عند تحقق المكافأة عبر OnUserEarnedRewardListener
     * @param onAdDismissed استدعاء عند إغلاق الإعلان
     * @param onFallbackNeeded في بيئة الاختبار والتطوير فقط
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit,
        onFallbackNeeded: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardGranted = false

            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    preloadRewardedAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content")
                }
            }

            val rewardListener = OnUserEarnedRewardListener { rewardItem ->
                // لا يتم احتساب الإعلان إلا بعد وصول هذا الـ Reward callback
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardGranted = true
                onRewardEarned()
            }

            currentAd.show(activity, rewardListener)
        } else {
            if (BuildConfig.DEBUG && AdConfig.isTestAdMode) {
                Log.d(TAG, "Rewarded ad is not ready. Triggering test simulation in DEBUG mode.")
                onFallbackNeeded()
            } else {
                Log.w(TAG, "Rewarded ad is not ready yet in Production.")
            }
            preloadRewardedAd()
        }
    }
}
