# 📱 AttendU — Smart University Attendance System

<p align="center">
  <img src="screenshots/App Logo.png" width="180" alt="AttendU App Logo">
</p>

<h2 align="center">AttendU</h2>

<p align="center">
  <b>Smart University Attendance System</b>
</p>

<p align="center">
  A modern Android application designed to simplify university attendance
  management through digital attendance sessions, QR-based session
  identification, Bluetooth Low Energy (BLE) proximity verification,
  Firebase Authentication, and Cloud Firestore.
</p>

<p align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge)
![Language](https://img.shields.io/badge/Language-Kotlin-blue?style=for-the-badge)
![Database](https://img.shields.io/badge/Database-Cloud%20Firestore-orange?style=for-the-badge)
![Authentication](https://img.shields.io/badge/Authentication-Firebase%20Auth-yellow?style=for-the-badge)
![BLE](https://img.shields.io/badge/Proximity-Bluetooth%20Low%20Energy-purple?style=for-the-badge)
![UI](https://img.shields.io/badge/UI-XML-informational?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blueviolet?style=for-the-badge)
![IDE](https://img.shields.io/badge/IDE-Android%20Studio-success?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Completed-brightgreen?style=for-the-badge)

</p>

---

# 📖 Table of Contents

- [Overview](#-overview)
- [Problem Statement](#-problem-statement)
- [Objectives](#-objectives)
- [Solution](#-solution)
- [Key Features](#-key-features)
- [System Roles](#-system-roles)
- [Admin Module](#-admin-module)
- [Teacher Module](#-teacher-module)
- [Student Module](#-student-module)
- [Authentication](#-authentication)
- [QR Code Attendance](#-qr-code-attendance)
- [BLE Proximity Verification](#-ble-proximity-verification)
- [Attendance Workflow](#-attendance-workflow)
- [Attendance Validation](#-attendance-validation)
- [Duplicate Attendance Prevention](#-duplicate-attendance-prevention)
- [Attendance History](#-attendance-history)
- [Subject-wise Attendance](#-subject-wise-attendance)
- [Low Attendance Warning](#-low-attendance-warning)
- [Schedule Management](#-schedule-management)
- [Firebase Integration](#-firebase-integration)
- [Firestore Collections](#-firestore-collections)
- [Application Architecture](#-application-architecture)
- [Project Structure](#-project-structure)
- [Technologies Used](#-technologies-used)
- [Android Permissions](#-android-permissions)
- [Application Screenshots](#-application-screenshots)
- [Complete UI Flow](#-complete-ui-flow)
- [Installation](#-installation)
- [Firebase Setup](#-firebase-setup)
- [Running the Application](#-running-the-application)
- [Testing](#-testing)
- [BLE Testing](#-ble-testing)
- [Security Considerations](#-security-considerations)
- [Advantages](#-advantages)
- [Limitations](#-limitations)
- [Future Enhancements](#-future-enhancements)
- [Learning Outcomes](#-learning-outcomes)
- [Author](#-author)
- [License](#-license)

---

# 📖 Overview

**AttendU** is a smart Android-based university attendance management system
developed to digitize and simplify the traditional attendance process used
in educational institutions.

The application provides a centralized platform where administrators,
teachers, and students interact with the attendance system according to their
assigned roles.

Instead of depending completely on traditional paper-based attendance,
AttendU allows teachers to create live attendance sessions while students
can view active sessions, verify classroom proximity using Bluetooth Low
Energy (BLE), and record their attendance digitally.

The application also provides attendance history and subject-wise attendance
information so that students can monitor their attendance more easily.

---

# 🎯 Problem Statement

Traditional university attendance systems often depend on manual registers
or disconnected digital records.

This can create several problems:

- Manual attendance consumes classroom time.
- Paper records are difficult to maintain.
- Attendance records can become difficult to search.
- Teachers have to manually maintain attendance information.
- Students may not have an easy way to monitor attendance.
- Duplicate attendance records may occur.
- Administrators have to manage large amounts of academic information.
- Attendance information may not be centralized.

AttendU addresses these problems by providing a centralized Android-based
attendance management platform.

---

# 🎯 Objectives

The major objectives of AttendU are:

1. Eliminate manual attendance processes.
2. Reduce paperwork.
3. Reduce the time required to conduct attendance.
4. Digitize university attendance records.
5. Provide centralized attendance management.
6. Organize academic and attendance information.
7. Associate attendance with the correct class and subject.
8. Reduce duplicate attendance records.
9. Provide classroom proximity verification through BLE.
10. Provide students with attendance history.
11. Provide subject-wise attendance information.
12. Highlight low attendance.
13. Provide administrators with academic management functionality.
14. Provide teachers with a live attendance workflow.
15. Provide students with a simple and user-friendly interface.

---

# 💡 Solution

AttendU provides a role-based attendance management system with three primary
roles:

```text
                    ┌─────────────────────┐
                    │       AttendU        │
                    │ University System    │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
         ┌──────────┐     ┌──────────┐     ┌──────────┐
         │  Admin   │     │ Teacher  │     │ Student  │
         └──────────┘     └──────────┘     └──────────┘
              │                │                │
              ▼                ▼                ▼
        Manage System     Conduct Live     Mark & View
                          Attendance       Attendance
✨ Key Features
🔐 Authentication
Admin registration and login
Teacher registration and login
Student registration and login
Firebase Authentication
Role-based navigation
Authentication state management
Logout functionality
👨‍💼 Admin Features
Admin dashboard
Teacher management
Student management
Department management
Subject management
Class management
Teacher-class assignments
Teacher-subject assignments
Student-class assignments
Student-subject enrollment
Schedule management
Attendance-related management
👨‍🏫 Teacher Features
Teacher login
Teacher dashboard
View assigned classes
View assigned subjects
Manage assigned students
Start live attendance session
Generate attendance session QR
BLE attendance broadcasting
View attendance records
End attendance session
👨‍🎓 Student Features
Student login
Student dashboard
View student information
View today's schedule
View active attendance sessions
Detect teacher BLE signal
Verify classroom proximity
Mark attendance
View attendance history
View subject-wise attendance
View attendance percentage
Low attendance warning
👥 System Roles

AttendU provides three major application roles.

                   AttendU
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
      Admin        Teacher       Student
        │             │             │
        ▼             ▼             ▼
   Management     Attendance     Attendance
   & Setup        Sessions       & History
👨‍💼 Admin Module

The Admin module provides centralized management of academic and user
information.

The administrator can manage:

Admin
 │
 ├── Departments
 │
 ├── Subjects
 │
 ├── Classes
 │
 ├── Teachers
 │
 ├── Students
 │
 ├── Teacher Assignments
 │
 ├── Student Class Assignments
 │
 ├── Student Subject Enrollment
 │
 └── Schedules

The Admin dashboard provides access to the major administrative functions.

👨‍🏫 Teacher Module

The Teacher module is responsible for conducting live attendance sessions.

A teacher is associated with the relevant:

Class
Subject
Schedule

The teacher can start an attendance session for the relevant class and
subject.

Teacher Workflow
Teacher Login
      ↓
Teacher Dashboard
      ↓
Assigned Class / Subject
      ↓
Start Attendance
      ↓
Create Live Session
      ↓
Generate QR
      ↓
Start BLE Advertisement
      ↓
Students Detect Session
      ↓
Attendance Records
👨‍🎓 Student Module

The Student module allows students to participate in live attendance and
monitor their attendance information.

Students can:

View their assigned class.
View enrolled subjects.
View today's schedule.
View active attendance sessions.
Detect teacher BLE signals.
Verify proximity.
Mark attendance.
View attendance history.
View subject-wise attendance.
Monitor attendance percentage.
View low attendance warnings.
🔐 Authentication

AttendU uses Firebase Authentication for user authentication.

The general authentication flow is:

User
 │
 ▼
Login / Registration
 │
 ▼
Firebase Authentication
 │
 ▼
Authenticated User
 │
 ▼
Role Identification
 │
 ├── Admin
 ├── Teacher
 └── Student

Firebase Authentication manages the user's authentication identity while
additional application information is stored in Firestore.

📲 QR Code Attendance

AttendU includes QR-based attendance session information on the teacher side.

When a teacher starts a live attendance session, a session-specific QR
payload can be generated.

The current QR payload format is:

UOL_ATTENDANCE|sessionId

Where:

UOL_ATTENDANCE

identifies the attendance application payload and:

sessionId

identifies the particular live attendance session.

QR Workflow
Teacher
   │
   ▼
Start Live Attendance
   │
   ▼
Create Attendance Session
   │
   ▼
Generate Session QR
   │
   ▼
Display QR
Current Implementation

The current student attendance workflow does not depend on a dedicated
student-side QR scanner.

The active attendance session is obtained in the student application and
attendance is validated using the session information together with BLE
proximity verification.

A dedicated student QR scanning workflow can be added in a future version.

📡 BLE Proximity Verification

AttendU uses Bluetooth Low Energy (BLE) as a classroom proximity
verification mechanism.

When a teacher starts a live attendance session, the teacher's Android
device can advertise a temporary session-specific BLE token.

The student's Android device scans for BLE advertisements and checks whether
the expected attendance session token is detected.

BLE Workflow
                 TEACHER PHONE
                      │
                      │
                      │ BLE Advertisement
                      │ Session Token
                      ▼
                 STUDENT PHONE
                      │
                      ▼
                  BLE Scanner
                      │
                      ▼
                Token Detected
                      │
                      ▼
              Session Verification
                      │
                      ▼
               Proximity Verified
                      │
                      ▼
               Mark Attendance
📶 BLE Detection Configuration

The current BLE detection configuration is based on:

Minimum RSSI        = -70 dBm
Required Detections = 3
Detection Window    = 5000 ms

RSSI represents received Bluetooth signal strength.

A stronger signal can generally indicate closer devices, but RSSI can be
affected by:

Walls
Human bodies
Phone orientation
Device hardware
Bluetooth interference
Classroom environment

Therefore, BLE/RSSI is treated as an approximate proximity mechanism rather
than an exact distance measurement.

🔄 Attendance Workflow

The complete attendance workflow is:

                    TEACHER
                       │
                       ▼
                Teacher Login
                       │
                       ▼
              Select Class/Subject
                       │
                       ▼
              Start Live Session
                       │
              ┌────────┴────────┐
              │                 │
              ▼                 ▼
         Generate QR       Start BLE
                           Broadcast
              │                 │
              └────────┬────────┘
                       │
                       ▼
                    STUDENT
                       │
                       ▼
                Student Login
                       │
                       ▼
              Student Dashboard
                       │
                       ▼
              Active Session
                       │
                       ▼
              BLE Signal Search
                       │
                       ▼
              Session Detected
                       │
                       ▼
              Eligibility Check
                       │
                       ▼
              Proximity Check
                       │
                       ▼
              Attendance Marking
                       │
                       ▼
                  Firestore
                       │
                       ▼
             Attendance Record
✅ Attendance Validation

Before attendance is recorded, the application checks the relevant student
and attendance session information.

The intended validation flow is:

Active Session
      +
Correct Class
      +
Correct Subject
      +
Student Enrollment
      +
Teacher Session
      +
BLE Proximity
      +
No Duplicate Record
      ↓
Attendance Saved

This ensures that attendance is associated with the relevant academic
context.

🚫 Duplicate Attendance Prevention

The application uses deterministic attendance record identification and
transaction-based logic to help prevent duplicate attendance records for the
same student and attendance session.

The concept is:

Student + Session
       │
       ▼
Existing Record?
    │       │
   YES      NO
    │       │
    ▼       ▼
 Prevent   Create
 Duplicate Record
📊 Attendance History

Students can access their attendance history from the Student Dashboard.

Attendance history can include:

Subject name
Course code
Teacher
Present classes
Total classes
Attendance percentage
Attendance dates
📚 Subject-wise Attendance

AttendU provides subject-wise attendance summaries.

For each enrolled subject, information can include:

Subject
Course Code
Teacher
Present Classes
Total Classes
Attendance Percentage
Attendance Dates

Example:

Database Systems
CS-301

Present: 8
Total:   10

Attendance: 80%
⚠️ Low Attendance Warning

The attendance summary can identify subjects where attendance is at or below
the configured warning threshold.

This allows students to quickly identify subjects where their attendance
requires attention.

📅 Schedule Management

The application supports academic schedules containing information such as:

Teacher
Class
Subject
Day
Start time
End time
Room number

Schedules help associate attendance sessions with the appropriate academic
context.

🔥 Firebase Integration

AttendU uses Firebase as its backend infrastructure.

Firebase Authentication

Firebase Authentication is used for:

User registration
Login
Authentication identity
Authentication state
Cloud Firestore

Cloud Firestore is used for:

Teacher records
Student records
Academic records
Assignments
Schedules
Attendance sessions
Attendance records
🗃️ Firestore Collections

The current implementation uses Firestore collections including:

teachers
students
studentSubjectAssignments
attendanceSessions
attendance_records
schedules

Additional academic collections may be present depending on the project
configuration.

🧑‍🏫 Teacher Records

The teachers collection stores teacher-related information.

Teacher data is used to associate teachers with:

Classes
Subjects
Attendance sessions
👨‍🎓 Student Records

The students collection stores student-related information.

Student data is used to determine:

Student identity
Class membership
Subject eligibility
Attendance information
📚 Student Subject Assignments

The studentSubjectAssignments collection/data associates students with
their enrolled subjects.

This information is used to determine whether a student is eligible for a
subject attendance session.

🟢 Attendance Sessions

The attendanceSessions collection represents live attendance sessions.

A session can contain information related to:

Session ID
Teacher
Class
Subject
Start time
End time
Active state
BLE token
📝 Attendance Records

The attendance_records collection stores attendance information.

Attendance records are associated with the relevant student and attendance
session.

This provides persistent digital attendance history.

🏗️ Application Architecture

AttendU follows an Android architecture based around:

MVVM principles
Repository pattern
Activity-based UI
View Binding
Firebase services
BLE utility/manager components
RecyclerView adapters

High-level architecture:

┌──────────────────────────────┐
│          Android UI          │
│ Activities / XML / Adapters  │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│       ViewModel / Logic      │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│          Repository          │
│    Data & Business Logic     │
└──────────────┬───────────────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
┌─────────────┐   ┌─────────────┐
│  Firestore  │   │ BLE Manager │
└─────────────┘   └─────────────┘
📂 Project Structure
QR-Code-Based-Attendance-System/
│
├── app/
│   │
│   ├── src/
│   │   │
│   │   ├── androidTest/
│   │   │
│   │   ├── main/
│   │   │   │
│   │   │   ├── java/
│   │   │   │   │
│   │   │   │   └── com/university/attendance/
│   │   │   │       │
│   │   │   │       ├── MainActivity.kt
│   │   │   │       │
│   │   │   │       ├── ActivityAdminDashboard.kt
│   │   │   │       ├── ActivityTeacherDashboard.kt
│   │   │   │       ├── ActivityStudentDashboard.kt
│   │   │   │       │
│   │   │   │       ├── ActivityAdminSignIn.kt
│   │   │   │       ├── ActivityTeacherSignIn.kt
│   │   │   │       ├── ActivityStudentSignIn.kt
│   │   │   │       │
│   │   │   │       ├── ActivityAdminSignUp.kt
│   │   │   │       ├── ActivityTeacherSignUp.kt
│   │   │   │       ├── ActivityStudentSignUp.kt
│   │   │   │       │
│   │   │   │       ├── ActivityAddTeacher.kt
│   │   │   │       ├── ActivityAddStudent.kt
│   │   │   │       │
│   │   │   │       ├── ViewModels/
│   │   │   │       ├── Repositories/
│   │   │   │       ├── Adapters/
│   │   │   │       ├── Models/
│   │   │   │       └── Utility Classes
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   ├── mipmap/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/
│   │
│   ├── build.gradle.kts
│   └── google-services.json
│
├── gradle/
│
├── Screenshots/
│   ├── Admin Dashboard 1.png
│   ├── Admin Dashboard 2.png
│   ├── Admin Login.png
│   ├── App Logo.png
│   ├── Onboarding 1.png
│   ├── Onboarding 2.png
│   ├── Onboarding 3.png
│   ├── Role Selection.png
│   ├── Student Dashboard 1.png
│   ├── Student Dashboard 2.png
│   ├── Teacher Dashboard 1.png
│   ├── Teacher Dashboard 2.png
│   ├── Teacher Dashboard 3.png
│   └── Teacher Login.png
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
🛠️ Technologies Used
Technology	Purpose
Kotlin	Android application development
Android Studio	Application development
Firebase Authentication	User authentication
Cloud Firestore	Cloud database
XML	User interface
View Binding	Type-safe UI access
RecyclerView	Dynamic lists
Gradle Kotlin DSL	Build system
Bluetooth Low Energy	Classroom proximity verification
QR Code	Attendance session identification
MVVM	Application architecture
Repository Pattern	Data and business logic separation
📱 Android Permissions

For modern Android versions, BLE functionality may require the following
permissions:

BLUETOOTH_SCAN
BLUETOOTH_ADVERTISE
BLUETOOTH_CONNECT

For older Android versions, BLE scanning may require:

ACCESS_FINE_LOCATION

The application requests the permissions required for its BLE operations
according to the Android version.

🖼️ Application Screenshots

The following screenshots demonstrate the actual user interface of the
AttendU Android application.

🎨 App Logo
<p align="center"> <img src="screenshots/App Logo.png" width="220" alt="AttendU App Logo"> </p>
🚀 Onboarding Screens

The onboarding section introduces users to the application before they enter
the main system.

Onboarding — Screen 1
<p align="center"> <img src="screenshots/Onboarding 1.png" width="280" alt="AttendU Onboarding 1"> </p>
Onboarding — Screen 2
<p align="center"> <img src="screenshots/Onboarding 2.png" width="280" alt="AttendU Onboarding 2"> </p>
Onboarding — Screen 3
<p align="center"> <img src="screenshots/Onboarding 3.png" width="280" alt="AttendU Onboarding 3"> </p>
👤 Role Selection

The Role Selection screen allows the user to select the appropriate
application role.

Supported roles include:

Admin
Teacher
Student
<p align="center"> <img src="screenshots/Role Selection.png" width="280" alt="AttendU Role Selection"> </p>
👨‍💼 Admin Screens

The Admin interface provides administrative functionality for managing the
university attendance system.

🔐 Admin Login

The Admin Login screen allows the administrator to authenticate before
accessing the Admin Dashboard.

<p align="center"> <img src="screenshots/Admin Login.png" width="280" alt="Admin Login"> </p>
📊 Admin Dashboard — Screen 1

The first Admin Dashboard screen provides the main administrative overview.

<p align="center"> <img src="screenshots/Admin Dashboard 1.png" width="280" alt="Admin Dashboard 1"> </p>
📊 Admin Dashboard — Screen 2

The second Admin Dashboard screen provides additional management options.

<p align="center"> <img src="screenshots/Admin Dashboard 2.png" width="280" alt="Admin Dashboard 2"> </p>
👨‍🏫 Teacher Screens

The Teacher interface is designed for teachers to manage their assigned
academic information and attendance sessions.

🔐 Teacher Login

The Teacher Login screen allows teachers to securely authenticate.

<p align="center"> <img src="screenshots/Teacher Login.png" width="280" alt="Teacher Login"> </p>
📊 Teacher Dashboard — Screen 1

The first Teacher Dashboard provides the main teacher overview.

<p align="center"> <img src="screenshots/Teacher Dashboard 1.png" width="280" alt="Teacher Dashboard 1"> </p>
📊 Teacher Dashboard — Screen 2

The second Teacher Dashboard provides additional teacher functionality.

<p align="center"> <img src="screenshots/Teacher Dashboard 2.png" width="280" alt="Teacher Dashboard 2"> </p>
📊 Teacher Dashboard — Screen 3

The third Teacher Dashboard screen provides additional teacher and
attendance-related functionality.

<p align="center"> <img src="screenshots/Teacher Dashboard 3.png" width="280" alt="Teacher Dashboard 3"> </p>
👨‍🎓 Student Screens

The Student interface allows students to access attendance sessions,
academic information, and attendance records.

📊 Student Dashboard — Screen 1

The first Student Dashboard screen provides the main student overview.

<p align="center"> <img src="screenshots/Student Dashboard 1.png" width="280" alt="Student Dashboard 1"> </p>
📊 Student Dashboard — Screen 2

The second Student Dashboard screen provides additional student attendance
and academic information.

<p align="center"> <img src="screenshots/Student Dashboard 2.png" width="280" alt="Student Dashboard 2"> </p>
🔄 Complete UI Flow

The major UI flow of AttendU can be represented as:

                         ┌──────────────┐
                         │ App Launch   │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │   App Logo   │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │  Onboarding  │
                         │   1 → 2 → 3  │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │Role Selection│
                         └──────┬───────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               ▼                ▼                ▼
          ┌─────────┐      ┌──────────┐     ┌──────────┐
          │  Admin  │      │ Teacher  │     │ Student  │
          └────┬────┘      └────┬─────┘     └────┬─────┘
               │                │                │
               ▼                ▼                ▼
          Admin Login      Teacher Login    Student Login
               │                │                │
               ▼                ▼                ▼
        Admin Dashboard   Teacher Dashboard Student Dashboard
🧪 Testing

The application can be tested through the following areas.

Authentication Testing
Admin registration
Admin login
Teacher registration
Teacher login
Student registration
Student login
Logout
Authentication state
Admin Testing
Add teacher
Add student
Manage subjects
Manage classes
Manage departments
Assign teacher
Assign student class
Assign student subjects
Create schedules
Teacher Testing
Teacher login
View dashboard
View assigned class
View assigned subject
Start attendance
Create live session
Generate QR
Start BLE advertising
View attendance records
End attendance
Student Testing
Student login
View dashboard
View class
View enrolled subjects
View today's schedule
View active attendance
Detect BLE
Mark attendance
Prevent duplicate attendance
View attendance history
View subject attendance
View attendance percentage
View low attendance warning
📡 BLE Testing

Actual BLE proximity testing is best performed using two Android devices.

┌────────────────────────┐
│      Teacher Phone     │
│                        │
│ Start Attendance       │
│         ↓              │
│ BLE Advertisement      │
└───────────┬────────────┘
            │
            │ Bluetooth
            │
            ▼
┌────────────────────────┐
│      Student Phone     │
│                        │
│ BLE Scanner             │
│         ↓              │
│ Detect Teacher Signal  │
│         ↓              │
│ Verify Session         │
│         ↓              │
│ Mark Attendance        │
└────────────────────────┘

During development, a single-device test mode can be used to test the
attendance interface when a second physical Android device is not available.

For real classroom proximity testing, separate teacher and student devices
should be used.

🔒 Security Considerations

AttendU combines multiple checks when processing attendance:

Authenticated Student
        +
Correct Class
        +
Correct Subject
        +
Student Enrollment
        +
Active Session
        +
BLE Proximity
        +
No Existing Attendance
        ↓
Attendance Record

The system is designed to associate attendance with the correct academic
context.

BLE Security Limitation

BLE RSSI is an approximate proximity mechanism and cannot cryptographically
prove physical presence.

Signal strength can be affected by:

Walls
Human bodies
Phone orientation
Hardware differences
Radio interference

For production deployment, stronger server-side authorization and
anti-spoofing mechanisms should also be considered.

📈 Advantages
For Administrators
Centralized academic management
Digital records
Easier user management
Reduced paperwork
For Teachers
Faster attendance process
Live attendance sessions
QR session generation
BLE classroom proximity
Digital attendance records
For Students
Quick attendance marking
Attendance history
Subject-wise attendance
Attendance percentage
Low attendance warnings
For Institutions
Centralized attendance infrastructure
Reduced manual work
Better attendance monitoring
Digital record management
Scalable application architecture
⚠️ Limitations

The current implementation has some limitations.

BLE Limitations

BLE RSSI cannot guarantee exact physical distance.

Device Dependency

BLE functionality depends on the Android device's Bluetooth capabilities
and permissions.

Internet Dependency

Firebase-based functionality generally requires network connectivity.

QR Workflow

The current implementation generates QR information on the teacher side but
does not depend on a dedicated student-side QR scanner.

Security

Production deployment should include stronger backend authorization and
Firestore security rules.

🔮 Future Enhancements

Possible future improvements include:

📲 Dedicated QR Scanner

Add a complete student-side QR scanning workflow.

📄 PDF Attendance Reports

Generate downloadable attendance reports in PDF format.

📊 Advanced Analytics

Add:

Attendance charts
Monthly statistics
Subject comparisons
Class statistics
Department statistics
Teacher reports
🔔 Push Notifications

Notify students when:

Attendance session starts
Attendance is successfully marked
Attendance becomes low
Important class information is available
👤 Face Recognition

Face recognition can be added as an additional attendance verification
method.

🌙 Dark Mode

Add complete application-wide dark mode support.

☁️ Cloud Backup

Improve backup and recovery mechanisms.

🔐 Advanced Security

Future security improvements may include:

Stronger Firestore security rules
Server-side attendance validation
Improved role-based authorization
Secure session token management
Better BLE anti-spoofing mechanisms
📚 Learning Outcomes

The development of AttendU provides practical experience in:

Android application development
Kotlin programming
Firebase Authentication
Cloud Firestore
XML UI development
View Binding
RecyclerView
MVVM architecture
Repository pattern
CRUD operations
Real-time Firebase data handling
BLE scanning
BLE advertising
QR code generation
Authentication management
Database design
Role-based application design
Mobile UI/UX development
🧩 Core System Components
┌──────────────────────────────────────────────┐
│                    AttendU                   │
├──────────────────────────────────────────────┤
│                                              │
│ Authentication                               │
│   ├── Admin                                  │
│   ├── Teacher                                │
│   └── Student                                │
│                                              │
│ Academic Management                          │
│   ├── Departments                            │
│   ├── Subjects                               │
│   ├── Classes                                │
│   ├── Teachers                               │
│   ├── Students                               │
│   └── Schedules                              │
│                                              │
│ Attendance                                   │
│   ├── Live Sessions                          │
│   ├── QR Session Information                 │
│   ├── BLE Proximity                          │
│   └── Attendance Records                     │
│                                              │
│ Student Monitoring                            │
│   ├── Attendance History                     │
│   ├── Subject Attendance                     │
│   └── Attendance Warning                     │
│                                              │
└──────────────────────────────────────────────┘
📱 Application Identity
Property	Details
Application Name	AttendU
Tagline	Smart University Attendance System
Platform	Android
Language	Kotlin
UI	XML
Authentication	Firebase Authentication
Database	Cloud Firestore
Proximity Technology	Bluetooth Low Energy
Attendance Technology	Live Session + QR Session Information
Architecture	MVVM / Repository Pattern
Package	com.university.attendance
📸 Screenshot Directory

All screenshots used in this README are stored inside:

screenshots/
│
├── Admin Dashboard 1.png
├── Admin Dashboard 2.png
├── Admin Login.png
├── App Logo.png
├── Onboarding 1.png
├── Onboarding 2.png
├── Onboarding 3.png
├── Role Selection.png
├── Student Dashboard 1.png
├── Student Dashboard 2.png
├── Teacher Dashboard 1.png
├── Teacher Dashboard 2.png
├── Teacher Dashboard 3.png
└── Teacher Login.png
🚀 Installation
Requirements

Before running AttendU, install:

Android Studio
Android SDK
JDK compatible with the project's Gradle configuration
Android device or emulator
Firebase account
Internet connection

For BLE testing, two Android devices are recommended.

1️⃣ Clone the Repository
git clone <YOUR-GITHUB-REPOSITORY-URL>

Open the cloned project in Android Studio.

2️⃣ Open in Android Studio

Open:

Android Studio
      ↓
File
      ↓
Open
      ↓
AttendU Project

Wait for Gradle synchronization to complete.

3️⃣ Firebase Configuration

Create a Firebase project and register the Android application.

Download:

google-services.json

Place it inside:

app/google-services.json
4️⃣ Enable Firebase Authentication

Open Firebase Console:

Firebase Console
      ↓
Authentication
      ↓
Sign-in Method

Enable the authentication provider required by the application.

5️⃣ Configure Cloud Firestore

Open:

Firebase Console
      ↓
Firestore Database

Create and configure the Firestore database.

6️⃣ Build the Project

In Android Studio:

Build
   ↓
Make Project

Or using Gradle:

./gradlew build

On Windows:

gradlew.bat build
7️⃣ Run the Application

Connect an Android device or start an emulator.

Then select:

Run
 ↓
Run 'app'
👨‍💻 Author
Muhammad Umer

BS Software Engineering

GitHub:

https://github.com/muhammadumer611

📄 License

This project is developed for educational and learning purposes.

⭐ AttendU
<p align="center">
📱 AttendU

Smart University Attendance System

Built with ❤️ using Kotlin, Firebase, Android, QR-based session
identification, and Bluetooth Low Energy.

</p> ```
