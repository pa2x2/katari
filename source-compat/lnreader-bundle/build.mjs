// Builds the LNReader library chunks into the module's resources, records them in SHA256SUMS (checked by Gradle's
// checkVendoredScripts) and writes a licence-screen entry for every npm package that ends up in a chunk.
import { build } from 'esbuild';
import { createHash } from 'node:crypto';
import { readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const CHUNKS = ['core', 'urlencode', 'protobuf', 'ciphers'];
const OUT_DIR = '../src/main/resources/eu/kanade/tachiyomi/source/lnreader/libs';
const LICENCES_DIR = '../../app/licences';
// Licence-screen files this script owns; it rewrites all of them on every build
const LICENCE_PREFIX = 'lnreader-npm-';

const inputs = new Set();
for (const chunk of CHUNKS) {
  const result = await build({
    entryPoints: [`entries/${chunk}.js`],
    outfile: `${OUT_DIR}/${chunk}.js`,
    bundle: true,
    format: 'iife',
    platform: 'browser',
    target: 'es2020',
    minify: true,
    legalComments: 'none',
    metafile: true,
    logLevel: 'warning',
  });
  Object.keys(result.metafile.inputs).forEach((input) => inputs.add(input));
}

const lines = CHUNKS.map((chunk) => {
  const path = `${OUT_DIR}/${chunk}.js`;
  return `${createHash('sha256').update(readFileSync(path)).digest('hex')}  ${path}`;
});
writeFileSync('SHA256SUMS', lines.join('\n') + '\n');

function packageDirOf(input) {
  const parts = input.split('/');
  const last = parts.lastIndexOf('node_modules');
  if (last < 0) return null;
  const nameParts = parts[last + 1].startsWith('@') ? 2 : 1;
  return parts.slice(0, last + 1 + nameParts).join('/');
}

// The package's own licence file, which carries its copyright line; null when it ships none
function licenceText(dir) {
  const file = readdirSync(dir).find((name) => /^(licen[cs]e|copying)(\.(md|txt))?$/i.test(name));
  return file ? readFileSync(join(dir, file), 'utf8').replace(/\r\n/g, '\n') : null;
}

function websiteOf(pkg) {
  if (pkg.homepage) return pkg.homepage;
  const repo = typeof pkg.repository === 'string' ? pkg.repository : pkg.repository?.url;
  if (!repo) return `https://www.npmjs.com/package/${pkg.name}`;
  return repo.replace(/^git\+/, '').replace(/^git:\/\//, 'https://').replace(/\.git$/, '').replace(/^github:/, 'https://github.com/');
}

for (const kind of ['libraries', 'licenses']) {
  readdirSync(join(LICENCES_DIR, kind))
    .filter((name) => name.startsWith(LICENCE_PREFIX))
    .forEach((name) => rmSync(join(LICENCES_DIR, kind, name)));
}
// One entry per package name; a package bundled at several versions lists them all, with each distinct licence text
const packages = new Map();
for (const input of inputs) {
  const dir = packageDirOf(input);
  if (!dir) continue;
  const pkg = JSON.parse(readFileSync(join(dir, 'package.json'), 'utf8'));
  const entry = packages.get(pkg.name) ?? { pkg, versions: new Set(), texts: new Set() };
  entry.versions.add(pkg.version);
  const text = licenceText(dir);
  if (text) entry.texts.add(text);
  packages.set(pkg.name, entry);
}
const json = (value) => JSON.stringify(value, null, 2) + '\n';
for (const [name, { pkg, versions, texts }] of [...packages].sort((a, b) => a[0].localeCompare(b[0]))) {
  const id = LICENCE_PREFIX + name.replace('@', '').replace('/', '-');
  // Without a file of its own, the package's SPDX id, which the licence screen knows
  if (texts.size) {
    writeFileSync(join(LICENCES_DIR, 'licenses', `${id}.json`), json({ hash: id, name: pkg.license, content: [...texts].join('\n\n') }));
  }
  writeFileSync(join(LICENCES_DIR, 'libraries', `${id}.json`), json({
    uniqueId: `npm:${name}`,
    artifactVersion: [...versions].sort((a, b) => a.localeCompare(b, 'en', { numeric: true })).join(', '),
    name,
    description: pkg.description ?? '',
    website: websiteOf(pkg),
    licenses: [texts.size ? id : pkg.license],
  }));
}
console.log(`${CHUNKS.length} chunks, ${packages.size} packages`);
