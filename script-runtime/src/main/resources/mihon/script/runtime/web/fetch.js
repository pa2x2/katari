// fetch, Response, Headers and FormData over the host's HTTP client (NetworkShims). Sized by what LNReader plugins
// were measured to reach: no Request, Blob, AbortSignal or streaming bodies; unknown init keys are ignored.
(function () {
  function define(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  function pairsOf(init) {
    if (init === undefined || init === null) return [];
    if (init instanceof Headers) return init._lines.slice();
    if (Array.isArray(init)) return init.map(function (pair) { return [String(pair[0]), String(pair[1])]; });
    return Object.keys(init).map(function (name) { return [name, String(init[name])]; });
  }

  // Header names compare case-insensitively; a Headers object keeps one combined line per name, as the standard does
  function Headers(init) {
    if (!(this instanceof Headers)) throw new TypeError("Class constructor Headers cannot be invoked without 'new'");
    this._lines = [];
    var self = this;
    pairsOf(init).forEach(function (pair) { self.append(pair[0], pair[1]); });
  }
  Headers.prototype._index = function (name) {
    var key = String(name).toLowerCase();
    for (var i = 0; i < this._lines.length; i++) if (this._lines[i][0] === key) return i;
    return -1;
  };
  Headers.prototype.append = function (name, value) {
    var i = this._index(name);
    if (i < 0) this._lines.push([String(name).toLowerCase(), String(value)]);
    else this._lines[i][1] += ', ' + String(value);
  };
  Headers.prototype.set = function (name, value) {
    var i = this._index(name);
    if (i < 0) this._lines.push([String(name).toLowerCase(), String(value)]);
    else this._lines[i][1] = String(value);
  };
  Headers.prototype.get = function (name) {
    var i = this._index(name);
    return i < 0 ? null : this._lines[i][1];
  };
  Headers.prototype.has = function (name) { return this._index(name) >= 0; };
  Headers.prototype['delete'] = function (name) {
    var i = this._index(name);
    if (i >= 0) this._lines.splice(i, 1);
  };
  Headers.prototype.forEach = function (callback, thisArg) {
    var self = this;
    this._lines.slice().sort(function (a, b) { return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0; })
      .forEach(function (line) { callback.call(thisArg, line[1], line[0], self); });
  };
  Headers.prototype.entries = function () {
    var lines = [];
    this.forEach(function (value, name) { lines.push([name, value]); });
    return lines[Symbol.iterator]();
  };
  Headers.prototype.keys = function () { return Array.from(this.entries(), function (l) { return l[0]; })[Symbol.iterator](); };
  Headers.prototype.values = function () { return Array.from(this.entries(), function (l) { return l[1]; })[Symbol.iterator](); };
  Headers.prototype[Symbol.iterator] = Headers.prototype.entries;
  define('Headers', Headers);

  function FormData() {
    if (!(this instanceof FormData)) throw new TypeError("Class constructor FormData cannot be invoked without 'new'");
    this._parts = [];
  }
  FormData.prototype.append = function (name, value) { this._parts.push([String(name), String(value)]); };
  FormData.prototype.set = function (name, value) {
    this['delete'](name);
    this.append(name, value);
  };
  FormData.prototype.get = function (name) {
    for (var i = 0; i < this._parts.length; i++) if (this._parts[i][0] === String(name)) return this._parts[i][1];
    return null;
  };
  FormData.prototype.getAll = function (name) {
    return this._parts.filter(function (p) { return p[0] === String(name); }).map(function (p) { return p[1]; });
  };
  FormData.prototype.has = function (name) { return this.get(name) !== null; };
  FormData.prototype['delete'] = function (name) {
    this._parts = this._parts.filter(function (p) { return p[0] !== String(name); });
  };
  FormData.prototype.forEach = function (callback, thisArg) {
    var self = this;
    this._parts.forEach(function (p) { callback.call(thisArg, p[1], p[0], self); });
  };
  FormData.prototype.entries = function () { return this._parts.map(function (p) { return [p[0], p[1]]; })[Symbol.iterator](); };
  FormData.prototype[Symbol.iterator] = FormData.prototype.entries;
  define('FormData', FormData);

  function Response(meta) {
    this._body = meta.body;
    this.status = meta.status;
    this.statusText = meta.statusText;
    this.ok = meta.status >= 200 && meta.status < 300;
    this.url = meta.url;
    this.redirected = meta.redirected;
    this.type = 'basic';
    this.headers = new Headers(meta.headers);
  }
  Response.prototype.text = function () {
    var id = this._body;
    return Promise.resolve().then(function () { return __hostBody(id, 'text'); });
  };
  Response.prototype.json = function () {
    return this.text().then(JSON.parse);
  };
  Response.prototype.arrayBuffer = function () {
    var id = this._body;
    return Promise.resolve().then(function () {
      var bytes = __hostBody(id, 'bytes');
      return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
    });
  };
  define('Response', Response);

  function base64Of(bytes) {
    var binary = '';
    for (var i = 0; i < bytes.length; i += 0x8000) {
      binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    }
    return btoa(binary);
  }

  function bodyOf(body) {
    if (body === undefined || body === null) return null;
    if (body instanceof FormData) return { kind: 'form', parts: body._parts };
    if (body instanceof URLSearchParams) {
      return { kind: 'text', text: String(body), type: 'application/x-www-form-urlencoded;charset=UTF-8' };
    }
    if (body instanceof ArrayBuffer) return { kind: 'bytes', base64: base64Of(new Uint8Array(body)) };
    if (ArrayBuffer.isView(body)) {
      return { kind: 'bytes', base64: base64Of(new Uint8Array(body.buffer, body.byteOffset, body.byteLength)) };
    }
    return { kind: 'text', text: String(body), type: 'text/plain;charset=UTF-8' };
  }

  define('fetch', function (input, init) {
    init = init || {};
    var request;
    try {
      var method = String(init.method || 'GET');
      if (/^(get|head|post|put|delete|options|patch)$/i.test(method)) method = method.toUpperCase();
      request = {
        url: new URL(String(input)).href,
        method: method,
        headers: pairsOf(init.headers),
        body: bodyOf(init.body),
      };
    } catch (e) {
      return Promise.reject(e);
    }
    return __hostFetch(JSON.stringify(request)).then(
      function (meta) { return new Response(JSON.parse(meta)); },
      function (e) { throw new TypeError('Network request failed: ' + (e && e.message ? e.message : String(e))); }
    );
  });
})();
