// setTimeout and clearTimeout on the host's cancellable timers (LanguageShims). A callback that throws is reported as
// an unhandled rejection, as a browser reports it, instead of failing the call.
(function () {
  var nextId = 1;

  function define(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  define('setTimeout', function (callback, delay) {
    var id = nextId++;
    var args = Array.prototype.slice.call(arguments, 2);
    __hostTimer(id, Number(delay) || 0).then(function (fired) {
      if (fired && typeof callback === 'function') callback.apply(undefined, args);
    });
    return id;
  });

  define('clearTimeout', function (id) {
    if (typeof id === 'number') __hostTimerClear(id);
  });
})();
