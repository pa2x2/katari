// @libs/storage: LNReader's helpers/storage.ts (app dc394e85) over the plugin's settings file (LnReaderStorage). Each
// key holds LNReader's envelope `{created, value, expires}` as JSON; `require` gives new instances every time, as
// LNReader's does.
(function () {
  function Storage() {}

  Storage.prototype.set = function (key, value, expires) {
    var item = {
      created: new Date(),
      value: value,
      expires: expires instanceof Date ? expires.getTime() : expires,
    };
    __lnreaderStorageSet(String(key), JSON.stringify(item));
  };

  Storage.prototype.get = function (key, raw) {
    var storedItem = __lnreaderStorageGet(String(key));
    if (storedItem) {
      var item = JSON.parse(storedItem);
      if (item.expires) {
        if (Date.now() > item.expires) {
          this['delete'](key);
          return undefined;
        }
        if (raw) {
          item.expires = new Date(item.expires).getTime();
        }
      }
      return raw ? item : item.value;
    }
    return undefined;
  };

  Storage.prototype['delete'] = function (key) {
    __lnreaderStorageDelete(String(key));
  };

  Storage.prototype.clearAll = function () {
    var self = this;
    this.getAllKeys().forEach(function (key) {
      self['delete'](key);
    });
  };

  Storage.prototype.getAllKeys = function () {
    return __lnreaderStorageKeys();
  };

  // What the site's own localStorage and sessionStorage held when the user last opened it in the WebView
  function siteStorage(kind) {
    return {
      get: function () {
        var data = __lnreaderSiteStorage(kind);
        return data ? JSON.parse(data) : undefined;
      },
    };
  }

  __lnreaderModule('@libs/storage', function () {
    return {
      storage: new Storage(),
      localStorage: siteStorage('local'),
      sessionStorage: siteStorage('session'),
    };
  });
})();
