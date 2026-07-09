document.addEventListener('DOMContentLoaded', () => {
  const historyBody = document.querySelector('[data-history-body]');
  const statusEl = document.querySelector('[data-history-status]');
  if (!historyBody || !statusEl) {
    return;
  }

  const apiBaseUrl = resolveApiBaseUrl();
  loadOrderHistory(apiBaseUrl, historyBody, statusEl);
});

async function loadOrderHistory(apiBaseUrl, historyBody, statusEl) {
  setStatus(statusEl, '注文履歴を取得しています...', 'pending');

  try {
    const response = await fetch(`${apiBaseUrl}/api/orders`, {
      headers: {
        Accept: 'application/json',
      },
    });

    const data = await safeJson(response);

    if (!response.ok) {
      setStatus(statusEl, data?.message || '注文履歴を取得できませんでした。', 'error');
      renderEmptyRow(historyBody, '注文履歴を取得できませんでした。');
      return;
    }

    const items = Array.isArray(data) ? data : [];
    renderHistoryRows(historyBody, items);
    setStatus(statusEl, items.length > 0 ? `${items.length} 件の注文履歴を表示しています。` : '注文履歴はまだありません。', 'success');
  } catch {
    setStatus(statusEl, '注文履歴を取得できませんでした。', 'error');
    renderEmptyRow(historyBody, '注文履歴を取得できませんでした。');
  }
}

function renderHistoryRows(historyBody, items) {
  historyBody.replaceChildren();

  if (items.length === 0) {
    renderEmptyRow(historyBody, '注文履歴はまだありません。');
    return;
  }

  const formatter = new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });

  items.forEach((item) => {
    const row = document.createElement('tr');

    appendCell(row, item.orderId);
    appendCell(row, item.productName || '');
    appendCell(row, item.customerName || '');
    appendCell(row, item.email || '');
    appendCell(row, item.postalCode || '');
    appendCell(row, item.address || '');
    appendCell(row, item.quantity);
    appendCell(row, `¥${Number(item.totalAmount || 0).toLocaleString('ja-JP')}`);
    appendCell(row, item.note || '');

    const createdAtText = item.createdAt ? formatter.format(new Date(item.createdAt)) : '';
    appendCell(row, createdAtText);

    historyBody.appendChild(row);
  });
}

function renderEmptyRow(historyBody, message) {
  historyBody.replaceChildren();
  const row = document.createElement('tr');
  const cell = document.createElement('td');
  cell.colSpan = 10;
  cell.className = 'history-empty';
  cell.textContent = message;
  row.appendChild(cell);
  historyBody.appendChild(row);
}

function appendCell(row, value) {
  const cell = document.createElement('td');
  cell.textContent = value == null ? '' : String(value);
  row.appendChild(cell);
}

function resolveApiBaseUrl() {
  const raw = window.API_BASE_URL || 'https://shogun-sakura-demo.onrender.com';
  return raw.replace(/\/+$/, '');
}

function setStatus(statusEl, message, tone) {
  statusEl.textContent = message;
  statusEl.classList.remove('is-success', 'is-error', 'is-pending');

  if (tone === 'success') {
    statusEl.classList.add('is-success');
  } else if (tone === 'error') {
    statusEl.classList.add('is-error');
  } else if (tone === 'pending') {
    statusEl.classList.add('is-pending');
  }
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
