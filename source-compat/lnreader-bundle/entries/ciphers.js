// @libs/aes and @libs/utils.
import { gcm } from '@noble/ciphers/aes.js';
import { utf8ToBytes, bytesToUtf8 } from '@noble/ciphers/utils.js';

globalThis.__lnreaderLibs = Object.assign(globalThis.__lnreaderLibs || {}, {
  aes: { gcm },
  utils: { utf8ToBytes, bytesToUtf8 },
});
