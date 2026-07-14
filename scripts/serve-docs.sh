#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
venv="$repo_root/.venv-docs"
requirements="$repo_root/requirements-docs.txt"
installed_requirements="$venv/.requirements-docs.txt"
entry_api_output="$repo_root/entry-source-api/build/dokka/html"
book_api_output="$repo_root/book-api/build/dokka/html"
sdk_api_docs="$repo_root/docs/developers/sdk/api"
sdk_doc_version="${SDK_DOC_VERSION:-development}"

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

echo "Generating Entry Source and Book API references ($sdk_doc_version)..."
"$repo_root/gradlew" --quiet \
    :entry-source-api:dokkaGeneratePublicationHtml \
    :book-api:dokkaGeneratePublicationHtml \
    -PsourceApiVersion="$sdk_doc_version"

rm -rf "$sdk_api_docs"
mkdir -p "$sdk_api_docs"
cp -R "$entry_api_output"/. "$sdk_api_docs"/
mkdir -p "$sdk_api_docs/book"
cp -R "$book_api_output"/. "$sdk_api_docs/book"/

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
