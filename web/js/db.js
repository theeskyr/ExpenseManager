/**
 * Database Layer using IndexedDB for 100% Offline Support
 */
const DB_NAME = 'ExpenseManagerDB';
const DB_VERSION = 1;

let dbInstance = null;

function openDB() {
  return new Promise((resolve, reject) => {
    if (dbInstance) return resolve(dbInstance);

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = event.target.result;

      // 1. Transactions
      if (!db.objectStoreNames.contains('transactions')) {
        const txStore = db.createObjectStore('transactions', { keyPath: 'id' });
        txStore.createIndex('timestamp', 'timestamp', { unique: false });
        txStore.createIndex('accountId', 'accountId', { unique: false });
        txStore.createIndex('type', 'type', { unique: false });
      }

      // 2. Categories
      if (!db.objectStoreNames.contains('categories')) {
        db.createObjectStore('categories', { keyPath: 'id' });
      }

      // 3. Accounts
      if (!db.objectStoreNames.contains('accounts')) {
        db.createObjectStore('accounts', { keyPath: 'id' });
      }

      // 4. Budgets
      if (!db.objectStoreNames.contains('budgets')) {
        db.createObjectStore('budgets', { keyPath: 'id' });
      }

      // 5. Investments
      if (!db.objectStoreNames.contains('investments')) {
        db.createObjectStore('investments', { keyPath: 'id' });
      }

      // 6. Trips
      if (!db.objectStoreNames.contains('trips')) {
        db.createObjectStore('trips', { keyPath: 'id' });
      }

      // 7. Trip Expenses
      if (!db.objectStoreNames.contains('tripExpenses')) {
        const tripExpStore = db.createObjectStore('tripExpenses', { keyPath: 'id' });
        tripExpStore.createIndex('tripId', 'tripId', { unique: false });
        tripExpStore.createIndex('dayIndex', 'dayIndex', { unique: false });
      }

      // 8. Chat Messages
      if (!db.objectStoreNames.contains('chatMessages')) {
        db.createObjectStore('chatMessages', { keyPath: 'id' });
      }

      // 9. Settings
      if (!db.objectStoreNames.contains('settings')) {
        db.createObjectStore('settings', { keyPath: 'key' });
      }
    };

    request.onsuccess = async (event) => {
      dbInstance = event.target.result;
      await seedInitialData(dbInstance);
      resolve(dbInstance);
    };

    request.onerror = (event) => {
      console.error('IndexedDB error:', event.target.error);
      reject(event.target.error);
    };
  });
}

async function seedInitialData(db) {
  // Check and seed accounts
  const accounts = await getAll('accounts');
  if (accounts.length === 0) {
    const defaultAccounts = [
      { id: 1, name: 'Tiền mặt', type: 'CASH', initialBalance: 0, currentBalance: 0, isReserve: false, colorHex: '#10B981', iconName: 'banknote' },
      { id: 2, name: 'Tài khoản ngân hàng', type: 'BANK', initialBalance: 0, currentBalance: 0, isReserve: false, colorHex: '#3B82F6', iconName: 'landmark' },
      { id: 3, name: 'Ví điện tử (MoMo / ZaloPay)', type: 'E_WALLET', initialBalance: 0, currentBalance: 0, isReserve: false, colorHex: '#EC4899', iconName: 'wallet' },
      { id: 4, name: 'Quỹ khẩn cấp / Dự phòng', type: 'RESERVE', initialBalance: 0, currentBalance: 0, isReserve: true, colorHex: '#8B5CF6', iconName: 'shield-check' }
    ];
    for (const acc of defaultAccounts) {
      await putItem('accounts', acc);
    }
  }

  // Check and seed categories
  const categories = await getAll('categories');
  if (categories.length === 0) {
    const defaultCategories = [
      { id: 'food', name: 'Ăn uống', type: 'EXPENSE', iconName: 'utensils', colorHex: '#F97316' },
      { id: 'shopping', name: 'Mua sắm', type: 'EXPENSE', iconName: 'shopping-bag', colorHex: '#EC4899' },
      { id: 'transport', name: 'Đi lại / Xăng xe', type: 'EXPENSE', iconName: 'car', colorHex: '#6366F1' },
      { id: 'bills', name: 'Hóa đơn / Tiện ích', type: 'EXPENSE', iconName: 'receipt', colorHex: '#F59E0B' },
      { id: 'housing', name: 'Nhà cửa / Thuê trọ', type: 'EXPENSE', iconName: 'home', colorHex: '#84CC16' },
      { id: 'entertainment', name: 'Giải trí / Giao lưu', type: 'EXPENSE', iconName: 'gamepad-2', colorHex: '#A855F7' },
      { id: 'health', name: 'Sức khỏe / Y tế', type: 'EXPENSE', iconName: 'heart-pulse', colorHex: '#14B8A6' },
      { id: 'education', name: 'Học tập / Phát triển', type: 'EXPENSE', iconName: 'graduation-cap', colorHex: '#06B6D4' },
      { id: 'other_expense', name: 'Chi phí khác', type: 'EXPENSE', iconName: 'more-horizontal', colorHex: '#64748B' },
      // Income
      { id: 'salary', name: 'Tiền lương', type: 'INCOME', iconName: 'badge-dollar-sign', colorHex: '#10B981' },
      { id: 'bonus', name: 'Thưởng & Hoa hồng', type: 'INCOME', iconName: 'gift', colorHex: '#84CC16' },
      { id: 'investment', name: 'Đầu tư / Lãi tiết kiệm', type: 'INCOME', iconName: 'trending-up', colorHex: '#0EA5E9' },
      { id: 'other_income', name: 'Thu nhập phụ / Khác', type: 'INCOME', iconName: 'piggy-bank', colorHex: '#F59E0B' }
    ];
    for (const cat of defaultCategories) {
      await putItem('categories', cat);
    }
  }

  // Seed default settings
  const modelSetting = await getSetting('gemini_model_name');
  if (!modelSetting) {
    await saveSetting('gemini_model_name', 'gemini-3.1-flash-lite');
  }
}

// Generic IndexedDB CRUD helpers
function getAll(storeName) {
  return new Promise((resolve, reject) => {
    openDB().then((db) => {
      const tx = db.transaction(storeName, 'readonly');
      const store = tx.objectStore(storeName);
      const request = store.getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    }).catch(reject);
  });
}

function getItem(storeName, key) {
  return new Promise((resolve, reject) => {
    openDB().then((db) => {
      const tx = db.transaction(storeName, 'readonly');
      const store = tx.objectStore(storeName);
      const request = store.get(key);
      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error);
    }).catch(reject);
  });
}

function putItem(storeName, item) {
  return new Promise((resolve, reject) => {
    openDB().then((db) => {
      const tx = db.transaction(storeName, 'readwrite');
      const store = tx.objectStore(storeName);
      const request = store.put(item);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    }).catch(reject);
  });
}

function deleteItem(storeName, key) {
  return new Promise((resolve, reject) => {
    openDB().then((db) => {
      const tx = db.transaction(storeName, 'readwrite');
      const store = tx.objectStore(storeName);
      const request = store.delete(key);
      request.onsuccess = () => resolve(true);
      request.onerror = () => reject(request.error);
    }).catch(reject);
  });
}

function clearStore(storeName) {
  return new Promise((resolve, reject) => {
    openDB().then((db) => {
      const tx = db.transaction(storeName, 'readwrite');
      const store = tx.objectStore(storeName);
      const request = store.clear();
      request.onsuccess = () => resolve(true);
      request.onerror = () => reject(request.error);
    }).catch(reject);
  });
}

// Settings helpers
async function getSetting(key, defaultValue = '') {
  const item = await getItem('settings', key);
  return item ? item.value : defaultValue;
}

async function saveSetting(key, value) {
  return putItem('settings', { key, value });
}
