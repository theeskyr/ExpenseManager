package com.expensetracker.app.data.model

import java.util.UUID

data class SplitMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCurrentUser: Boolean = false
)

data class SplitExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val paidByMemberId: String,
    val splitAmongMemberIds: List<String> = emptyList() // Nếu rỗng hoặc đầy đủ = chia cho tất cả
)

data class ScannedReceiptItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var quantity: Int = 1,
    var amount: Double,
    var assignedMemberIds: Set<String> = emptySet()
)

data class DebtSettlement(
    val fromMemberName: String,
    val toMemberName: String,
    val amount: Double
)

data class MemberBalanceSummary(
    val member: SplitMember,
    val totalPaid: Double,
    val fairShare: Double,
    val netBalance: Double
)

data class SplitBillResult(
    val eventTitle: String,
    val totalAmount: Double,
    val perPersonAvg: Double,
    val myActualShare: Double,
    val myNetBalance: Double,
    val memberSummaries: List<MemberBalanceSummary>,
    val settlements: List<DebtSettlement>,
    val aiSummary: String? = null,
    val aiMessageTemplate: String? = null,
    val aiAdvice: String? = null
)

object SplitBillCalculator {
    fun calculate(
        eventTitle: String,
        members: List<SplitMember>,
        items: List<SplitExpenseItem>
    ): SplitBillResult {
        if (members.isEmpty()) {
            return SplitBillResult(
                eventTitle = eventTitle,
                totalAmount = 0.0,
                perPersonAvg = 0.0,
                myActualShare = 0.0,
                myNetBalance = 0.0,
                memberSummaries = emptyList(),
                settlements = emptyList()
            )
        }

        val totalAmount = items.sumOf { it.amount }
        val count = members.size
        val perPersonAvg = if (count > 0) totalAmount / count else 0.0

        // Map memberId -> totalPaid
        val paidMap = members.associate { it.id to 0.0 }.toMutableMap()
        // Map memberId -> fairShare (tổng chi phí công bằng từng người phải chịu)
        val fairShareMap = members.associate { it.id to 0.0 }.toMutableMap()

        items.forEach { item ->
            paidMap[item.paidByMemberId] = (paidMap[item.paidByMemberId] ?: 0.0) + item.amount

            // Xác định những ai tham gia chia khoản này
            val sharers = if (item.splitAmongMemberIds.isEmpty()) {
                members.map { it.id }
            } else {
                item.splitAmongMemberIds.filter { mId -> members.any { it.id == mId } }
            }

            val shareCount = if (sharers.isNotEmpty()) sharers.size else members.size
            val perSharerAmount = item.amount / shareCount

            sharers.forEach { sharerId ->
                fairShareMap[sharerId] = (fairShareMap[sharerId] ?: 0.0) + perSharerAmount
            }
        }

        val memberSummaries = members.map { member ->
            val paid = paidMap[member.id] ?: 0.0
            val fairShare = fairShareMap[member.id] ?: 0.0
            val net = paid - fairShare
            MemberBalanceSummary(
                member = member,
                totalPaid = paid,
                fairShare = fairShare,
                netBalance = net
            )
        }

        // Greedy debt minimization settlement algorithm
        val debtors = memberSummaries
            .filter { it.netBalance < -1.0 }
            .map { it.member.name to -it.netBalance }
            .toMutableList()

        val creditors = memberSummaries
            .filter { it.netBalance > 1.0 }
            .map { it.member.name to it.netBalance }
            .toMutableList()

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val settlements = mutableListOf<DebtSettlement>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]

            val settleAmount = minOf(debtor.second, creditor.second)
            if (settleAmount > 1.0) {
                settlements.add(
                    DebtSettlement(
                        fromMemberName = debtor.first,
                        toMemberName = creditor.first,
                        amount = settleAmount
                    )
                )
            }

            debtors[dIdx] = debtor.first to (debtor.second - settleAmount)
            creditors[cIdx] = creditor.first to (creditor.second - settleAmount)

            if (debtors[dIdx].second <= 1.0) dIdx++
            if (creditors[cIdx].second <= 1.0) cIdx++
        }

        val mySummary = memberSummaries.firstOrNull { it.member.isCurrentUser }
        val myActualShare = mySummary?.fairShare ?: perPersonAvg
        val myNetBalance = mySummary?.netBalance ?: 0.0

        return SplitBillResult(
            eventTitle = eventTitle,
            totalAmount = totalAmount,
            perPersonAvg = perPersonAvg,
            myActualShare = myActualShare,
            myNetBalance = myNetBalance,
            memberSummaries = memberSummaries,
            settlements = settlements
        )
    }
}
