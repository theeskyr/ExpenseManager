package com.expensetracker.app

import com.expensetracker.app.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testStartAndEndOfDay() {
        val now = System.currentTimeMillis()
        val start = DateUtils.getStartOfDay(now)
        val end = DateUtils.getEndOfDay(now)

        assertTrue(start <= now)
        assertTrue(end >= now)
        assertEquals(86399999L, end - start)
    }

    @Test
    fun testStartAndEndOfMonth() {
        val start = DateUtils.getStartOfMonth(5, 2026) // May 2026
        val end = DateUtils.getEndOfMonth(5, 2026)

        assertTrue(start < end)

        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(2026, cal.get(Calendar.YEAR))

        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MAY, endCal.get(Calendar.MONTH))
    }
}
