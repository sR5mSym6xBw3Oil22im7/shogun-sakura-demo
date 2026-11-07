document.addEventListener('DOMContentLoaded', function () {
  var form = document.querySelector('[data-order-form]');
  if (!form) {
    return;
  }

  var emailInput = form.querySelector('#email');
  if (emailInput) {
    var prefix = emailInput.dataset.emailPrefix || 'yamanda@demo';
    emailInput.value = prefix + leftPad(String(Math.floor(Math.random() * 10000)), 4, '0') + '.com';
  }

  setupClearOnFocusFields(form);

  var statusEl = form.querySelector('[data-form-status]');
  var fieldErrorMap = {};
  toArray(form.querySelectorAll('[data-error-for]')).forEach(function (element) {
    fieldErrorMap[element.dataset.errorFor] = element;
  });
  var quantityInput = form.querySelector('[data-quantity-input]');
  var quantityWarningEl = form.querySelector('[data-quantity-warning]');

  if (quantityInput) {
    setupQuantityRestrictions(quantityInput, quantityWarningEl);
  }

  form.addEventListener('submit', function (event) {
    event.preventDefault();

    clearFeedback(form, fieldErrorMap, statusEl);

    var payload = readPayload(form);
    var validation = validatePayload(payload);

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

function readPayload(form) {
  function getValue(name) {
    var element = form.elements.namedItem(name);
    return element ? element.value.trim() : '';
  }

  var postalCode = getValue('postalCode');
  var note = getValue('note');

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
  var fieldErrors = {};
  var normalized = {
    name: payload.name,
    email: payload.email,
    postalCode: payload.postalCode,
    address: payload.address,
    quantity: payload.quantity,
    note: payload.note,
  };

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
    var quantity = Number(normalized.quantity);
    if (!isInteger(quantity) || quantity < 1 || quantity > 9) {
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
    fieldErrors: fieldErrors,
    payload: normalized,
  };
}

function applyFieldErrors(form, fieldErrorMap, fieldErrors) {
  Object.keys(fieldErrors).forEach(function (name) {
    var message = fieldErrors[name];
    var input = form.elements.namedItem(name);
    var errorElement = fieldErrorMap[name];

    if (input) {
      input.setAttribute('aria-invalid', 'true');
    }
    if (errorElement) {
      errorElement.textContent = message;
    }
  });
}

function clearFeedback(form, fieldErrorMap, statusEl) {
  toArray(form.elements).forEach(function (element) {
    if (
      element instanceof HTMLInputElement ||
      element instanceof HTMLTextAreaElement ||
      element instanceof HTMLSelectElement
    ) {
      element.removeAttribute('aria-invalid');
    }
  });

  Object.keys(fieldErrorMap).forEach(function (name) {
    fieldErrorMap[name].textContent = '';
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

function focusFirstInvalidField(form, fieldErrors) {
  var firstFieldName = Object.keys(fieldErrors)[0];
  if (!firstFieldName) {
    return;
  }

  var field = form.elements.namedItem(firstFieldName);
  if (field && typeof field.focus === 'function') {
    field.focus();
  }
}

function setupClearOnFocusFields(form) {
  var fields = form.querySelectorAll('[data-clear-on-focus="true"]');

  toArray(fields).forEach(function (field) {
    var originalValue = field.value;

    field.dataset.originalValue = originalValue;

    field.addEventListener('focus', function () {
      if (field.value !== originalValue) {
        return;
      }

      field.value = '';
      field.classList.remove('prefilled-value');
    });

    field.addEventListener('blur', function () {
      if (field.value.trim() !== '') {
        return;
      }

      field.value = field.dataset.originalValue || '';
      field.classList.add('prefilled-value');
    });
  });
}

function setupQuantityRestrictions(quantityInput, warningEl) {
  var lastValidValue = quantityInput.value || '1';
  var allowPointerChange = false;
  var allowKeyboardChange = false;
  var warningMessage = '数量は直接入力できません。矢印ボタンか上下の矢印キーで変更してください。';

  function showWarning() {
    if (warningEl) {
      warningEl.textContent = warningMessage;
    }
  }

  function clearWarning() {
    if (warningEl) {
      warningEl.textContent = '';
    }
  }

  function allowEdit() {
    return allowPointerChange || allowKeyboardChange;
  }

  function resetFlags() {
    allowPointerChange = false;
    allowKeyboardChange = false;
  }

  quantityInput.addEventListener('pointerdown', function () {
    allowPointerChange = true;
  });

  quantityInput.addEventListener('pointerup', function () {
    allowPointerChange = false;
  });

  quantityInput.addEventListener('pointercancel', function () {
    allowPointerChange = false;
  });

  quantityInput.addEventListener('keydown', function (event) {
    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      allowKeyboardChange = true;
      return;
    }

    if (containsKey(['Tab', 'Shift', 'Control', 'Alt', 'Meta', 'Escape', 'Home', 'End', 'Enter'], event.key)) {
      return;
    }

    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('beforeinput', function (event) {
    if (allowEdit()) {
      return;
    }

    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('paste', function (event) {
    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener('drop', function (event) {
    event.preventDefault();
    showWarning();
  });

  quantityInput.addEventListener(
    'wheel',
    function (event) {
      event.preventDefault();
      showWarning();
    },
    false
  );

  quantityInput.addEventListener('input', function () {
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

  quantityInput.addEventListener('blur', function () {
    quantityInput.value = lastValidValue;
    clearWarning();
    resetFlags();
  });
}

function toArray(list) {
  return Array.prototype.slice.call(list);
}

function leftPad(value, length, fillChar) {
  var padded = value;
  while (padded.length < length) {
    padded = fillChar + padded;
  }
  return padded;
}

function isInteger(value) {
  return typeof value === 'number' && isFinite(value) && Math.floor(value) === value;
}

function containsKey(values, target) {
  var index;
  for (index = 0; index < values.length; index += 1) {
    if (values[index] === target) {
      return true;
    }
  }
  return false;
}
