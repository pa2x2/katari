// WHATWG URL and URLSearchParams for script runtimes; QuickJS has neither.
import URL from 'core-js-pure/actual/url/index.js';
import URLSearchParams from 'core-js-pure/actual/url-search-params/index.js';

globalThis.URL = URL;
globalThis.URLSearchParams = URLSearchParams;
