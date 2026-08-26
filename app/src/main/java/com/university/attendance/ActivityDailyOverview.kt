package com.university.attendance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityDailyOverviewBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Screen: Admin -> Daily Overview.
 * Shows every class (across ALL departments/sessions) for a selected date,
 * grouped Department -> Session -> Subject, with each subject marked
 * either "X/Y Present" (attendance was recorded) or "Not Marked" (no
 * attendance_records exist for that class+subject+date combination).
 *
 * Defaults to today's date on open.
 */
class ActivityDailyOverview : AppCompatActivity() {

    private lateinit var binding: ActivityDailyOverviewBinding
    private lateinit var viewModel: DailyOverviewViewModel
    private lateinit var adapter: DailyOverviewAdapter

    private val storageFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDailyOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DailyOverviewViewModel::class.java]

        adapter = DailyOverviewAdapter(emptyList())
        binding.recyclerOverview.layoutManager = LinearLayoutManager(this)
        binding.recyclerOverview.adapter = adapter

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.dateSelector.setOnClickListener { showDatePicker() }

        updateDateLabel(viewModel.selectedDate)
        observeViewModel()

        viewModel.loadOverview()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        try {
            calendar.time = storageFormat.parse(viewModel.selectedDate) ?: Date()
        } catch (e: Exception) {
            calendar.time = Date()
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val newDate = storageFormat.format(calendar.time)
                updateDateLabel(newDate)
                viewModel.setDate(newDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun updateDateLabel(dateString: String) {
        val todayString = storageFormat.format(Date())
        binding.tvSelectedDate.text = if (dateString == todayString) {
            "Today"
        } else {
            try {
                displayFormat.format(storageFormat.parse(dateString) ?: Date())
            } catch (e: Exception) {
                dateString
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is DailyOverviewViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is DailyOverviewViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.groups.observe(this) { groups ->
            adapter.updateGroups(groups)
            val hasAnyClasses = groups.any { it.sessionGroups.any { s -> s.classStatuses.isNotEmpty() } }
            binding.tvEmptyState.visibility = if (hasAnyClasses) View.GONE else View.VISIBLE
            binding.recyclerOverview.visibility = if (hasAnyClasses) View.VISIBLE else View.GONE
        }

        viewModel.summary.observe(this) { summary ->
            binding.tvHeldCount.text = summary.heldCount.toString()
            binding.tvNotMarkedCount.text = summary.notMarkedCount.toString()
        }
    }
}