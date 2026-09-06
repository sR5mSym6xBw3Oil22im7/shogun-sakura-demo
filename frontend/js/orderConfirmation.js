var SLOW_RESPONSE_MESSAGE = '一定時間アクセスがない場合はバックエンドの起動に時間がかかり、応答が遅くなることがあります。';

document.addEventListener('DOMContentLoaded', function () {
  if (!window.sessionStorage.getItem('pendingOrderQuantity')) {
    window.location.replace('index.html');
    return;
  }

  var button = document.querySelector('[data-confirm-order-button]');
  var statusEl = document.querySelector('[data-confirmation-status]');
  var lockTargets = toArray(document.querySelectorAll('[data-confirmation-lock-target]'));
  var fieldMap = {};
  toArray(document.querySelectorAll('[data-confirmation-field]')).forEach(function (element) {
    fieldMap[element.dataset.confirmationField] = element;
  });

  if (!button || !statusEl || Object.keys(fieldMap).length === 0) {
    return;
  }

  var apiBaseUrl = resolveApiBaseUrl();
  var quantity = getStoredQuantity();
  var confirmationData = buildConfirmationData(quantity);
  var originalButtonText = button.textContent;

  renderConfirmationData(fieldMap, confirmationData);

  button.addEventListener('click', function () {
    lockConfirmationUi(lockTargets);
    button.disabled = true;
    button.textContent = '処理中...';
    setStatus(statusEl, '注文を送信しています...', 'pending');

    // 5秒経過してもバックエンドから反応がない場合は起動遅延を案内する
    var waitTimer = setTimeout(function () {
      setStatus(statusEl, SLOW_RESPONSE_MESSAGE, 'pending');
    }, 5000);

    requestJson(apiBaseUrl + '/api/orders', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(confirmationData),
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
          var errorMessage = '注文の送信に失敗しました。しばらくしてからもう一度お試しください。';
          if (
            result.response.status !== 404 &&
            result.data &&
            result.data.message &&
            result.data.message !== 'Not found'
          ) {
            errorMessage = result.data.message;
          }

          if (!result.response.ok && result.data && result.data.fieldErrors && Object.keys(result.data.fieldErrors).length > 0) {
            var fieldMessages = Object.values(result.data.fieldErrors).filter(Boolean);
            if (fieldMessages.length > 0) {
              errorMessage = fieldMessages.join(' ');
            }
          }

          setStatus(statusEl, errorMessage, 'error');
          button.disabled = false;
          button.textContent = originalButtonText;
          unlockConfirmationUi(lockTargets);
          return;
        }

        window.sessionStorage.removeItem('pendingOrderQuantity');
        window.sessionStorage.setItem('orderConfirmed', '1');
        window.location.href = 'orderConfirmed.html';
      })
      .catch(function () {
        clearTimeout(waitTimer);
        button.disabled = false;
        button.textContent = originalButtonText;
        unlockConfirmationUi(lockTargets);
        setStatus(
          statusEl,
          '注文の送信に失敗しました。しばらくしてからもう一度お試しください。',
          'error'
        );
      });
  });
});

function resolveApiBaseUrl() {
  var raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || getDefaultApiBaseUrl();
  return raw.replace(/\/+$/, '');
}

function getDefaultApiBaseUrl() {
  if (isLocalEnvironment()) {
    return 'http://localhost:8080';
  }

  return 'https://shogun-sakura-demo.onrender.com';
}

function isLocalEnvironment() {
  return location.protocol === 'file:' ||
    location.hostname === 'localhost' ||
    location.hostname === '127.0.0.1' ||
    location.hostname === '[::1]' ||
    location.hostname === '';
}

function getStoredQuantity() {
  var raw = window.sessionStorage.getItem('pendingOrderQuantity');
  var quantity = Number(raw);
  return isInteger(quantity) && quantity >= 1 && quantity <= 9 ? quantity : 1;
}

function buildConfirmationData(quantity) {
  var emailLocalPart = randomAlphaNumeric(8);
  var emailDomainPart = randomAlphaNumeric(8);
  var emailTldPart = randomAlphaNumeric(8);

  return {
    name: 'テスト氏名' + randomDigits(8),
    address: 'テスト住所' + randomDigits(8),
    postalCode: randomDigits(3) + '-' + randomDigits(4),
    email: emailLocalPart + '@' + emailDomainPart + '.' + emailTldPart,
    note: 'テスト備考' + randomDigits(8),
    quantity: quantity,
  };
}

function renderConfirmationData(fieldMap, data) {
  Object.keys(fieldMap).forEach(function (key) {
    if (key in data) {
      fieldMap[key].textContent = String(data[key]);
    }
  });
}

function randomDigits(length) {
  var values = new Uint8Array(length);
  var index;

  if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
    window.crypto.getRandomValues(values);
  } else {
    for (index = 0; index < length; index += 1) {
      values[index] = Math.floor(Math.random() * 10);
    }
    return joinArray(values, function (value) {
      return String(value);
    });
  }

  return joinArray(values, function (value) {
    return String(value % 10);
  });
}

function randomAlphaNumeric(length) {
  var chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  var values = new Uint8Array(length);
  var index;

  if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
    window.crypto.getRandomValues(values);
  } else {
    for (index = 0; index < length; index += 1) {
      values[index] = Math.floor(Math.random() * chars.length);
    }
    return joinArray(values, function (value) {
      return chars[value % chars.length];
    });
  }

  return joinArray(values, function (value) {
    return chars[value % chars.length];
  });
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

function lockConfirmationUi(targets) {
  targets.forEach(function (element) {
    if (element instanceof HTMLAnchorElement) {
      element.dataset.originalHref = element.getAttribute('href') || '';
      element.removeAttribute('href');
      element.setAttribute('aria-disabled', 'true');
      element.tabIndex = -1;
      element.classList.add('is-disabled');
      return;
    }

    if (element instanceof HTMLButtonElement) {
      element.disabled = true;
    }
  });
}

function unlockConfirmationUi(targets) {
  targets.forEach(function (element) {
    if (element instanceof HTMLAnchorElement) {
      var originalHref = element.dataset.originalHref;
      if (originalHref) {
        element.setAttribute('href', originalHref);
      }
      delete element.dataset.originalHref;
      element.removeAttribute('aria-disabled');
      element.removeAttribute('tabindex');
      element.classList.remove('is-disabled');
      return;
    }

    if (element instanceof HTMLButtonElement) {
      element.disabled = false;
    }
  });
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

function toArray(list) {
  return Array.prototype.slice.call(list);
}

function isInteger(value) {
  return typeof value === 'number' && isFinite(value) && Math.floor(value) === value;
}

function joinArray(values, mapper) {
  var parts = [];
  var index;

  for (index = 0; index < values.length; index += 1) {
    parts.push(mapper(values[index], index));
  }

  return parts.join('');
}
