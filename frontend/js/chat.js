document.addEventListener('DOMContentLoaded', () => {
  const root = document.querySelector('[data-chatbot]');
  if (!root) {
    return;
  }

  const toggle = root.querySelector('[data-chatbot-toggle]');
  const panel = root.querySelector('[data-chatbot-panel]');
  const closeButton = root.querySelector('[data-chatbot-close]');
  const form = root.querySelector('[data-chatbot-form]');
  const input = root.querySelector('[data-chatbot-input]');
  const sendButton = root.querySelector('[data-chatbot-send]');
  const messages = root.querySelector('[data-chatbot-messages]');
  const status = root.querySelector('[data-chatbot-status]');
  const count = root.querySelector('[data-chatbot-count]');
  const apiBaseUrl = resolveChatApiBaseUrl();
  let isSending = false;

  const setOpen = (isOpen) => {
    panel.hidden = !isOpen;
    toggle.setAttribute('aria-expanded', String(isOpen));
    if (isOpen) {
      input.focus();
    } else {
      toggle.focus();
    }
  };

  toggle.addEventListener('click', () => {
    setOpen(panel.hidden);
  });

  closeButton.addEventListener('click', () => {
    setOpen(false);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !panel.hidden) {
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
        appendMessage(messages, data?.message || '回答できませんでした。しばらくしてからもう一度お試しください。', 'bot');
        setStatus(status, '', '');
        return;
      }
      appendMessage(messages, data?.answer || 'このサイト内に記載がないため、お答えできません。', 'bot');
      setStatus(status, '', '');
    } catch {
      appendMessage(messages, '回答できませんでした。しばらくしてからもう一度お試しください。', 'bot');
      setStatus(status, '', '');
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

function appendMessage(messages, text, sender) {
  const element = document.createElement('p');
  element.className = `chatbot-message chatbot-message--${sender}`;
  element.textContent = text;
  messages.appendChild(element);
  messages.scrollTop = messages.scrollHeight;
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
  status.textContent = message;
  status.classList.toggle('is-error', tone === 'error');
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
