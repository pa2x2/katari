// @libs/fetch: LNReader's helpers/fetch.ts (app dc394e85), ported line for line except where noted.
(function () {
  // Merges LNReader's default headers under the plugin's, mutating the plugin's init as LNReader does
  function makeInit(init) {
    var defaultHeaders = {
      'Connection': 'keep-alive',
      'Accept': '*/*',
      'Accept-Language': '*',
      'Sec-Fetch-Mode': 'cors',
      'Accept-Encoding': 'gzip, deflate',
      'Cache-Control': 'max-age=0',
      'User-Agent': __lnreaderUserAgent(),
    };
    if (init && init.headers) {
      if (init.headers instanceof Headers) {
        if (!init.headers.get('User-Agent') && defaultHeaders['User-Agent']) {
          init.headers.set('User-Agent', defaultHeaders['User-Agent']);
        }
      } else {
        init.headers = Object.assign({}, defaultHeaders, init.headers);
      }
    } else {
      init = Object.assign({}, init, { headers: defaultHeaders });
    }
    return init;
  }

  async function fetchApi(url, init) {
    init = makeInit(init);
    return await fetch(url, init);
  }

  // LNReader reads the body through FileReader.readAsText(blob, encoding), which React Native decodes with the Java
  // charset of that name (UTF-8 without one) whatever the response's own charset
  async function fetchText(url, init, encoding) {
    init = makeInit(init);
    try {
      var res = await fetch(url, init);
      if (!res.ok) {
        throw new Error();
      }
      var bytes = new Uint8Array(await res.arrayBuffer());
      return __lnreaderDecodeText(bytes, encoding === undefined || encoding === null ? 'UTF-8' : String(encoding));
    } catch (e) {
      return '';
    }
  }

  var BYTE_MARK = BigInt((1 << 8) - 1);

  // gRPC-web framing as LNReader's. The response is read as bytes; LNReader goes through a data URL whose prefix it
  // assumes is application/octet-stream, which breaks its only user (WuxiaWorld) there
  async function fetchProto(protoInit, url, init) {
    __scriptLoad('protobuf');
    var protoRoot = __lnreaderLibs.protobuf.parse(protoInit.proto).root;
    var RequestMessge = protoRoot.lookupType(protoInit.requestType);
    if (RequestMessge.verify(protoInit.requestData)) {
      throw new Error('Invalid Proto');
    }
    var encodedrequest = RequestMessge.encode(protoInit.requestData).finish();
    var requestLength = BigInt(encodedrequest.length);
    var headers = new Uint8Array(
      Array(5)
        .fill(0)
        .map(function (v, idx) {
          if (idx === 0) {
            return 0;
          }
          return Number((requestLength >> BigInt(8 * (5 - idx - 1))) & BYTE_MARK);
        })
    );
    init = makeInit(init);
    var bodyArray = new Uint8Array(headers.length + encodedrequest.length);
    bodyArray.set(headers, 0);
    bodyArray.set(encodedrequest, headers.length);

    var response = await fetch(url, Object.assign({ method: 'POST' }, init, { body: bodyArray }));
    var payload = new Uint8Array(await response.arrayBuffer());
    var length = Number(
      BigInt(payload[1] << 24) | BigInt(payload[2] << 16) | BigInt(payload[3] << 8) | BigInt(payload[4])
    );
    var ResponseMessage = protoRoot.lookupType(protoInit.responseType);
    return ResponseMessage.decode(payload.slice(5, 5 + length));
  }

  var helpers = { fetchApi: fetchApi, fetchText: fetchText, fetchProto: fetchProto };
  __lnreaderModule('@libs/fetch', function () {
    return helpers;
  });
})();
