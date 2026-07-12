#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
venv="$repo_root/.venv-docs"
requirements="$repo_root/requirements-docs.txt"
installed_requirements="$venv/.requirements-docs.txt"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Python 3 is required to preview the documentation." >&2
    exit 1
fi

if [[ -x "$venv/bin/python" ]] && ! "$venv/bin/python" -c 'import sys' >/dev/null 2>&1; then
    echo "Recreating stale documentation environment..."
    rm -rf "$venv"
fi

if [[ ! -x "$venv/bin/python" ]]; then
    echo "Creating documentation environment..."
    python3 -m venv "$venv"
fi

if [[ ! -f "$installed_requirements" ]] || ! cmp --silent "$requirements" "$installed_requirements"; then
    echo "Installing documentation dependencies..."
    "$venv/bin/python" -m pip install --disable-pip-version-check --quiet -r "$requirements"
    cp "$requirements" "$installed_requirements"
fi

has_address=false
for argument in "$@"; do
    if [[ "$argument" == "--dev-addr" || "$argument" == --dev-addr=* ]]; then
        has_address=true
        break
    fi
done

arguments=(serve)
if [[ "$has_address" == false ]]; then
    arguments+=(--dev-addr "${MKDOCS_DEV_ADDR:-0.0.0.0:8000}")
fi
arguments+=("$@")

cd "$repo_root"
exec "$venv/bin/python" -m mkdocs "${arguments[@]}"
