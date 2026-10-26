// For @libs/fetch's fetchProto. protobufjs constructs a TextDecoder while it loads.
import { parse } from 'protobufjs';

globalThis.__lnreaderLibs = Object.assign(globalThis.__lnreaderLibs || {}, { protobuf: { parse } });
