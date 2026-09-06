var SLOW_RESPONSE_MESSAGE = '一定時間アクセスがない場合はバックエンドの起動に時間がかかり、応答が遅くなることがあります。';

document.addEventListener('DOMContentLoaded', function () {
  var root = document.querySelector('[data-chatbot]');
  if (!root) {
    return;
  }

  var toggle = root.querySelector('[data-chatbot-toggle]');
  var closeButton = root.querySelector('[data-chatbot-close]');
  var panel = root.querySelector('[data-chatbot-panel]');
  var messages = root.querySelector('[data-chatbot-messages]');
  var form = root.querySelector('[data-chatbot-form]');
  var input = root.querySelector('[data-chatbot-input]');
  var sendButton = root.querySelector('[data-chatbot-send]');
  var status = root.querySelector('[data-chatbot-status]');
  var count = root.querySelector('[data-chatbot-count]');
  var apiBaseUrl = resolveChatApiBaseUrl();
  var isSending = false;
  var isOpen = false;
  var fallbackAnswer = '応答なし';
  var legacyUnanswerableMessage = 'このサイト内に記載がないため、お答えできません。';

  function setOpen(nextOpen) {
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
  }

  toggle.addEventListener('click', function () {
    setOpen(!isOpen);
  });

  if (closeButton) {
    closeButton.addEventListener('click', function () {
      setOpen(false);
    });
  }

  document.addEventListener('pointerdown', function (event) {
    if (isOpen && !isSending && !root.contains(event.target)) {
      setOpen(false);
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && isOpen) {
      setOpen(false);
    }
  });

  input.addEventListener('input', function () {
    updateCount(input, count);
  });

  input.addEventListener('keydown', function (event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (typeof form.requestSubmit === 'function') {
        form.requestSubmit();
      } else {
        form.dispatchEvent(new Event('submit', { cancelable: true }));
      }
    }
  });

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    var message = input.value.trim();

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

    // 5秒経過してもバックエンドから反応がない場合は起動遅延を案内する
    var waitTimer = setTimeout(function () {
      setStatus(status, SLOW_RESPONSE_MESSAGE, '');
    }, 5000);

    requestJson(apiBaseUrl + '/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message: message }),
    })
      .then(function (response) {
        return safeJson(response).then(function (data) {
          return {
            response: response,
            data: data,
          };
        });
      })
      .then(function (result) {
        clearTimeout(waitTimer);
        if (!result.response.ok) {
          setStatus(
            status,
            result.data && result.data.message ? result.data.message : '回答できませんでした。しばらくしてからもう一度お試しください。',
            'error'
          );
          input.value = message;
          updateCount(input, count);
          return;
        }

        appendMessage(messages, resolveAssistantAnswer(result.data, fallbackAnswer, legacyUnanswerableMessage), 'assistant');
        setStatus(status, '', '');
      })
      .catch(function () {
        clearTimeout(waitTimer);
        setStatus(status, '回答できませんでした。しばらくしてからもう一度お試しください。', 'error');
        input.value = message;
        updateCount(input, count);
      })
      .then(function () {
        isSending = false;
        setLoading(sendButton, false);
        input.focus();
      }, function () {
        isSending = false;
        setLoading(sendButton, false);
        input.focus();
      });
  });

  updateCount(input, count);
});

function resolveChatApiBaseUrl() {
  // When opened via file:// (local disk), prefer the local backend on localhost
  if (typeof window.location === 'object' && window.location.protocol === 'file:') {
    return 'http://localhost:8080';
  }

  var raw = window.API_BASE_URL || document.body.dataset.apiBaseUrl || 'https://shogun-sakura-demo.onrender.com';
  return raw.replace(/\/+$/, '');
}

function updateCount(input, count) {
  if (count) {
    count.textContent = input.value.length + ' / ' + input.maxLength;
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

  var element = document.createElement('p');
  element.className = 'chatbot-message chatbot-message--' + sender;
  element.textContent = message;
  messages.appendChild(element);
  messages.scrollTop = messages.scrollHeight;
}

function resolveAssistantAnswer(data, fallbackAnswer, legacyUnanswerableMessage) {
  var answer = data && typeof data.answer === 'string' ? data.answer.trim() : '';
  if (!answer || answer === legacyUnanswerableMessage) {
    return fallbackAnswer;
  }
  return answer;
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
