document.addEventListener('DOMContentLoaded', () => {
  const root = document.querySelector('[data-chatbot]');
  if (!root) {
    return;
  }

  const toggle = root.querySelector('[data-chatbot-toggle]');
  const closeButton = root.querySelector('[data-chatbot-close]');
  const panel = root.querySelector('[data-chatbot-panel]');
  const messages = root.querySelector('[data-chatbot-messages]');
  const form = root.querySelector('[data-chatbot-form]');
  const input = root.querySelector('[data-chatbot-input]');
  const sendButton = root.querySelector('[data-chatbot-send]');
  const status = root.querySelector('[data-chatbot-status]');
  const count = root.querySelector('[data-chatbot-count]');
  const apiBaseUrl = resolveChatApiBaseUrl();
  let isSending = false;
  let isOpen = false;
  const fallbackAnswer = 'このサイト内に記載がないため、お答えできません。';

  const setOpen = (nextOpen) => {
    isOpen = nextOpen;
    panel.hidden = !isOpen;
    root.classList.toggle('is-open', isOpen);
    toggle.setAttribute('aria-expanded', String(isOpen));
    toggle.setAttribute('aria-label', isOpen ? 'AI案内を閉じる' : 'AI案内を開く');
    if (isOpen) {
      input.focus();
    } else {
      toggle.focus();
    }
  };

  toggle.addEventListener('click', () => {
    setOpen(!isOpen);
  });

  closeButton?.addEventListener('click', () => {
    setOpen(false);
  });

  document.addEventListener('pointerdown', (event) => {
    if (isOpen && !isSending && !root.contains(event.target)) {
      setOpen(false);
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && isOpen) {
      setOpen(false);
    }
  });

  input.addEventListener('input', () => {
    updateCount(input, count);
  });

  input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const message = input.value.trim();

    if (isSending || !message) {
      setStatus(status, message ? '' : '質問を入力してください。', 'error');
      return;
    }

    isSending = true;
    setLoading(sendButton, true);
    setStatus(status, '送信中...', '');
    appendMessage(messages, message, 'user');
    input.value = '';
    updateCount(input, count);

    try {
      const response = await fetch(`${apiBaseUrl}/api/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message }),
      });
      const data = await safeJson(response);
      if (!response.ok) {
        setStatus(status, data?.message || '回答できませんでした。しばらくしてからもう一度お試しください。', 'error');
        input.value = message;
        updateCount(input, count);
        return;
      }
      appendMessage(messages, data?.answer || fallbackAnswer, 'assistant');
      setStatus(status, '', '');
    } catch {
      setStatus(status, '回答できませんでした。しばらくしてからもう一度お試しください。', 'error');
      input.value = message;
      updateCount(input, count);
    } finally {
      isSending = false;
      setLoading(sendButton, false);
      input.focus();
    }
  });

  updateCount(input, count);
});

function resolveChatApiBaseUrl() {
  const raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || 'https://shogun-sakura-demo.onrender.com';
  return raw.replace(/\/+$/, '');
}

function updateCount(input, count) {
  if (count) {
    count.textContent = `${input.value.length} / ${input.maxLength}`;
  }
}

function setLoading(button, isLoading) {
  button.disabled = isLoading;
  button.textContent = isLoading ? '送信中' : '送信';
}

function setStatus(status, message, tone) {
  if (!status) {
    return;
  }

  status.textContent = message;
  status.hidden = !message;
  status.classList.toggle('is-error', tone === 'error');
}

function appendMessage(messages, message, sender) {
  if (!messages) {
    return;
  }

  const element = document.createElement('p');
  element.className = `chatbot-message chatbot-message--${sender}`;
  element.textContent = message;
  messages.appendChild(element);
  messages.scrollTop = messages.scrollHeight;
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
