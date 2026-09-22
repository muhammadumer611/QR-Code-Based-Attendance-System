package com.university.attendance

import DailySessionGroup


/** Groups DailyClassStatus rows under a Department -> Session heading for display. */
data class DailyDepartmentGroup(
    val departmentName: String,
    val sessionGroups: List<DailySessionGroup>
)