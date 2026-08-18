package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityAdminDashboardBinding

class ActivityAdminDashboard : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var feedViewModel: DashboardFeedViewModel
    private lateinit var searchViewModel: SearchViewModel

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

        // ---------- Stats cards (Total Students / Teachers / Departments / Subjects) ----------
        setupStatsCards()

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
    }

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
            }
        })

        searchViewModel.results.observe(this) { results ->
            binding.recyclerSearchResults.adapter = SearchResultAdapter(results) { result ->
                // No individual detail screen exists yet -- route to the
                // matching management screen so Admin can find the full
                // record there.
                when (result.resultType) {
                    SearchResultType.STUDENT -> startActivity(Intent(this, ActivityAddStudent::class.java))
                    SearchResultType.TEACHER -> startActivity(Intent(this, ActivityAddTeacher::class.java))
                }
                binding.etSearch.setText("")
                binding.searchResultsCard.visibility = View.GONE
            }
            binding.searchResultsCard.visibility =
                if (results.isEmpty() && binding.etSearch.text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    // ---------------------------------------------------------------
    // Stats cards
    // ---------------------------------------------------------------

    private fun setupStatsCards() {
        // These 4 cards reuse the SAME management screens as the Quick
        // Action cards below -- there's no separate read-only "browse
        // all students" list screen yet, so tapping the stat card takes
        // Admin to the same management screen.
        binding.cardTotalStudents.setOnClickListener {
            startActivity(Intent(this, ActivityAddStudent::class.java))
        }
        binding.cardTotalTeachers.setOnClickListener {
            startActivity(Intent(this, ActivityAddTeacher::class.java))
        }
        binding.cardTotalDepartments.setOnClickListener {
            startActivity(Intent(this, ActivityDepartmentManagement::class.java))
        }
        binding.cardTotalSubjects.setOnClickListener {
            startActivity(Intent(this, ActivitySubjectManagement::class.java))
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