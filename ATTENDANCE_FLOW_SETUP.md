# Class-Scoped Live Attendance Flow

The project now uses a class-scoped relation instead of `subjects.teacherId` as the source of truth.

## Admin assignment

1. Open Teacher Subject Assignment.
2. Select Teacher.
3. Select Semester.
4. Enter Session, e.g. `2022`.
5. Select the exact Class / Section created from student records.
6. The app loads only subjects belonging to that class's department/program and semester.
7. Select subjects and save.

Each record in `teacherSubjectAssignments` represents:

`teacherId + classId + subjectId + semester + session`

The subject document's `teacherId` is intentionally not updated. This allows the same subject to be taught by different teachers in different classes.

## Admin schedule

When Admin creates a class schedule, the flow is:

`Teacher -> Semester -> Session -> Assigned Class -> Assigned Subject`

The schedule stores the exact `teacherId`, `classId`, `subjectId`, semester, session and section.
The repository checks the exact assignment again before saving the schedule.

## Teacher attendance

Teacher opens a class from Today's Classes and presses `Start Live Attendance`.
A live `attendanceSessions` document is created for that schedule. The session is time-limited and is linked to the exact teacher, class and subject.
The QR is still generated on the teacher side for optional classroom display, but students do not need to scan it.

## Student attendance

The Student Dashboard listens for active attendance sessions for the student's exact `classId`.
The student sees the live subject and one `Save Attendance` button.
No camera permission or QR scanner is required.

Before saving, the app rechecks:

- student is active and logged in
- session is active and not expired
- classId matches
- department, program, semester, session and section match
- schedule matches the session
- teacher profile matches
- exact teacher/class/subject assignment exists
- duplicate attendance does not already exist

Attendance is stored in `attendance_records` using:

`${studentId}_${subjectId}_${date}`

so the same attendance collection can still be used by existing admin reports/manual attendance.

## Firebase rule required for the explicit student assignment collection
This version creates `studentSubjectAssignments` records so the student-to-subject-to-teacher relationship is explicit. Deploy the included `firestore.rules` file to the same Firebase project used by `google-services.json` before testing attendance.

## Final flow
1. Admin creates students with the exact class identity (program/session/section/semester/classId).
2. Admin opens Teacher Assignment, selects Teacher -> Semester -> Session -> Class -> one or more Subjects, and saves.
3. The save creates teacher-class-subject assignments and explicit student-subject-teacher links for the active students in that class.
4. Admin opens Class Schedule. Teacher -> Semester -> Session -> Assigned Class -> Assigned Subject are dependent selections; only valid assignments are offered.
5. Teacher opens today's assigned class and starts Live Attendance. The QR is generated as an optional traditional display, while the primary student flow is live in-app attendance.
6. Only students whose class and explicit subject/teacher assignment match the live session see the Save Attendance card.
7. Save Attendance performs server-data revalidation in the client: student identity, active session, date/expiry, class, semester, session, section, schedule, teacher UID, teacher assignment and explicit student assignment. A transaction prevents duplicate attendance.
