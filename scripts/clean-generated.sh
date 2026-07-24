#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

if [[ ! -d "$repo_root" ]]; then
  echo "Unable to resolve repository root." >&2
  exit 1
fi

rm -rf "$repo_root/.tmp" "$repo_root/backend/target" "$repo_root/backend/.pytest_cache" "$repo_root/.pytest_cache"
find "$repo_root" -type d \( -name '__pycache__' \) -prune -exec rm -rf {} +
find "$repo_root" -type f \( -name '*.pyc' \) -delete

echo "Cleaned generated files under $repo_root"
