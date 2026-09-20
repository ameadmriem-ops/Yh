package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ads.AdConfig
import com.example.data.ads.AdRewardManager
import com.example.data.model.Channel
import com.example.data.repository.LocalStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalStorageRepository(application)
    private val adManager = AdRewardManager(application)

    // قائمة القنوات - تبدأ فارغة تماماً بدون أي قنوات افتراضية
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    // عدد الإعلانات المشاهدة اليوم (0 إلى 3)
    private val _watchCount = MutableStateFlow(0)
    val watchCount: StateFlow<Int> = _watchCount.asStateFlow()

    // حالة فتح المشاهدة لليوم
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // القناة الجاري تشغيلها حالياً
    private val _currentPlayingChannel = MutableStateFlow<Channel?>(null)
    val currentPlayingChannel: StateFlow<Channel?> = _currentPlayingChannel.asStateFlow()

    // إظهار نافذة إضافة قناة
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // إظهار نافذة تفاصيل الإعلانات
    private val _showAdSettingsDialog = MutableStateFlow(false)
    val showAdSettingsDialog: StateFlow<Boolean> = _showAdSettingsDialog.asStateFlow()

    // رسائل للمستخدم
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadDataFromLocalStorage()
    }

    /**
     * قراءة القنوات ونظام المشاهدة من localStorage
     * لا توجد أي قنوات افتراضية إطلاقاً
     */
    fun loadDataFromLocalStorage() {
        val storedChannels = repository.getChannels()
        _channels.value = storedChannels

        val currentCount = repository.getWatchCount()
        _watchCount.value = currentCount
        _isUnlocked.value = repository.isUnlockedForToday()
    }

    /**
     * إضافة قناة يدوياً وحفظها في localStorage
     */
    fun addChannel(name: String, url: String, category: String) {
        if (name.isBlank() || url.isBlank()) {
            _snackbarMessage.value = "يرجى إدخال اسم القناة ورابط البث"
            return
        }

        viewModelScope.launch {
            repository.addChannel(name, url, category)
            _channels.value = repository.getChannels()
            _showAddDialog.value = false
            _snackbarMessage.value = "تمت إضافة القناة بنجاح"
        }
    }

    /**
     * حذف قناة من التخزين المحلي
     */
    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
            _channels.value = repository.getChannels()
            if (_currentPlayingChannel.value?.id == channelId) {
                _currentPlayingChannel.value = null
            }
            _snackbarMessage.value = "تم حذف القناة"
        }
    }

    /**
     * حذف جميع القنوات لتصبح القائمة فارغة
     */
    fun clearAllChannels() {
        viewModelScope.launch {
            repository.clearAllChannels()
            _channels.value = emptyList()
            _currentPlayingChannel.value = null
            _snackbarMessage.value = "تم حذف جميع القنوات"
        }
    }

    /**
     * بدء مشاهدة إعلان مكافأة لزيادة الرصيد اليومي:
     * - يعرض الإعلان الحقيقي إذا وجد.
     * - إذا لم يجد التطبيق إعلاناً جاهزاً، يتم احتساب المشاهدة مباشرة للمستخدم.
     */
    fun watchRewardAd(activity: Activity) {
        if (_isUnlocked.value) {
            _snackbarMessage.value = "المشاهدة مفتوحة بالفعل لليوم بالكامل!"
            return
        }

        adManager.showRewardedAd(
            activity = activity,
            onRewardEarned = { wasAdShown ->
                onUserEarnedRewardCallback(wasAdShown)
            },
            onAdDismissed = {
                // تم إغلاق الإعلان
            }
        )
    }

    /**
     * احتساب المشاهدة وتحديث حالة القنوات
     */
    fun onUserEarnedRewardCallback(wasAdShown: Boolean = true) {
        val newCount = repository.incrementWatchCount()
        _watchCount.value = newCount
        val unlocked = repository.isUnlockedForToday()
        _isUnlocked.value = unlocked

        if (unlocked) {
            _snackbarMessage.value = "تهانينا! اكتملت المشاهدات (3/3)، تم فتح جميع القنوات حتى الغد!"
        } else {
            if (wasAdShown) {
                _snackbarMessage.value = "تم احتساب المشاهدة ($newCount / 3). تبقى ${3 - newCount} لفتح القنوات."
            } else {
                _snackbarMessage.value = "لا يتوفر إعلان حالياً، تم احتساب المشاهدة لك مباشرة! ($newCount / 3)"
            }
        }
    }

    /**
     * محاولة تشغيل قناة
     */
    fun playChannel(channel: Channel) {
        if (!_isUnlocked.value) {
            _snackbarMessage.value = "يجب إكمال مشاهدة إعلانات اليوم (3/3) لتشغيل القنوات. المشاهدات الحالية: ${_watchCount.value}/3"
            return
        }
        _currentPlayingChannel.value = channel
    }

    fun stopPlayback() {
        _currentPlayingChannel.value = null
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun setShowAdSettingsDialog(show: Boolean) {
        _showAdSettingsDialog.value = show
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
