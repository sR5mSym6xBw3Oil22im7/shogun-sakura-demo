const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const htmlFiles = [
  'frontend/index.html',
  'frontend/orderConfirmation.html',
  'frontend/orderhistory.html',
];

test('local frontend pages point to the local backend on port 8080', () => {
  for (const relativePath of htmlFiles) {
    const fullPath = path.join(__dirname, '..', '..', relativePath);
    const html = fs.readFileSync(fullPath, 'utf8');

    assert.match(html, /http:\/\/localhost:8080/);
    assert.doesNotMatch(html, /http:\/\/localhost:8081/);
  }
});
