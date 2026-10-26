# Script runtime bundle

The JavaScript script runtimes need from npm: `URL` and `URLSearchParams` (core-js-pure). The built file is committed
in `../src/main/resources/mihon/script/runtime/web/`; Gradle never runs npm. `SHA256SUMS` records it, and
`checkVendoredScripts` (part of `spotlessCheck`) fails when the committed file doesn't match.

Rebuild after changing a version in `package.json`:

```sh
npm install --ignore-scripts   # updates package-lock.json
npm run build                  # writes the bundle and SHA256SUMS
```

`npm ci --ignore-scripts && npm run build` reproduces the committed file byte for byte (Node 24.18.0, npm 11.16.0).
