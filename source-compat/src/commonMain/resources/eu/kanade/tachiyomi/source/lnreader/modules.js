// LNReader's `require` table (pluginManager.ts `packages`, app dc394e85) with the same values. Libraries come from
// the bundled chunks (libs/): `core` is already loaded, the others are evaluated on the first `require` that needs
// them. Other LNReader scripts add their modules with __lnreaderModule. Unknown names give `undefined`, as LNReader.
(function () {
  var modules = Object.create(null);

  function hidden(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  // `get` runs on every require; a module that must stay one object across requires keeps it itself (once)
  hidden('__lnreaderModule', function (name, get) {
    modules[name] = get;
  });

  hidden('__lnreaderRequire', function (name) {
    var get = modules[name];
    return get ? get() : undefined;
  });

  function once(get) {
    var value;
    var done = false;
    return function () {
      if (!done) {
        value = get();
        done = true;
      }
      return value;
    };
  }

  function lib(chunk, name) {
    return once(function () {
      if (chunk !== 'core') __scriptLoad(chunk);
      return __lnreaderLibs[name];
    });
  }

  __lnreaderModule('htmlparser2', lib('core', 'htmlparser2'));
  __lnreaderModule('cheerio', lib('core', 'cheerio'));
  __lnreaderModule('dayjs', lib('core', 'dayjs'));
  __lnreaderModule('urlencode', lib('urlencode', 'urlencode'));
  __lnreaderModule('@libs/aes', lib('ciphers', 'aes'));
  __lnreaderModule('@libs/utils', lib('ciphers', 'utils'));

  var NovelStatus = {
    Unknown: 'Unknown',
    Ongoing: 'Ongoing',
    Completed: 'Completed',
    Licensed: 'Licensed',
    PublishingFinished: 'Publishing Finished',
    Cancelled: 'Cancelled',
    OnHiatus: 'On Hiatus',
    STUB: 'STUB',
    Inactive: 'Inactive',
  };
  var FilterTypes = {
    TextInput: 'Text',
    Picker: 'Picker',
    CheckboxGroup: 'Checkbox',
    Switch: 'Switch',
    ExcludableCheckboxGroup: 'XCheckbox',
  };

  function isUrlAbsolute(url) {
    if (url) {
      if (url.indexOf('//') === 0) return true;
      if (url.indexOf('://') === -1) return false;
      if (url.indexOf('.') === -1) return false;
      if (url.indexOf('/') === -1) return false;
      if (url.indexOf(':') > url.indexOf('/')) return false;
      if (url.indexOf('://') < url.indexOf('.')) return true;
    }
    return false;
  }

  __lnreaderModule('@libs/novelStatus', once(function () { return { NovelStatus: NovelStatus }; }));
  __lnreaderModule('@libs/filterInputs', once(function () { return { FilterTypes: FilterTypes }; }));
  __lnreaderModule('@libs/isAbsoluteUrl', once(function () { return { isUrlAbsolute: isUrlAbsolute }; }));
  __lnreaderModule('@libs/defaultCover', once(function () {
    return {
      defaultCover:
        'https://github.com/lnreader/lnreader-plugins/blob/master/public/static/coverNotAvailable.webp?raw=true',
    };
  }));
})();
