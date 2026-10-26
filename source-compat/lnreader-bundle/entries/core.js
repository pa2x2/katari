// Evaluated by every LNReader runtime: the libraries most plugins require.
import { load } from 'cheerio';
import { Parser } from 'htmlparser2';
import dayjs from 'dayjs';

globalThis.__lnreaderLibs = Object.assign(globalThis.__lnreaderLibs || {}, {
  cheerio: { load },
  htmlparser2: { Parser },
  dayjs,
});
