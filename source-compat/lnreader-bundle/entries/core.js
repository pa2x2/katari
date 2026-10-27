// Evaluated by every LNReader runtime: the libraries most plugins require.
import { load } from 'cheerio';
import { Parser } from 'htmlparser2';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import localeData from 'dayjs/plugin/localeData';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import relativeTime from 'dayjs/plugin/relativeTime';
import calendar from 'dayjs/plugin/calendar';
import duration from 'dayjs/plugin/duration';

// The plugins LNReader extends the dayjs it hands to plugins with (src/i18n/translations.ts), so `format('LLL')` gives
// a date and `dayjs(text, format)` parses by the format. The locale stays dayjs's English, whose dates Katari reads
// back, where LNReader sets the app's language.
dayjs.extend(customParseFormat);
dayjs.extend(localeData);
dayjs.extend(localizedFormat);
dayjs.extend(relativeTime);
dayjs.extend(calendar);
dayjs.extend(duration);

globalThis.__lnreaderLibs = Object.assign(globalThis.__lnreaderLibs || {}, {
  cheerio: { load },
  htmlparser2: { Parser },
  dayjs,
});
