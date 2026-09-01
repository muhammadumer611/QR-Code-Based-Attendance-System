package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.university.attendance.databinding.ActivityAuthBaseBinding

abstract class BaseAuthActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityAuthBaseBinding

    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore

    protected lateinit var etFirstName: EditText
    protected lateinit var etLastName: EditText
    protected lateinit var etEmail: EditText
    protected lateinit var etPassword: EditText

    // Department field for Teacher sign-up.
    protected lateinit var etDepartment: EditText

    // Teacher ID field for Teacher sign-up (must match an existing
    // "teachers/TCH-XXXX" doc created beforehand by Admin).
    protected lateinit var etTeacherId: EditText

    // Student ID field for Student sign-up.
    // Admin creates the Student record first.
    protected lateinit var etStudentId: EditText

    abstract val role: String
    abstract val isSignUp: Boolean
    abstract val accentColor: Int
    abstract val orbDrawableTop: Int
    abstract val orbDrawableBottom: Int
    abstract val iconBgColor: Int
    abstract val roleIcon: String
    abstract val badgeBg: Int
    abstract val tagBg: Int

    // Har subclass batayegi kahan jana hai
    abstract fun getOppositeScreen(): Class<*>
    abstract fun getDashboardScreen(): Class<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_dark)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityAuthBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupTheme()
        setupContent()
        setupFields()
        animateViews()

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Main button
        binding.btnMain.setOnClickListener {
            it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()

                when {

                    // =========================================================
                    // ADMIN
                    // =========================================================

                    role == "ADMIN" && isSignUp -> {

                        registerAdmin {

                            startActivity(
                                Intent(
                                    this,
                                    getDashboardScreen()
                                )
                            )

                            finish()
                        }
                    }

                    role == "ADMIN" && !isSignUp -> {

                        signInAdmin {

                            startActivity(
                                Intent(
                                    this,
                                    getDashboardScreen()
                                )
                            )

                            finish()
                        }
                    }

                    // =========================================================
                    // TEACHER
                    // =========================================================

                    role == "TEACHER" && isSignUp -> {

                        registerTeacher {

                            startActivity(
                                Intent(
                                    this,
                                    getDashboardScreen()
                                )
                            )

                            finish()
                        }
                    }

                    role == "TEACHER" && !isSignUp -> {

                        signInTeacher {

                            startActivity(
                                Intent(
                                    this,
                                    getDashboardScreen()
                                )
                            )

                            finish()
                        }
                    }

                    // =========================================================
                    // STUDENT
                    // =========================================================

                    role == "STUDENT" && isSignUp -> {

                        registerStudent {

                            startActivity(
                                Intent(
                                    this,
                                    getDashboardScreen()
                                )
                            )

                            finish()
                        }
                    }

                    // Student sign-in abhi agar existing code mein nahi hai
                    // to yahan baad mein add karenge.

                    else -> {

                        startActivity(
                            Intent(
                                this,
                                getDashboardScreen()
                            )
                        )

                        finish()
                    }
                }
            }.start()
        }

        // Switch — SignIn <-> SignUp
        binding.tvSwitch.setOnClickListener {
            startActivity(Intent(this, getOppositeScreen()))
            finish()
        }
    }

    private fun setupTheme() {
        binding.gridOverlay.background = GridDrawable()
        binding.orbTop.setBackgroundResource(orbDrawableTop)
        binding.orbBottom.setBackgroundResource(orbDrawableBottom)
        binding.accentLine.setBackgroundResource(accentColor)
        binding.iconWrap.setCardBackgroundColor(
            ContextCompat.getColor(this, iconBgColor)
        )
        binding.tvRoleIcon.text = roleIcon
        binding.roleBadge.setBackgroundResource(badgeBg)
        binding.btnMain.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, accentColor)
            )
    }

    private fun setupContent() {
        val roleName = role.lowercase().replaceFirstChar { it.uppercase() }
        binding.tvBadgeText.text = roleName
        binding.tvBadgeText.setTextColor(ContextCompat.getColor(this, accentColor))

        if (isSignUp) {
            binding.tvHeading.text = when (role) {
                "ADMIN" -> "Create\nAccount"
                "TEACHER" -> "Join as\nEducator"
                else -> "Start Your\nJourney"
            }
            binding.tvSubHeading.text = when (role) {
                "ADMIN" -> "Set up your admin workspace"
                "TEACHER" -> "Create your teaching account"
                else -> "Register your student account"
            }
            binding.btnMain.text = "Create $roleName Account"
            binding.tvSwitch.text = "Already have an account? Sign In"
        } else {
            binding.tvHeading.text = when (role) {
                "TEACHER" -> "Welcome\nEducator"
                else -> "Welcome\nBack"
            }
            binding.tvSubHeading.text = when (role) {
                "ADMIN" -> "Sign in to your admin panel"
                "TEACHER" -> "Sign in to manage your classes"
                else -> "Sign in to track your attendance"
            }
            binding.btnMain.text = "Sign In as $roleName"
            binding.tvSwitch.text = "Don't have an account? Sign Up"
        }
    }

    private fun setupFields() {
        val container = binding.fieldsContainer
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        if (isSignUp) {
            val nameRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            nameRow.addView(makeField("First Name", "Muhammad", "firstName", weight = 1f))
            nameRow.addView(
                makeField("Last Name", "Umer", "lastName", weight = 1f, marginStart = 10)
            )
            container.addView(nameRow)
        }

        val emailLabel = when (role) {
            "TEACHER" -> "Faculty Email"
            "STUDENT" -> "Personal Email"
            else -> "Official UOL Email"
        }
        val emailHint = when (role) {
            "ADMIN" -> "admin@uol.edu.pk"
            "TEACHER" -> "teacher@uol.edu.pk"
            "STUDENT" -> "student@gmail.com"
            else -> "${role.lowercase()}@university.edu"
        }
        container.addView(makeField(emailLabel, emailHint, "email"))

        if (isSignUp) {
            when (role) {
                "TEACHER" -> {
                    container.addView(
                        makeField(
                            "Teacher ID",
                            "e.g. TCH-7A92BC41",
                            "teacherId"
                        )
                    )

                    container.addView(
                        makeField(
                            "Department",
                            "Computer Science",
                            "department"
                        )
                    )
                }
                "STUDENT" -> {
                    container.addView(
                        makeField(
                            "Student ID",
                            "e.g. BSSE-B2026-001",
                            "studentId"
                        )
                    )
                }
            }
        }

        container.addView(makeField("Password", "********", "password", true))

        if (!isSignUp) {
            val forgot = TextView(this).apply {
                text = "Forgot password?"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@BaseAuthActivity, accentColor))
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (-6 * dp).toInt(), 0, (14 * dp).toInt()) }
            }
            container.addView(forgot)
        }
    }

    private fun makeField(
        label: String,
        hint: String,
        key: String,
        isPassword: Boolean = false,
        weight: Float = 0f,
        marginStart: Int = 0
    ): LinearLayout {
        val dp = resources.displayMetrics.density

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = if (weight > 0) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    .apply { setMargins((marginStart * dp).toInt(), 0, 0, (12 * dp).toInt()) }
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, (12 * dp).toInt()) }
            }
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_muted))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (5 * dp).toInt()) }
        }

        val field = EditText(this).apply {
            this.hint = hint
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_muted))
            background = ContextCompat.getDrawable(this@BaseAuthActivity, R.drawable.bg_input_field)
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            if (isPassword) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // EditText ko reference variables me save karo
        when (key) {
            "firstName" -> etFirstName = field
            "lastName" -> etLastName = field
            "email" -> etEmail = field
            "password" -> etPassword = field
            "department" -> etDepartment = field
            "teacherId" -> etTeacherId = field
            "studentId" -> etStudentId = field
        }

        wrapper.addView(labelView)
        wrapper.addView(field)
        return wrapper
    }

    private fun animateViews() {
        val views = listOf(
            binding.btnBack, binding.iconWrap, binding.roleBadge,
            binding.tvHeading, binding.tvSubHeading,
            binding.fieldsContainer, binding.btnMain, binding.tvSwitch
        )
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f).translationY(0f)
                .setDuration(420)
                .setStartDelay((i * 70).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    // =================================================================
    // ADMIN
    // =================================================================

    protected fun validateAdmin(): Boolean {

        if (etFirstName.text.toString().trim().isEmpty()) {
            etFirstName.error = "Enter First Name"
            return false
        }

        if (etLastName.text.toString().trim().isEmpty()) {
            etLastName.error = "Enter Last Name"
            return false
        }

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter Email"
            return false
        }

        if (!email.lowercase().endsWith("@uol.edu.pk")) {
            etEmail.error = "Only Official UOL Email Allowed"
            return false
        }

        val password = etPassword.text.toString()

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            return false
        }

        return true
    }

    protected fun registerAdmin(onSuccess: () -> Unit) {

        if (!validateAdmin()) return

        val first = etFirstName.text.toString().trim()
        val last = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val uid = auth.currentUser!!.uid

                val admin = hashMapOf(
                    "uid" to uid,
                    "firstName" to first,
                    "lastName" to last,
                    "email" to email,
                    "role" to "ADMIN",
                    "university" to "University Of Lahore",
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.collection("admins")
                    .document(uid)
                    .set(admin)
                    .addOnSuccessListener {

                        auth.currentUser?.sendEmailVerification()

                        Toast.makeText(
                            this,
                            "Admin Account Created Successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        onSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    protected fun validateAdminSignIn(): Boolean {

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter Email"
            return false
        }

        if (!email.lowercase().endsWith("@uol.edu.pk")) {
            etEmail.error = "Only Official UOL Email Allowed"
            return false
        }

        val password = etPassword.text.toString()

        if (password.isEmpty()) {
            etPassword.error = "Enter Password"
            return false
        }

        return true
    }

    protected fun signInAdmin(onSuccess: () -> Unit) {

        if (!validateAdminSignIn()) return

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        binding.btnMain.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val uid = auth.currentUser!!.uid

                db.collection("admins")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        binding.btnMain.isEnabled = true

                        if (!document.exists()) {
                            auth.signOut()
                            Toast.makeText(
                                this,
                                "No Admin Account Found With This Email",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        val isActive = document.getBoolean("isActive") ?: false

                        if (!isActive) {
                            auth.signOut()
                            Toast.makeText(
                                this,
                                "This Admin Account Has Been Deactivated",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        onSuccess()
                    }
                    .addOnFailureListener {
                        binding.btnMain.isEnabled = true
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                binding.btnMain.isEnabled = true
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    // =================================================================
    // TEACHER
    // Sign-up flow is different from ADMIN: Admin creates the Teacher
    // record first (see Teacher.kt), so on sign-up we must first VERIFY
    // the Teacher ID against Firestore before creating a Firebase Auth
    // account, then link that account back onto the same "teachers" doc.
    //
    //   Teacher ID
    //      -> teachers/TCH-XXXX
    //      -> exists?
    //      -> already linked (accountLinked == true)?
    //      -> email on record matches entered email?
    //      -> Firebase Auth create
    //      -> save authUid + mark accountLinked = true
    //
    // Sign-in looks the teacher doc up by authUid (uid), since the doc
    // ID is the Teacher ID (TCH-XXXX), not the Firebase Auth uid.
    // =================================================================

    // =================================================================
    // STUDENT
    // Admin creates the Student record first.
    // Student signs up using the Student ID generated by Admin.
    // =================================================================

    protected fun validateStudent(): Boolean {

        if (etFirstName.text.toString().trim().isEmpty()) {
            etFirstName.error = "Enter First Name"
            return false
        }

        if (etLastName.text.toString().trim().isEmpty()) {
            etLastName.error = "Enter Last Name"
            return false
        }

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter Personal Email"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return false
        }

        val studentId = etStudentId.text.toString()
            .trim()
            .uppercase()

        if (studentId.isEmpty()) {
            etStudentId.error = "Enter Student ID"
            return false
        }

        if (!studentId.startsWith("BS")) {
            etStudentId.error = "Invalid Student ID"
            return false
        }

        val password = etPassword.text.toString()

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            return false
        }

        return true
    }

    protected fun validateTeacher(): Boolean {

        if (etFirstName.text.toString().trim().isEmpty()) {
            etFirstName.error = "Enter First Name"
            return false
        }

        if (etLastName.text.toString().trim().isEmpty()) {
            etLastName.error = "Enter Last Name"
            return false
        }

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter Email"
            return false
        }

        if (!email.lowercase().endsWith("@uol.edu.pk")) {
            etEmail.error = "Only Official UOL Email Allowed"
            return false
        }

        val teacherId = etTeacherId.text.toString().trim().uppercase()

        if (teacherId.isEmpty()) {
            etTeacherId.error = "Enter Teacher ID"
            return false
        }

        if (!teacherId.startsWith("TCH-")) {
            etTeacherId.error = "Invalid Teacher ID"
            return false
        }

        if (etDepartment.text.toString().trim().isEmpty()) {
            etDepartment.error = "Enter Department"
            return false
        }

        val password = etPassword.text.toString()

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters"
            return false
        }

        return true
    }

    protected fun registerTeacher(onSuccess: () -> Unit) {

        if (!validateTeacher()) return

        val first = etFirstName.text.toString().trim()
        val last = etLastName.text.toString().trim()
        val fullName = "$first $last".trim()
        val email = etEmail.text.toString().trim()
        val department = etDepartment.text.toString().trim()
        val teacherId = etTeacherId.text.toString().trim().uppercase()
        val password = etPassword.text.toString()

        binding.btnMain.isEnabled = false

        // Step 1: Teacher ID must already exist (Admin creates it first).
        db.collection("teachers")
            .document(teacherId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    binding.btnMain.isEnabled = true
                    etTeacherId.error = "Teacher ID Not Found"
                    Toast.makeText(
                        this,
                        "No Teacher record found for this Teacher ID. Contact Admin.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val accountLinked = document.getBoolean("accountLinked") ?: false

                if (accountLinked) {
                    binding.btnMain.isEnabled = true
                    etTeacherId.error = "Already Linked"
                    Toast.makeText(
                        this,
                        "This Teacher ID is already linked to an account.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val recordEmail = document.getString("email")?.trim()?.lowercase() ?: ""

                if (recordEmail.isNotEmpty() && recordEmail != email.lowercase()) {
                    binding.btnMain.isEnabled = true
                    etEmail.error = "Email Does Not Match Teacher Record"
                    Toast.makeText(
                        this,
                        "This email does not match the email on record for this Teacher ID.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                // Step 2: Teacher ID verified — now create the Auth account.
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {

                        val uid = auth.currentUser!!.uid

                        val updates = hashMapOf<String, Any?>(
                            "authUid" to uid,
                            "fullName" to fullName,
                            "email" to email,
                            "departmentName" to department,
                            "accountLinked" to true,
                            "linkedAt" to FieldValue.serverTimestamp()
                        )

                        // Step 3: Link the new Auth account onto the same
                        // teacher doc (keyed by teacherId, e.g. TCH-XXXX).
                        db.collection("teachers")
                            .document(teacherId)
                            .update(updates)
                            .addOnSuccessListener {

                                binding.btnMain.isEnabled = true
                                auth.currentUser?.sendEmailVerification()

                                Toast.makeText(
                                    this,
                                    "Teacher Account Linked Successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                onSuccess()
                            }
                            .addOnFailureListener { error ->
                                binding.btnMain.isEnabled = true
                                Toast.makeText(
                                    this,
                                    error.message ?: "Teacher account creation failed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { error ->
                        binding.btnMain.isEnabled = true
                        Toast.makeText(
                            this,
                            error.message ?: "Teacher account creation failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                binding.btnMain.isEnabled = true
                Toast.makeText(
                    this,
                    "Unable to verify Teacher ID: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    protected fun registerStudent(onSuccess: () -> Unit) {

        if (!validateStudent()) return

        val first = etFirstName.text.toString().trim()
        val last = etLastName.text.toString().trim()

        val fullName = "$first $last".trim()

        val email = etEmail.text.toString().trim()

        val studentId = etStudentId.text.toString()
            .trim()
            .uppercase()

        val password = etPassword.text.toString()

        binding.btnMain.isEnabled = false

        // ---------------------------------------------------------
        // STEP 1
        // Verify Student ID created by Admin
        // ---------------------------------------------------------

        db.collection("students")
            .whereEqualTo("studentGeneratedId", studentId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {

                    binding.btnMain.isEnabled = true

                    etStudentId.error = "Student ID Not Found"

                    Toast.makeText(
                        this,
                        "No Student record found for this Student ID. Contact Admin.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val document = snapshot.documents.first()
                val studentDocRef = document.reference

                // -------------------------------------------------
                // STEP 2
                // Check whether account is already linked
                // -------------------------------------------------

                val accountLinked =
                    document.getBoolean("accountLinked") ?: false

                if (accountLinked) {

                    binding.btnMain.isEnabled = true

                    etStudentId.error = "Already Linked"

                    Toast.makeText(
                        this,
                        "This Student ID is already linked to an account.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // -------------------------------------------------
                // STEP 3
                // Verify personal email if Admin already saved it
                // -------------------------------------------------

                val recordEmail =
                    document.getString("email")
                        ?.trim()
                        ?.lowercase()
                        ?: ""

                if (
                    recordEmail.isNotEmpty() &&
                    recordEmail != email.lowercase()
                ) {

                    binding.btnMain.isEnabled = true

                    etEmail.error =
                        "Email Does Not Match Student Record"

                    Toast.makeText(
                        this,
                        "This email does not match the email saved by Admin.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // -------------------------------------------------
                // STEP 4
                // Create Firebase Authentication account
                // -------------------------------------------------

                auth.createUserWithEmailAndPassword(
                    email,
                    password
                )
                    .addOnSuccessListener {

                        val uid =
                            auth.currentUser?.uid

                        if (uid == null) {

                            binding.btnMain.isEnabled = true

                            Toast.makeText(
                                this,
                                "Unable to create Student account.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        // -------------------------------------------------
                        // STEP 5
                        // Link Firebase Auth account with Student document
                        // -------------------------------------------------

                        val updates =
                            hashMapOf<String, Any?>(
                                "authUid" to uid,
                                "fullName" to fullName,
                                "email" to email,
                                "accountLinked" to true,
                                "linkedAt" to FieldValue.serverTimestamp()
                            )

                        studentDocRef
                            .update(updates)
                            .addOnSuccessListener {

                                binding.btnMain.isEnabled = true

                                auth.currentUser
                                    ?.sendEmailVerification()

                                Toast.makeText(
                                    this,
                                    "Student Account Linked Successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                onSuccess()
                            }
                            .addOnFailureListener { error ->

                                binding.btnMain.isEnabled = true

                                Toast.makeText(
                                    this,
                                    error.message
                                        ?: "Unable to link Student account.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { error ->

                        binding.btnMain.isEnabled = true

                        Toast.makeText(
                            this,
                            error.message
                                ?: "Student account creation failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->

                binding.btnMain.isEnabled = true

                Toast.makeText(
                    this,
                    "Unable to verify Student ID: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    protected fun validateTeacherSignIn(): Boolean {

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter Email"
            return false
        }

        if (!email.lowercase().endsWith("@uol.edu.pk")) {
            etEmail.error = "Only Official UOL Email Allowed"
            return false
        }

        val password = etPassword.text.toString()

        if (password.isEmpty()) {
            etPassword.error = "Enter Password"
            return false
        }

        return true
    }

    protected fun signInTeacher(onSuccess: () -> Unit) {

        if (!validateTeacherSignIn()) return

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        binding.btnMain.isEnabled = false

        // ---------------------------------------------------------
        // STEP 1
        // Firebase Authentication
        // ---------------------------------------------------------

        auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {

                val firebaseUser = auth.currentUser

                if (firebaseUser == null) {

                    binding.btnMain.isEnabled = true

                    Toast.makeText(
                        this,
                        "Unable to load teacher account.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val uid = firebaseUser.uid

                // -------------------------------------------------
                // STEP 2
                // Find Teacher using Firebase Auth UID
                // -------------------------------------------------

                db.collection("teachers")
                    .whereEqualTo("authUid", uid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        if (snapshot.isEmpty) {

                            auth.signOut()

                            binding.btnMain.isEnabled = true

                            Toast.makeText(
                                this,
                                "Teacher profile is not linked with this account.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        // -------------------------------------------------
                        // STEP 3
                        // Convert Firestore document into Teacher
                        // -------------------------------------------------

                        val document = snapshot.documents.first()

                        val teacher =
                            document.toObject(Teacher::class.java)

                        if (teacher == null) {

                            auth.signOut()

                            binding.btnMain.isEnabled = true

                            Toast.makeText(
                                this,
                                "Invalid teacher profile.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        // Firestore document ID = our Teacher ID
                        teacher.teacherId = document.id

                        // -------------------------------------------------
                        // STEP 4
                        // Check account status
                        // -------------------------------------------------

                        if (!teacher.isActive) {

                            auth.signOut()

                            binding.btnMain.isEnabled = true

                            Toast.makeText(
                                this,
                                "Your teacher account has been deactivated by Admin.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        // -------------------------------------------------
                        // STEP 5
                        // Make sure account is linked
                        // -------------------------------------------------

                        if (!teacher.accountLinked) {

                            auth.signOut()

                            binding.btnMain.isEnabled = true

                            Toast.makeText(
                                this,
                                "Your teacher account is not linked yet.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        // -------------------------------------------------
                        // STEP 6
                        // Save Teacher Session
                        // -------------------------------------------------

                        TeacherSession.save(
                            this,
                            teacher
                        )

                        binding.btnMain.isEnabled = true

                        Toast.makeText(
                            this,
                            "Welcome ${teacher.fullName}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // -------------------------------------------------
                        // STEP 7
                        // Open Teacher Dashboard
                        // -------------------------------------------------

                        onSuccess()
                    }
                    .addOnFailureListener { error ->

                        auth.signOut()

                        binding.btnMain.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to load teacher profile: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->

                binding.btnMain.isEnabled = true

                Toast.makeText(
                    this,
                    error.message ?: "Login failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}