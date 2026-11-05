package com.expensetracker.app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    private val fullDateFormat = ThreadLocal.withInitial { SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")) }
    private val monthYearFormat = ThreadLocal.withInitial { SimpleDateFormat("MM/yyyy", Locale.getDefault()) }
    private val dayMonthFormat = ThreadLocal.withInitial { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    fun formatTime(timestamp: Long): String {
        val offset = TimeZone.getDefault().getOffset(timestamp)
        val totalSeconds = (timestamp + offset) / 1000L
        val secondOfDay = ((totalSeconds % 86400L) + 86400L) % 86400L
        val hour = (secondOfDay / 3600L).toInt()
        val min = ((secondOfDay % 3600L) / 60L).toInt()
        val h1 = (hour / 10) + '0'.code
        val h2 = (hour % 10) + '0'.code
        val m1 = (min / 10) + '0'.code
        val m2 = (min % 10) + '0'.code
        return String(charArrayOf(h1.toChar(), h2.toChar(), ':', m1.toChar(), m2.toChar()))
    }
    fun formatDate(timestamp: Long): String = dateFormat.get()!!.format(Date(timestamp))
    fun formatFullDate(timestamp: Long): String = fullDateFormat.get()!!.format(Date(timestamp))
    fun formatMonthYear(timestamp: Long): String = monthYearFormat.get()!!.format(Date(timestamp))

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val offset = TimeZone.getDefault().getOffset(timestamp)
        return ((timestamp + offset) / 86400000L) * 86400000L - offset
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        return getStartOfDay(timestamp) + 86399999L
    }

    fun getStartOfMonth(month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfMonth(month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfWeek(): Long {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun isToday(timestamp: Long): Boolean {
        val todayStart = getStartOfDay()
        return timestamp >= todayStart && timestamp < todayStart + 86400000L
    }

    fun isYesterday(timestamp: Long): Boolean {
        val yesterdayStart = getStartOfDay() - 86400000L
        return timestamp >= yesterdayStart && timestamp < yesterdayStart + 86400000L
    }

    fun getHeaderDateString(timestamp: Long): String {
        val todayStart = getStartOfDay()
        val yesterdayStart = todayStart - 86400000L
        val dayStart = getStartOfDay(timestamp)
        return when (dayStart) {
            todayStart -> "Hôm nay, " + dayMonthFormat.get()!!.format(Date(timestamp))
            yesterdayStart -> "Hôm qua, " + dayMonthFormat.get()!!.format(Date(timestamp))
            else -> formatFullDate(timestamp)
        }
    }
}
