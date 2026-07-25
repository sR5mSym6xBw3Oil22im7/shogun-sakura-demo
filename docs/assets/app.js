
document.addEventListener('DOMContentLoaded', function () {
  var input = document.querySelector('[data-doc-search]');
  if (!input) return;
  var cards = Array.prototype.slice.call(document.querySelectorAll('[data-doc-card]'));
  input.addEventListener('input', function () {
    var q = input.value.trim().toLowerCase();
    cards.forEach(function (card) {
      var hit = !q || card.textContent.toLowerCase().indexOf(q) !== -1;
      card.classList.toggle('hidden', !hit);
    });
  });
});
