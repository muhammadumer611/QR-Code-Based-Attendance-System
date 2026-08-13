import com.university.attendance.DailyClassStatus

data class DailySessionGroup(
    val session: String,
    val classStatuses: List<DailyClassStatus>
)