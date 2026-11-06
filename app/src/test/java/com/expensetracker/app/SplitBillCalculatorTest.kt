package com.expensetracker.app

import com.expensetracker.app.data.model.SplitBillCalculator
import com.expensetracker.app.data.model.SplitExpenseItem
import com.expensetracker.app.data.model.SplitMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitBillCalculatorTest {

    @Test
    fun testEmptyMembers() {
        val result = SplitBillCalculator.calculate(
            eventTitle = "Empty Test",
            members = emptyList(),
            items = emptyList()
        )
        assertEquals(0.0, result.totalAmount, 0.001)
        assertEquals(0, result.settlements.size)
    }

    @Test
    fun testEqualSplitTwoMembers() {
        val memberA = SplitMember(id = "1", name = "An", isCurrentUser = true)
        val memberB = SplitMember(id = "2", name = "Bình")
        val members = listOf(memberA, memberB)

        // An pays 100,000 for dinner shared by both
        val item = SplitExpenseItem(
            id = "item1",
            title = "Ăn tối",
            amount = 100_000.0,
            paidByMemberId = "1",
            splitAmongMemberIds = listOf("1", "2")
        )

        val result = SplitBillCalculator.calculate(
            eventTitle = "Ăn tối cuối tuần",
            members = members,
            items = listOf(item)
        )

        assertEquals(100_000.0, result.totalAmount, 0.001)
        assertEquals(50_000.0, result.myActualShare, 0.001)
        assertEquals(50_000.0, result.myNetBalance, 0.001)
        assertEquals(1, result.settlements.size)
        val settlement = result.settlements.first()
        assertEquals("Bình", settlement.fromMemberName)
        assertEquals("An", settlement.toMemberName)
        assertEquals(50_000.0, settlement.amount, 0.001)
    }

    @Test
    fun testThreeMembersUnevenSplit() {
        val memberA = SplitMember(id = "1", name = "An", isCurrentUser = true)
        val memberB = SplitMember(id = "2", name = "Bình")
        val memberC = SplitMember(id = "3", name = "Cường")
        val members = listOf(memberA, memberB, memberC)

        // Item 1: An pays 300k for all 3 (100k each)
        // Item 2: Bình pays 90k for only Bình and Cường (45k each)
        val item1 = SplitExpenseItem(id = "i1", title = "Khách sạn", amount = 300_000.0, paidByMemberId = "1")
        val item2 = SplitExpenseItem(id = "i2", title = "Vé tham quan", amount = 90_000.0, paidByMemberId = "2", splitAmongMemberIds = listOf("2", "3"))

        val result = SplitBillCalculator.calculate("Du lịch", members, listOf(item1, item2))

        assertEquals(390_000.0, result.totalAmount, 0.001)

        val sumA = result.memberSummaries.first { it.member.id == "1" }
        val sumB = result.memberSummaries.first { it.member.id == "2" }
        val sumC = result.memberSummaries.first { it.member.id == "3" }

        assertEquals(100_000.0, sumA.fairShare, 0.001)
        assertEquals(200_000.0, sumA.netBalance, 0.001)

        assertEquals(145_000.0, sumB.fairShare, 0.001)
        assertEquals(-55_000.0, sumB.netBalance, 0.001)

        assertEquals(145_000.0, sumC.fairShare, 0.001)
        assertEquals(-145_000.0, sumC.netBalance, 0.001)

        val totalSettled = result.settlements.sumOf { it.amount }
        assertEquals(200_000.0, totalSettled, 0.001)
        assertTrue(result.settlements.all { it.toMemberName == "An" })
    }
}
