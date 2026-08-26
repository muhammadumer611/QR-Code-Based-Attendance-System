package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DashboardFeedViewModel(
    private val repository: DashboardFeedRepository = DashboardFeedRepository()
) : ViewModel() {

    private val _recentActivities = MutableLiveData<List<Log>>(emptyList())
    val recentActivities: LiveData<List<Log>> = _recentActivities

    private val _notifications = MutableLiveData<List<AppNotification>>(emptyList())
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    fun loadRecentActivities() {
        viewModelScope.launch {
            try {
                _recentActivities.value = repository.getRecentActivities()
            } catch (e: Exception) {
                _recentActivities.value = emptyList()
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _notifications.value = repository.getNotifications()
                _unreadCount.value = repository.getUnreadNotificationCount()
            } catch (e: Exception) {
                _notifications.value = emptyList()
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
            loadNotifications() // refresh list + badge count
        }
    }
}