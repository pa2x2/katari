# LNReader library bundle

The libraries LNReader plugins `require`, at the versions LNReader's own lockfile resolves (app `dc394e85`, v2.1.4):
cheerio 1.0.0-rc.12, htmlparser2 12.0.0, dayjs 1.11.21, urlencode 2.0.0 (with buffer 5.7.1 and string_decoder 1.3.0
from LNReader's tree), protobufjs 8.7.1 and @noble/ciphers 2.2.0. `entries/` splits them into chunks:

- `core`: cheerio, htmlparser2 and dayjs, evaluated by every LNReader runtime.
- `urlencode`, `protobuf`, `ciphers`: evaluated on the first `require` that needs them.

The built chunks are committed in `../src/main/resources/eu/kanade/tachiyomi/source/lnreader/libs/`; Gradle never runs
npm. `SHA256SUMS` records them, and `checkVendoredScripts` (part of `spotlessCheck`) fails when a committed chunk
doesn't match. The build also rewrites the licence-screen entries of every bundled package
(`app/licences/*/lnreader-npm-*.json`).

Rebuild after changing a version in `package.json`:

```sh
npm install --ignore-scripts   # updates package-lock.json
npm run build                  # writes the chunks, SHA256SUMS and the licence entries
```

`npm ci --ignore-scripts && npm run build` reproduces the committed files byte for byte (Node 24.18.0, npm 11.16.0).
