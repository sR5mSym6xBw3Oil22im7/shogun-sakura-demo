document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('[data-order-form]');
  if (!form) {
    return;
  }

  const submitButton = form.querySelector('[data-submit-button]');
  const statusEl = form.querySelector('[data-form-status]');
  const fieldErrorMap = new Map(
    Array.from(form.querySelectorAll('[data-error-for]')).map((element) => [element.dataset.errorFor, element])
  );
  const apiBaseUrl = resolveApiBaseUrl();

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    clearFeedback(form, fieldErrorMap, statusEl);

    const payload = readPayload(form);
    const validation = validatePayload(payload);

    if (!validation.isValid) {
      applyFieldErrors(form, fieldErrorMap, validation.fieldErrors);
      setStatus(statusEl, '入力内容を確認してください。', 'error');
      focusFirstInvalidField(form, validation.fieldErrors);
      return;
    }

    setLoadingState(submitButton, true);
    setStatus(statusEl, '注文デモを送信しています...', 'pending');

    try {
      const response = await fetch(`${apiBaseUrl}/api/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(validation.payload),
      });

      const data = await safeJson(response);

      if (!response.ok) {
        if (data && data.fieldErrors) {
          applyFieldErrors(form, fieldErrorMap, data.fieldErrors);
        }
        setStatus(
          statusEl,
          data?.message || '現在、注文デモを受け付けできません。時間をおいて再度お試しください。',
          'error'
        );
        return;
      }

      setStatus(statusEl, `注文デモを受け付けました。注文番号: ${data.orderId}`, 'success');
      form.reset();
      const quantityField = form.elements.quantity;
      if (quantityField) {
        quantityField.value = '1';
      }
    } catch {
      setStatus(statusEl, '現在、注文デモを受け付けできません。時間をおいて再度お試しください。', 'error');
    } finally {
      setLoadingState(submitButton, false);
    }
  });
});

function resolveApiBaseUrl() {
  const raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || 'http://localhost:8080';
  return raw.replace(/\/+$/, '');
}

function readPayload(form) {
  const getValue = (name) => {
    const element = form.elements.namedItem(name);
    return element ? element.value.trim() : '';
  };

  const postalCode = getValue('postalCode');
  const note = getValue('note');

  return {
    name: getValue('name'),
    email: getValue('email'),
    postalCode: postalCode ? postalCode : null,
    address: getValue('address'),
    quantity: getValue('quantity'),
    note: note ? note : null,
  };
}

function validatePayload(payload) {
  const fieldErrors = {};
  const normalized = { ...payload };

  if (!normalized.name) {
    fieldErrors.name = 'お名前を入力してください。';
  } else if (normalized.name.length > 50) {
    fieldErrors.name = 'お名前は50文字以内で入力してください。';
  }

  if (!normalized.email) {
    fieldErrors.email = 'メールアドレスを入力してください。';
  } else if (normalized.email.length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized.email)) {
    fieldErrors.email = 'メールアドレスを正しい形式で入力してください。';
  }

  if (normalized.postalCode && (!/^[0-9-]+$/.test(normalized.postalCode) || normalized.postalCode.length > 8)) {
    fieldErrors.postalCode = '郵便番号は数字とハイフンで入力してください。';
  }

  if (!normalized.address) {
    fieldErrors.address = '住所を入力してください。';
  } else if (normalized.address.length > 200) {
    fieldErrors.address = '住所は200文字以内で入力してください。';
  }

  if (normalized.quantity === '') {
    fieldErrors.quantity = '数量を入力してください。';
  } else {
    const quantity = Number(normalized.quantity);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 9) {
      fieldErrors.quantity = '数量は1〜9の範囲で入力してください。';
    } else {
      normalized.quantity = quantity;
    }
  }

  if (normalized.note && normalized.note.length > 200) {
    fieldErrors.note = '備考は200文字以内で入力してください。';
  }

  return {
    isValid: Object.keys(fieldErrors).length === 0,
    fieldErrors,
    payload: normalized,
  };
}

function applyFieldErrors(form, fieldErrorMap, fieldErrors) {
  Object.entries(fieldErrors).forEach(([name, message]) => {
    const input = form.elements.namedItem(name);
    const errorElement = fieldErrorMap.get(name);
    if (input) {
      input.setAttribute('aria-invalid', 'true');
    }
    if (errorElement) {
      errorElement.textContent = message;
    }
  });
}

function clearFeedback(form, fieldErrorMap, statusEl) {
  Array.from(form.elements).forEach((element) => {
    if (
      element instanceof HTMLInputElement ||
      element instanceof HTMLTextAreaElement ||
      element instanceof HTMLSelectElement
    ) {
      element.removeAttribute('aria-invalid');
    }
  });

  fieldErrorMap.forEach((element) => {
    element.textContent = '';
  });

  setStatus(statusEl, '', '');
}

function setStatus(statusEl, message, tone) {
  if (!statusEl) {
    return;
  }

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

function setLoadingState(button, isLoading) {
  if (!button) {
    return;
  }

  button.disabled = isLoading;
  button.textContent = isLoading ? '送信中...' : '注文デモを送信する';
}

function focusFirstInvalidField(form, fieldErrors) {
  const firstFieldName = Object.keys(fieldErrors)[0];
  if (!firstFieldName) {
    return;
  }

  const field = form.elements.namedItem(firstFieldName);
  if (field && typeof field.focus === 'function') {
    field.focus();
  }
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
