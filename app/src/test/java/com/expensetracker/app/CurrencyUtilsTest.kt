package com.expensetracker.app

import com.expensetracker.app.utils.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun testFormatVndZero() {
        val result = CurrencyUtils.formatVnd(0.0)
        assertEquals("0 ₫", result)
    }

    @Test
    fun testFormatVndPositiveAmounts() {
        assertEquals("50.000 ₫", CurrencyUtils.formatVnd(50_000.0))
        assertEquals("1.500.000 ₫", CurrencyUtils.formatVnd(1_500_000.0))
        assertEquals("25.000.000 ₫", CurrencyUtils.formatVnd(25_000_000.0))
    }

    @Test
    fun testFormatVndNegativeAmounts() {
        assertEquals("-50.000 ₫", CurrencyUtils.formatVnd(-50_000.0))
    }

    @Test
    fun testParseAmount() {
        assertEquals(50000.0, CurrencyUtils.parseAmount("50.000"), 0.001)
        assertEquals(1500000.0, CurrencyUtils.parseAmount("1,500,000 ₫"), 0.001)
        assertEquals(0.0, CurrencyUtils.parseAmount(""), 0.001)
        assertEquals(0.0, CurrencyUtils.parseAmount("abc"), 0.001)
    }
}
