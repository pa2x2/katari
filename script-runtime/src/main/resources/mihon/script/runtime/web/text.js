// TextDecoder, TextEncoder, atob and btoa, which QuickJS lacks. Decoding is done by the host with Java's charsets
// (LanguageShims, TextEncodings); encoding is UTF-8 only, as the standard defines it.
(function () {
  function define(name, value) {
    Object.defineProperty(globalThis, name, { value: value, writable: true, configurable: true, enumerable: false });
  }

  function bytesOf(input) {
    if (input === undefined) return new Uint8Array(0);
    if (input instanceof ArrayBuffer) return new Uint8Array(input);
    if (!ArrayBuffer.isView(input)) {
      throw new TypeError('The "input" argument must be an ArrayBuffer or an ArrayBufferView');
    }
    return input instanceof Uint8Array ? input : new Uint8Array(input.buffer, input.byteOffset, input.byteLength);
  }

  function TextDecoder(label, options) {
    if (!(this instanceof TextDecoder)) throw new TypeError("Class constructor TextDecoder cannot be invoked without 'new'");
    var requested = label === undefined ? 'utf-8' : String(label);
    var encoding = __hostEncodingName(requested);
    if (encoding === null) throw new RangeError('The "' + requested + '" encoding is not supported');
    Object.defineProperty(this, 'encoding', { value: encoding });
    Object.defineProperty(this, 'fatal', { value: !!(options && options.fatal) });
    Object.defineProperty(this, 'ignoreBOM', { value: !!(options && options.ignoreBOM) });
  }
  TextDecoder.prototype.decode = function (input) {
    return __hostDecode(bytesOf(input), this.encoding, this.fatal, this.ignoreBOM);
  };
  define('TextDecoder', TextDecoder);

  function TextEncoder() {
    if (!(this instanceof TextEncoder)) throw new TypeError("Class constructor TextEncoder cannot be invoked without 'new'");
  }
  Object.defineProperty(TextEncoder.prototype, 'encoding', { value: 'utf-8' });
  TextEncoder.prototype.encode = function (input) {
    var text = input === undefined ? '' : String(input);
    var bytes = [];
    for (var i = 0; i < text.length; i++) {
      var code = text.codePointAt(i);
      if (code > 0xffff) i++;
      else if (code >= 0xd800 && code <= 0xdfff) code = 0xfffd;
      if (code < 0x80) {
        bytes.push(code);
      } else if (code < 0x800) {
        bytes.push(0xc0 | (code >> 6), 0x80 | (code & 63));
      } else if (code < 0x10000) {
        bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 63), 0x80 | (code & 63));
      } else {
        bytes.push(0xf0 | (code >> 18), 0x80 | ((code >> 12) & 63), 0x80 | ((code >> 6) & 63), 0x80 | (code & 63));
      }
    }
    return new Uint8Array(bytes);
  };
  define('TextEncoder', TextEncoder);

  var ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

  define('btoa', function (input) {
    var text = String(input);
    var out = '';
    for (var i = 0; i < text.length; i += 3) {
      var a = text.charCodeAt(i), b = text.charCodeAt(i + 1), c = text.charCodeAt(i + 2);
      if (a > 255 || b > 255 || c > 255) throw new Error('Invalid character: btoa takes a Latin-1 string');
      var n = (a << 16) | ((b || 0) << 8) | (c || 0);
      out += ALPHABET[n >> 18] + ALPHABET[(n >> 12) & 63] +
        (i + 1 < text.length ? ALPHABET[(n >> 6) & 63] : '=') + (i + 2 < text.length ? ALPHABET[n & 63] : '=');
    }
    return out;
  });

  define('atob', function (input) {
    var text = String(input).replace(/[\t\n\f\r ]/g, '');
    if (text.length % 4 === 0) text = text.replace(/==?$/, '');
    if (text.length % 4 === 1 || /[^A-Za-z0-9+/]/.test(text)) throw new Error('Invalid character: not a base64 string');
    var out = '';
    var bits = 0, value = 0;
    for (var i = 0; i < text.length; i++) {
      value = (value << 6) | ALPHABET.indexOf(text[i]);
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        out += String.fromCharCode((value >> bits) & 255);
      }
    }
    return out;
  });
})();
