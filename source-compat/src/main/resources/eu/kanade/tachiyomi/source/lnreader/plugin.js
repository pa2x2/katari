// Loads the plugin and answers the host's calls on it.
//
// - __lnreaderRegister(factory): the plugin file arrives wrapped as LNReader's loader wraps it,
//   `function (require, module) { const exports = module.exports = {}; <code>; return exports.default }`, so it runs
//   in sloppy mode with the same free names. Like LNReader, gives `imageRequestInit` a User-Agent when it has none.
// - __lnreaderDescribe(): the plugin's fields the host maps to a source, and the names of its methods.
// - __lnreaderInvoke(method, args): calls one of the plugin's methods with `this` the plugin.
(function () {
  var plugin = null;

  function hidden(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  hidden('__lnreaderRegister', function (factory) {
    var loaded = factory(__lnreaderRequire, {});
    if (!loaded || typeof loaded !== 'object') throw new TypeError('The plugin exports no plugin object');
    if (!loaded.imageRequestInit) {
      loaded.imageRequestInit = { headers: { 'User-Agent': __lnreaderUserAgent() } };
    } else {
      if (!loaded.imageRequestInit.headers) loaded.imageRequestInit.headers = {};
      var hasUserAgent = Object.keys(loaded.imageRequestInit.headers).some(function (name) {
        return name.toLowerCase() === 'user-agent';
      });
      if (!hasUserAgent) loaded.imageRequestInit.headers['User-Agent'] = __lnreaderUserAgent();
    }
    plugin = loaded;
  });

  function methodsOf(object) {
    var names = [];
    for (var o = object; o && o !== Object.prototype; o = Object.getPrototypeOf(o)) {
      Object.getOwnPropertyNames(o).forEach(function (name) {
        var descriptor = Object.getOwnPropertyDescriptor(o, name);
        if (name !== 'constructor' && typeof descriptor.value === 'function' && names.indexOf(name) < 0) {
          names.push(name);
        }
      });
    }
    return names.sort();
  }

  function requestInitOf(init) {
    var headers = init.headers instanceof Headers ? Object.fromEntries(init.headers.entries()) : init.headers;
    return { method: init.method, headers: headers, body: init.body };
  }

  hidden('__lnreaderDescribe', function () {
    return {
      id: plugin.id,
      name: plugin.name,
      site: plugin.site,
      version: plugin.version,
      icon: plugin.icon,
      filters: plugin.filters,
      pluginSettings: plugin.pluginSettings,
      imageRequestInit: requestInitOf(plugin.imageRequestInit),
      webStorageUtilized: plugin.webStorageUtilized,
      customJS: plugin.customJS,
      customCSS: plugin.customCSS,
      methods: methodsOf(plugin),
    };
  });

  hidden('__lnreaderInvoke', function (method, args) {
    if (typeof plugin[method] !== 'function') throw new TypeError('The plugin has no method ' + method);
    return plugin[method].apply(plugin, args);
  });
})();
