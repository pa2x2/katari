import { mkdir, readdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const outputRoot = path.join(repoRoot, 'docs', '.vitepress', 'dist')

async function collectHtmlFiles(directory) {
  const files = []

  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const absolutePath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      if (absolutePath.startsWith(path.join(outputRoot, 'developers', 'sdk', 'api'))) continue
      files.push(...(await collectHtmlFiles(absolutePath)))
    } else if (entry.isFile() && entry.name.endsWith('.html')) {
      files.push(absolutePath)
    }
  }

  return files
}

function redirectDocument(target) {
  const escapedTarget = target.replaceAll('&', '&amp;').replaceAll('"', '&quot;')
  const serializedTarget = JSON.stringify(target)

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="refresh" content="0; url=${escapedTarget}">
    <meta name="robots" content="noindex">
    <link rel="canonical" href="${escapedTarget}">
    <title>Redirecting…</title>
    <script>location.replace(${serializedTarget} + location.search + location.hash)</script>
  </head>
  <body><a href="${escapedTarget}">Continue to the documentation</a></body>
</html>
`
}

for (const htmlFile of await collectHtmlFiles(outputRoot)) {
  if (path.basename(htmlFile) === 'index.html' || path.basename(htmlFile) === '404.html') continue

  const redirectDirectory = htmlFile.slice(0, -'.html'.length)
  await mkdir(redirectDirectory, { recursive: true })

  const cleanTarget = `../${path.basename(htmlFile, '.html')}`
  await writeFile(path.join(redirectDirectory, 'index.html'), redirectDocument(cleanTarget))
}
