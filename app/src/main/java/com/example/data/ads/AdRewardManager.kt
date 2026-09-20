package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير إعلانات المكافأة (Rewarded Ads Manager)
 * يضمن الالتزام الصارم بقواعد الاختبار:
 * - استخدام معرف Google الرسمي لإعلانات الاختبار عندما يكون IS_TEST_AD = true
 * - عدم استخدام الإعلان الحقيقي في وضع الاختبار
 * - عدم احتساب أي مكافأة إلا بعد وصول Reward Callback
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
     * تحميل إعلان المكافأة مسبقاً
     */
    fun preloadRewardedAd() {
        if (isAdLoading || rewardedAd != null) return

        isAdLoading = true
        val adUnitId = AdConfig.getActiveRewardedAdUnitId()
        Log.d(TAG, "Loading Rewarded Ad with Unit ID: $adUnitId (IS_TEST_AD = ${AdConfig.IS_TEST_AD})")

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
     * عرض إعلان المكافأة
     * @param activity النشاط الحالي
     * @param onRewardEarned استدعاء عند تحقق المكافأة فقط (Reward callback)
     * @param onAdDismissed استدعاء عند إغلاق الإعلان
     * @param onFallbackNeeded إذا تعذر عرض الإعلان عبر Google Play Services في بيئة الاختبار
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

            currentAd.show(activity) { rewardItem ->
                // لا يتم احتساب الإعلان إلا بعد وصول هذا الـ Reward callback
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardGranted = true
                onRewardEarned()
            }
        } else {
            // الإعلان لم يجهز بعد أو غير متاح في بيئة المحاكي/الاختبار
            Log.d(TAG, "Rewarded ad is not ready. Triggering test fallback.")
            onFallbackNeeded()
            preloadRewardedAd()
        }
    }
}
