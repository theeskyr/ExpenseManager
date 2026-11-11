/**
 * SplitBillCalculator: Thuật toán tính toán chia tiền nhóm và tối ưu số lần chuyển khoản
 */
const SplitBillCalculator = {
  calculate(eventTitle, members, items) {
    if (!members || members.length === 0) {
      return {
        eventTitle,
        totalAmount: 0,
        perPersonAvg: 0,
        myActualShare: 0,
        myNetBalance: 0,
        memberSummaries: [],
        settlements: []
      };
    }

    const totalAmount = items.reduce((acc, item) => acc + (Number(item.amount) || 0), 0);
    const count = members.length;
    const perPersonAvg = count > 0 ? totalAmount / count : 0;

    const paidMap = {};
    const fairShareMap = {};
    members.forEach((m) => {
      paidMap[m.id] = 0;
      fairShareMap[m.id] = 0;
    });

    items.forEach((item) => {
      const amount = Number(item.amount) || 0;
      paidMap[item.paidByMemberId] = (paidMap[item.paidByMemberId] || 0) + amount;

      const sharers = (!item.splitAmongMemberIds || item.splitAmongMemberIds.length === 0)
        ? members.map((m) => m.id)
        : item.splitAmongMemberIds.filter((id) => members.some((m) => m.id === id));

      const shareCount = sharers.length > 0 ? sharers.length : members.length;
      const perSharer = amount / shareCount;

      sharers.forEach((sId) => {
        fairShareMap[sId] = (fairShareMap[sId] || 0) + perSharer;
      });
    });

    const memberSummaries = members.map((m) => {
      const totalPaid = paidMap[m.id] || 0;
      const fairShare = fairShareMap[m.id] || 0;
      const netBalance = totalPaid - fairShare;
      return {
        member: m,
        totalPaid,
        fairShare,
        netBalance
      };
    });

    // Greedy debt settlement algorithm
    const debtors = []; // Phải trả thêm
    const creditors = []; // Được nhận lại

    memberSummaries.forEach((ms) => {
      if (ms.netBalance < -1) {
        debtors.push({ name: ms.member.name, isMe: ms.member.isCurrentUser, amount: -ms.netBalance });
      } else if (ms.netBalance > 1) {
        creditors.push({ name: ms.member.name, isMe: ms.member.isCurrentUser, amount: ms.netBalance });
      }
    });

    const settlements = [];
    let dIdx = 0;
    let cIdx = 0;

    while (dIdx < debtors.length && cIdx < creditors.length) {
      const debtor = debtors[dIdx];
      const creditor = creditors[cIdx];
      const settleAmount = Math.min(debtor.amount, creditor.amount);

      if (settleAmount >= 1) {
        settlements.push({
          fromMemberName: debtor.name,
          toMemberName: creditor.name,
          fromIsMe: debtor.isMe,
          toIsMe: creditor.isMe,
          amount: Math.round(settleAmount)
        });
      }

      debtor.amount -= settleAmount;
      creditor.amount -= settleAmount;

      if (debtor.amount < 1) dIdx++;
      if (creditor.amount < 1) cIdx++;
    }

    const mySummary = memberSummaries.find((s) => s.member.isCurrentUser) || memberSummaries[0];

    return {
      eventTitle: eventTitle || 'Buổi chia tiền',
      totalAmount,
      perPersonAvg,
      myActualShare: mySummary ? mySummary.fairShare : 0,
      myNetBalance: mySummary ? mySummary.netBalance : 0,
      memberSummaries,
      settlements
    };
  },

  exportToCSV(result) {
    const lines = [];
    lines.push(`BẢNG TÍNH CHIA TIỀN: ${result.eventTitle}`);
    lines.push(`Ngày xuất: ${new Date().toLocaleString('vi-VN')}`);
    lines.push(`Tổng chi phí: ${formatMoney(result.totalAmount)}`);
    lines.push(`Trung bình / người: ${formatMoney(result.perPersonAvg)}`);
    lines.push('');
    lines.push('--- CHI TIẾT TỪNG THÀNH VIÊN ---');
    lines.push('Tên,Đã thanh toán trước,Phần phải chịu,Số dư chênh lệch,Trạng thái');

    result.memberSummaries.forEach((ms) => {
      let status = 'Đã hòa vốn';
      if (ms.netBalance > 100) status = `Được nhận lại ${formatMoney(ms.netBalance)}`;
      else if (ms.netBalance < -100) status = `Cần chuyển khoản trả ${formatMoney(-ms.netBalance)}`;
      lines.push(`"${ms.member.name}",${ms.totalPaid},${ms.fairShare},${ms.netBalance},"${status}"`);
    });

    lines.push('');
    lines.push('--- HƯỚNG DẪN CHUYỂN KHOẢN TỐI ƯU ---');
    lines.push('Người gửi,Người nhận,Số tiền');
    result.settlements.forEach((st) => {
      lines.push(`"${st.fromMemberName}","${st.toMemberName}",${st.amount}`);
    });

    const csvContent = '\uFEFF' + lines.join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ChiaTien_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
};
