document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('[data-order-form]');
  if (!form) {
    return;
  }

  const emailInput = form.querySelector('#email');
  if (emailInput) {
    const prefix = emailInput.dataset.emailPrefix || 'yamanda@demo';
    emailInput.value = `${prefix}${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}.com`;
  }

  setupClearOnFocusFields(form);

  const submitButton = form.querySelector('[data-submit-button]');
  const statusEl = form.querySelector('[data-form-status]');
  const fieldErrorMap = new Map(
    Array.from(form.querySelectorAll('[data-error-for]')).map((element) => [element.dataset.errorFor, element])
  );
  const quantityInput = form.querySelector('[data-quantity-input]');
  const quantityWarningEl = form.querySelector('[data-quantity-warning]');
  const apiBaseUrl = resolveApiBaseUrl();

  if (quantityInput) {
    setupQuantityRestrictions(quantityInput, quantityWarningEl);
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    clearFeedback(form, fieldErrorMap, statusEl);

    const payload = readPayload(form);
    const validation = validatePayload(payload);

    if (!validation.isValid) {
      applyFieldErrors(form, fieldErrorMap, validation.fieldErrors);
      setStatus(statusEl, 'Please check the form fields.', 'error');
      focusFirstInvalidField(form, validation.fieldErrors);
      return;
    }

    window.sessionStorage.setItem('pendingOrderQuantity', String(validation.payload.quantity));
    window.location.href = 'orderConfirmation.html';
  });
});

function resolveApiBaseUrl() {
  const raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || 'https://shogun-sakura-demo.onrender.com';
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
    fieldErrors.name = 'Please enter your name.';
  } else if (normalized.name.length > 50) {
    fieldErrors.name = 'Name must be 50 characters or fewer.';
  }

  if (!normalized.email) {
    fieldErrors.email = 'Please enter your email address.';
  } else if (normalized.email.length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized.email)) {
    fieldErrors.email = 'Please enter a valid email address.';
  }

  if (normalized.postalCode && (!/^[0-9-]+$/.test(normalized.postalCode) || normalized.postalCode.length > 8)) {
    fieldErrors.postalCode = 'Postal code may contain only numbers and hyphens, up to 8 characters.';
  }

  if (!normalized.address) {
    fieldErrors.address = 'Please enter your address.';
  } else if (normalized.address.length > 200) {
    fieldErrors.address = 'Address must be 200 characters or fewer.';
  }

  if (normalized.quantity === '') {
    fieldErrors.quantity = 'Please enter the quantity.';
  } else {
    const quantity = Number(normalized.quantity);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 9) {
      fieldErrors.quantity = 'Quantity must be between 1 and 9.';
    } else {
      normalized.quantity = quantity;
    }
  }

  if (normalized.note && normalized.note.length > 200) {
    fieldErrors.note = 'Note must be 200 characters or fewer.';
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
  button.textContent = isLoading ? 'Sending...' : 'Place Order';
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

function setupClearOnFocusFields(form) {
  const fields = form.querySelectorAll('[data-clear-on-focus="true"]');

  fields.forEach((field) => {
    const originalValue = field.value;

    field.dataset.originalValue = originalValue;

    field.addEventListener('focus', () => {
      if (field.value !== originalValue) {
        return;
      }

      field.value = '';
      field.classList.remove('prefilled-value');
    });

    field.addEventListener('blur', () => {
      if (field.value.trim() !== '') {
        return;
      }

      field.value = field.dataset.originalValue || '';
      field.classList.add('prefilled-value');
    });
  });
}

function setupQuantityRestrictions(quantityInput, warningEl) {
  let lastValidValue = quantityInput.value || '1';
  let allowPointerChange = false;
  let allowKeyboardChange = false;

  const warningMessage = '数量は直接入力できません。矢印ボタンか上下の矢印キーで変更してください。';

  const showWarning = () => {
    if (warningEl) {
      warningEl.textContent = warningMessage;
    }
  };

  const clearWarning = () => {
    if (warningEl) {
      warningEl.textContent = '';
    }
  };

  const allowEdit = () => {
    if (allowPointerChange || allowKeyboardChange) {
      return true;
    }

    return false;
  };

  const resetFlags = () => {
    allowPointerChange = false;
    allowKeyboardChange = false;
  };

  quantityInput.addEventListener('pointerdown', () => {
    allowPointerChange = true;
  });

  quantityInput.addEventListener('pointerup', () => {
    allowPointerChange = false;
  });

  quantityInput.addEventListener('pointercancel', () => {
    allowPointerChange = false;
  });

  quantityInput.addEventListener('keydown', (event) => {
    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      allowKeyboardChange = true;
      return;
    }

    if (['Tab', 'Shift', 'Control', 'Alt', 'Meta', 'Escape', 'Home', 'End', 'Enter'].includes(event.key)) {
      return;
    }

    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('beforeinput', (event) => {
    if (allowEdit()) {
      return;
    }

    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('paste', (event) => {
    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('drop', (event) => {
    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener(
    'wheel',
    (event) => {
      event.preventDefault();
      showWarning();
    },
    { passive: false }
  );

  quantityInput.addEventListener('input', () => {
    if (allowEdit()) {
      lastValidValue = quantityInput.value;
      clearWarning();
      if (allowKeyboardChange) {
        allowKeyboardChange = false;
      }
      return;
    }

    quantityInput.value = lastValidValue;
    showWarning();
  });

  quantityInput.addEventListener('blur', () => {
    quantityInput.value = lastValidValue;
    clearWarning();
    resetFlags();
  });
}
