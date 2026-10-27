// QuickJS has no Intl. Plugins reach only for Intl.DateTimeFormat().resolvedOptions().timeZone (RanobeLib sends it to
// its API), so this stand-in answers with the device's time zone and locale and formats dates as ISO text.
(function () {
  var device = null;

  function deviceLocale() {
    if (device === null) device = JSON.parse(__lnreaderDeviceLocale());
    return device;
  }

  function DateTimeFormat() {
    if (!(this instanceof DateTimeFormat)) return new DateTimeFormat();
  }
  DateTimeFormat.prototype.resolvedOptions = function () {
    var locale = deviceLocale();
    return { locale: locale.locale, calendar: 'gregory', numberingSystem: 'latn', timeZone: locale.timeZone };
  };
  DateTimeFormat.prototype.format = function (date) {
    return new Date(date === undefined ? Date.now() : date).toISOString();
  };

  Object.defineProperty(globalThis, 'Intl', {
    value: { DateTimeFormat: DateTimeFormat },
    writable: true,
    configurable: true,
    enumerable: false,
  });
})();
