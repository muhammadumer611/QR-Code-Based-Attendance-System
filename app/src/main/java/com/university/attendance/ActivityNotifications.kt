package com.university.attendance

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityNotificationsBinding

/**
 * Screen: Admin -> Notifications (opened by tapping the bell icon on the
 * Dashboard header). Shows all notifications, newest first; tapping an
 * unread one marks it read.
 */
class ActivityNotifications : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var viewModel: DashboardFeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DashboardFeedViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(this)

        observeViewModel()
        viewModel.loadNotifications()
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(this) { notifications ->
            binding.tvEmptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerNotifications.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerNotifications.adapter = NotificationAdapter(notifications) { notificationId ->
                viewModel.markNotificationRead(notificationId)
            }
        }
    }
}