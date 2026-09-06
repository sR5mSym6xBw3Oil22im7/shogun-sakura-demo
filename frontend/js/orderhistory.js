var SLOW_RESPONSE_MESSAGE = '一定時間アクセスがない場合はバックエンドの起動に時間がかかり、応答が遅くなることがあります。';

document.addEventListener('DOMContentLoaded', function () {
  var historyBody = document.querySelector('[data-history-body]');
  var statusEl = document.querySelector('[data-history-status]');
  if (!historyBody || !statusEl) {
    return;
  }

  var apiBaseUrl = resolveApiBaseUrl();
  loadOrderHistory(apiBaseUrl, historyBody, statusEl);
});

function loadOrderHistory(apiBaseUrl, historyBody, statusEl) {
  setStatus(statusEl, '注文履歴を取得しています…', 'pending');

  // 5秒経過してもバックエンドから反応がない場合は起動遅延を案内する
  var waitTimer = setTimeout(function () {
    setStatus(statusEl, SLOW_RESPONSE_MESSAGE, 'pending');
  }, 5000);

  requestJson(apiBaseUrl + '/api/orders', {
    headers: {
      Accept: 'application/json',
    },
  })
    .then(function (response) {
      clearTimeout(waitTimer);
      return safeJson(response).then(function (data) {
        return {
          response: response,
          data: data,
        };
      });
    })
    .then(function (result) {
      if (!result.response.ok) {
        var message = result.data && result.data.message ? result.data.message : '注文履歴を取得できませんでした。';
        setStatus(statusEl, message, 'error');
        renderEmptyRow(historyBody, message);
        return;
      }

      var items = Array.isArray(result.data) ? result.data : [];
      renderHistoryRows(historyBody, items);
      setStatus(
        statusEl,
        items.length > 0 ? items.length + ' 件の注文履歴を表示しています。' : '注文履歴はまだありません。',
        'success'
      );
    })
    .catch(function () {
      clearTimeout(waitTimer);
      var fallbackMessage = '注文履歴を取得できませんでした。';
      setStatus(statusEl, fallbackMessage, 'error');
      renderEmptyRow(historyBody, fallbackMessage);
    });
}

function renderHistoryRows(historyBody, items) {
  clearChildren(historyBody);

  if (items.length === 0) {
    renderEmptyRow(historyBody, '注文履歴はまだありません。');
    return;
  }

  var formatter = new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });

  items.forEach(function (item) {
    var row = document.createElement('tr');

    appendCell(row, item.orderId);
    appendCell(row, item.productName || '');
    appendCell(row, item.customerName || '');
    appendCell(row, item.email || '');
    appendCell(row, item.postalCode || '');
    appendCell(row, item.address || '');
    appendCell(row, item.quantity);
    appendCell(row, '¥' + Number(item.totalAmount || 0).toLocaleString('ja-JP'));
    appendCell(row, item.note || '');

    var createdAtText = item.createdAt ? formatter.format(new Date(item.createdAt)) : '';
    appendCell(row, createdAtText);

    historyBody.appendChild(row);
  });
}

function renderEmptyRow(historyBody, message) {
  clearChildren(historyBody);
  var row = document.createElement('tr');
  var cell = document.createElement('td');
  cell.colSpan = 10;
  cell.className = 'history-empty';
  cell.textContent = message;
  row.appendChild(cell);
  historyBody.appendChild(row);
}

function appendCell(row, value) {
  var cell = document.createElement('td');
  cell.textContent = value == null ? '' : String(value);
  row.appendChild(cell);
}

function clearChildren(element) {
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

function resolveApiBaseUrl() {
  var raw = window.API_BASE_URL || 'https://shogun-sakura-demo.onrender.com';
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

function safeJson(response) {
  return response.json().catch(function () {
    return null;
  });
}

function requestJson(url, options) {
  if (typeof window.fetch === 'function') {
    return window.fetch(url, options);
  }

  return new Promise(function (resolve, reject) {
    var request = new XMLHttpRequest();
    var requestHeaders = options && options.headers ? options.headers : {};
    var method = options && options.method ? options.method : 'GET';
    request.open(method, url, true);

    Object.keys(requestHeaders).forEach(function (name) {
      request.setRequestHeader(name, requestHeaders[name]);
    });

    request.onload = function () {
      resolve(buildXhrResponse(request));
    };
    request.onerror = function () {
      reject(new Error('Network request failed.'));
    };
    request.send(options && options.body ? options.body : null);
  });
}

function buildXhrResponse(request) {
  return {
    ok: request.status >= 200 && request.status < 300,
    status: request.status,
    json: function () {
      return new Promise(function (resolve, reject) {
        try {
          resolve(request.responseText ? JSON.parse(request.responseText) : null);
        } catch (error) {
          reject(error);
        }
      });
    },
  };
}
