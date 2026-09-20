package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير إعلانات المكافأة (Rewarded Ads Manager)
 * - يستخدم حصراً معرف الإعلان الحقيقي الرسمي من Google AdMob.
 * - في حال عدم وجود إعلان متاح (No ad fill / not ready / load error)، يتم احتساب المشاهدة مباشرة للمستخدم.
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
     * تحميل إعلان المكافأة الحقيقي مسبقاً (RewardedAd.load)
     */
    fun preloadRewardedAd() {
        if (isAdLoading || rewardedAd != null) return

        isAdLoading = true
        val adUnitId = AdConfig.REWARDED_AD_UNIT_ID
        Log.d(TAG, "Preloading real Rewarded Ad: $adUnitId")

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
                    Log.d(TAG, "Real Rewarded Ad successfully loaded!")
                    rewardedAd = ad
                    isAdLoading = false
                    _isAdLoaded.value = true
                }
            }
        )
    }

    /**
     * عرض إعلان المكافأة الحقيقي:
     * - إذا وجد الإعلان: يعرضه بالكامل ويحتسب المكافأة عند انتهائه.
     * - إذا لم يجد التطبيق إعلاناً جاهزاً (أو تعذر عرضه): يتم احتسابه فوراً للمستخدم.
     *
     * @param activity النشاط الحالي
     * @param onRewardEarned استدعاء احتساب المكافأة مع معامل يوضح إن كان الإعلان قد عُرض أم احتُسب لعدم توفره
     * @param onAdDismissed استدعاء عند إغلاق الإعلان بعد المشاهدة
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (wasAdShown: Boolean) -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Real Ad was dismissed by user")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    preloadRewardedAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Ad failed to show: ${adError.message}. Counting reward automatically as requested.")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    preloadRewardedAd()
                    onRewardEarned(false)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Real Ad showed full screen content")
                }
            }

            val rewardListener = OnUserEarnedRewardListener { rewardItem ->
                Log.d(TAG, "User earned reward from AdMob: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned(true)
            }

            currentAd.show(activity, rewardListener)
        } else {
            // لم يجد التطبيق إعلاناً جاهزاً -> يتم احتسابه مباشرة
            Log.d(TAG, "No ad available or ad not ready. Crediting view directly as requested.")
            preloadRewardedAd()
            onRewardEarned(false)
        }
    }
}
