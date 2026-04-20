package com.example.skyboxcricket

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BookingDateUtils {

    private const val DATE_TIME_PATTERN = "dd MMM yyyy, hh:mm a"
    private const val DATE_LABEL_PATTERN = "dd MMMM yyyy"
    private const val TIME_PATTERN = "hh:mm a"

    fun parseDateTime(value: String): Date? {
        return try {
            SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).parse(value)
        } catch (_: Exception) {
            null
        }
    }

    fun formatDateLabel(timeInMillis: Long): String {
        return SimpleDateFormat(DATE_LABEL_PATTERN, Locale.getDefault()).format(Date(timeInMillis))
    }

    fun formatRangeLabel(startMillis: Long, endMillis: Long): String {
        return "${formatDateLabel(startMillis)} - ${formatDateLabel(endMillis)}"
    }

    fun extractTime(value: String): String {
        return parseDateTime(value)?.let {
            SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(it)
        } ?: value
    }

    fun startOfDay(timeInMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
