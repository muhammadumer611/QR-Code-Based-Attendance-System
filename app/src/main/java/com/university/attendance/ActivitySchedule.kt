package com.university.attendance

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityScheduleBinding
import com.university.attendance.databinding.DialogScheduleUploadBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Screen: Admin -> Schedule.
 *
 * Placeholder implementation per current requirements: stores only the
 * schedule file's NAME and an optional note/link in Firestore -- no real
 * PDF upload/storage yet (that needs Firebase Storage, deferred for now).
 * "View" opens the note as a link if one was provided; otherwise it
 * explains that no viewable file/link exists yet.
 */
class ActivitySchedule : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var viewModel: ScheduleViewModel

    private val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ScheduleViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.btnUpload.setOnClickListener { showUploadDialog() }
        binding.btnView.setOnClickListener { handleView() }

        observeViewModel()
        viewModel.loadSchedule()
    }

    private fun showUploadDialog() {
        val dialogBinding = DialogScheduleUploadBinding.inflate(LayoutInflater.from(this))

        // Pre-fill with existing values, if any, so re-uploading is easy to edit.
        viewModel.schedule.value?.let { current ->
            dialogBinding.etFileName.setText(current.fileName)
            dialogBinding.etNote.setText(current.note)
        }

        AlertDialog.Builder(this)
            .setTitle("Upload Schedule")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val fileName = dialogBinding.etFileName.text.toString()
                val note = dialogBinding.etNote.text.toString()
                viewModel.saveSchedule(fileName, note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleView() {
        val schedule = viewModel.schedule.value
        if (schedule == null || schedule.fileName.isBlank()) {
            Toast.makeText(this, "No schedule file uploaded yet.", Toast.LENGTH_SHORT).show()
            return
        }
        if (schedule.note.isBlank()) {
            Toast.makeText(
                this,
                "No link available for this file yet -- PDF viewing isn't set up.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Link/Note: ${schedule.note}", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is ScheduleViewModel.UiState.Loading) View.VISIBLE else View.GONE

            when (state) {
                is ScheduleViewModel.UiState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                is ScheduleViewModel.UiState.SaveSuccess ->
                    Toast.makeText(this, "Schedule updated.", Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }

        viewModel.schedule.observe(this) { schedule ->
            if (schedule == null || schedule.fileName.isBlank()) {
                binding.tvFileName.text = "No file uploaded yet"
                binding.tvUploadedInfo.text = ""
            } else {
                binding.tvFileName.text = schedule.fileName
                binding.tvUploadedInfo.text = schedule.uploadedAt?.let {
                    "Uploaded ${displayFormat.format(it)} by ${schedule.uploadedBy}"
                } ?: ""
            }
        }
    }
}