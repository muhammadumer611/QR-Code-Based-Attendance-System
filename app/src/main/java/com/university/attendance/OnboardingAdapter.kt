package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemonboardingBinding

class OnboardingAdapter(
    private val list: List<OnboardingModel>
) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    // FIX 1: ViewHolder type was wrong (itemonboarding -> ItemOnboardingBinding)
    inner class ViewHolder(val binding: ItemonboardingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // FIX 2: Binding class name corrected
        val binding = ItemonboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.imgOnboard.setImageResource(item.image)
        holder.binding.txtTitle.text = item.title
        holder.binding.txtDesc.text = item.desc
        holder.binding.txtBadge.text = item.badge // Add badge field to model

        val anim = AnimationUtils.loadAnimation(
            holder.itemView.context,
            R.anim.slide_up_fade  // Better animation
        )
        holder.binding.root.startAnimation(anim)
    }
}