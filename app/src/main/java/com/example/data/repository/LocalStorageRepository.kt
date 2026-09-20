package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Channel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * مستودع التخزين المحلي (LocalStorage / SharedPreferences)
 * - لا يحتوي على أي قنوات افتراضية أو تجريبية إطلاقاً
 * - يبدأ بقائمة فارغة تماماً emptyList()
 * - يحفظ ويقرأ القنوات ونظام المشاهدة اليومية
 */
class LocalStorageRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("my_iptv_local_storage", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val channelListType = Types.newParameterizedType(List::class.java, Channel::class.java)
    private val channelAdapter = moshi.adapter<List<Channel>>(channelListType)

    companion object {
        private const val KEY_CHANNELS_JSON = "iptv_channels_json"
        private const val KEY_WATCH_COUNT = "iptv_daily_watch_count"
        private const val KEY_LAST_WATCH_DATE = "iptv_last_watch_date"
        const val MAX_DAILY_WATCH = 3
    }

    // ------------------------------------------------------------------------
    // إدارة القنوات (Channels Storage)
    // ------------------------------------------------------------------------

    /**
     * قراءة القنوات من التخزين المحلي.
     * إذا لم توجد بيانات، تُترك القائمة فارغة تماماً بدون أي fallback.
     */
    fun getChannels(): List<Channel> {
        val json = prefs.getString(KEY_CHANNELS_JSON, null)
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            channelAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * حفظ قائمة القنوات في التخزين المحلي
     */
    fun saveChannels(channels: List<Channel>) {
        val json = channelAdapter.toJson(channels)
        prefs.edit().putString(KEY_CHANNELS_JSON, json).apply()
    }

    /**
     * إضافة قناة يدوياً وحفظها في التخزين المحلي
     */
    fun addChannel(name: String, url: String, category: String): Channel {
        val currentList = getChannels().toMutableList()
        val newChannel = Channel(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            streamUrl = url.trim(),
            category = if (category.isBlank()) "عامة" else category.trim(),
            addedTimestamp = System.currentTimeMillis()
        )
        currentList.add(0, newChannel) // Add to top
        saveChannels(currentList)
        return newChannel
    }

    /**
     * حذف قناة محددة
     */
    fun deleteChannel(channelId: String) {
        val currentList = getChannels().toMutableList()
        currentList.removeAll { it.id == channelId }
        saveChannels(currentList)
    }

    /**
     * حذف جميع القنوات لتصبح القائمة فارغة تماماً
     */
    fun clearAllChannels() {
        prefs.edit().remove(KEY_CHANNELS_JSON).apply()
    }

    // ------------------------------------------------------------------------
    // نظام المشاهدة اليومية (Daily Watch System: 0/3 -> 1/3 -> 2/3 -> 3/3)
    // ------------------------------------------------------------------------

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    /**
     * التحقق من اليوم وتصفير العداد إذا بدأ يوم جديد
     */
    fun checkAndResetDailyWatch() {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_WATCH_DATE, "")
        if (lastDate != today) {
            // يوم جديد: إعادة التعيين إلى 0
            prefs.edit()
                .putString(KEY_LAST_WATCH_DATE, today)
                .putInt(KEY_WATCH_COUNT, 0)
                .apply()
        }
    }

    /**
     * الحصول على عدد مرات المشاهدة اليومية الحالية (0 إلى 3)
     */
    fun getWatchCount(): Int {
        checkAndResetDailyWatch()
        return prefs.getInt(KEY_WATCH_COUNT, 0).coerceIn(0, MAX_DAILY_WATCH)
    }

    /**
     * التحقق مما إذا كانت المشاهدة مفتوحة لليوم (تم إكمال 3/3)
     */
    fun isUnlockedForToday(): Boolean {
        return getWatchCount() >= MAX_DAILY_WATCH
    }

    /**
     * زيادة عداد المشاهدة عند استلام Reward callback
     * لا يتم الاستدعاء إلا بعد تأكيد المكافأة من الإعلان
     */
    fun incrementWatchCount(): Int {
        checkAndResetDailyWatch()
        val current = prefs.getInt(KEY_WATCH_COUNT, 0)
        val next = (current + 1).coerceAtMost(MAX_DAILY_WATCH)
        prefs.edit()
            .putInt(KEY_WATCH_COUNT, next)
            .putString(KEY_LAST_WATCH_DATE, getTodayDateString())
            .apply()
        return next
    }
}
