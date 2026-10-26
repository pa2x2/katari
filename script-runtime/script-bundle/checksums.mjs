// Writes SHA256SUMS for the built files, in `sha256sum -c` format relative to this directory. The Gradle check
// checkVendoredScripts compares the committed files with it.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';

const outputs = ['../src/main/resources/mihon/script/runtime/web/url.js'];
const lines = outputs.map((path) => `${createHash('sha256').update(readFileSync(path)).digest('hex')}  ${path}`);
writeFileSync('SHA256SUMS', lines.join('\n') + '\n');
