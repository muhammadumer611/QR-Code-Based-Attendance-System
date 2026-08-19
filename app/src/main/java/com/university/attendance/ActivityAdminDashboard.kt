package com.university.attendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityAdminDashboardBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ActivityAdminDashboard : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var feedViewModel: DashboardFeedViewModel
    private lateinit var searchViewModel: SearchViewModel
    private val statsRepository = StatsRepository()

    // ---------------------------------------------------------------
    // Card filtering (real-time show/hide as the admin types)
    // ---------------------------------------------------------------

    private data class CardSearchTarget(val keywords: List<String>, val view: View)

    // Built lazily so it's only touched after `binding` is inflated in onCreate.
    private val cardSearchTargets by lazy {
        listOf(
            CardSearchTarget(listOf("student", "students", "student management"), binding.cardStudentManagement),
            CardSearchTarget(listOf("teacher", "teachers", "teacher management"), binding.cardTeacherManagement),
            CardSearchTarget(listOf("department", "departments"), binding.cardDepartments),
            CardSearchTarget(listOf("subject", "subjects"), binding.cardSubjects),
            CardSearchTarget(listOf("attendance", "manage records"), binding.cardAttendance),
            CardSearchTarget(listOf("manual", "manual update", "edit attendance"), binding.cardManualUpdate),
            CardSearchTarget(listOf("daily", "daily overview", "today"), binding.cardDailyOverview),
            CardSearchTarget(listOf("assign", "assignment", "teacher subject", "assign subjects"), binding.cardTeacherAssignment),
            CardSearchTarget(listOf("schedule", "timetable"), binding.cardSchedule),
            CardSearchTarget(listOf("report", "reports", "analytics"), binding.cardReports),
            CardSearchTarget(listOf("total students"), binding.cardTotalStudents),
            CardSearchTarget(listOf("total teachers"), binding.cardTotalTeachers),
            CardSearchTarget(listOf("total departments"), binding.cardTotalDepartments),
            CardSearchTarget(listOf("total subjects"), binding.cardTotalSubjects),
            CardSearchTarget(listOf("setting", "settings"), binding.cardSettings),
            CardSearchTarget(listOf("announcement", "notification", "notifications"), binding.cardAnnouncement),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        feedViewModel = ViewModelProvider(this)[DashboardFeedViewModel::class.java]
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        // ---------- Header icons ----------

        // Hamburger menu -> opens the Navigation Drawer
        binding.menuIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Profile icon -> opens Admin Profile screen (editable name/photo, email, role, logout)
        binding.imgProfile.setOnClickListener {
            startActivity(Intent(this, ActivityAdminProfile::class.java))
        }

        // Notification bell -> opens full Notifications screen
        binding.imgNotification.setOnClickListener {
            startActivity(Intent(this, ActivityNotifications::class.java))
        }

        // ---------- Live search ----------
        setupSearch()

        // ---------- Header profile name + photo (real data, not the
        // hardcoded "Administrator" placeholder) ----------
        loadAdminHeaderInfo()

        // ---------- Stats cards (Total Students / Teachers / Departments / Subjects) ----------
        // Display-only, live counts from Firestore -- no navigation on tap.
        loadStats()

        // ---------- Quick Action cards ----------

        binding.cardStudentManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddStudent::class.java))
        }

        binding.cardTeacherManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddTeacher::class.java))
        }

        binding.cardDepartments.setOnClickListener {
            startActivity(Intent(this, ActivityDepartmentManagement::class.java))
        }

        binding.cardSubjects.setOnClickListener {
            startActivity(Intent(this, ActivitySubjectManagement::class.java))
        }

        binding.cardAttendance.setOnClickListener {
            startActivity(Intent(this, ActivityAttendanceDepartmentList::class.java))
        }

        binding.cardManualUpdate.setOnClickListener {
            startActivity(Intent(this, ActivityManualAttendanceFilter::class.java))
        }

        binding.cardDailyOverview.setOnClickListener {
            startActivity(Intent(this, ActivityDailyOverview::class.java))
        }

        binding.cardTeacherAssignment.setOnClickListener {
            startActivity(Intent(this, ActivityTeacherSubjectAssignment::class.java))
        }

        binding.cardSchedule.setOnClickListener {
            startActivity(Intent(this, ActivitySchedule::class.java))
        }

        binding.cardReports.setOnClickListener {
            startActivity(Intent(this, ActivityReports::class.java))
        }

        // ---------- Settings card ----------
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }

        setupRecentActivities()
        observeFeed()
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time the dashboard becomes visible again (e.g.
        // after returning from Add Student, Manual Update, Profile edit,
        // etc.) so Recent Activities, the notification badge, and the
        // announcement preview reflect anything that just happened.
        feedViewModel.loadRecentActivities()
        feedViewModel.loadNotifications()
        loadStats()
        loadAdminHeaderInfo()
    }

    /**
     * Fills the header's name (txtAdminName) and profile photo (imgProfile)
     * with the real saved values instead of the hardcoded "Administrator"
     * placeholder -- same source of truth as ActivityAdminProfile:
     * LocalProfileStore first, falling back to FirebaseAuth display name /
     * email, so it's never blank.
     */
    private fun loadAdminHeaderInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "admin@university.edu"

        val savedName = LocalProfileStore.getDisplayName(this)
        val fallbackName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

        binding.txtAdminName.text = savedName ?: fallbackName

        val savedPhotoPath = LocalProfileStore.getPhotoPath(this)
        if (savedPhotoPath != null) {
            binding.imgProfile.setImageURI(Uri.fromFile(File(savedPhotoPath)))
        }
    }

    /** Fetches live Students/Teachers/Departments/Subjects counts from Firestore and fills the stat cards. */
    private fun loadStats() {
        lifecycleScope.launch {
            val stats = statsRepository.getDashboardStats()
            binding.tvTotalStudentsCount.text = formatCount(stats.studentCount)
            binding.tvTotalTeachersCount.text = formatCount(stats.teacherCount)
            binding.tvTotalDepartmentsCount.text = formatCount(stats.departmentCount)
            binding.tvTotalSubjectsCount.text = formatCount(stats.subjectCount)
        }
    }

    private fun formatCount(count: Int): String = NumberFormat.getInstance(Locale.US).format(count)

    // ---------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------

    private fun setupSearch() {
        binding.recyclerSearchResults.layoutManager = LinearLayoutManager(this)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                searchViewModel.onQueryChanged(query)
                binding.searchResultsCard.visibility = if (query.isBlank()) View.GONE else View.VISIBLE

                // Real-time filter: as the admin types, only cards whose
                // keywords match stay visible -- everything else hides
                // immediately, and clearing the box brings everything back.
                filterQuickActionCards(query)
            }
        })

        searchViewModel.results.observe(this) { results ->
            // SearchResultAdapter now expands each row in place (contact,
            // CNIC, department, reg no / main subject) when tapped, instead
            // of navigating away -- so no click callback is needed here
            // anymore, and we don't clear/close the dropdown on tap.
            binding.recyclerSearchResults.adapter = SearchResultAdapter(results)
            binding.searchResultsCard.visibility =
                if (results.isEmpty() && binding.etSearch.text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    /**
     * Real-time filter over the dashboard cards (Quick Actions, Stats,
     * Settings, Announcement). While the admin types, only cards whose
     * keywords match the query stay visible -- the rest are set to
     * [View.GONE] so they collapse out of the layout immediately, exactly
     * like a live filter in any normal app. Clearing the search box (or an
     * empty/too-short query) brings every card back.
     */
    private fun filterQuickActionCards(query: String) {
        val normalized = query.trim().lowercase()

        if (normalized.isBlank()) {
            cardSearchTargets.forEach { it.view.visibility = View.VISIBLE }
            return
        }

        cardSearchTargets.forEach { target ->
            val matches = target.keywords.any { keyword -> keyword.contains(normalized) }
            target.view.visibility = if (matches) View.VISIBLE else View.GONE
        }
    }

    // ---------------------------------------------------------------
    // Recent Activities + Notifications preview
    // ---------------------------------------------------------------

    private fun setupRecentActivities() {
        binding.recyclerRecentActivity.layoutManager = LinearLayoutManager(this)
    }

    private fun observeFeed() {
        feedViewModel.recentActivities.observe(this) { activities ->
            binding.recyclerRecentActivity.adapter = RecentActivityAdapter(activities)
        }

        feedViewModel.unreadCount.observe(this) { count ->
            if (count > 0) {
                binding.badge.visibility = View.VISIBLE
                binding.badge.text = if (count > 9) "9+" else count.toString()
            } else {
                binding.badge.visibility = View.GONE
            }
        }

        feedViewModel.notifications.observe(this) { notifications ->
            val latest = notifications.firstOrNull()
            if (latest != null) {
                binding.tvAnnouncementTitle.text = latest.title
                binding.tvAnnouncementDescription.text = latest.description
            } else {
                binding.tvAnnouncementTitle.text = "No announcements yet"
                binding.tvAnnouncementDescription.text = ""
            }

            binding.cardAnnouncement.setOnClickListener {
                startActivity(Intent(this, ActivityNotifications::class.java))
            }
        }
    }
}