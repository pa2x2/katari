// console, written to logcat at debug level by the host (LanguageShims).
(function () {
  function text(args) {
    return Array.prototype.map.call(args, function (value) {
      if (typeof value === 'string') return value;
      if (value instanceof Error) return value.stack ? value.name + ': ' + value.message + '\n' + value.stack : String(value);
      try {
        var json = JSON.stringify(value);
        return json === undefined ? String(value) : json;
      } catch (e) {
        return String(value);
      }
    }).join(' ');
  }

  var console = {};
  ['log', 'info', 'warn', 'error', 'debug'].forEach(function (level) {
    console[level] = function () {
      __hostLog(level, text(arguments));
    };
  });
  Object.defineProperty(globalThis, 'console', { value: console, writable: true, configurable: true, enumerable: false });
})();
