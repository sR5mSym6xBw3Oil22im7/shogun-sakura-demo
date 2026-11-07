const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const staticDir = path.join(__dirname, '..', '..', 'main', 'resources', 'static');

const htmlFiles = [
  'index.html',
  'orderConfirmation.html',
  'orderhistory.html',
];

test('pages opened from file:// point to the local backend on port 8080', () => {
  for (const relativePath of htmlFiles) {
    const fullPath = path.join(staticDir, relativePath);
    const html = fs.readFileSync(fullPath, 'utf8');

    assert.match(html, /http:\/\/localhost:8080/);
    assert.doesNotMatch(html, /http:\/\/localhost:8081/);
  }
});

test('pages served by the backend call the API on the same origin', () => {
  for (const relativePath of htmlFiles) {
    const html = fs.readFileSync(path.join(staticDir, relativePath), 'utf8');

    assert.match(html, /location\.origin/);
    assert.doesNotMatch(html, /shogun-sakura-demo\.onrender\.com/);
  }
});
