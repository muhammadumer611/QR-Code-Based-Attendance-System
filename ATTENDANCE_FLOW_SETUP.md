# Attendance Flow Setup

This project uses a shared `classId` as the link between students, teacher allocations, class schedules and live attendance.

## 1. Admin setup order

Use the Admin Dashboard in this order:

1. Create Departments.
2. Create Subjects for the correct department, program and semester.
3. Create Students. A student's initial class is created from program + session + section.
4. Use **Student Class Allocation** to move/assign a student to an existing class when required.
5. Use **Teacher Allocation** to select Teacher -> Semester -> Session -> Class -> one or more Subjects.
6. Use **Student Subject Enrollment** to select a student and enroll the subjects available for that student's class/semester.
7. Use **Class Schedule** to create timetable entries.

## 2. Teacher allocation

Every `teacherSubjectAssignments` record represents this exact relation:

`teacherId + classId + subjectId + semester + session`

The class is not a free-text value. It comes from the shared `classes` collection. This prevents a teacher from being scheduled for a class that does not belong to the selected cohort.

## 3. Student class allocation

`Student Class Allocation` updates the student's `classId`, department, program, session and section to the selected existing class.

When a student moves to another class, the student's old `studentSubjectAssignments` are removed because those subject enrollments were tied to the old class. Re-enroll the student from **Student Subject Enrollment** after the move.

## 4. Student subject enrollment

`studentSubjectAssignments` represents:

`studentId + classId + subjectId + semester + session + section`

Subjects are loaded from Subject Management using the student's department, program and semester. The class relation is saved with every enrollment so attendance can verify the exact class.

## 5. Admin class schedule

When Admin creates a schedule, the dialog works from the teacher's real allocations:

`Teacher -> Assigned Class -> Assigned Subject`

The Assigned Class dropdown is populated from `teacherSubjectAssignments`. The selected allocation also supplies the exact semester and session automatically. The Assigned Subject dropdown is then populated only with subjects assigned to that teacher for that exact class/semester/session.

The saved `classSchedules` record contains the exact `teacherId`, `classId`, `subjectId`, semester, session and section.

## 6. Teacher live attendance

The teacher opens one of today's scheduled classes and starts live attendance.

Before creating a live session the app verifies:

- the logged-in teacher owns the schedule;
- the teacher is assigned to the exact class + subject;
- the schedule is for today;
- the current time is within the attendance window.

A time-limited `attendanceSessions` document is created. The QR payload is generated for optional classroom display; the current primary student flow is in-app live attendance, so a camera scan is not required.

## 7. Student live classes

The student dashboard uses the same `classId` stored in the student's profile.

Today's `classSchedules` are therefore automatically visible for that student when the schedule belongs to the student's class, semester, session, section, department and program.

For live attendance, the student receives the active session only when:

- class identity matches;
- semester/session/section match;
- the session is active and not expired;
- the student is enrolled in the session's subject.

## 8. Attendance save validation

Before writing an attendance record, the app rechecks the live session, student class identity, student subject enrollment, schedule identity, teacher identity and exact teacher-class-subject assignment.

The attendance record ID is:

`${studentId}_${subjectId}_${date}`

A Firestore transaction prevents the same student from creating a duplicate attendance record for the same subject on the same date.

## 9. Recommended testing sequence

Create at least two students in the same class, create one teacher, assign that teacher to the class and a subject, enroll both students in that subject, create today's schedule, then log in as the teacher and start attendance. Both students should see the same live class and be able to mark attendance once.
