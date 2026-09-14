# Final Attendance Data Flow

The app uses these Firestore collections as the source of truth:

- departments: master departments
- subjects: master subject catalogue only (no student/teacher relationship required)
- teachers: teacher profiles
- students: student profiles + exact class/session/semester/section
- teacherSubjectAssignments: exact Teacher + Class + Section + Subject + Semester + Session relation
- studentSubjectAssignments: exact Student + TeacherAssignment + Subject relation
- classSchedules: scheduled class linked to teacherSubjectAssignments via assignmentId
- attendanceSessions: live attendance session linked to the schedule/assignment
- attendance_records: final attendance records

## Required admin order

1. Add Department
2. Add Class / students so the class has an exact classId, section, semester and session
3. Add Teacher
4. Add Subject in Subject Management for the exact Department + Program + Semester
5. Admin Dashboard -> Teacher Assignment:
   - select teacher
   - select semester
   - enter the exact session used by students
   - select class/section
   - select the subject
   - Save
6. Confirm `teacherSubjectAssignments` now exists and contains the exact relation.
7. Admin Dashboard -> Student Subject Assignment:
   - select the student
   - the subject now appears only if the exact teacher assignment matches class + section + semester + session
   - select subject and Save
8. Admin -> Class Schedule -> FAB:
   - select teacher
   - semester + exact session
   - Assigned Class dropdown shows only that teacher's assigned classes
   - select class
   - Assigned Subject dropdown shows only that teacher's assigned subjects for that class
   - save schedule
9. Teacher starts Live Attendance from the saved schedule.
10. Matching enrolled students receive the live QR/session on their dashboard and press Save Attendance; no student camera scan is required.

## Important

Do NOT manually put student IDs or teacher IDs into the `subjects` master document. Teacher/student relationships are intentionally stored in the assignment collections so one subject can be taught by different teachers/classes without corrupting the catalogue.

Existing old `studentSubjectAssignments` documents may not contain `teacherAssignmentId`. Re-save those students through Admin -> Student Subject Assignment after creating the teacher assignment.

Build could not be run in this environment because Gradle 8.13 could not be downloaded from services.gradle.org (network DNS unavailable). XML/brace/static checks were performed.
