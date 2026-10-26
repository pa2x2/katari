// Host glue every runtime evaluates first (QuickJsScriptRuntime).
//
// - __scriptCall(name, argsJson): calls a global function and settles it into JSON, so a script's error never
//   escapes as an engine exception. Ends the call's host work when it has its result (__hostCallEnded).
// - __scriptLoad(name): evaluates one of the spec's lazy scripts at global scope, once.
// - __scriptWrapAsync(names): an async host function gives CALL_ENDED once its call has ended; the script then
//   never sees it settle, as if the work were still running when the call returned.
(function () {
  var CALL_ENDED = '\u0000script-call-ended';
  var loaded = Object.create(null);

  function hidden(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  hidden('__scriptLoad', function (name) {
    if (loaded[name]) return;
    loaded[name] = true;
    (0, eval)(__hostScriptSource(name));
  });

  hidden('__scriptWrapAsync', function (names) {
    names.forEach(function (name) {
      var raw = globalThis[name];
      hidden(name, function () {
        return raw.apply(undefined, arguments).then(function (result) {
          return result === CALL_ENDED ? new Promise(function () {}) : result;
        });
      });
    });
  });

  hidden('__scriptCall', async function (name, argsJson) {
    try {
      var value = await globalThis[name].apply(undefined, JSON.parse(argsJson));
      return JSON.stringify({ ok: true, value: value === undefined ? null : value });
    } catch (e) {
      var error = e instanceof Error ? e : null;
      return JSON.stringify({
        ok: false,
        name: error ? String(error.name) : 'Error',
        message: error ? String(error.message) : String(e),
        stack: error && error.stack ? String(error.stack) : null,
      });
    } finally {
      __hostCallEnded();
    }
  });
})();
