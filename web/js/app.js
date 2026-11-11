/**
 * Main Application Controller (State & UI Interactivity)
 */
let state = {
  currentTab: 'home',
  transactions: [],
  categories: [],
  accounts: [],
  investments: [],
  trips: [],
  monthlyBudget: 0,
  activeTrip: null,
  splitBill: {
    eventTitle: 'Bữa ăn nhóm cuối tuần',
    members: [
      { id: 'mem_1', name: 'Tôi', isCurrentUser: true },
      { id: 'mem_2', name: 'Tuấn', isCurrentUser: false },
      { id: 'mem_3', name: 'Linh', isCurrentUser: false }
    ],
    items: [],
    result: null
  },
  filter: {
    type: 'ALL', // ALL, EXPENSE, INCOME
    accountId: null,
    search: ''
  }
};

let chartInstance = null;

// Initialize App
document.addEventListener('DOMContentLoaded', async () => {
  await openDB();
  await loadData();
  renderApp();
  initSpeechRecognition();

  // Register Service Worker for PWA
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').then(() => {
      console.log('PWA Service Worker registered');
    }).catch((err) => {
      console.warn('SW registration failed:', err);
    });
  }
});

async function loadData() {
  state.transactions = (await getAll('transactions')).sort((a, b) => b.timestamp - a.timestamp);
  state.categories = await getAll('categories');
  state.accounts = await getAll('accounts');
  state.investments = await getAll('investments');
  state.trips = await getAll('trips');
  state.monthlyBudget = Number(await getSetting('monthly_budget', 0)) || 0;
}

function switchTab(tabName) {
  state.currentTab = tabName;
  renderApp();
}

// Render Master UI
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

function updateBottomNav() {
  const tabs = ['home', 'stats', 'ai', 'split', 'trips', 'settings'];
  tabs.forEach((tab) => {
    const btn = document.getElementById(`nav-${tab}`);
    if (btn) {
      if (state.currentTab === tab || (tab === 'trips' && state.activeTrip)) {
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
            <p class="mt-1 text-xs text-emerald-100">Quỹ dự phòng: <span class="font-bold text-white">${formatMoney(reserveBalance)}</span></p>
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
        <button onclick="openOcrScanModal()" class="flex flex-col items-center justify-center rounded-2xl bg-purple-50 p-3 text-purple-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-600 text-white shadow-sm">
            <i data-lucide="scan-line" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Quét Bill</span>
        </button>

        <button onclick="switchTab('split')" class="flex flex-col items-center justify-center rounded-2xl bg-blue-50 p-3 text-blue-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm">
            <i data-lucide="users" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Chia tiền</span>
        </button>

        <button onclick="switchTab('trips')" class="flex flex-col items-center justify-center rounded-2xl bg-amber-50 p-3 text-amber-700 transition active:scale-95">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-600 text-white shadow-sm">
            <i data-lucide="luggage" class="h-5 w-5"></i>
          </div>
          <span class="mt-1.5 text-[11px] font-bold">Chuyến đi</span>
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

// ----------------- STATS SCREEN -----------------
function renderStatsScreen() {
  return `
    <div class="space-y-4 pb-24">
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
        <h2 class="text-sm font-bold text-slate-800">Biểu đồ cơ cấu chi tiêu tháng</h2>
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
  const ctx = document.getElementById('categoryChart');
  if (!ctx) return;

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

// ----------------- AI ASSISTANT SCREEN -----------------
let chatMessages = [
  { sender: 'ai', text: 'Xin chào! Tôi là Trợ lý AI Tài chính. Bạn có thể gõ nội dung thu chi, bấm nút micro để nói tiếng Việt, hoặc gửi ảnh hóa đơn/bill chuyển khoản để tôi tự động ghi nhận nhé!' }
];
let isAiLoading = false;

function renderAiScreen() {
  return `
    <div class="flex flex-col h-[calc(100vh-140px)]">
      <!-- Chat history -->
      <div id="chat-messages" class="flex-1 space-y-3 overflow-y-auto p-2">
        ${chatMessages.map((msg) => `
          <div class="flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}">
            <div class="max-w-[85%] rounded-3xl px-4 py-2.5 text-xs leading-relaxed ${
              msg.sender === 'user' ? 'bg-emerald-600 text-white rounded-br-sm' : 'bg-white border border-slate-100 text-slate-800 shadow-sm rounded-bl-sm'
            }">
              ${msg.image ? `<img src="${msg.image}" class="mb-2 max-h-48 rounded-xl object-cover" />` : ''}
              <p>${msg.text}</p>
            </div>
          </div>
        `).join('')}
        ${isAiLoading ? `
          <div class="flex justify-start">
            <div class="flex items-center space-x-1.5 rounded-full bg-slate-100 px-4 py-2 text-xs text-slate-500">
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500"></span>
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500" style="animation-delay: 0.2s"></span>
              <span class="inline-block h-2 w-2 animate-bounce rounded-full bg-emerald-500" style="animation-delay: 0.4s"></span>
              <span class="ml-1">AI đang suy nghĩ...</span>
            </div>
          </div>
        ` : ''}
      </div>

      <!-- Chat input bar -->
      <div class="mt-2 rounded-3xl border border-slate-100 bg-white p-2 shadow-sm">
        <form onsubmit="handleSendAiMessage(event)" class="flex items-center space-x-1.5">
          <label class="flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-slate-50 text-slate-500 hover:bg-slate-100">
            <i data-lucide="image" class="h-4 w-4"></i>
            <input type="file" accept="image/*" class="hidden" onchange="handleAiImageSelect(event)" />
          </label>

          <input id="ai-input-text" type="text" placeholder="Nhắn hoặc bấm micro để nói..." class="flex-1 bg-transparent px-2 text-xs outline-none text-slate-800 placeholder-slate-400" />

          <button type="button" onclick="startVoiceInput()" id="mic-btn" class="flex h-9 w-9 items-center justify-center rounded-full bg-blue-50 text-blue-600 transition active:scale-95">
            <i data-lucide="mic" class="h-4 w-4"></i>
          </button>

          <button type="submit" class="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-600 text-white shadow-sm transition active:scale-95">
            <i data-lucide="send" class="h-4 w-4"></i>
          </button>
        </form>
      </div>
    </div>
  `;
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
  if (!text || isAiLoading) return;

  chatMessages.push({ sender: 'user', text });
  input.value = '';
  isAiLoading = true;
  renderApp();

  try {
    const result = await GeminiManager.chatWithAi(text);
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

      chatMessages.push({ sender: 'ai', text: ` Đã tự động tạo giao dịch: ${cat.name} (${formatMoney(act.amount)}) vào ví ${acc.name}!` });
    }
  } catch (err) {
    chatMessages.push({ sender: 'ai', text: `❌ ${err.message}` });
  } finally {
    isAiLoading = false;
    renderApp();
  }
}

function handleAiImageSelect(e) {
  const file = e.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = async (event) => {
    const base64 = event.target.result;
    chatMessages.push({ sender: 'user', text: 'Phân tích hóa đơn này giúp tôi:', image: base64 });
    isAiLoading = true;
    renderApp();

    try {
      const result = await GeminiManager.chatWithAi('Phân tích chi tiết hóa đơn/giao dịch này và cho tôi biết số tiền, bên nhận.', base64);
      chatMessages.push({ sender: 'ai', text: result.text });
    } catch (err) {
      chatMessages.push({ sender: 'ai', text: `❌ ${err.message}` });
    } finally {
      isAiLoading = false;
      renderApp();
    }
  };
  reader.readAsDataURL(file);
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
          <button onclick="promptAddMember()" class="text-xs font-bold text-emerald-600">+ Thêm bạn</button>
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
            <button onclick="openOcrScanModal()" class="rounded-full bg-purple-50 px-2.5 py-1 text-xs font-bold text-purple-700">⚡ Quét Bill AI</button>
            <button onclick="promptAddSplitItem()" class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">+ Thêm món</button>
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

function promptAddMember() {
  const name = prompt('Nhập tên thành viên mới:');
  if (name && name.trim()) {
    state.splitBill.members.push({
      id: 'mem_' + Date.now(),
      name: name.trim(),
      isCurrentUser: false
    });
    renderApp();
  }
}

function removeMember(id) {
  state.splitBill.members = state.splitBill.members.filter((m) => m.id !== id);
  renderApp();
}

function promptAddSplitItem() {
  const title = prompt('Tên khoản chi (ví dụ: Nước ngọt, Tiền lẩu):');
  if (!title) return;
  const amountStr = prompt('Số tiền (hỗ trợ 50k, 1.5tr):');
  const amount = parseMoneyInput(amountStr);
  if (amount <= 0) return;

  state.splitBill.items.push({
    id: 'item_' + Date.now(),
    title,
    amount,
    paidByMemberId: state.splitBill.members[0].id,
    splitAmongMemberIds: []
  });
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
        <button onclick="promptCreateTrip()" class="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white shadow">
          + Tạo chuyến đi
        </button>
      </div>

      ${state.trips.length === 0 ? `
        <div class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center">
          <i data-lucide="luggage" class="mx-auto h-8 w-8 text-slate-300"></i>
          <p class="mt-2 text-xs font-semibold text-slate-500">Chưa có chuyến đi nào</p>
          <button onclick="promptCreateTrip()" class="mt-3 rounded-full bg-emerald-600 px-4 py-1.5 text-xs font-bold text-white">Tạo ngay</button>
        </div>
      ` : `
        <div class="space-y-3">
          ${state.trips.map((trip) => `
            <div onclick="openTripDetail('${trip.id}')" class="cursor-pointer rounded-3xl border border-slate-100 bg-white p-4 shadow-sm transition hover:shadow">
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="text-sm font-bold text-slate-800">${trip.title}</h3>
                  <p class="text-xs text-slate-400">${trip.destination || 'Chưa đặt điểm đến'} • ${trip.totalDays} ngày</p>
                </div>
                <span class="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">Xem chi tiết →</span>
              </div>
            </div>
          `).join('')}
        </div>
      `}
    </div>
  `;
}

function openTripDetail(tripId) {
  state.activeTrip = state.trips.find((t) => t.id === tripId);
  renderApp();
}

function renderTripDetailScreen() {
  const trip = state.activeTrip;
  if (!trip) return renderTripsScreen();

  return `
    <div class="space-y-4 pb-24">
      <div class="flex items-center justify-between">
        <button onclick="state.activeTrip = null; renderApp();" class="text-xs font-bold text-slate-500">← Quay lại danh sách</button>
        <button onclick="deleteTrip('${trip.id}')" class="text-xs text-rose-500 font-semibold">Xóa chuyến đi</button>
      </div>

      <div class="rounded-3xl bg-gradient-to-br from-teal-600 to-emerald-800 p-5 text-white shadow-lg">
        <h2 class="text-xl font-black">${trip.title}</h2>
        <p class="text-xs text-emerald-200">${trip.destination} • ${trip.totalDays} ngày</p>
      </div>

      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
        <h3 class="text-sm font-bold text-slate-800">Tính năng chia tiền chuyến đi với AI</h3>
        <p class="mt-1 text-xs text-slate-500">Bạn có thể chuyển dữ liệu chuyến đi này sang phần Chia tiền để AI tự động tính nợ.</p>
        <button onclick="syncTripToSplitBill('${trip.id}')" class="mt-3 w-full rounded-2xl bg-indigo-600 py-2.5 text-xs font-bold text-white shadow">
          ⚡ Đưa sang Chia tiền AI
        </button>
      </div>
    </div>
  `;
}

async function promptCreateTrip() {
  const title = prompt('Tên chuyến đi (ví dụ: Du lịch Đà Lạt 3N2Đ):');
  if (!title) return;
  const destination = prompt('Điểm đến (ví dụ: Đà Lạt):') || '';
  const days = parseInt(prompt('Số ngày diễn ra (mặc định 3):') || '3', 10) || 3;

  const newTrip = {
    id: 'trip_' + Date.now(),
    title,
    destination,
    totalDays: days,
    createdAt: Date.now()
  };

  await putItem('trips', newTrip);
  await loadData();
  renderApp();
}

async function deleteTrip(tripId) {
  if (confirm('Bạn có chắc muốn xóa chuyến đi này?')) {
    await deleteItem('trips', tripId);
    state.activeTrip = null;
    await loadData();
    renderApp();
  }
}

function syncTripToSplitBill(tripId) {
  const trip = state.trips.find((t) => t.id === tripId);
  if (!trip) return;
  state.splitBill.eventTitle = trip.title;
  switchTab('split');
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
        <button onclick="promptAddInvestment()" class="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white shadow">
          + Thêm khoản
        </button>
      </div>

      ${state.investments.length === 0 ? `
        <div class="rounded-3xl border border-dashed border-slate-200 bg-white p-8 text-center text-xs text-slate-400">
          Chưa có khoản đầu tư nào (Vàng, Cổ phiếu, Quỹ, Crypto).
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
                </div>
              </div>
            `;
          }).join('')}
        </div>
      `}
    </div>
  `;
}

async function promptAddInvestment() {
  const name = prompt('Tên khoản đầu tư (ví dụ: Vàng nhẫn 9999, Cổ phiếu FPT):');
  if (!name) return;
  const investedStr = prompt('Số vốn ban đầu (ví dụ: 10tr):');
  const amountInvested = parseMoneyInput(investedStr);
  const currentStr = prompt('Giá trị thị trường hiện tại (ví dụ: 12tr):');
  const currentValue = parseMoneyInput(currentStr);

  const inv = {
    id: 'inv_' + Date.now(),
    name,
    amountInvested,
    currentValue,
    createdAt: Date.now()
  };

  await putItem('investments', inv);
  await loadData();
  renderApp();
}

// ----------------- SETTINGS SCREEN -----------------
async function renderSettingsScreen() {
  const currentApiKey = await GeminiManager.getApiKey();
  const currentModel = await GeminiManager.getSelectedModel();

  return `
    <div class="space-y-4 pb-24">
      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <h3 class="text-sm font-bold text-slate-800">Cấu hình Google Gemini AI</h3>
        <div>
          <label class="text-xs font-semibold text-slate-600">Gemini API Key</label>
          <div class="mt-1 flex space-x-2">
            <input id="setting-api-key" type="password" value="${currentApiKey}" placeholder="Dán AIzaSy..." class="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-xs outline-none focus:border-emerald-500" />
            <button onclick="saveApiKeySetting()" class="rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow">Lưu</button>
          </div>
          <p class="mt-1 text-[11px] text-slate-400">Lấy key miễn phí tại <a href="https://aistudio.google.com" target="_blank" class="text-emerald-600 underline">aistudio.google.com</a></p>
        </div>

        <div class="pt-2">
          <label class="text-xs font-semibold text-slate-600">Mô hình AI mặc định</label>
          <select id="setting-model" onchange="saveModelSetting(this.value)" class="mt-1 w-full rounded-xl border border-slate-200 p-2 text-xs outline-none">
            ${GeminiManager.AVAILABLE_MODELS.map((m) => `
              <option value="${m.id}" ${m.id === currentModel ? 'selected' : ''}>${m.name}</option>
            `).join('')}
          </select>
        </div>
      </div>

      <div class="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm space-y-3">
        <h3 class="text-sm font-bold text-slate-800">Sao lưu & Quản lý dữ liệu</h3>
        <div class="grid grid-cols-2 gap-2">
          <button onclick="exportDataBackup()" class="rounded-xl bg-slate-100 py-2.5 text-xs font-bold text-slate-700">Sao lưu file JSON</button>
          <button onclick="resetAllData()" class="rounded-xl bg-rose-50 py-2.5 text-xs font-bold text-rose-600">Xóa hết dữ liệu</button>
        </div>
      </div>
    </div>
  `;
}

async function saveApiKeySetting() {
  const input = document.getElementById('setting-api-key');
  if (input) {
    await GeminiManager.setApiKey(input.value);
    alert('Đã lưu Gemini API Key thành công!');
  }
}

async function saveModelSetting(model) {
  await GeminiManager.setSelectedModel(model);
  alert('Đã chuyển mô hình AI sang: ' + model);
}

async function exportDataBackup() {
  const data = {
    transactions: state.transactions,
    accounts: state.accounts,
    categories: state.categories,
    investments: state.investments,
    trips: state.trips
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

// OCR Scan Receipt Modal
function openOcrScanModal() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.capture = 'environment'; // Triggers back camera on mobile
  input.onchange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      const base64 = event.target.result;
      alert('Đang gửi ảnh lên Gemini Vision để bóc tách hóa đơn... Vui lòng đợi trong giây lát!');
      try {
        const items = await GeminiManager.scanReceipt(base64);
        if (items.length === 0) {
          alert('Không nhận diện được món ăn hoặc số tiền từ ảnh này. Vui lòng thử chụp lại rõ nét hơn!');
          return;
        }

        // Add scanned items to split bill
        state.splitBill.items.push(...items.map((it) => ({
          id: it.id,
          title: it.name,
          amount: it.amount,
          paidByMemberId: state.splitBill.members[0].id,
          splitAmongMemberIds: []
        })));

        switchTab('split');
        alert(`⚡ Đã bóc tách thành công ${items.length} món từ hóa đơn!`);
      } catch (err) {
        alert('Lỗi quét hóa đơn: ' + err.message);
      }
    };
    reader.readAsDataURL(file);
  };
  input.click();
}

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('hidden');
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('hidden');
}
