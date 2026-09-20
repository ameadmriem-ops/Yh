package com.example.data.ads

import com.example.BuildConfig

/**
 * إعدادات إعلانات AdMob
 * - في نسخة Release Production: يتم استخدام المعرفات الحقيقية فقط وبشكل قطعي.
 * - إعلانات الاختبار متاحة حصراً في وضع Debug للتطوير (BuildConfig.DEBUG).
 */
object AdConfig {
    // --------------------------------------------------------------------
    // المعرفات الحقيقية الرسمية (Real IDs) - المستخدمة لنسخة Release Production
    // --------------------------------------------------------------------
    const val REAL_ADMOB_APP_ID = "ca-app-pub-8410578267301371~7948926096"
    const val REAL_AD_UNIT_ID = "ca-app-pub-8410578267301371/4181816334"

    /**
     * المعرف الفعلي لوحدة الإعلان:
     * - في Release: يعود دائماً بـ REAL_AD_UNIT_ID ("ca-app-pub-8410578267301371/4181816334").
     * - في Debug: يعود بالقيمة المعرفة للتطوير من BuildConfig.
     */
    val REWARDED_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) {
            BuildConfig.REWARDED_AD_UNIT_ID
        } else {
            REAL_AD_UNIT_ID
        }

    /**
     * معرف تطبيق AdMob
     */
    val ADMOB_APP_ID: String
        get() = if (BuildConfig.DEBUG) {
            BuildConfig.ADMOB_APP_ID
        } else {
            REAL_ADMOB_APP_ID
        }

    /**
     * التحقق من وضع الاختبار:
     * - في Release: دائمًا false قطعياً.
     * - في Debug: متاح فقط أثناء التطوير.
     */
    val isTestAdMode: Boolean
        get() = BuildConfig.DEBUG && debugSimulationActive

    // متغير تحكم محلي للتطوير فقط في وضع Debug
    var debugSimulationActive: Boolean = BuildConfig.DEBUG
}
