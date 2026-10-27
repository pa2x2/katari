import { encode, decode } from 'urlencode';

globalThis.__lnreaderLibs = Object.assign(globalThis.__lnreaderLibs || {}, { urlencode: { encode, decode } });
