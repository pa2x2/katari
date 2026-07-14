#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
entry_api_output="$repo_root/entry-source-api/build/dokka/html"
book_api_output="$repo_root/book-api/build/dokka/html"
sdk_api_docs="$repo_root/docs/public/developers/sdk/api"
sdk_doc_version="${SDK_DOC_VERSION:-development}"
pnpm_version="10.20.0"

if ! command -v node >/dev/null 2>&1; then
    echo "Node.js 22 or newer is required to preview the documentation." >&2
    exit 1
fi

node_major="$(node -p 'Number(process.versions.node.split(".")[0])')"
if (( node_major < 22 )); then
    echo "Node.js 22 or newer is required; found $(node --version)." >&2
    exit 1
fi

if command -v pnpm >/dev/null 2>&1; then
    pnpm_command=(pnpm)
elif command -v corepack >/dev/null 2>&1; then
    pnpm_command=(corepack pnpm)
elif command -v bun >/dev/null 2>&1; then
    echo "pnpm is not installed; bootstrapping pnpm $pnpm_version with Bun..."
    pnpm_command=(bun x "pnpm@$pnpm_version")
else
    echo "Install pnpm $pnpm_version, enable Corepack, or install Bun to preview the documentation." >&2
    exit 1
fi

echo "Installing documentation dependencies..."
cd "$repo_root"
"${pnpm_command[@]}" install --frozen-lockfile --prefer-offline

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

has_host=false
has_port=false
for argument in "$@"; do
    case "$argument" in
        --host|--host=*) has_host=true ;;
        --port|--port=*) has_port=true ;;
    esac
done

arguments=(dev docs)
if [[ "$has_host" == false ]]; then
    arguments+=(--host "${VITEPRESS_HOST:-0.0.0.0}")
fi
if [[ "$has_port" == false ]]; then
    arguments+=(--port "${VITEPRESS_PORT:-8000}")
fi
arguments+=("$@")

echo "Starting Katari documentation; VitePress will print the local URL."
exec "${pnpm_command[@]}" exec vitepress "${arguments[@]}"
