package com.example.caredose
import com.example.caredose.database.entities.DurationType
import java.text.SimpleDateFormat
import java.util.*

object DurationHelper {
    fun calculateEndDate(
        durationType: DurationType,
        durationValue: Int?,
        startDate: Long
    ): Long? {
        return when (durationType) {
            DurationType.CONTINUOUS -> null

            DurationType.DAYS -> {
                require(durationValue != null && durationValue > 0) {
                    "Duration value must be positive for DAYS"
                }
                startDate + (durationValue * 24 * 60 * 60 * 1000L)
            }

            DurationType.WEEKS -> {
                require(durationValue != null && durationValue > 0) {
                    "Duration value must be positive for WEEKS"
                }
                startDate + (durationValue * 7 * 24 * 60 * 60 * 1000L)
            }

            DurationType.MONTHS -> {
                require(durationValue != null && durationValue > 0) {
                    "Duration value must be positive for MONTHS"
                }
                Calendar.getInstance().apply {
                    timeInMillis = startDate
                    add(Calendar.MONTH, durationValue)
                }.timeInMillis
            }
        }
    }

    fun isScheduleActive(
        endDate: Long?,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        return endDate == null || endDate > currentTime
    }

    fun getRemainingDuration(
        endDate: Long?,
        currentTime: Long = System.currentTimeMillis()
    ): String {
        if (endDate == null) return "Continuous"

        val remainingMillis = endDate - currentTime
        if (remainingMillis <= 0) return "Expired"

        val remainingDays = remainingMillis / (24 * 60 * 60 * 1000L)

        return when {
            remainingDays > 30 -> {
                val months = remainingDays / 30
                if (months == 1L) "1 month left" else "$months months left"
            }
            remainingDays > 7 -> {
                val weeks = remainingDays / 7
                if (weeks == 1L) "1 week left" else "$weeks weeks left"
            }
            remainingDays == 1L -> "1 day left"
            else -> "$remainingDays days left"
        }
    }

    fun formatEndDate(
        endDate: Long?,
        pattern: String = "MMM dd, yyyy"
    ): String {
        if (endDate == null) return "No end date"

        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return dateFormat.format(Date(endDate))
    }
    fun getDurationSummary(
        durationType: DurationType,
        durationValue: Int?,
        endDate: Long?
    ): String {
        return when (durationType) {
            DurationType.CONTINUOUS -> "Continuous (no end date)"
            else -> {
                val valueStr = when (durationType) {
                    DurationType.DAYS -> "$durationValue ${if (durationValue == 1) "Day" else "Days"}"
                    DurationType.WEEKS -> "$durationValue ${if (durationValue == 1) "Week" else "Weeks"}"
                    DurationType.MONTHS -> "$durationValue ${if (durationValue == 1) "Month" else "Months"}"
                    else -> ""
                }
                "$valueStr (until ${formatEndDate(endDate)})"
            }
        }
    }
    fun validateDurationValue(
        durationType: DurationType,
        durationValue: Int?
    ): String? {
        if (durationType == DurationType.CONTINUOUS) {
            return null
        }

        if (durationValue == null || durationValue <= 0) {
            return "Please enter a valid duration"
        }

        return when (durationType) {
            DurationType.DAYS -> {
                if (durationValue > 365) "Duration cannot exceed 365 days" else null
            }
            DurationType.WEEKS -> {
                if (durationValue > 52) "Duration cannot exceed 52 weeks" else null
            }
            DurationType.MONTHS -> {
                if (durationValue > 12) "Duration cannot exceed 12 months" else null
            }
            else -> null
        }
    }

    fun generateScheduleGroupId(): String {
        return UUID.randomUUID().toString()
    }
}