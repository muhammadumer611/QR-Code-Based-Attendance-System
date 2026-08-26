package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemNotificationBinding

/**
 * Adapter for the Notifications list. Unread notifications show a filled
 * accent dot and bold title; read ones are dimmed. Tapping a row marks
 * it read via onMarkRead.
 */
class NotificationAdapter(
    private val notifications: List<AppNotification>,
    private val onMarkRead: (notificationId: String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.tvTitle.text = notification.title
            binding.tvDescription.text = notification.description
            binding.tvTimeAgo.text = RecentActivityAdapter.formatRelativeTime(notification.timestamp?.time)
            binding.ivIcon.setImageResource(RecentActivityAdapter.iconForType(notification.type))

            binding.unreadDot.visibility = if (notification.isRead) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            binding.tvTitle.alpha = if (notification.isRead) 0.6f else 1.0f
            binding.tvDescription.alpha = if (notification.isRead) 0.6f else 1.0f

            binding.root.setOnClickListener {
                if (!notification.isRead) onMarkRead(notification.notificationId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}