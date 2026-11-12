/**
 * Main Application Logic for Expense Manager PWA
 */

// Application Global State
const state = {
  currentTab: 'home',
  transactions: [],
  categories: [],
  accounts: [],
  investments: [],
  trips: [],
  tripExpenses: [],
  activeTrip: null,
  tripActiveDay: 'ALL',
  tripActiveTab: 'settlement',
  filter: {
    type: 'ALL',
    accountId: null
  },
  splitBill: {
    eventTitle: 'Buổi chia tiền',
    members: [
      { id: 'mem_me', name: 'Tôi', isCurrentUser: true },
      { id: 'mem_tuan', name: 'Tuấn', isCurrentUser: false },
      { id: 'mem_linh', name: 'Linh', isCurrentUser: false }
    ],
    items: [],
    result: null
  },
  settings: {
    apiKey: '',
    selectedModel: 'gemini-3.1-flash-lite'
  },
  statsBarMode: 'WEEKLY',
  scannedOcrItems: []
};

let chartInstance = null;
let barChartInstance = null;

// Initialize Application on Page Load
window.addEventListener('DOMContentLoaded', async () => {
  await loadData();
  renderApp();
  initSpeechRecognition();
});

// Load all data from IndexedDB into memory
async function loadData() {
  try {
    state.accounts = await getAll('accounts');
    state.categories = await getAll('categories');
    state.transactions = (await getAll('transactions')).sort((a, b) => b.timestamp - a.timestamp);
    state.investments = await getAll('investments');
    state.trips = await getAll('trips');
    state.tripExpenses = await getAll('tripExpenses');

    // Preload settings synchronously into state to avoid [object Promise] in UI
    state.settings.apiKey = await GeminiManager.getApiKey();
    state.settings.selectedModel = await GeminiManager.getSelectedModel();
  } catch (err) {
    console.error('Error loading data from IndexedDB:', err);
  }
}

// Main Render Router
function renderApp() {
  const container = document.getElementById('main-content');
  if (!container) return;

  updateBottomNav();

  switch (state.currentTab) {
    case 'home':
      container.innerHTML = renderHomeScreen();
      break;
    case 'stats':
      container.innerHTML = renderStatsScreen();
      setTimeout(renderChart, 50);
      break;
    case 'ai':
      container.innerHTML = renderAiScreen();
      scrollChatToBottom();
      break;
    case 'split':
      container.innerHTML = renderSplitScreen();
      break;
    case 'trips':
      container.innerHTML = state.activeTrip ? renderTripDetailScreen() : renderTripsScreen();
      break;
    case 'investments':
      container.innerHTML = renderInvestmentsScreen();
      break;
    case 'accounts':
      container.innerHTML = renderAccountsScreen();
      break;
    case 'settings':
      container.innerHTML = renderSettingsScreen();
      break;
    default:
      container.innerHTML = renderHomeScreen();
  }

  // Refresh Lucide Icons
  if (window.lucide) {
    lucide.createIcons();
  }
}

function switchTab(tabName) {
  state.currentTab = tabName;
  if (tabName !== 'trips') {
    state.activeTrip = null;
  }
  renderApp();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateBottomNav() {
  const tabs = ['home', 'stats', 'split', 'trips', 'ai'];
  tabs.forEach((tab) => {
    const btn = document.getElementById(`nav-${tab}`);
    if (btn) {
      if (state.currentTab === tab) {
        btn.classList.add('text-emerald-600', 'font-bold');
        btn.classList.remove('text-slate-400');
      } else {
        btn.classList.remove('text-emerald-600', 'font-bold');
        btn.classList.add('text-slate-400');
      }
    }
  });
}

// ----------------- HOME SCREEN -----------------
function renderHomeScreen() {
  const now = new Date();
  const currentMonth = now.getMonth();
  const currentYear = now.getFullYear();

  let totalBalance = 0;
  let reserveBalance = 0;
  state.accounts.forEach((acc) => {
    const bal = Number(acc.currentBalance) || 0;
    if (acc.isReserve) reserveBalance += bal;
    else totalBalance += bal;
  });

  const monthTxs = state.transactions.filter((tx) => {
    const d = new Date(tx.timestamp);
    return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
  });

  const monthExpense = monthTxs.filter((t) => t.type === 'EXPENSE').reduce((s, t) => s + t.amount, 0);
  const monthIncome = monthTxs.filter((t) => t.type === 'INCOME').reduce((s, t) => s + t.amount, 0);

  // Filtered transactions
  let filtered = state.transactions;
  if (state.filter.type !== 'ALL') {
    filtered = filtered.filter((t) => t.type === state.filter.type);
  }
  if (state.filter.accountId) {
    filtered = filtered.filter((t) => t.accountId == state.filter.accountId);
  }

  return `
    <div class="space-y-4 pb-24">
      <!-- Hero Balance Card -->
      <div class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-600 to-teal-800 p-6 text-white shadow-lg">
        <div class="relative z-10 space-y-4">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold uppercase tracking-wider text-emerald-100">Số dư khả dụng</span>
            <button onclick="openModal('add-transaction-modal')" class="flex items-center space-x-1 rounded-full bg-white/20 px-3 py-1 text-xs font-bold backdrop-blur-sm transition hover:bg-white/30">
              <i data-lucide="plus" class="h-3.5 w-3.5"></i>
              <span>Thêm mới</span>
            </button>
          </div>
          <div>
            <h1 class="text-3xl font-extrabold tracking-tight">${formatMoney(totalBalance)}</h1>
            <p class="mt-1 text-xs text-emerald-100">Quỹ khẩn cấp / Dự phòng: <span class="font-bold text-white">${formatMoney(reserveBalance)}</span></p>
          </div>
          <div class="grid grid-cols-2 gap-3 pt-2">
            <div class="rounded-2xl bg-white/10 p-3 backdrop-blur-sm">
              <div class="flex items-center space-x-1.5 text-xs text-emerald-200">
                <i data-lucide="arrow-down-left" class="h-4 w-4 text-emerald-300"></i>
                <span>Thu tháng này</span>
              </div>
              <p class="mt-1 text-sm font-bold text-white">${formatMoney(monthIncome)}</p>
            </div>
            <div class="rounded-2xl bg-white/10 p-3 backdrop-blur-sm">
              <div class="flex items-center space-x-1.5 text-xs text-rose-200">
                <i data-lucide="arrow-up-right" class="h-4 w-4 text-rose-300"></i>
                <span>Chi tháng này</span>
              </div>
              <p class="mt-1 text-sm font-bold text-white">${formatMoney(monthExpense)}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Action Buttons -->
      <div class="grid grid-cols-4 gap-2">
        <button onclick="openOcrScanModal('split')" class="flex flex-col items-center justify-center rounded-2xl bg-purple-50 p-3 text-purple-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-600 text-white shadow-sm">
            <i data-lucide="scan-line" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Quét Bill</span>
        </button>

        <button onclick="switchTab('accounts')" class="flex flex-col items-center justify-center rounded-2xl bg-emerald-50 p-3 text-emerald-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-sm">
            <i data-lucide="credit-card" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Ví tiền</span>
        </button>

        <button onclick="switchTab('split')" class="flex flex-col items-center justify-center rounded-2xl bg-blue-50 p-3 text-blue-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm">
            <i data-lucide="users" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Chia tiền</span>
        </button>

        <button onclick="switchTab('investments')" class="flex flex-col items-center justify-center rounded-2xl bg-indigo-50 p-3 text-indigo-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-sm">
            <i data-lucide="trending-up" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Đầu tư</span>
        </button>
      </div>

      <!-- Filter Bar -->
      <div class="flex items-center justify-between pt-2">
        <h2 class="text-sm font-bold text-slate-800">Lịch sử giao dịch</h2>
        <div class="flex space-x-1 rounded-xl bg-slate-100 p-1">
          <button onclick="setFilterType('ALL')" class="rounded-lg px-2.5 py-1 text-xs font-semibold ${state.filter.type === 'ALL' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500'}">Tất cả</button>
          <button onclick="setFilterType('EXPENSE')" class="rounded-lg px-2.5 py-1 text-xs font-semibold ${state.filter.type === 'EXPENSE' ? 'bg-white text-rose-600 shadow-sm' : 'text-slate-500'}">Chi tiêu</button>
          <button onclick="setFilterType('INCOME')" class="rounded-lg px-2.5 py-1 text-xs font-semibold ${state.filter.type === 'INCOME' ? 'bg-white text-emerald-600 shadow-sm' : 'text-slate-500'}">Thu nhập</button>
        </div>
      </div>

      <!-- Transaction List -->
      ${renderTransactionList(filtered)}
    </div>
  `;
}

function renderTransactionList(transactions) {
  if (transactions.length === 0) {
    return `
      <div class="flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center">
        <div class="flex h-12 w-12 items-center justify-center rounded-full bg-slate-50 text-slate-400">
          <i data-lucide="receipt-text" class="h-6 w-6"></i>
        </div>
        <p class="mt-3 text-xs font-semibold text-slate-500">Chưa có giao dịch nào</p>
        <button onclick="openModal('add-transaction-modal')" class="mt-3 rounded-full bg-emerald-600 px-4 py-1.5 text-xs font-bold text-white shadow transition hover:bg-emerald-700">
          + Thêm giao dịch đầu tiên
        </button>
      </div>
    `;
  }

  return `
    <div class="space-y-2">
      ${transactions.map((tx) => {
        const cat = state.categories.find((c) => c.id === tx.categoryId) || { name: 'Khác', colorHex: '#64748B', iconName: 'circle' };
        const acc = state.accounts.find((a) => a.id == tx.accountId) || { name: 'Ví' };
        const isExp = tx.type === 'EXPENSE';
        const dateStr = new Date(tx.timestamp).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });

        return `
          <div class="flex items-center justify-between rounded-2xl border border-slate-100 bg-white p-3.5 shadow-sm transition hover:shadow">
            <div class="flex items-center space-x-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-2xl" style="background-color: ${cat.colorHex}15; color: ${cat.colorHex}">
                <i data-lucide="${cat.iconName || 'tag'}" class="h-5 w-5"></i>
              </div>
              <div>
                <p class="text-sm font-bold text-slate-800">${cat.name}</p>
                <p class="text-[11px] text-slate-400">${dateStr} • ${acc.name} ${tx.note ? '• ' + tx.note : ''}</p>
              </div>
            </div>
            <div class="text-right">
              <p class="text-sm font-extrabold ${isExp ? 'text-rose-600' : 'text-emerald-600'}">
                ${isExp ? '-' : '+'}${formatMoney(tx.amount)}
              </p>
              <button onclick="deleteTransaction('${tx.id}')" class="text-[11px] text-slate-300 hover:text-rose-500">Xóa</button>
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function setFilterType(type) {
  state.filter.type = type;
  renderApp();
}

// ----------------- ACCOUNTS / WALLETS SCREEN -----------------
function renderAccountsScreen() {
  let totalBalance = 0;
  let reserveBalance = 0;
  state.accounts.forEach((acc) => {
    const bal = Number(acc.currentBalance) || 0;
    if (acc.isReserve) reserveBalance += bal;
    else totalBalance += bal;
  });

  return `
    <div class="space-y-4 pb-24">
      <div class="rounded-3xl bg-gradient-to-br from-slate-900 to-slate-800 p-5 text-white shadow-lg space-y-3">
        <span class="text-xs font-semibold uppercase text-slate-300">Tổng tài sản các ví</span>
        <h2 class="text-3xl font-black">${formatMoney(totalBalance + reserveBalance)}</h2>
        <div class="flex justify-between border-t border-white/10 pt-2 text-xs">
          <span>Khả dụng: <b class="text-emerald-400">${formatMoney(totalBalance)}</b></span>
          <span>Dự phòng: <b class="text-purple-400">${formatMoney(reserveBalance)}</b></span>
        </div>
      </div>

      <div class="flex items-center justify-between">
        <h3 class="text-sm font-bold text-slate-800">Danh sách Ví & Tài khoản (${state.accounts.length})</h3>
        <button onclick="openModal('add-account-modal')" class="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white shadow">
          + Thêm ví mới
        </button>
      </div>

      <div class="space-y-2.5">
        ${state.accounts.map((acc) => {
          const typeLabels = {
            CASH: 'Tiền mặt',
            BANK: 'Ngân hàng',
            E_WALLET: 'Ví điện tử',
            RESERVE: 'Quỹ dự phòng'
          };
          return `
            <div class="flex items-center justify-between rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
              <div class="flex items-center space-x-3">
                <div class="flex h-11 w-11 items-center justify-center rounded-2xl" style="background-color: ${acc.colorHex || '#10B981'}15; color: ${acc.colorHex || '#10B981'}">
                  <i data-lucide="${acc.iconName || 'credit-card'}" class="h-6 w-6"></i>
                </div>
                <div>
                  <div class="flex items-center space-x-2">
                    <h4 class="text-sm font-bold text-slate-900">${acc.name}</h4>
                    ${acc.isReserve ? '<span class="rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-bold text-purple-700">Dự phòng</span>' : ''}
                  </div>
                  <p class="text-[11px] text-slate-400">${typeLabels[acc.type] || 'Tài khoản'}</p>
                </div>
              </div>
              <div class="text-right">
                <p class="text-sm font-black text-slate-900">${formatMoney(acc.currentBalance)}</p>
                <div class="mt-1 flex space-x-2 justify-end">
                  <button onclick="openEditBalanceModal(${acc.id})" class="text-[11px] font-bold text-emerald-600 hover:underline">Sửa số dư</button>
                  ${state.accounts.length > 1 ? `<button onclick="deleteAccount(${acc.id})" class="text-[11px] text-slate-300 hover:text-rose-500">Xóa</button>` : ''}
                </div>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

function openEditBalanceModal(accId) {
  const acc = state.accounts.find((a) => a.id == accId);
  if (!acc) return;
  document.getElementById('edit-balance-acc-id').value = acc.id;
  document.getElementById('edit-balance-acc-name').innerText = `Tài khoản: ${acc.name} (Hiện tại: ${formatMoney(acc.currentBalance)})`;
  document.getElementById('edit-balance-input').value = acc.currentBalance;
  openModal('edit-balance-modal');
}

async function submitEditBalance() {
  const accId = document.getElementById('edit-balance-acc-id').value;
  const inputStr = document.getElementById('edit-balance-input').value;
  const newBal = parseMoneyInput(inputStr);

  const acc = state.accounts.find((a) => a.id == accId);
  if (acc) {
    acc.currentBalance = newBal;
    await putItem('accounts', acc);
    await loadData();
    closeModal('edit-balance-modal');
    renderApp();
  }
}

async function submitAddAccount() {
  const name = document.getElementById('acc-name').value.trim();
  if (!name) {
    alert('Vui lòng nhập tên tài khoản / ví!');
    return;
  }
  const type = document.getElementById('acc-type').value;
  const balStr = document.getElementById('acc-balance').value;
  const initialBalance = parseMoneyInput(balStr);
  const isReserve = document.getElementById('acc-is-reserve').checked;

  const colorMap = {
    BANK: '#3B82F6',
    CASH: '#10B981',
    E_WALLET: '#EC4899',
    RESERVE: '#8B5CF6'
  };

  const iconMap = {
    BANK: 'landmark',
    CASH: 'banknote',
    E_WALLET: 'wallet',
    RESERVE: 'shield-check'
  };

  const newAcc = {
    id: Date.now(),
    name,
    type,
    initialBalance,
    currentBalance: initialBalance,
    isReserve: isReserve || type === 'RESERVE',
    colorHex: colorMap[type] || '#10B981',
    iconName: iconMap[type] || 'wallet'
  };

  await putItem('accounts', newAcc);
  await loadData();
  closeModal('add-account-modal');
  renderApp();
}

async function deleteAccount(accId) {
  if (confirm('Bạn có chắc muốn xóa ví này? Các giao dịch liên quan sẽ được giữ lại.')) {
    await deleteItem('accounts', accId);
    await loadData();
    renderApp();
  }
}

// ----------------- STATS SCREEN -----------------
function setStatsBarMode(mode) {
  state.statsBarMode = mode;
  renderApp();
}

function getWeeklyBarData() {
  const now = new Date();
  const currentMonth = now.getMonth();
  const currentYear = now.getFullYear();

  const monthTxs = state.transactions.filter((tx) => {
    const d = new Date(tx.timestamp);
    return tx.type === 'EXPENSE' && d.getMonth() === currentMonth && d.getFullYear() === currentYear;
  });

  const weekRanges = [
    { label: 'Tuần 1', sub: '01-07', start: 1, end: 7 },
    { label: 'Tuần 2', sub: '08-14', start: 8, end: 14 },
    { label: 'Tuần 3', sub: '15-21', start: 15, end: 21 },
    { label: 'Tuần 4', sub: '22-28', start: 22, end: 28 },
    { label: 'Tuần 5', sub: '29-31', start: 29, end: 31 }
  ];

  const labels = weekRanges.map((w) => w.label);
  const data = weekRanges.map((w) => {
    return monthTxs.filter((tx) => {
      const day = new Date(tx.timestamp).getDate();
      return day >= w.start && day <= w.end;
    }).reduce((s, t) => s + t.amount, 0);
  });

  return { labels, data };
}

function getMonthlyBarData() {
  const now = new Date();
  const labels = [];
  const data = [];

  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const m = d.getMonth();
    const y = d.getFullYear();
    labels.push(`Th ${m + 1}`);

    const total = state.transactions.filter((tx) => {
      const txDate = new Date(tx.timestamp);
      return tx.type === 'EXPENSE' && txDate.getMonth() === m && txDate.getFullYear() === y;
    }).reduce((s, t) => s + t.amount, 0);

    data.push(total);
  }

  return { labels, data };
}

function renderStatsScreen() {
  const isWeekly = state.statsBarMode === 'WEEKLY';
  const barData = isWeekly ? getWeeklyBarData() : getMonthlyBarData();
  const totalBarExpense = barData.data.reduce((s, v) => s + v, 0);
  const avgBarExpense = barData.data.length > 0 ? totalBarExpense / barData.data.length : 0;

  return `
    <div class="space-y-4 pb-24">
      <!-- Biểu đồ cột: Mức độ chi tiêu giữa các tuần/tháng -->
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-sm font-bold text-slate-800">Mức độ chi tiêu</h2>
            <p class="text-[11px] text-slate-400">${isWeekly ? 'So sánh các tuần trong tháng' : 'So sánh 6 tháng gần nhất'}</p>
          </div>
          <div class="flex space-x-1 rounded-xl bg-slate-100 p-1 text-xs font-semibold">
            <button onclick="setStatsBarMode('WEEKLY')" class="rounded-lg px-2.5 py-1 transition ${isWeekly ? 'bg-white text-emerald-700 font-bold shadow-sm' : 'text-slate-500'}">Tuần</button>
            <button onclick="setStatsBarMode('MONTHLY')" class="rounded-lg px-2.5 py-1 transition ${!isWeekly ? 'bg-white text-emerald-700 font-bold shadow-sm' : 'text-slate-500'}">Tháng</button>
          </div>
        </div>

        <!-- Info summary pill -->
        <div class="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2 text-xs">
          <span class="text-slate-600 font-medium">Trung bình: <b class="text-slate-800">${formatMoney(avgBarExpense)}</b> / ${isWeekly ? 'tuần' : 'tháng'}</span>
          <span class="font-bold text-emerald-600">Tổng: ${formatMoney(totalBarExpense)}</span>
        </div>

        <div class="relative h-44 w-full pt-2">
          <canvas id="comparisonBarChart"></canvas>
        </div>
      </div>

      <!-- Biểu đồ tròn: Cơ cấu chi tiêu theo danh mục -->
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
        <h2 class="text-sm font-bold text-slate-800">Biểu đồ cơ cấu chi tiêu</h2>
        <div class="relative mx-auto mt-4 h-64 w-64">
          <canvas id="categoryChart"></canvas>
        </div>
      </div>

      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
        <h2 class="text-sm font-bold text-slate-800">Chi tiết theo danh mục</h2>
        <div class="mt-3 space-y-3">
          ${renderCategoryBreakdown()}
        </div>
      </div>
    </div>
  `;
}

function renderCategoryBreakdown() {
  const expenseTxs = state.transactions.filter((t) => t.type === 'EXPENSE');
  const total = expenseTxs.reduce((s, t) => s + t.amount, 0);
  if (total === 0) return '<p class="text-xs text-slate-400">Chưa có chi tiêu để thống kê.</p>';

  const catMap = {};
  expenseTxs.forEach((tx) => {
    catMap[tx.categoryId] = (catMap[tx.categoryId] || 0) + tx.amount;
  });

  return Object.entries(catMap)
    .sort((a, b) => b[1] - a[1])
    .map(([catId, amount]) => {
      const cat = state.categories.find((c) => c.id === catId) || { name: 'Khác', colorHex: '#64748B' };
      const pct = Math.round((amount / total) * 100);
      return `
        <div>
          <div class="flex justify-between text-xs font-semibold">
            <span class="text-slate-700">${cat.name} (${pct}%)</span>
            <span class="text-slate-900">${formatMoney(amount)}</span>
          </div>
          <div class="mt-1 h-2 w-full overflow-hidden rounded-full bg-slate-100">
            <div class="h-full rounded-full" style="width: ${pct}%; background-color: ${cat.colorHex}"></div>
          </div>
        </div>
      `;
    }).join('');
}

function renderChart() {
  // 1. Doughnut Chart
  const ctx = document.getElementById('categoryChart');
  if (ctx) {
    const expenseTxs = state.transactions.filter((t) => t.type === 'EXPENSE');
    const catMap = {};
    expenseTxs.forEach((tx) => {
      catMap[tx.categoryId] = (catMap[tx.categoryId] || 0) + tx.amount;
    });

    const labels = [];
    const data = [];
    const colors = [];

    Object.entries(catMap).forEach(([catId, amount]) => {
      const cat = state.categories.find((c) => c.id === catId) || { name: 'Khác', colorHex: '#64748B' };
      labels.push(cat.name);
      data.push(amount);
      colors.push(cat.colorHex);
    });

    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data: data.length > 0 ? data : [1],
          backgroundColor: colors.length > 0 ? colors : ['#CBD5E1'],
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        cutout: '72%'
      }
    });
  }

  // 2. Bar Chart: Weekly/Monthly Comparison
  const barCtx = document.getElementById('comparisonBarChart');
  if (barCtx) {
    if (barChartInstance) barChartInstance.destroy();
    const isWeekly = state.statsBarMode === 'WEEKLY';
    const barData = isWeekly ? getWeeklyBarData() : getMonthlyBarData();

    barChartInstance = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: barData.labels,
        datasets: [{
          data: barData.data,
          backgroundColor: '#10B981',
          hoverBackgroundColor: '#047857',
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ' ' + formatMoney(ctx.raw)
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (val) => {
                if (val >= 1000000) return (val / 1000000) + 'tr';
                if (val >= 1000) return (val / 1000) + 'k';
                return val;
              },
              font: { size: 10 }
            },
            grid: { color: '#F1F5F9' }
          },
          x: {
            grid: { display: false },
            ticks: { font: { size: 11, weight: 'bold' } }
          }
        }
      }
    });
  }
}

// ----------------- AI ASSISTANT SCREEN -----------------
let chatMessages = [
  { sender: 'ai', text: 'Xin chào! Tôi là Trợ lý AI Tài chính. Bạn có thể gõ nội dung thu chi, bấm micro để nói tiếng Việt, hoặc tải/chụp ảnh hóa đơn, bill chuyển khoản để tôi tự động ghi nhận nhé!' }
];
let isAiLoading = false;
let aiPendingImageBase64 = null;

function renderAiScreen() {
  const currentModel = state.settings.selectedModel || 'gemini-3.1-flash-lite';
  const hasKey = Boolean(state.settings.apiKey && state.settings.apiKey.trim());

  return `
    <div class="flex flex-col h-[calc(100vh-140px)]">
      
      <!-- Top AI Status & Configuration Header -->
      <div class="mb-2 rounded-2xl border border-slate-100 bg-white p-3 shadow-sm">
        ${!hasKey ? `
          <div class="rounded-xl bg-amber-50 p-3 border border-amber-200 space-y-2">
            <div class="flex items-center space-x-2 text-amber-800">
              <i data-lucide="key" class="h-4 w-4"></i>
              <span class="text-xs font-bold">Chưa cài đặt Gemini API Key</span>
            </div>
            <p class="text-[11px] text-amber-700 leading-tight">Cần có Gemini API Key để trò chuyện và quét hóa đơn AI. Hoàn toàn miễn phí từ Google!</p>
            <div class="flex space-x-1.5">
              <input id="quick-api-key-input" type="password" placeholder="Dán API Key (AIzaSy...)" class="flex-1 rounded-xl border border-amber-300 bg-white px-3 py-1.5 text-xs outline-none focus:border-amber-600" />
              <button onclick="saveQuickApiKey()" class="rounded-xl bg-amber-600 px-3 py-1.5 text-xs font-bold text-white shadow hover:bg-amber-700 transition active:scale-95">Lưu Key</button>
            </div>
            <p class="text-[10px] text-amber-600">Lấy key miễn phí tại: <a href="https://aistudio.google.com" target="_blank" class="underline font-bold">aistudio.google.com</a></p>
          </div>
        ` : `
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-purple-100 text-purple-700">
                <i data-lucide="bot" class="h-4 w-4"></i>
              </div>
              <div>
                <div class="flex items-center space-x-1.5">
                  <span class="text-xs font-extrabold text-slate-800">Model:</span>
                  <select id="chat-model-select" onchange="switchChatModel(this.value)" class="rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs font-bold text-purple-700 outline-none">
                    ${GeminiManager.AVAILABLE_MODELS.map((m) => `
                      <option value="${m.id}" ${m.id === currentModel ? 'selected' : ''}>${m.name.split(' (')[0]}</option>
                    `).join('')}
                  </select>
                </div>
                <div class="flex items-center space-x-1">
                  <span class="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                  <span class="text-[10px] font-semibold text-emerald-600">AI Ready</span>
                </div>
              </div>
            </div>
            <button onclick="promptEditApiKey()" title="Cài đặt lại API Key" class="flex items-center space-x-1 rounded-xl bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-600 hover:bg-slate-200 transition">
              <i data-lucide="key" class="h-3 w-3"></i>
              <span>Đổi Key</span>
            </button>
          </div>
        `}
      </div>

      <!-- Chat history -->
      <div id="chat-messages" class="flex-1 space-y-3 overflow-y-auto p-2">
        ${chatMessages.map((msg, idx) => `
          <div class="flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}">
            <div class="max-w-[85%] rounded-3xl px-4 py-2.5 text-xs leading-relaxed ${
              msg.sender === 'user' ? 'bg-emerald-600 text-white rounded-br-sm' : 'bg-white border border-slate-100 text-slate-800 shadow-sm rounded-bl-sm'
            }">
              ${msg.image ? `<img src="${msg.image}" class="mb-2 max-h-48 rounded-xl object-cover border border-slate-200/50" />` : ''}
              <p class="whitespace-pre-wrap">${msg.text}</p>
            </div>
          </div>
        `).join('')}
        ${isAiLoading ? `
          <div class="flex justify-start">
            <div class="flex items-center space-x-1.5 rounded-full bg-slate-100 px-4 py-2 text-xs text-slate-500">
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500"></span>
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500" style="animation-delay: 0.2s"></span>
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500" style="animation-delay: 0.4s"></span>
              <span class="ml-1">AI đang xử lý...</span>
            </div>
          </div>
        ` : ''}
      </div>

      <!-- Chat input area with Image Preview -->
      <div class="mt-2 rounded-3xl border border-slate-100 bg-white p-2 shadow-sm">
        ${aiPendingImageBase64 ? `
          <div class="mb-2 px-2 pt-1 flex items-center justify-between border-b border-slate-100 pb-2">
            <div class="flex items-center space-x-2">
              <img src="${aiPendingImageBase64}" class="h-12 w-12 rounded-xl object-cover border border-purple-200 shadow-sm" />
              <div>
                <p class="text-xs font-bold text-purple-900">Đã đính kèm ảnh hóa đơn</p>
                <p class="text-[10px] text-slate-400">Gõ yêu cầu hoặc bấm gửi để AI phân tích</p>
              </div>
            </div>
            <button type="button" onclick="clearAiPendingImage()" class="h-6 w-6 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 hover:bg-rose-50 hover:text-rose-600 transition">
              <i data-lucide="x" class="h-3.5 w-3.5"></i>
            </button>
          </div>
        ` : ''}

        <form onsubmit="handleSendAiMessage(event)" class="flex items-center space-x-1.5">
          <!-- Button 1: Gallery / Files -->
          <label title="Tải ảnh từ thư viện" class="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-purple-50 text-purple-700 hover:bg-purple-100 transition active:scale-95">
            <i data-lucide="image" class="h-4 w-4"></i>
            <input type="file" accept="image/*" class="hidden" onchange="handleAiImageSelect(event)" />
          </label>

          <!-- Button 2: Camera Capture -->
          <label title="Chụp ảnh mới bằng Camera" class="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-purple-50 text-purple-700 hover:bg-purple-100 transition active:scale-95">
            <i data-lucide="camera" class="h-4 w-4"></i>
            <input type="file" accept="image/*" capture="environment" class="hidden" onchange="handleAiImageSelect(event)" />
          </label>

          <input id="ai-input-text" type="text" placeholder="${aiPendingImageBase64 ? 'Hỏi về hóa đơn này hoặc bấm Gửi...' : 'Nhắn hoặc bấm micro để nói...'}" class="flex-1 bg-transparent px-2 text-xs outline-none text-slate-800 placeholder-slate-400" />

          <button type="button" onclick="startVoiceInput()" id="mic-btn" title="Nói tiếng Việt" class="flex h-9 w-9 items-center justify-center rounded-full bg-blue-50 text-blue-600 transition active:scale-95">
            <i data-lucide="mic" class="h-4 w-4"></i>
          </button>

          <button type="submit" title="Gửi" class="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-600 text-white shadow-sm transition active:scale-95 hover:bg-emerald-700">
            <i data-lucide="send" class="h-4 w-4"></i>
          </button>
        </form>
      </div>

    </div>
  `;
}

async function saveQuickApiKey() {
  const input = document.getElementById('quick-api-key-input');
  if (!input) return;
  const val = input.value.trim();
  if (!val) {
    alert('Vui lòng nhập API Key hợp lệ!');
    return;
  }
  await GeminiManager.setApiKey(val);
  state.settings.apiKey = val;
  renderApp();
  alert('Đã lưu Gemini API Key thành công! Giờ bạn có thể trò chuyện và quét hóa đơn AI.');
}

async function promptEditApiKey() {
  const currentKey = state.settings.apiKey || '';
  const newKey = prompt('Nhập Gemini API Key mới của bạn:', currentKey);
  if (newKey !== null) {
    const val = newKey.trim();
    await GeminiManager.setApiKey(val);
    state.settings.apiKey = val;
    renderApp();
    alert('Đã cập nhật Gemini API Key!');
  }
}

async function switchChatModel(model) {
  await GeminiManager.setSelectedModel(model);
  state.settings.selectedModel = model;
  renderApp();
}

function clearAiPendingImage() {
  aiPendingImageBase64 = null;
  renderApp();
}

function handleAiImageSelect(e) {
  const file = e.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
    aiPendingImageBase64 = event.target.result;
    renderApp();
    setTimeout(() => {
      const input = document.getElementById('ai-input-text');
      if (input) input.focus();
    }, 100);
  };
  reader.readAsDataURL(file);
}

function scrollChatToBottom() {
  setTimeout(() => {
    const el = document.getElementById('chat-messages');
    if (el) el.scrollTop = el.scrollHeight;
  }, 50);
}

async function handleSendAiMessage(e) {
  e.preventDefault();
  const input = document.getElementById('ai-input-text');
  const text = input ? input.value.trim() : '';
  const attachedImage = aiPendingImageBase64;

  if (!text && !attachedImage) return;
  if (isAiLoading) return;

  // Append user message
  chatMessages.push({
    sender: 'user',
    text: text || 'Phân tích hóa đơn này và đọc các món, tổng tiền giúp tôi:',
    image: attachedImage
  });

  if (input) input.value = '';
  aiPendingImageBase64 = null;
  isAiLoading = true;
  renderApp();
  scrollChatToBottom();

  try {
    const promptText = text || 'Phân tích chi tiết hóa đơn/bill này và đọc các món, tổng tiền giúp tôi:';
    const result = await GeminiManager.chatWithAi(promptText, attachedImage);
    chatMessages.push({ sender: 'ai', text: result.text });

    if (result.actionData && result.actionData.action === 'add_transaction') {
      const act = result.actionData;
      const cat = state.categories.find((c) => c.name.toLowerCase() === (act.category || '').toLowerCase()) || state.categories[0];
      const acc = state.accounts[0];

      await addTransaction({
        amount: act.amount,
        type: act.type || 'EXPENSE',
        categoryId: cat ? cat.id : 'food',
        accountId: acc ? acc.id : 1,
        note: act.note || 'AI tự động ghi',
        timestamp: Date.now()
      });

      chatMessages.push({ sender: 'ai', text: ` Đã tự động tạo giao dịch: ${cat ? cat.name : 'Chi tiêu'} (${formatMoney(act.amount)}) vào ví ${acc ? acc.name : 'Mặc định'}!` });
    }
  } catch (err) {
    chatMessages.push({ sender: 'ai', text: `❌ ${err.message}` });
  } finally {
    isAiLoading = false;
    renderApp();
    scrollChatToBottom();
  }
}

// Web Speech Recognition (Google Speech on Chrome/Safari)
let recognition = null;
function initSpeechRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) return;

  recognition = new SpeechRecognition();
  recognition.lang = 'vi-VN';
  recognition.continuous = false;
  recognition.interimResults = false;

  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    const input = document.getElementById('ai-input-text');
    if (input) {
      input.value = transcript;
    }
    const micBtn = document.getElementById('mic-btn');
    if (micBtn) micBtn.classList.remove('bg-rose-500', 'text-white', 'animate-pulse');
  };

  recognition.onerror = () => {
    const micBtn = document.getElementById('mic-btn');
    if (micBtn) micBtn.classList.remove('bg-rose-500', 'text-white', 'animate-pulse');
  };

  recognition.onend = () => {
    const micBtn = document.getElementById('mic-btn');
    if (micBtn) micBtn.classList.remove('bg-rose-500', 'text-white', 'animate-pulse');
  };
}

function startVoiceInput() {
  if (!recognition) {
    alert('Trình duyệt của bạn chưa hỗ trợ nhận diện giọng nói Web Speech. Vui lòng sử dụng Safari trên iOS hoặc Chrome.');
    return;
  }
  const micBtn = document.getElementById('mic-btn');
  if (micBtn) micBtn.classList.add('bg-rose-500', 'text-white', 'animate-pulse');
  recognition.start();
}

// ----------------- SPLIT BILL SCREEN -----------------
function renderSplitScreen() {
  const { eventTitle, members, items, result } = state.splitBill;

  return `
    <div class="space-y-4 pb-24">
      <!-- Result Card if calculated -->
      ${result ? `
        <div class="rounded-3xl bg-gradient-to-br from-indigo-900 to-purple-900 p-5 text-white shadow-lg space-y-4">
          <div class="flex items-center justify-between">
            <span class="text-xs font-semibold uppercase text-purple-200">Kết quả chia tiền</span>
            <button onclick="SplitBillCalculator.exportToCSV(state.splitBill.result)" class="flex items-center space-x-1 rounded-full bg-white/20 px-3 py-1 text-xs font-bold hover:bg-white/30">
              <i data-lucide="download" class="h-3 w-3"></i>
              <span>Xuất CSV</span>
            </button>
          </div>
          <div>
            <h3 class="text-2xl font-black">${formatMoney(result.totalAmount)}</h3>
            <p class="text-xs text-purple-200">Trung bình: <span class="font-bold text-white">${formatMoney(result.perPersonAvg)}</span> / người</p>
          </div>

          <div class="space-y-2 pt-2 border-t border-white/10">
            <h4 class="text-xs font-bold text-purple-200">Ai cần chuyển cho ai:</h4>
            ${result.settlements.length === 0 ? '<p class="text-xs text-emerald-300">Tất cả mọi người đã hòa tiền!</p>' : `
              <div class="space-y-1.5">
                ${result.settlements.map((st) => `
                  <div class="flex items-center justify-between rounded-xl bg-white/10 px-3 py-2 text-xs">
                    <span><b>${st.fromMemberName}</b> chuyển <b>${st.toMemberName}</b></span>
                    <span class="font-black text-amber-300">${formatMoney(st.amount)}</span>
                  </div>
                `).join('')}
              </div>
            `}
          </div>
        </div>
      ` : ''}

      <!-- Members Management -->
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-bold text-slate-800">1. Thành viên tham gia (${members.length})</h3>
          <button onclick="openModal('add-member-modal')" class="text-xs font-bold text-emerald-600">+ Thêm bạn</button>
        </div>
        <div class="flex flex-wrap gap-2">
          ${members.map((m) => `
            <span class="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
              ${m.isCurrentUser ? '👑 ' : ''}${m.name}
              ${!m.isCurrentUser ? `<button onclick="removeMember('${m.id}')" class="ml-1.5 text-slate-400 hover:text-rose-500">×</button>` : ''}
            </span>
          `).join('')}
        </div>
      </div>

      <!-- Expense Items -->
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-bold text-slate-800">2. Các món & khoản chi (${items.length})</h3>
          <div class="flex space-x-2">
            <button onclick="openOcrScanModal('split')" class="rounded-full bg-purple-50 px-2.5 py-1 text-xs font-bold text-purple-700">⚡ Quét Bill AI</button>
            <button onclick="openAddSplitItemModal()" class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">+ Thêm món</button>
          </div>
        </div>

        ${items.length === 0 ? `
          <p class="text-center py-4 text-xs text-slate-400">Chưa có món nào. Bấm "+ Thêm món" hoặc "Quét Bill AI" để thêm.</p>
        ` : `
          <div class="space-y-2">
            ${items.map((it, idx) => {
              const payer = members.find((m) => m.id === it.paidByMemberId) || { name: 'Ai đó' };
              return `
                <div class="flex items-center justify-between rounded-xl bg-slate-50 p-2.5 text-xs">
                  <div>
                    <p class="font-bold text-slate-800">${it.title}</p>
                    <p class="text-[11px] text-slate-400">${payer.name} đã trả</p>
                  </div>
                  <div class="flex items-center space-x-2">
                    <span class="font-bold text-slate-900">${formatMoney(it.amount)}</span>
                    <button onclick="removeSplitItem(${idx})" class="text-slate-400 hover:text-rose-500">×</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>

      <!-- Action Button -->
      <button onclick="calculateSplitBill()" class="w-full rounded-2xl bg-emerald-600 py-3.5 text-sm font-bold text-white shadow-md transition active:scale-95">
        ⚡ Tính toán và chia tiền ngay
      </button>
    </div>
  `;
}

function submitAddMember() {
  const input = document.getElementById('new-member-name');
  const name = input ? input.value.trim() : '';
  if (name) {
    state.splitBill.members.push({
      id: 'mem_' + Date.now(),
      name,
      isCurrentUser: false
    });
    input.value = '';
    closeModal('add-member-modal');
    renderApp();
  }
}

function removeMember(id) {
  state.splitBill.members = state.splitBill.members.filter((m) => m.id !== id);
  renderApp();
}

function openAddSplitItemModal() {
  const sel = document.getElementById('split-item-payer');
  if (sel) {
    sel.innerHTML = state.splitBill.members.map((m) => `
      <option value="${m.id}">${m.name} ${m.isCurrentUser ? '(Tôi)' : ''}</option>
    `).join('');
  }
  document.getElementById('split-item-title').value = '';
  document.getElementById('split-item-amount').value = '';
  document.getElementById('split-item-amount-preview').innerText = '';
  openModal('add-split-item-modal');
}

function submitAddSplitItem() {
  const title = document.getElementById('split-item-title').value.trim();
  const amtStr = document.getElementById('split-item-amount').value;
  const amount = parseMoneyInput(amtStr);
  const payerId = document.getElementById('split-item-payer').value;

  if (!title) {
    alert('Vui lòng nhập tên món/khoản chi!');
    return;
  }
  if (amount <= 0) {
    alert('Vui lòng nhập số tiền hợp lệ!');
    return;
  }

  state.splitBill.items.push({
    id: 'item_' + Date.now(),
    title,
    amount,
    paidByMemberId: payerId,
    splitAmongMemberIds: []
  });

  closeModal('add-split-item-modal');
  renderApp();
}

function removeSplitItem(index) {
  state.splitBill.items.splice(index, 1);
  renderApp();
}

function calculateSplitBill() {
  if (state.splitBill.items.length === 0) {
    alert('Vui lòng thêm ít nhất 1 khoản chi trước khi tính toán!');
    return;
  }
  state.splitBill.result = SplitBillCalculator.calculate(
    state.splitBill.eventTitle,
    state.splitBill.members,
    state.splitBill.items
  );
  renderApp();
}

// ----------------- TRIPS SCREEN -----------------
function renderTripsScreen() {
  return `
    <div class="space-y-4 pb-24">
      <div class="flex items-center justify-between">
        <h2 class="text-sm font-bold text-slate-800">Danh sách chuyến đi (${state.trips.length})</h2>
        <button onclick="openModal('create-trip-modal')" class="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white shadow">
          + Tạo chuyến đi
        </button>
      </div>

      ${state.trips.length === 0 ? `
        <div class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center">
          <i data-lucide="luggage" class="mx-auto h-8 w-8 text-slate-300"></i>
          <p class="mt-2 text-xs font-semibold text-slate-500">Chưa có chuyến đi nào</p>
          <button onclick="openModal('create-trip-modal')" class="mt-3 rounded-full bg-emerald-600 px-4 py-1.5 text-xs font-bold text-white">Tạo ngay</button>
        </div>
      ` : `
        <div class="space-y-3">
          ${state.trips.map((trip) => {
            const expenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
            const totalCost = expenses.reduce((s, e) => s + e.amount, 0);

            return `
              <div onclick="openTripDetail('${trip.id}')" class="cursor-pointer rounded-3xl border border-slate-100 bg-white p-4 shadow-sm transition hover:shadow">
                <div class="flex items-start justify-between">
                  <div>
                    <h3 class="text-sm font-bold text-slate-800">${trip.title}</h3>
                    <p class="text-xs text-slate-400">${trip.destination || 'Chưa đặt điểm đến'} • ${trip.totalDays} ngày</p>
                    <p class="mt-1 text-xs font-extrabold text-emerald-600">Tổng chi: ${formatMoney(totalCost)}</p>
                  </div>
                  <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">Xem chi tiết →</span>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      `}
    </div>
  `;
}

let selectedTripExpenseSharers = [];

function getTripMembers(trip) {
  if (trip && trip.members && Array.isArray(trip.members) && trip.members.length > 0) {
    return trip.members;
  }
  const defaultMembers = [
    { id: 'mem_me', name: 'Tôi', isCurrentUser: true },
    { id: 'mem_tuan', name: 'Tuấn', isCurrentUser: false },
    { id: 'mem_linh', name: 'Linh', isCurrentUser: false }
  ];
  if (trip) {
    trip.members = defaultMembers;
    putItem('trips', trip);
  }
  return defaultMembers;
}

function calculateTripSettlement(trip) {
  const members = getTripMembers(trip);
  const expenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
  const items = expenses.map((e) => ({
    id: e.id,
    title: e.title,
    amount: e.amount,
    paidByMemberId: e.payerMemberId || members[0].id,
    splitAmongMemberIds: (e.sharerMemberIds && e.sharerMemberIds.length > 0) ? e.sharerMemberIds : members.map((m) => m.id)
  }));
  return SplitBillCalculator.calculate(trip.title, members, items);
}

function openTripDetail(tripId) {
  state.activeTrip = state.trips.find((t) => t.id === tripId);
  state.tripActiveDay = 'ALL';
  const exps = state.tripExpenses.filter((e) => e.tripId === tripId);
  state.tripActiveTab = exps.length > 0 ? 'settlement' : 'expenses';
  renderApp();
}

function setTripTab(tab) {
  state.tripActiveTab = tab;
  renderApp();
}

function renderTripDetailScreen() {
  const trip = state.activeTrip;
  if (!trip) return renderTripsScreen();

  const members = getTripMembers(trip);
  const allExpenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
  const totalCost = allExpenses.reduce((s, e) => s + e.amount, 0);
  const perPersonAvg = members.length > 0 ? totalCost / members.length : 0;

  let filteredExpenses = allExpenses;
  if (state.tripActiveDay !== 'ALL') {
    filteredExpenses = filteredExpenses.filter((e) => e.dayIndex === Number(state.tripActiveDay));
  }

  const daysCount = Math.max(1, trip.totalDays || 1);
  const daysArray = Array.from({ length: daysCount }, (_, i) => i + 1);
  const isSettlement = state.tripActiveTab === 'settlement';
  const calc = calculateTripSettlement(trip);

  return `
    <div class="space-y-4 pb-24">
      <!-- Navigation Header -->
      <div class="flex items-center justify-between">
        <button onclick="state.activeTrip = null; renderApp();" class="flex items-center space-x-1 text-xs font-bold text-slate-500 hover:text-slate-800 transition">
          <i data-lucide="chevron-left" class="h-4 w-4"></i>
          <span>Danh sách chuyến đi</span>
        </button>
        <button onclick="deleteTrip('${trip.id}')" class="text-xs text-rose-500 font-semibold hover:underline">Xóa chuyến đi</button>
      </div>

      <!-- Main Trip Header Banner -->
      <div class="rounded-3xl bg-gradient-to-br from-teal-600 via-emerald-700 to-emerald-900 p-5 text-white shadow-xl space-y-3">
        <div class="flex items-center justify-between">
          <span class="rounded-full bg-white/20 px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-emerald-100">
            ${allExpenses.length > 0 ? '🟢 Đang tính toán quyết toán' : 'Chuyến đi mới'}
          </span>
          <span class="text-xs font-semibold text-emerald-100">${trip.destination || 'Việt Nam'} • ${trip.totalDays} ngày</span>
        </div>
        
        <div>
          <h2 class="text-xl font-black leading-tight">${trip.title}</h2>
        </div>

        <div class="grid grid-cols-2 gap-2 border-t border-white/15 pt-3">
          <div>
            <p class="text-[10px] text-emerald-200 uppercase font-bold">Tổng chi cả chuyến</p>
            <p class="text-lg font-black text-amber-300 leading-none mt-0.5">${formatMoney(totalCost)}</p>
          </div>
          <div class="text-right">
            <p class="text-[10px] text-emerald-200 uppercase font-bold">Bình quân / người</p>
            <p class="text-sm font-extrabold text-white leading-none mt-0.5">${formatMoney(perPersonAvg)}</p>
          </div>
        </div>
      </div>

      <!-- Trip Members Bar -->
      <div class="rounded-2xl border border-slate-100 bg-white p-3 shadow-sm space-y-2">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-1.5">
            <i data-lucide="users" class="h-4 w-4 text-emerald-600"></i>
            <span class="text-xs font-extrabold text-slate-800">Thành viên (${members.length})</span>
          </div>
          <button onclick="openAddTripMemberModal()" class="text-xs font-bold text-emerald-600 hover:text-emerald-700">+ Thêm bạn</button>
        </div>
        <div class="flex flex-wrap gap-1.5">
          ${members.map((m) => `
            <span class="inline-flex items-center space-x-1 rounded-full px-2.5 py-1 text-[11px] font-bold ${m.isCurrentUser ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}">
              <span>${m.name}</span>
              ${m.isCurrentUser ? '<span class="text-[9px] bg-emerald-600 text-white rounded-full px-1">Bạn</span>' : ''}
              ${!m.isCurrentUser && members.length > 1 ? `<button onclick="removeTripMember('${m.id}')" title="Xóa" class="text-slate-400 hover:text-rose-600 ml-0.5 font-bold">×</button>` : ''}
            </span>
          `).join('')}
        </div>
      </div>

      <!-- Mode Switcher: Chi phí vs Bảng quyết toán chia tiền -->
      <div class="grid grid-cols-2 gap-1 rounded-2xl bg-slate-200/70 p-1 text-xs font-bold">
        <button onclick="setTripTab('settlement')" class="rounded-xl py-2.5 transition active:scale-95 ${isSettlement ? 'bg-emerald-600 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">
          ⚡ Quyết toán & Chia tiền
        </button>
        <button onclick="setTripTab('expenses')" class="rounded-xl py-2.5 transition active:scale-95 ${!isSettlement ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900'}">
          📋 Chi phí theo ngày (${allExpenses.length})
        </button>
      </div>

      ${isSettlement ? `
        <!-- SETTLEMENT & CALCULATION VIEW -->
        <div class="space-y-4">
          
          <!-- Net Balance Card for 'Me' -->
          <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
            <h3 class="text-xs font-extrabold text-slate-400 uppercase tracking-wider">Tổng kết tài chính của Bạn (Tôi)</h3>
            <div class="grid grid-cols-2 gap-3">
              <div class="rounded-2xl bg-slate-50 p-3">
                <span class="text-[10px] font-bold text-slate-500 uppercase">Phần bạn cần chia</span>
                <p class="text-base font-black text-slate-900 mt-0.5">${formatMoney(calc.myActualShare)}</p>
              </div>
              <div class="rounded-2xl ${calc.myNetBalance > 1 ? 'bg-emerald-50 text-emerald-800' : (calc.myNetBalance < -1 ? 'bg-rose-50 text-rose-800' : 'bg-slate-50 text-slate-700')} p-3">
                <span class="text-[10px] font-bold uppercase ${calc.myNetBalance > 1 ? 'text-emerald-600' : (calc.myNetBalance < -1 ? 'text-rose-600' : 'text-slate-500')}">
                  ${calc.myNetBalance > 1 ? 'Được nhận lại' : (calc.myNetBalance < -1 ? 'Cần chuyển thêm' : 'Số dư')}
                </span>
                <p class="text-base font-black mt-0.5">
                  ${calc.myNetBalance > 1 ? '+' : ''}${formatMoney(calc.myNetBalance)}
                </p>
              </div>
            </div>
          </div>

          <!-- Member Breakdown List -->
          <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
            <div class="flex items-center justify-between">
              <h3 class="text-sm font-extrabold text-slate-800">Chi tiết chi tiêu từng người</h3>
              <span class="text-[10px] text-slate-400 font-semibold">${calc.memberSummaries.length} thành viên</span>
            </div>
            
            <div class="space-y-2">
              ${calc.memberSummaries.map((ms) => `
                <div class="rounded-2xl border border-slate-100 bg-slate-50/60 p-3 text-xs space-y-1.5">
                  <div class="flex items-center justify-between">
                    <span class="font-bold text-slate-800 flex items-center space-x-1">
                      <span>${ms.member.name}</span>
                      ${ms.member.isCurrentUser ? '<span class="text-[9px] bg-emerald-600 text-white rounded px-1 font-extrabold">Tôi</span>' : ''}
                    </span>
                    <span class="font-extrabold ${ms.netBalance > 1 ? 'text-emerald-600' : (ms.netBalance < -1 ? 'text-rose-600' : 'text-slate-500')}">
                      ${ms.netBalance > 1 ? `Nhận lại: +${formatMoney(ms.netBalance)}` : (ms.netBalance < -1 ? `Cần trả: -${formatMoney(Math.abs(ms.netBalance))}` : 'Đã đủ')}
                    </span>
                  </div>
                  <div class="flex justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-200/60">
                    <span>Đã chi trước: <b>${formatMoney(ms.totalPaid)}</b></span>
                    <span>Phần cần chia: <b>${formatMoney(ms.fairShare)}</b></span>
                  </div>
                </div>
              `).join('')}
            </div>
          </div>

          <!-- Optimal Settlement Transactions -->
          <div class="rounded-3xl border border-indigo-100 bg-indigo-50/30 p-5 shadow-sm space-y-3">
            <div class="flex items-center space-x-2">
              <div class="flex h-7 w-7 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-sm">
                <i data-lucide="arrow-right-left" class="h-3.5 w-3.5"></i>
              </div>
              <h3 class="text-sm font-extrabold text-slate-800">Kế hoạch chuyển khoản gọn nhất</h3>
            </div>
            <p class="text-[11px] text-slate-500 leading-tight">AI tự động tối ưu để giảm thiểu tối đa số lần chuyển tiền giữa các bạn:</p>

            ${calc.settlements.length === 0 ? `
              <div class="rounded-2xl bg-white p-4 text-center border border-indigo-100 space-y-1">
                <p class="text-xs font-bold text-emerald-600">🎉 Mọi người đã chia đều và không nợ nhau!</p>
                <p class="text-[10px] text-slate-400">Không cần thực hiện thêm giao dịch chuyển khoản nào.</p>
              </div>
            ` : `
              <div class="space-y-2">
                ${calc.settlements.map((s, idx) => `
                  <div class="flex items-center justify-between rounded-2xl bg-white p-3 shadow-sm border border-indigo-100/80">
                    <div class="flex items-center space-x-2">
                      <span class="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-black text-indigo-700">${idx + 1}</span>
                      <div class="text-xs">
                        <span class="font-extrabold ${s.fromIsMe ? 'text-rose-600 underline' : 'text-slate-800'}">${s.fromMemberName}</span>
                        <span class="text-slate-400 mx-1">chuyển cho</span>
                        <span class="font-extrabold ${s.toIsMe ? 'text-emerald-600 underline' : 'text-slate-800'}">${s.toMemberName}</span>
                      </div>
                    </div>
                    <span class="text-xs font-black text-indigo-700">${formatMoney(s.amount)}</span>
                  </div>
                `).join('')}
              </div>
            `}
          </div>

          <!-- Settlement Action Tools -->
          <div class="grid grid-cols-2 gap-2 pt-1">
            <button onclick="copyTripSettlementText('${trip.id}')" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-indigo-600 py-3 text-xs font-bold text-white shadow hover:bg-indigo-700 transition active:scale-95">
              <i data-lucide="copy" class="h-4 w-4"></i>
              <span>Sao chép kết quả</span>
            </button>
            <button onclick="saveTripMyShareToPersonal('${trip.id}')" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-600 py-3 text-xs font-bold text-white shadow hover:bg-emerald-700 transition active:scale-95">
              <i data-lucide="wallet" class="h-4 w-4"></i>
              <span>Ghi vào ví cá nhân</span>
            </button>
          </div>

          <div class="grid grid-cols-2 gap-2">
            <button onclick="exportTripCsv('${trip.id}')" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-slate-100 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-200 transition">
              <i data-lucide="download" class="h-4 w-4"></i>
              <span>Xuất file CSV</span>
            </button>
            <button onclick="syncTripToSplitBill('${trip.id}')" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-purple-50 py-2.5 text-xs font-bold text-purple-700 hover:bg-purple-100 transition">
              <i data-lucide="users" class="h-4 w-4"></i>
              <span>Đưa sang Chia tiền AI</span>
            </button>
          </div>

        </div>
      ` : `
        <!-- EXPENSES BY DAY VIEW -->
        <div class="space-y-4">
          <!-- Day Tabs -->
          <div class="flex items-center space-x-1.5 overflow-x-auto pb-1">
            <button onclick="setTripDayFilter('ALL')" class="rounded-full px-3 py-1.5 text-xs font-bold whitespace-nowrap transition ${state.tripActiveDay === 'ALL' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600'}">
              Tất cả (${allExpenses.length})
            </button>
            ${daysArray.map((d) => `
              <button onclick="setTripDayFilter(${d})" class="rounded-full px-3 py-1.5 text-xs font-bold whitespace-nowrap transition ${state.tripActiveDay === d ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-600'}">
                Ngày ${d}
              </button>
            `).join('')}
          </div>

          <!-- Quick Action Buttons for Trip -->
          <div class="grid grid-cols-2 gap-2">
            <button onclick="openAddTripExpenseModal()" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-emerald-50 py-2.5 text-xs font-bold text-emerald-700 transition active:scale-95 hover:bg-emerald-100">
              <i data-lucide="plus" class="h-4 w-4"></i>
              <span>Thêm chi phí</span>
            </button>
            <button onclick="openOcrScanModal('trip')" class="flex items-center justify-center space-x-1.5 rounded-2xl bg-purple-50 py-2.5 text-xs font-bold text-purple-700 transition active:scale-95 hover:bg-purple-100">
              <i data-lucide="scan-line" class="h-4 w-4"></i>
              <span>Quét Bill AI</span>
            </button>
          </div>

          <!-- Expense Items List -->
          <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
            <h3 class="text-sm font-bold text-slate-800">
              Khoản chi ${state.tripActiveDay === 'ALL' ? 'toàn bộ chuyến đi' : `Ngày ${state.tripActiveDay}`} (${filteredExpenses.length})
            </h3>

            ${filteredExpenses.length === 0 ? `
              <div class="py-8 text-center text-slate-400 space-y-2">
                <i data-lucide="receipt" class="h-8 w-8 mx-auto text-slate-300"></i>
                <p class="text-xs">Chưa có khoản chi nào cho ngày này.</p>
              </div>
            ` : `
              <div class="space-y-2">
                ${filteredExpenses.map((exp) => {
                  const payer = members.find((m) => m.id === exp.payerMemberId);
                  const payerName = payer ? payer.name : (members[0]?.name || 'Tôi');
                  const sharerCount = (exp.sharerMemberIds && exp.sharerMemberIds.length > 0) ? exp.sharerMemberIds.length : members.length;
                  return `
                    <div class="flex items-center justify-between rounded-2xl bg-slate-50 p-3 text-xs border border-slate-100">
                      <div>
                        <p class="font-bold text-slate-800">${exp.title}</p>
                        <p class="text-[10px] text-slate-500 font-medium">
                          Ngày ${exp.dayIndex || 1} • <span class="text-emerald-700 font-bold">${payerName} trả</span> • Chia ${sharerCount}/${members.length} người
                        </p>
                      </div>
                      <div class="flex items-center space-x-2">
                        <span class="font-extrabold text-slate-900">${formatMoney(exp.amount)}</span>
                        <button onclick="deleteTripExpense('${exp.id}')" title="Xóa" class="h-6 w-6 rounded-full flex items-center justify-center text-slate-300 hover:text-rose-500 transition">×</button>
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>
            `}
          </div>

          <!-- Bottom Switch to Settlement Banner -->
          <div class="rounded-3xl border border-indigo-100 bg-gradient-to-r from-indigo-50 to-purple-50 p-4 shadow-sm flex items-center justify-between">
            <div>
              <p class="text-xs font-extrabold text-indigo-950">Đã xong chuyến đi?</p>
              <p class="text-[10px] text-indigo-600">Bấm để AI tính toán chia nợ sòng phẳng</p>
            </div>
            <button onclick="setTripTab('settlement')" class="rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-bold text-white shadow hover:bg-indigo-700 transition active:scale-95">
              ⚡ Xem quyết toán
            </button>
          </div>
        </div>
      `}
    </div>
  `;
}

function setTripDayFilter(day) {
  state.tripActiveDay = day;
  renderApp();
}

async function submitCreateTrip() {
  const title = document.getElementById('trip-title').value.trim();
  const destination = document.getElementById('trip-destination').value.trim();
  const days = parseInt(document.getElementById('trip-days').value || '3', 10) || 3;
  const membersStr = document.getElementById('trip-members')?.value || 'Tôi, Tuấn, Linh';

  if (!title) {
    alert('Vui lòng nhập tên chuyến đi!');
    return;
  }

  const rawNames = membersStr.split(',').map((n) => n.trim()).filter(Boolean);
  const members = rawNames.length > 0 ? rawNames.map((name, idx) => ({
    id: idx === 0 ? 'mem_me' : 'mem_' + Date.now() + '_' + idx,
    name,
    isCurrentUser: idx === 0 || name.toLowerCase() === 'tôi'
  })) : [
    { id: 'mem_me', name: 'Tôi', isCurrentUser: true },
    { id: 'mem_tuan', name: 'Tuấn', isCurrentUser: false },
    { id: 'mem_linh', name: 'Linh', isCurrentUser: false }
  ];

  const newTrip = {
    id: 'trip_' + Date.now(),
    title,
    destination: destination || 'Việt Nam',
    totalDays: days,
    members,
    createdAt: Date.now()
  };

  await putItem('trips', newTrip);
  await loadData();
  closeModal('create-trip-modal');
  state.activeTrip = newTrip;
  state.tripActiveTab = 'expenses';
  renderApp();
}

function openAddTripMemberModal() {
  if (!state.activeTrip) return;
  const input = document.getElementById('new-trip-member-name');
  if (input) input.value = '';
  openModal('add-trip-member-modal');
}

async function submitAddTripMember() {
  if (!state.activeTrip) return;
  const name = document.getElementById('new-trip-member-name').value.trim();
  if (!name) {
    alert('Vui lòng nhập tên thành viên!');
    return;
  }
  const members = getTripMembers(state.activeTrip);
  if (members.some((m) => m.name.toLowerCase() === name.toLowerCase())) {
    alert('Thành viên này đã có trong chuyến đi!');
    return;
  }
  const newMember = {
    id: 'mem_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
    name,
    isCurrentUser: false
  };
  members.push(newMember);
  state.activeTrip.members = members;
  await putItem('trips', state.activeTrip);
  await loadData();
  closeModal('add-trip-member-modal');
  renderApp();
}

async function removeTripMember(memberId) {
  if (!state.activeTrip) return;
  const members = getTripMembers(state.activeTrip);
  const target = members.find((m) => m.id === memberId);
  if (!target || target.isCurrentUser) return;

  const exps = state.tripExpenses.filter((e) => e.tripId === state.activeTrip.id);
  const hasExpenses = exps.some((e) => e.payerMemberId === memberId || (e.sharerMemberIds && e.sharerMemberIds.includes(memberId)));
  if (hasExpenses) {
    alert(`Không thể xóa "${target.name}" vì đang có khoản chi liên quan đến người này.`);
    return;
  }

  if (confirm(`Bạn có chắc muốn xóa "${target.name}" khỏi chuyến đi?`)) {
    state.activeTrip.members = members.filter((m) => m.id !== memberId);
    await putItem('trips', state.activeTrip);
    await loadData();
    renderApp();
  }
}

function openAddTripExpenseModal() {
  if (!state.activeTrip) return;
  const trip = state.activeTrip;
  const members = getTripMembers(trip);

  const selDay = document.getElementById('trip-exp-day');
  if (selDay) {
    const daysCount = Math.max(1, trip.totalDays || 1);
    selDay.innerHTML = Array.from({ length: daysCount }, (_, i) => `
      <option value="${i + 1}" ${state.tripActiveDay == i + 1 ? 'selected' : ''}>Ngày ${i + 1}</option>
    `).join('');
  }

  const selPayer = document.getElementById('trip-exp-payer');
  if (selPayer) {
    selPayer.innerHTML = members.map((m) => `
      <option value="${m.id}">${m.name} ${m.isCurrentUser ? '(Tôi)' : ''}</option>
    `).join('');
  }

  selectedTripExpenseSharers = members.map((m) => m.id);
  populateTripExpenseSharers();

  document.getElementById('trip-exp-title').value = '';
  document.getElementById('trip-exp-amount').value = '';
  openModal('add-trip-expense-modal');
}

function populateTripExpenseSharers() {
  const container = document.getElementById('trip-exp-sharers');
  if (!container || !state.activeTrip) return;
  const members = getTripMembers(state.activeTrip);
  container.innerHTML = members.map((m) => {
    const isSelected = selectedTripExpenseSharers.includes(m.id);
    return `
      <button type="button" onclick="toggleTripSharer('${m.id}')" class="rounded-full px-3 py-1 text-xs font-semibold border transition active:scale-95 ${isSelected ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm' : 'bg-slate-50 text-slate-600 border-slate-200'}">
        ${isSelected ? '✓ ' : ''}${m.name}
      </button>
    `;
  }).join('');

  const btnToggle = document.getElementById('btn-toggle-trip-sharers');
  if (btnToggle) {
    btnToggle.innerText = selectedTripExpenseSharers.length === members.length ? 'Bỏ chọn tất cả' : 'Chọn tất cả';
  }
}

function toggleTripSharer(memberId) {
  if (selectedTripExpenseSharers.includes(memberId)) {
    if (selectedTripExpenseSharers.length > 1) {
      selectedTripExpenseSharers = selectedTripExpenseSharers.filter((id) => id !== memberId);
    } else {
      alert('Phải có ít nhất 1 người tham gia chia chi phí!');
      return;
    }
  } else {
    selectedTripExpenseSharers.push(memberId);
  }
  populateTripExpenseSharers();
}

function toggleSelectAllTripSharers() {
  if (!state.activeTrip) return;
  const members = getTripMembers(state.activeTrip);
  if (selectedTripExpenseSharers.length === members.length) {
    selectedTripExpenseSharers = [members[0].id];
  } else {
    selectedTripExpenseSharers = members.map((m) => m.id);
  }
  populateTripExpenseSharers();
}

async function submitAddTripExpense() {
  if (!state.activeTrip) return;
  const title = document.getElementById('trip-exp-title').value.trim();
  const amtStr = document.getElementById('trip-exp-amount').value;
  const amount = parseMoneyInput(amtStr);
  const dayIndex = parseInt(document.getElementById('trip-exp-day').value || '1', 10) || 1;
  const payerId = document.getElementById('trip-exp-payer')?.value || (getTripMembers(state.activeTrip)[0]?.id || 'mem_me');

  if (!title) {
    alert('Vui lòng nhập tên khoản chi!');
    return;
  }
  if (amount <= 0) {
    alert('Vui lòng nhập số tiền hợp lệ!');
    return;
  }

  const members = getTripMembers(state.activeTrip);
  const sharers = selectedTripExpenseSharers.length > 0 ? [...selectedTripExpenseSharers] : members.map((m) => m.id);

  const newExp = {
    id: 'tripexp_' + Date.now(),
    tripId: state.activeTrip.id,
    dayIndex,
    title,
    amount,
    payerMemberId: payerId,
    sharerMemberIds: sharers,
    timestamp: Date.now()
  };

  await putItem('tripExpenses', newExp);
  await loadData();
  closeModal('add-trip-expense-modal');
  renderApp();
}

async function deleteTripExpense(expId) {
  await deleteItem('tripExpenses', expId);
  await loadData();
  renderApp();
}

async function deleteTrip(tripId) {
  if (confirm('Bạn có chắc muốn xóa chuyến đi này cùng toàn bộ chi phí?')) {
    await deleteItem('trips', tripId);
    state.activeTrip = null;
    await loadData();
    renderApp();
  }
}

function syncTripToSplitBill(tripId) {
  const trip = state.trips.find((t) => t.id === tripId) || state.activeTrip;
  if (!trip) return;

  const expenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
  if (expenses.length === 0) {
    alert('Chuyến đi này chưa có khoản chi nào để chia tiền!');
    return;
  }

  const members = getTripMembers(trip);
  state.splitBill.eventTitle = trip.title;
  state.splitBill.members = JSON.parse(JSON.stringify(members));
  state.splitBill.items = expenses.map((e) => ({
    id: 'item_' + e.id,
    title: `[Ngày ${e.dayIndex || 1}] ${e.title}`,
    amount: e.amount,
    paidByMemberId: e.payerMemberId || members[0].id,
    splitAmongMemberIds: (e.sharerMemberIds && e.sharerMemberIds.length > 0) ? e.sharerMemberIds : members.map((m) => m.id)
  }));
  state.splitBill.result = SplitBillCalculator.calculate(trip.title, state.splitBill.members, state.splitBill.items);

  switchTab('split');
  alert(`⚡ Đã chuyển toàn bộ ${expenses.length} khoản chi sang bảng Chia tiền và tự động tính toán!`);
}

function copyTripSettlementText(tripId) {
  const trip = state.trips.find((t) => t.id === tripId) || state.activeTrip;
  if (!trip) return;

  const expenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
  const members = getTripMembers(trip);
  const calc = calculateTripSettlement(trip);

  let text = `🎉 BẢNG QUYẾT TOÁN CHUYẾN ĐI: ${trip.title.toUpperCase()}\n`;
  text += `📍 Điểm đến: ${trip.destination || 'Việt Nam'} (${trip.totalDays} ngày)\n`;
  text += `💰 Tổng chi phí: ${formatMoney(calc.totalAmount)}\n`;
  text += `👥 Trung bình mỗi người: ${formatMoney(calc.perPersonAvg)}\n\n`;

  text += `📊 CHI TIẾT TỪNG THÀNH VIÊN:\n`;
  calc.memberSummaries.forEach((ms) => {
    const status = ms.netBalance > 1
      ? `-> Nhận lại: +${formatMoney(ms.netBalance)}`
      : (ms.netBalance < -1 ? `-> Cần chuyển: -${formatMoney(Math.abs(ms.netBalance))}` : `-> Đã thanh toán đủ`);
    text += `• ${ms.member.name}${ms.member.isCurrentUser ? ' (Tôi)' : ''}: Đã chi ${formatMoney(ms.totalPaid)} | Phần chia ${formatMoney(ms.fairShare)} ${status}\n`;
  });

  text += `\n⚡ KẾ HOẠCH CHUYỂN KHOẢN TỐI ƯU:\n`;
  if (calc.settlements.length === 0) {
    text += `• Mọi người đã thanh toán cân bằng, không cần chuyển khoản thêm.\n`;
  } else {
    calc.settlements.forEach((s, idx) => {
      text += `${idx + 1}. ${s.fromMemberName} chuyển cho ${s.toMemberName}: ${formatMoney(s.amount)}\n`;
    });
  }

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      alert('📋 Đã sao chép bảng quyết toán chuyến đi vào clipboard! Bạn có thể dán vào nhóm Zalo / Messenger.');
    }).catch(() => {
      prompt('Sao chép nội dung bên dưới:', text);
    });
  } else {
    prompt('Sao chép nội dung bên dưới:', text);
  }
}

async function saveTripMyShareToPersonal(tripId) {
  const trip = state.trips.find((t) => t.id === tripId) || state.activeTrip;
  if (!trip) return;

  const calc = calculateTripSettlement(trip);
  if (calc.myActualShare <= 0) {
    alert('Không có chi phí cá nhân nào cần ghi nhận.');
    return;
  }

  if (confirm(`Ghi nhận khoản chi tiêu thực tế ${formatMoney(calc.myActualShare)} của bạn cho chuyến đi "${trip.title}" vào sổ chi tiêu?`)) {
    const cat = state.categories.find((c) => c.type === 'EXPENSE' && (c.id === 'travel' || c.name.includes('Du lịch') || c.name.includes('Di chuyển'))) || state.categories[0];
    const acc = state.accounts[0];

    await addTransaction({
      amount: calc.myActualShare,
      type: 'EXPENSE',
      categoryId: cat ? cat.id : 'travel',
      accountId: acc ? acc.id : 1,
      note: `Chi phí chuyến đi: ${trip.title}`,
      timestamp: Date.now()
    });

    alert(`✅ Đã ghi nhận ${formatMoney(calc.myActualShare)} vào sổ Chi tiêu cá nhân!`);
  }
}

function exportTripCsv(tripId) {
  const trip = state.trips.find((t) => t.id === tripId) || state.activeTrip;
  if (!trip) return;

  const expenses = state.tripExpenses.filter((e) => e.tripId === trip.id);
  const members = getTripMembers(trip);
  const calc = calculateTripSettlement(trip);

  const rows = [
    ['BANG QUYET TOAN CHUYEN DI', trip.title],
    ['Diem den', trip.destination || 'Viet Nam'],
    ['So ngay', trip.totalDays],
    ['Tong chi phi (VND)', calc.totalAmount],
    ['Binh quan moi nguoi (VND)', calc.perPersonAvg],
    [],
    ['--- CHI TIET KHOAN CHI ---'],
    ['Ngay', 'Ten khoan chi', 'So tien (VND)', 'Nguoi tra', 'So nguoi chia']
  ];

  expenses.forEach((e) => {
    const payer = members.find((m) => m.id === e.payerMemberId)?.name || 'Toi';
    const sharers = (e.sharerMemberIds && e.sharerMemberIds.length > 0) ? e.sharerMemberIds.length : members.length;
    rows.push([
      `Ngay ${e.dayIndex || 1}`,
      `"${e.title.replace(/"/g, '""')}"`,
      e.amount,
      `"${payer}"`,
      `"${sharers}/${members.length} nguoi"`
    ]);
  });

  rows.push([]);
  rows.push(['--- TONG KET THANH VIEN ---']);
  rows.push(['Thanh vien', 'Da chi truoc (VND)', 'Phan can chia (VND)', 'Chenh lech rong (VND)']);
  calc.memberSummaries.forEach((ms) => {
    rows.push([
      `"${ms.member.name}"`,
      ms.totalPaid,
      ms.fairShare,
      ms.netBalance
    ]);
  });

  rows.push([]);
  rows.push(['--- KE HOACH CHUYEN KHOAN TOI UU ---']);
  rows.push(['Nguoi chuyen', 'Nguoi nhan', 'So tien (VND)']);
  calc.settlements.forEach((s) => {
    rows.push([
      `"${s.fromMemberName}"`,
      `"${s.toMemberName}"`,
      s.amount
    ]);
  });

  const csvContent = '\uFEFF' + rows.map((r) => r.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `QuyetToan_${trip.title.replace(/\s+/g, '_')}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

// ----------------- INVESTMENTS SCREEN -----------------
function renderInvestmentsScreen() {
  const totalInvested = state.investments.reduce((s, i) => s + i.amountInvested, 0);
  const totalCurrent = state.investments.reduce((s, i) => s + i.currentValue, 0);
  const profit = totalCurrent - totalInvested;
  const roi = totalInvested > 0 ? ((profit / totalInvested) * 100).toFixed(1) : 0;

  return `
    <div class="space-y-4 pb-24">
      <div class="rounded-3xl bg-gradient-to-br from-blue-700 to-indigo-900 p-5 text-white shadow-lg space-y-3">
        <span class="text-xs font-semibold uppercase text-blue-200">Tổng giá trị danh mục đầu tư</span>
        <h2 class="text-3xl font-black">${formatMoney(totalCurrent)}</h2>
        <div class="flex justify-between border-t border-white/10 pt-2 text-xs">
          <span>Vốn: <b>${formatMoney(totalInvested)}</b></span>
          <span class="${profit >= 0 ? 'text-emerald-300' : 'text-rose-300'} font-bold">
            ${profit >= 0 ? '+' : ''}${formatMoney(profit)} (${roi}%)
          </span>
        </div>
      </div>

      <div class="flex items-center justify-between">
        <h3 class="text-sm font-bold text-slate-800">Danh mục tài sản (${state.investments.length})</h3>
        <button onclick="openModal('add-investment-modal')" class="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white shadow">
          + Thêm khoản
        </button>
      </div>

      ${state.investments.length === 0 ? `
        <div class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center text-xs text-slate-400">
          Chưa có khoản đầu tư nào (Vàng, Cổ phiếu, Quỹ, Tiết kiệm, Crypto).
        </div>
      ` : `
        <div class="space-y-2">
          ${state.investments.map((inv) => {
            const diff = inv.currentValue - inv.amountInvested;
            const itemRoi = inv.amountInvested > 0 ? ((diff / inv.amountInvested) * 100).toFixed(1) : 0;
            return `
              <div class="flex items-center justify-between rounded-2xl border border-slate-100 bg-white p-3.5 shadow-sm">
                <div>
                  <h4 class="text-sm font-bold text-slate-800">${inv.name}</h4>
                  <p class="text-[11px] text-slate-400">Vốn: ${formatMoney(inv.amountInvested)}</p>
                </div>
                <div class="text-right">
                  <p class="text-sm font-black text-slate-900">${formatMoney(inv.currentValue)}</p>
                  <p class="text-xs font-bold ${diff >= 0 ? 'text-emerald-600' : 'text-rose-600'}">
                    ${diff >= 0 ? '+' : ''}${itemRoi}%
                  </p>
                  <button onclick="deleteInvestment('${inv.id}')" class="mt-0.5 text-[10px] text-slate-300 hover:text-rose-500">Xóa</button>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      `}
    </div>
  `;
}

async function submitAddInvestment() {
  const name = document.getElementById('inv-name').value.trim();
  const investedStr = document.getElementById('inv-invested').value;
  const currentStr = document.getElementById('inv-current').value;

  const amountInvested = parseMoneyInput(investedStr);
  const currentValue = parseMoneyInput(currentStr);

  if (!name) {
    alert('Vui lòng nhập tên tài sản đầu tư!');
    return;
  }
  if (amountInvested <= 0) {
    alert('Vui lòng nhập số vốn đầu tư hợp lệ!');
    return;
  }

  const inv = {
    id: 'inv_' + Date.now(),
    name,
    amountInvested,
    currentValue: currentValue > 0 ? currentValue : amountInvested,
    createdAt: Date.now()
  };

  await putItem('investments', inv);
  await loadData();
  closeModal('add-investment-modal');
  renderApp();
}

async function deleteInvestment(invId) {
  if (confirm('Bạn có chắc muốn xóa khoản đầu tư này?')) {
    await deleteItem('investments', invId);
    await loadData();
    renderApp();
  }
}

// ----------------- SETTINGS SCREEN (SYNCHRONOUS HTML) -----------------
function renderSettingsScreen() {
  const currentApiKey = state.settings.apiKey || '';
  const currentModel = state.settings.selectedModel || 'gemini-3.1-flash-lite';

  return `
    <div class="space-y-4 pb-24">
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <div class="flex items-center space-x-2">
          <div class="flex h-8 w-8 items-center justify-center rounded-xl bg-purple-100 text-purple-700">
            <i data-lucide="sparkles" class="h-4 w-4"></i>
          </div>
          <h3 class="text-sm font-bold text-slate-800">Cấu hình Google Gemini AI</h3>
        </div>
        
        <div>
          <label class="text-xs font-semibold text-slate-600">Gemini API Key</label>
          <div class="mt-1 flex space-x-2">
            <input id="setting-api-key" type="password" value="${currentApiKey}" placeholder="Dán API Key (AIzaSy...)" class="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-xs outline-none focus:border-emerald-500" />
            <button onclick="saveApiKeySetting()" class="rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow hover:bg-emerald-700 transition active:scale-95">Lưu</button>
          </div>
          <div class="mt-2 flex items-center justify-between text-[11px]">
            <p class="text-slate-400">Lấy key miễn phí tại <a href="https://aistudio.google.com" target="_blank" class="text-emerald-600 underline font-semibold">aistudio.google.com</a></p>
            <button onclick="testGeminiApiKey()" id="btn-test-key" class="text-purple-600 font-bold hover:underline flex items-center space-x-1">
              <i data-lucide="zap" class="h-3 w-3"></i>
              <span>Kiểm tra kết nối</span>
            </button>
          </div>
          <div id="test-key-result" class="mt-1 hidden text-xs font-semibold"></div>
        </div>

        <div class="pt-2">
          <label class="text-xs font-semibold text-slate-600">Mô hình AI mặc định</label>
          <select id="setting-model" onchange="saveModelSetting(this.value)" class="mt-1 w-full rounded-xl border border-slate-200 p-2 text-xs font-semibold outline-none focus:border-purple-500">
            ${GeminiManager.AVAILABLE_MODELS.map((m) => `
              <option value="${m.id}" ${m.id === currentModel ? 'selected' : ''}>${m.name}</option>
            `).join('')}
          </select>
        </div>
      </div>

      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <h3 class="text-sm font-bold text-slate-800">Sao lưu & Quản lý dữ liệu</h3>
        <div class="grid grid-cols-2 gap-2">
          <button onclick="exportDataBackup()" class="rounded-xl bg-slate-100 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-200 transition">Sao lưu JSON</button>
          <button onclick="resetAllData()" class="rounded-xl bg-rose-50 py-2.5 text-xs font-bold text-rose-600 hover:bg-rose-100 transition">Xóa dữ liệu</button>
        </div>
      </div>
    </div>
  `;
}

async function testGeminiApiKey() {
  const resultEl = document.getElementById('test-key-result');
  const btnEl = document.getElementById('btn-test-key');
  const inputEl = document.getElementById('setting-api-key');
  const key = inputEl ? inputEl.value.trim() : state.settings.apiKey;

  if (!key) {
    alert('Vui lòng nhập API Key trước khi kiểm tra!');
    return;
  }

  if (resultEl) {
    resultEl.classList.remove('hidden', 'text-rose-600', 'text-emerald-600');
    resultEl.classList.add('text-slate-500');
    resultEl.innerText = 'Đang gửi yêu cầu kiểm tra đến Google Gemini...';
  }

  try {
    await GeminiManager.testConnection(key);
    if (resultEl) {
      resultEl.classList.remove('text-slate-500');
      resultEl.classList.add('text-emerald-600');
      resultEl.innerText = '✅ Kết nối Google Gemini API thành công! Key hoạt động tốt.';
    }
  } catch (err) {
    if (resultEl) {
      resultEl.classList.remove('text-slate-500');
      resultEl.classList.add('text-rose-600');
      resultEl.innerText = `❌ Lỗi kết nối: ${err.message}`;
    }
  }
}

async function saveApiKeySetting() {
  const input = document.getElementById('setting-api-key');
  if (input) {
    const val = input.value.trim();
    await GeminiManager.setApiKey(val);
    state.settings.apiKey = val;
    alert('Đã lưu Gemini API Key thành công!');
  }
}

async function saveModelSetting(model) {
  await GeminiManager.setSelectedModel(model);
  state.settings.selectedModel = model;
  alert('Đã chuyển mô hình AI sang: ' + model);
}

async function exportDataBackup() {
  const data = {
    transactions: state.transactions,
    accounts: state.accounts,
    categories: state.categories,
    investments: state.investments,
    trips: state.trips,
    tripExpenses: state.tripExpenses
  };
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `ExpenseManager_Backup_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
}

async function resetAllData() {
  if (confirm('CẢNH BÁO: Toàn bộ giao dịch và dữ liệu sẽ bị xóa hoàn toàn. Bạn có chắc không?')) {
    await clearStore('transactions');
    await clearStore('investments');
    await clearStore('trips');
    await clearStore('tripExpenses');
    await loadData();
    renderApp();
  }
}

// ----------------- TRANSACTION MODAL & ACTIONS -----------------
async function addTransaction(txData) {
  const tx = {
    id: 'tx_' + Date.now(),
    amount: Number(txData.amount) || 0,
    type: txData.type || 'EXPENSE',
    categoryId: txData.categoryId || 'food',
    accountId: Number(txData.accountId) || 1,
    note: txData.note || '',
    timestamp: txData.timestamp || Date.now()
  };

  await putItem('transactions', tx);

  // Update account balance
  const account = state.accounts.find((a) => a.id == tx.accountId);
  if (account) {
    if (tx.type === 'EXPENSE') account.currentBalance -= tx.amount;
    else account.currentBalance += tx.amount;
    await putItem('accounts', account);
  }

  await loadData();
  renderApp();
}

async function deleteTransaction(txId) {
  const tx = state.transactions.find((t) => t.id === txId);
  if (!tx) return;

  // Revert balance
  const account = state.accounts.find((a) => a.id == tx.accountId);
  if (account) {
    if (tx.type === 'EXPENSE') account.currentBalance += tx.amount;
    else account.currentBalance -= tx.amount;
    await putItem('accounts', account);
  }

  await deleteItem('transactions', txId);
  await loadData();
  renderApp();
}

// ----------------- OCR RECEIPT SCAN WORKFLOW -----------------
let ocrTarget = 'split'; // 'split', 'trip', or 'personal'
let currentOcrBase64 = null;

// Quét hóa đơn trực tiếp trong modal Thêm Giao Dịch
async function handleScanReceiptForTransaction(e) {
  const file = e.target.files[0];
  if (!file) return;

  const loadingEl = document.getElementById('tx-ocr-loading');
  const previewBox = document.getElementById('tx-bill-preview-box');
  const previewImg = document.getElementById('tx-bill-preview-img');
  const previewTitle = document.getElementById('tx-bill-preview-title');
  const previewSub = document.getElementById('tx-bill-preview-sub');

  if (loadingEl) loadingEl.classList.remove('hidden');
  if (previewBox) previewBox.classList.add('hidden');

  const reader = new FileReader();
  reader.onload = async (event) => {
    const base64 = event.target.result;
    try {
      const result = await GeminiManager.scanReceipt(base64);

      if (loadingEl) loadingEl.classList.add('hidden');

      // Điền số tiền
      if (result.totalAmount > 0) {
        const amtInput = document.getElementById('tx-amount');
        if (amtInput) {
          amtInput.value = result.totalAmount;
          previewAmount(result.totalAmount);
        }
      }

      // Chọn danh mục thông minh dựa trên kết quả OCR
      if (result.category) {
        const catNameLower = result.category.toLowerCase();
        const matched = state.categories.find((c) =>
          c.type === currentModalType &&
          (c.name.toLowerCase().includes(catNameLower) || catNameLower.includes(c.name.toLowerCase()))
        );
        if (matched) {
          selectCat(matched.id);
        }
      }

      // Điền ghi chú
      const noteInput = document.getElementById('tx-note');
      if (noteInput) {
        const summary = result.items.length > 0 ? result.items.map((i) => i.name).slice(0, 3).join(', ') : '';
        if (result.storeName && summary) {
          noteInput.value = `${result.storeName} (${summary})`;
        } else if (result.storeName) {
          noteInput.value = result.storeName;
        } else if (summary) {
          noteInput.value = summary;
        }
      }

      // Hiển thị preview ảnh hóa đơn
      if (previewBox && previewImg) {
        previewImg.src = base64;
        if (previewTitle) previewTitle.innerText = result.storeName || 'Hóa đơn đã bóc tách';
        if (previewSub) previewSub.innerText = `Tổng: ${formatMoney(result.totalAmount)} (${result.items.length} món)`;
        previewBox.classList.remove('hidden');
      }

      if (window.lucide) lucide.createIcons();
    } catch (err) {
      if (loadingEl) loadingEl.classList.add('hidden');
      alert('Lỗi quét hóa đơn: ' + err.message);
    }
  };
  reader.readAsDataURL(file);
}

function clearTxBillPreview() {
  const previewBox = document.getElementById('tx-bill-preview-box');
  const fileInput = document.getElementById('tx-bill-file-input');
  const camInput = document.getElementById('tx-bill-camera-input');
  if (previewBox) previewBox.classList.add('hidden');
  if (fileInput) fileInput.value = '';
  if (camInput) camInput.value = '';
}

function openOcrScanModal(target = 'split') {
  ocrTarget = target;
  currentOcrBase64 = null;
  state.scannedOcrResult = null;

  // Hiển thị màn hình chọn nguồn ảnh
  const srcView = document.getElementById('ocr-source-view');
  const loadView = document.getElementById('ocr-loading-view');
  const resView = document.getElementById('ocr-result-view');
  const btnTrip = document.getElementById('btn-ocr-trip');

  if (srcView) srcView.classList.remove('hidden');
  if (loadView) loadView.classList.add('hidden');
  if (resView) resView.classList.add('hidden');
  if (btnTrip) {
    if (target === 'trip' && state.activeTrip) {
      btnTrip.classList.remove('hidden');
    } else {
      btnTrip.classList.add('hidden');
    }
  }

  openModal('ocr-modal');
}

function resetOcrModal() {
  const srcView = document.getElementById('ocr-source-view');
  const loadView = document.getElementById('ocr-loading-view');
  const resView = document.getElementById('ocr-result-view');

  if (srcView) srcView.classList.remove('hidden');
  if (loadView) loadView.classList.add('hidden');
  if (resView) resView.classList.add('hidden');
}

async function handleOcrFileSelected(e) {
  const file = e.target.files[0];
  if (!file) return;

  const srcView = document.getElementById('ocr-source-view');
  const loadView = document.getElementById('ocr-loading-view');
  const resView = document.getElementById('ocr-result-view');
  const loadThumb = document.getElementById('ocr-loading-thumb');

  const reader = new FileReader();
  reader.onload = async (event) => {
    currentOcrBase64 = event.target.result;

    if (srcView) srcView.classList.add('hidden');
    if (loadView) loadView.classList.remove('hidden');
    if (resView) resView.classList.add('hidden');
    if (loadThumb) loadThumb.src = currentOcrBase64;

    try {
      const result = await GeminiManager.scanReceipt(currentOcrBase64);
      state.scannedOcrResult = { ...result, image: currentOcrBase64 };
      state.scannedOcrItems = result.items || [];

      if (loadView) loadView.classList.add('hidden');
      if (resView) resView.classList.remove('hidden');

      const resThumb = document.getElementById('ocr-result-thumb');
      const merchantEl = document.getElementById('ocr-merchant-name');
      const countEl = document.getElementById('ocr-items-count');
      const totalEl = document.getElementById('ocr-total-amount');
      const listContainer = document.getElementById('ocr-items-list');

      if (resThumb) resThumb.src = currentOcrBase64;
      if (merchantEl) merchantEl.innerText = result.storeName || 'Hóa đơn mua sắm / ăn uống';
      if (countEl) countEl.innerText = `${result.items.length} món nhận diện`;
      if (totalEl) totalEl.innerText = formatMoney(result.totalAmount);

      if (listContainer) {
        if (result.items.length === 0) {
          listContainer.innerHTML = '<p class="text-xs text-slate-400 text-center py-4">Không có danh sách món chi tiết, chỉ nhận diện được tổng tiền.</p>';
        } else {
          listContainer.innerHTML = result.items.map((it) => `
            <div class="flex items-center justify-between rounded-xl bg-slate-50 p-2.5 text-xs border border-slate-100">
              <div class="flex items-center space-x-1.5">
                <span class="font-bold text-slate-800">${it.name}</span>
                ${it.quantity > 1 ? `<span class="text-[10px] text-slate-500 font-semibold">(x${it.quantity})</span>` : ''}
              </div>
              <span class="font-extrabold text-emerald-600">${formatMoney(it.amount)}</span>
            </div>
          `).join('');
        }
      }

      if (window.lucide) lucide.createIcons();
    } catch (err) {
      if (loadView) loadView.classList.add('hidden');
      if (srcView) srcView.classList.remove('hidden');
      alert('Lỗi nhận diện hóa đơn: ' + err.message);
    }
  };
  reader.readAsDataURL(file);
}

function applyOcrToSplitBill() {
  const res = state.scannedOcrResult;
  if (!res || !res.items || res.items.length === 0) {
    if (res && res.totalAmount > 0) {
      // Thêm 1 item tổng
      state.splitBill.items.push({
        id: 'split_item_' + Date.now(),
        title: res.storeName || 'Hóa đơn tổng',
        amount: res.totalAmount,
        paidByMemberId: state.splitBill.members[0].id,
        splitAmongMemberIds: []
      });
    } else {
      alert('Không có dữ liệu món nào để đưa vào bảng chia tiền.');
      return;
    }
  } else {
    state.splitBill.items.push(...res.items.map((it) => ({
      id: it.id || ('item_' + Math.random().toString(36).substr(2, 9)),
      title: it.name,
      amount: it.amount,
      paidByMemberId: state.splitBill.members[0].id,
      splitAmongMemberIds: []
    })));
  }

  closeModal('ocr-modal');
  switchTab('split');
  alert(`⚡ Đã đưa các món trên hóa đơn vào bảng chia tiền!`);
}

async function applyOcrToPersonalTx() {
  const res = state.scannedOcrResult;
  if (!res || res.totalAmount <= 0) {
    alert('Không tìm thấy số tiền hợp lệ trên hóa đơn.');
    return;
  }

  const defaultCat = state.categories.find((c) => c.type === 'EXPENSE' && (c.id === 'food' || c.name.includes('Ăn uống'))) || state.categories[0];
  const summary = res.items.length > 0 ? res.items.map((i) => i.name).slice(0, 3).join(', ') : '';
  const note = res.storeName && summary ? `${res.storeName} (${summary})` : (res.storeName || summary || 'Hóa đơn quét AI');

  await addTransaction({
    amount: res.totalAmount,
    type: 'EXPENSE',
    categoryId: defaultCat ? defaultCat.id : 'food',
    accountId: state.accounts[0] ? state.accounts[0].id : 1,
    note: note,
    timestamp: Date.now()
  });

  closeModal('ocr-modal');
  switchTab('home');
  alert(`⚡ Đã lưu hóa đơn ${formatMoney(res.totalAmount)} vào mục Chi tiêu!`);
}

async function applyOcrToTrip() {
  const res = state.scannedOcrResult;
  if (!res || !state.activeTrip) return;
  const dayIndex = state.tripActiveDay !== 'ALL' ? Number(state.tripActiveDay) : 1;
  const members = getTripMembers(state.activeTrip);
  const payerId = members[0]?.id || 'mem_me';
  const sharerIds = members.map((m) => m.id);

  if (res.items && res.items.length > 0) {
    for (const item of res.items) {
      await putItem('tripExpenses', {
        id: 'tripexp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
        tripId: state.activeTrip.id,
        dayIndex,
        title: item.name,
        amount: item.amount,
        payerMemberId: payerId,
        sharerMemberIds: sharerIds,
        timestamp: Date.now()
      });
    }
  } else if (res.totalAmount > 0) {
    await putItem('tripExpenses', {
      id: 'tripexp_' + Date.now(),
      tripId: state.activeTrip.id,
      dayIndex,
      title: res.storeName || 'Hóa đơn quét AI',
      amount: res.totalAmount,
      payerMemberId: payerId,
      sharerMemberIds: sharerIds,
      timestamp: Date.now()
    });
  }

  await loadData();
  closeModal('ocr-modal');
  renderApp();
  alert(`⚡ Đã thêm các khoản chi từ hóa đơn vào chuyến đi "${state.activeTrip.title}"!`);
}

// ----------------- MODAL UTILITIES -----------------
function openModal(id) {
  const el = document.getElementById(id);
  if (el) {
    el.classList.remove('hidden');
    if (id === 'add-transaction-modal') {
      populateCategoryChips();
      populateAccountOptions();
      clearTxBillPreview();
      document.getElementById('tx-amount').value = '';
      document.getElementById('amount-preview').innerText = '';
      document.getElementById('tx-note').value = '';
    }
  }
  if (window.lucide) lucide.createIcons();
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('hidden');
}
