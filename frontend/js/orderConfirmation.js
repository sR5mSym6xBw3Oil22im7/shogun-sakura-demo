document.addEventListener('DOMContentLoaded', () => {
  if (!window.sessionStorage.getItem('pendingOrderQuantity')) {
    window.location.replace('index.html');
    return;
  }

  const button = document.querySelector('[data-confirm-order-button]');
  const statusEl = document.querySelector('[data-confirmation-status]');
  const lockTargets = Array.from(document.querySelectorAll('[data-confirmation-lock-target]'));
  const fieldMap = new Map(
    Array.from(document.querySelectorAll('[data-confirmation-field]')).map((element) => [
      element.dataset.confirmationField,
      element,
    ])
  );

  if (!button || !statusEl || fieldMap.size === 0) {
    return;
  }

  const apiBaseUrl = resolveApiBaseUrl();
  const quantity = getStoredQuantity();
  const confirmationData = buildConfirmationData(quantity);
  const originalButtonText = button.textContent;

  renderConfirmationData(fieldMap, confirmationData);

  button.addEventListener('click', async () => {
    lockConfirmationUi(lockTargets);
    button.disabled = true;
    button.textContent = '処理中...';
    setStatus(statusEl, '注文を送信しています...', 'pending');

    try {
      const response = await fetch(`${apiBaseUrl}/api/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(confirmationData),
      });

      const data = await safeJson(response);

      if (!response.ok) {
        const errorMessage =
          response.status === 404 || data?.message === 'Not found'
            ? '注文の送信に失敗しました。しばらくしてからもう一度お試しください。'
            : data?.message || '注文の送信に失敗しました。しばらくしてからもう一度お試しください。';
        setStatus(
          statusEl,
          errorMessage,
          'error'
        );
        button.disabled = false;
        button.textContent = originalButtonText;
        unlockConfirmationUi(lockTargets);
        return;
      }

      window.sessionStorage.removeItem('pendingOrderQuantity');
      window.sessionStorage.setItem('orderConfirmed', '1');
      window.location.href = 'orderConfirmed.html';
    } catch {
      button.disabled = false;
      button.textContent = originalButtonText;
      unlockConfirmationUi(lockTargets);
      setStatus(
        statusEl,
        '注文の送信に失敗しました。しばらくしてからもう一度お試しください。',
        'error'
      );
    }
  });
});

function resolveApiBaseUrl() {
  const raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || 'https://shogun-sakura-demo.onrender.com';
  return raw.replace(/\/+$/, '');
}

function getStoredQuantity() {
  const raw = window.sessionStorage.getItem('pendingOrderQuantity');
  const quantity = Number(raw);
  return Number.isInteger(quantity) && quantity >= 1 && quantity <= 9 ? quantity : 1;
}

function buildConfirmationData(quantity) {
  const emailLocalPart = randomAlphaNumeric(8);
  const emailDomainPart = randomAlphaNumeric(8);
  const emailTldPart = randomAlphaNumeric(8);

  return {
    name: `テスト氏名${randomDigits(8)}`,
    address: `テスト住所${randomDigits(8)}`,
    postalCode: `${randomDigits(3)}-${randomDigits(4)}`,
    email: `${emailLocalPart}@${emailDomainPart}.${emailTldPart}`,
    note: `テスト備考${randomDigits(8)}`,
    quantity,
  };
}

function renderConfirmationData(fieldMap, data) {
  fieldMap.forEach((element, key) => {
    if (key in data) {
      element.textContent = String(data[key]);
    }
  });
}

function randomDigits(length) {
  const values = new Uint8Array(length);
  if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
    window.crypto.getRandomValues(values);
  } else {
    for (let index = 0; index < length; index += 1) {
      values[index] = Math.floor(Math.random() * 10);
    }
    return Array.from(values).join('');
  }

  return Array.from(values, (value) => String(value % 10)).join('');
}

function randomAlphaNumeric(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  const values = new Uint8Array(length);

  if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
    window.crypto.getRandomValues(values);
  } else {
    for (let index = 0; index < length; index += 1) {
      values[index] = Math.floor(Math.random() * chars.length);
    }
    return Array.from(values, (value) => chars[value % chars.length]).join('');
  }

  return Array.from(values, (value) => chars[value % chars.length]).join('');
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
  targets.forEach((element) => {
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
  targets.forEach((element) => {
    if (element instanceof HTMLAnchorElement) {
      const originalHref = element.dataset.originalHref;
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

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
